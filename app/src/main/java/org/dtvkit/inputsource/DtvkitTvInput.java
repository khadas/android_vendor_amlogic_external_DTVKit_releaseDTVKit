package org.dtvkit.inputsource;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.Surface;

import org.dtvkit.companionlibrary.model.Channel;
import org.dtvkit.companionlibrary.utils.TvContractUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DtvkitTvInput extends TvInputService {
    private static final String TAG = "DtvkitTvInput";

    private LongSparseArray<Channel> mChannels;
    private ContentResolver mContentResolver;
    private final ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onChannelsChanged();
        }
    };

    private DtvkitEpgTimer mEpgTimer = null;

    public DtvkitTvInput() {
        Log.i(TAG, "DtvkitTvInput");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        mEpgTimer = new DtvkitEpgTimer(getApplicationContext());
        mContentResolver = getContentResolver();
        mContentResolver.registerContentObserver(TvContract.Channels.CONTENT_URI, true, mContentObserver);
        onChannelsChanged();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        mEpgTimer.cancel();
        mEpgTimer = null;
        mContentResolver.unregisterContentObserver(mContentObserver);
    }

    @Override
    public final Session onCreateSession(String inputId) {
        Log.i(TAG, String.format("onCreateSession: %s", inputId));
        DtvkitTvInputSession session = new DtvkitTvInputSession(this, inputId);
        session.setOverlayViewEnabled(true);
        return session;
    }

    private void onChannelsChanged() {
        mChannels = TvContractUtils.buildChannelMap(mContentResolver,
                TvContract.buildInputId(new ComponentName(getApplicationContext(), DtvkitTvInput.class)));
    }

    private Channel getChannel(Uri channelUri) {
        return mChannels.get(ContentUris.parseId(channelUri));
    }

    private String getChannelInternalDvbUri(Channel channel) {
        try {
            return channel.getInternalProviderData().get("dvbUri").toString();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return "dvb://0000.0000.0000";
        }
    }

    private void playerSetVolume(int volume) {
        try {
            JSONArray args = new JSONArray();
            args.put(volume);
            DtvkitGlueClient.getInstance().request("Player.setVolume", args);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void playerSetSubtitlesOn(boolean on) {
        try {
            JSONArray args = new JSONArray();
            args.put(on);
            DtvkitGlueClient.getInstance().request("Player.setSubtitlesOn", args);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private boolean playerPlay(String dvbUri) {
        try {
            JSONArray args = new JSONArray();
            args.put(dvbUri);
            DtvkitGlueClient.getInstance().request("Player.play", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    private void playerStop() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.stop", args);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void playerSetRectangle(int x, int y, int width, int height) {
        try {
            JSONArray args = new JSONArray();
            args.put(x);
            args.put(y);
            args.put(width);
            args.put(height);
            DtvkitGlueClient.getInstance().request("Player.setRectangle", args);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    class DtvkitTvInputSession extends TvInputService.Session {
        private Channel mTunedChannel;

        private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
            @Override
            public void onSignal(String signal, JSONObject data) {
                if (signal.equals("PlayerStatusChanged")) {
                    String state = "off";
                    try {
                        state = data.getString("state");
                    } catch (JSONException ignore) {
                    }

                    switch (state) {
                        case "playing":
                            if (mTunedChannel.getServiceType().equals(TvContract.Channels.SERVICE_TYPE_AUDIO)) {
                                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY);
                            } else {
                                notifyVideoAvailable();
                            }
                            mEpgTimer.notifyDecodingStarted();
                            break;
                        case "blocked":
                            notifyContentBlocked(TvContentRating.createRating("com.android.tv", "DVB", "DVB_18"));
                            mEpgTimer.notifyDecodingStarted();
                            break;
                        case "badsignal":
                            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL);
                            mEpgTimer.notifyDecodingStopped();
                            break;
                        default:
                            mEpgTimer.notifyDecodingStopped();
                            break;
                    }
                }
            }
        };

        private TvInputManager mManager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);

        DtvkitTvInputSession(Context context, String inputId) {
            super(context);
            Log.i(TAG, "DtvkitTvInputSession");
            DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
        }

        @Override
        public void layoutSurface(int left, int top, int right, int bottom) {
            Log.i(TAG, String.format("layoutSurface %d %d %d %d", left, top, right, bottom));
            playerSetRectangle(left, top, right, bottom);
        }

        @Override
        public void onSetStreamVolume(float volume) {
            Log.i(TAG, String.format("onStreamSetVolume %f", volume));
            playerSetVolume((int) (volume * 100));
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            Log.i(TAG, "onSetSurface");
            Platform platform = new Platform();
            return platform.setSurface(surface);
        }

        @Override
        public boolean onTune(Uri channelUri) {
            Log.i(TAG, String.format("onTune %s", channelUri.toString()));
            mTunedChannel = getChannel(channelUri);
            final String dvbUri = getChannelInternalDvbUri(mTunedChannel);
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);

            if (playerPlay(dvbUri)) {
            } else {
                mTunedChannel = null;
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
            }
            return (mTunedChannel != null);
        }

        @Override
        public void onUnblockContent(TvContentRating unblockedRating) {
            super.onUnblockContent(unblockedRating);
            Log.i(TAG, "onUnblockContent");
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            Log.i(TAG, "onSetCaptionEnabled");
            playerSetSubtitlesOn(enabled);
        }

        @Override
        public void onRelease() {
            Log.i(TAG, "onRelease");
            DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
            playerStop();
        }
    }
}
