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
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.Surface;
import android.view.View;

import org.dtvkit.companionlibrary.model.Channel;
import org.dtvkit.companionlibrary.utils.TvContractUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DtvkitTvInput extends TvInputService {
    private static final String TAG = "DtvkitTvInput";
    private LongSparseArray<Channel> mChannels;
    private ContentResolver mContentResolver;
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
        Log.i(TAG, "onCreateSession " + inputId);
        return new DtvkitTvInputSession(this);
    }

    // We do not indicate recording capabilities. TODO for recording.
    //public TvInputService.RecordingSession onCreateRecordingSession(String inputId)

    class DtvkitTvInputSession extends TvInputService.Session {
        private static final String TAG = "DtvkitTvInputSession";
        private Channel mTunedChannel;

        DtvkitTvInputSession(Context context) {
            super(context);
            Log.i(TAG, "DtvkitTvInputSession");
            DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
            setOverlayViewEnabled(false);
        }

        public void onRelease() {
            Log.i(TAG, "onRelease");
            playerStop();
            DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            Log.i(TAG, "onSetSurface " + surface);
            Platform platform = new Platform();
            return platform.setSurface(surface);
        }

        @Override
        public void onSurfaceChanged(int format, int width, int height) {
            Log.i(TAG, "onSurfaceChanged " + format + ", " + width + ", " + height);
        }

        // We do not enable the overlay. TODO for MHEG, TeleText etc.
        //public View onCreateOverlayView()

        @Override
        public void onOverlayViewSizeChanged(int width, int height) {
            Log.i(TAG, "onOverlayViewSizeChanged " + width + ", " + height);
        }

        @Override
        public boolean onTune(Uri channelUri) {
            Log.i(TAG, "onTune " + channelUri);
            mTunedChannel = getChannel(channelUri);
            final String dvbUri = getChannelInternalDvbUri(mTunedChannel);
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
            if (playerPlay(dvbUri)) {
            } else {
                mTunedChannel = null;
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
            }
            // TODO? notifyContentAllowed()
            return mTunedChannel != null;
        }

        // For private app params. Default calls onTune
        //public boolean onTune(Uri channelUri, Bundle params)

        @Override
        public void onSetStreamVolume(float volume) {
            Log.i(TAG, "onSetStreamVolume " + volume);
            playerSetVolume((int) (volume * 100));
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            Log.i(TAG, "onSetCaptionEnabled " + enabled);
            // TODO CaptioningManager.getLocale()
            playerSetSubtitlesOn(enabled);
        }

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            Log.i(TAG, "onSelectTrack " + type + ", " + trackId);
            // TODO notifyTrackSelected(int type, String trackId)
            return false;
        }

        @Override
        public void onUnblockContent(TvContentRating unblockedRating) {
            super.onUnblockContent(unblockedRating);
            Log.i(TAG, "onUnblockContent " + unblockedRating);
        }

        @Override
        public void onAppPrivateCommand(String action, Bundle data) {
            Log.i(TAG, "onAppPrivateCommand " + action + ", " + data);
        }

        //public void onTimeShiftPlay(Uri recordedProgramUri)
        //public void onTimeShiftPause()
        //public void onTimeShiftResume()
        //public void onTimeShiftSeekTo(long timeMs)
        //public void onTimeShiftSetPlaybackParams(PlaybackParams params)
        //public long onTimeShiftGetStartPosition()
        //public long onTimeShiftGetCurrentPosition()

        private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
            @Override
            public void onSignal(String signal, JSONObject data) {
                // TODO notifyChannelRetuned(Uri channelUri)
                // TODO notifyTracksChanged(List<TvTrackInfo> tracks)
                // TODO notifyTimeShiftStatusChanged(int status)
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

                            // TODO Cache and check if different
                            notifyTracksChanged(playerGetTracks());

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

    private List<TvTrackInfo> playerGetTracks() {
        List<TvTrackInfo> tracks = new ArrayList<>();
        TvTrackInfo.Builder build1 = new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, "1");
        build1.setLanguage("en");
        tracks.add(build1.build());
        TvTrackInfo.Builder build2 = new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, "1");
        build2.setLanguage("ga");
        tracks.add(build2.build());
        return tracks;
    }

    private final ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onChannelsChanged();
        }
    };
}
