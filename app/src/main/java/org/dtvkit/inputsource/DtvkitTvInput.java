package org.dtvkit.inputsource;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.PorterDuff;
import android.graphics.Paint;
import android.database.Cursor;
import android.media.PlaybackParams;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputManager.Hardware;
import android.media.tv.TvInputManager.HardwareCallback;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvStreamConfig;
import android.text.TextUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
import android.content.Intent;

import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.Surface;
import android.view.View;
import android.view.KeyEvent;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.widget.FrameLayout;

import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.dtvkit.companionlibrary.model.Channel;
import org.dtvkit.companionlibrary.model.InternalProviderData;
import org.dtvkit.companionlibrary.model.Program;
import org.dtvkit.companionlibrary.model.RecordedProgram;
import org.dtvkit.companionlibrary.utils.TvContractUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/*
dtvkit
 */
import org.dtvkit.inputsource.DtvkitDvbScan;
import org.dtvkit.inputsource.DtvkitDvbScan.ScannerEvent;

import com.droidlogic.settings.PropSettingManager;
import com.droidlogic.settings.ConvertSettingManager;
import com.droidlogic.settings.SysSettingManager;
import com.droidlogic.settings.ConstantManager;

//import com.droidlogic.app.tv.TvControlManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.Objects;
import java.util.Iterator;

public class DtvkitTvInput extends TvInputService {
    private static final String TAG = "DtvkitTvInput";
    private LongSparseArray<Channel> mChannels;
    private ContentResolver mContentResolver;

    protected DtvkitDvbScan mDtvkitDvbScan = null;
    protected String mInputId = null;
    protected String mProtocol = null;
   // private TvControlManager Tcm = null;
    private DtvScanHandler mDtvScanHandler;
    private static final int MSG_DO_TRY_SCAN = 0;
    private static final int RETRY_TIMES = 10;
    private int retry_times = RETRY_TIMES;

    TvInputInfo mTvInputInfo = null;
    protected Hardware mHardware;
    protected TvStreamConfig[] mConfigs;
    private TvInputManager mTvInputManager;
    private Surface mSurface;
    private SysSettingManager mSysSettingManager = null;

    private enum PlayerState {
        STOPPED, PLAYING
    }
    private enum RecorderState {
        STOPPED, STARTING, RECORDING
    }
    private RecorderState timeshiftRecorderState = RecorderState.STOPPED;
    private boolean timeshifting = false;
    private int numRecorders = 0;
    private int numActiveRecordings = 0;
    private boolean scheduleTimeshiftRecording = false;
    private Handler scheduleTimeshiftRecordingHandler = null;
    private Runnable timeshiftRecordRunnable;
    private long mDtvkitTvInputSessionCount = 0;

    public DtvkitTvInput() {
        Log.i(TAG, "DtvkitTvInput");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        mTvInputManager = (TvInputManager)this.getSystemService(Context.TV_INPUT_SERVICE);
        mDtvkitDvbScan = new DtvkitDvbScan();
        mContentResolver = getContentResolver();
        mContentResolver.registerContentObserver(TvContract.Channels.CONTENT_URI, true, mContentObserver);
        onChannelsChanged();

        TvInputInfo.Builder builder = new TvInputInfo.Builder(getApplicationContext(), new ComponentName(getApplicationContext(), DtvkitTvInput.class));
        numRecorders = recordingGetNumRecorders();
        if (numRecorders > 0) {
            builder.setCanRecord(true)
                    .setTunerCount(numRecorders);
            mContentResolver.registerContentObserver(TvContract.RecordedPrograms.CONTENT_URI, true, mRecordingsContentObserver);
            onRecordingsChanged();
        } else {
            builder.setCanRecord(false)
                    .setTunerCount(1);
        }
        getApplicationContext().getSystemService(TvInputManager.class).updateTvInputInfo(builder.build());
        mSysSettingManager = new SysSettingManager();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        mContentResolver.unregisterContentObserver(mContentObserver);
        mContentResolver.unregisterContentObserver(mRecordingsContentObserver);
        DtvkitGlueClient.getInstance().disConnectDtvkitClient();
        super.onDestroy();
    }

    @Override
    public final Session onCreateSession(String inputId) {
        Log.i(TAG, "onCreateSession " + inputId);
        return new DtvkitTvInputSession(this);
    }

    protected void setInputId(String name) {
        mInputId = name;
        Log.d(TAG, "set input id to " + mInputId);
    }

    private class DtvkitOverlayView extends FrameLayout {

        private NativeOverlayView nativeOverlayView;
        private CiMenuView ciOverlayView;

        private boolean mhegTookKey = false;

        public DtvkitOverlayView(Context context) {
            super(context);

            Log.i(TAG, "onCreateDtvkitOverlayView");

            nativeOverlayView = new NativeOverlayView(getContext());
            ciOverlayView = new CiMenuView(getContext());

            this.addView(nativeOverlayView);
            this.addView(ciOverlayView);
        }

        public void destroy() {
            ciOverlayView.destroy();
        }

        public void setSize(int width, int height) {
            nativeOverlayView.setSize(width, height);
        }

        public boolean handleKeyDown(int keyCode, KeyEvent event) {
            boolean result;
            if (ciOverlayView.handleKeyDown(keyCode, event)) {
                mhegTookKey = false;
                result = true;
            }
            else if (mhegKeypress(keyCode)){
                mhegTookKey = true;
                result = true;
            }
            else {
                mhegTookKey = false;
                result = false;
            }
            return result;
        }

        public boolean handleKeyUp(int keyCode, KeyEvent event) {
            boolean result;

            if (ciOverlayView.handleKeyUp(keyCode, event) || mhegTookKey) {
                result = true;
            }
            else {
                result = false;
            }
            mhegTookKey = false;

            return result;
        }

        private boolean mhegKeypress(int keyCode) {
            boolean used=false;
            try {
                JSONArray args = new JSONArray();
                args.put(keyCode);
                used = DtvkitGlueClient.getInstance().request("Mheg.notifyKeypress", args).getBoolean("data");
                Log.i(TAG, "Mheg keypress, used:" + used);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return used;
        }
    }

    class NativeOverlayView extends View
    {
        Bitmap overlay1 = null;
        Bitmap overlay2 = null;
        Bitmap overlay_update = null;
        Bitmap overlay_draw = null;
        Bitmap region = null;
        int region_width = 0;
        int region_height = 0;
        int left = 0;
        int top = 0;
        int width = 0;
        int height = 0;
        Rect src, dst;

        Semaphore sem = new Semaphore(1);

        private final DtvkitGlueClient.OverlayTarget mTarget = new DtvkitGlueClient.OverlayTarget() {
            @Override
            public void draw(int src_width, int src_height, int dst_x, int dst_y, int dst_width, int dst_height, byte[] data) {
                if (overlay1 == null) {
                    /* TODO The overlay size should come from the tif (and be updated on onOverlayViewSizeChanged) */
                    /* Create 2 layers for double buffering */
                    overlay1 = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
                    overlay2 = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);

                    overlay_draw = overlay1;
                    overlay_update = overlay2;

                    /* Clear the overlay that will be drawn initially */
                    Canvas canvas = new Canvas(overlay_draw);
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                }

                /* TODO Temporary private usage of API. Add explicit methods if keeping this mechanism */
                if (src_width == 0 || src_height == 0) {
                    if (dst_width == 9999) {
                        /* 9999 dst_width indicates the overlay should be cleared */
                        Canvas canvas = new Canvas(overlay_update);
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    }
                    else if (dst_height == 9999) {
                        /* 9999 dst_height indicates the drawn regions should be displayed on the overlay */
                        /* The update layer is now ready to be displayed so switch the overlays
                         * and use the other one for the next update */
                        sem.acquireUninterruptibly();
                        Bitmap temp = overlay_draw;
                        overlay_draw = overlay_update;
                        src = new Rect(0, 0, overlay_draw.getWidth(), overlay_draw.getHeight());
                        overlay_update = temp;
                        sem.release();
                        postInvalidate();
                        return;
                    }
                    else {
                        /* 0 dst_width and 0 dst_height indicates to add the region to overlay */
                        if (region != null) {
                            Canvas canvas = new Canvas(overlay_update);
                            Rect src = new Rect(0, 0, region_width, region_height);
                            Rect dst = new Rect(left, top, left + width, top + height);
                            Paint paint = new Paint();
                            paint.setAntiAlias(true);
                            paint.setFilterBitmap(true);
                            paint.setDither(true);
                            canvas.drawBitmap(region, src, dst, paint);
                            region = null;
                        }
                    }
                    return;
                }

                int part_bottom = 0;
                if (region == null) {
                    /* TODO Create temporary region buffer using region_width and overlay height */
                    region_width = src_width;
                    region_height = src_height;
                    region = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
                    left = dst_x;
                    top = dst_y;
                    width = dst_width;
                    height = dst_height;
                }
                else {
                    part_bottom = region_height;
                    region_height += src_height;
                }

                /* Build an array of ARGB_8888 pixels as signed ints and add this part to the region */
                int[] colors = new int[src_width * src_height];
                for (int i = 0, j = 0; i < src_width * src_height; i++, j += 4) {
                   colors[i] = (((int)data[j]&0xFF) << 24) | (((int)data[j+1]&0xFF) << 16) |
                      (((int)data[j+2]&0xFF) << 8) | ((int)data[j+3]&0xFF);
                }
                Bitmap part = Bitmap.createBitmap(colors, 0, src_width, src_width, src_height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(region);
                canvas.drawBitmap(part, 0, part_bottom, null);
            }
        };

        public NativeOverlayView(Context context) {
            super(context);
            DtvkitGlueClient.getInstance().setOverlayTarget(mTarget);
        }

        public void setSize(int width, int height) {
            dst = new Rect(0, 0, width, height);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            sem.acquireUninterruptibly();
            if (overlay_draw != null) {
                canvas.drawBitmap(overlay_draw, src, dst, null);
            }
            sem.release();
        }
    }

    // We do not indicate recording capabilities. TODO for recording.
    @RequiresApi(api = Build.VERSION_CODES.N)
    public TvInputService.RecordingSession onCreateRecordingSession(String inputId)
    {
        Log.i(TAG, "onCreateRecordingSession");
        removeScheduleTimeshiftRecordingTask();
        numActiveRecordings = recordingGetNumActiveRecordings();
        Log.i(TAG, "numActiveRecordings: " + numActiveRecordings);
        if (numActiveRecordings >= numRecorders) {
            Log.i(TAG, "stopping timeshift");
            boolean returnToLive = timeshifting;
            timeshiftRecorderState = RecorderState.STOPPED;
            scheduleTimeshiftRecording = false;
            playerStopTimeshiftRecording(returnToLive);
        }

        return new DtvkitRecordingSession(this, inputId);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    class DtvkitRecordingSession extends TvInputService.RecordingSession {
        private static final String TAG = "DtvkitRecordingSession";
        private Uri mChannel = null;
        private Uri mProgram = null;
        private Context mContext = null;
        private String mInputId = null;
        private long startRecordTimeMillis = 0;
        private long endRecordTimeMillis = 0;

        @RequiresApi(api = Build.VERSION_CODES.N)
        public DtvkitRecordingSession(Context context, String inputId) {
            super(context);
            mContext = context;
            mInputId = inputId;
            Log.i(TAG, "DtvkitRecordingSession");
        }

        @Override
        public void onTune(Uri uri) {
            Log.i(TAG, "onTune for recording " + uri);
            if (ContentUris.parseId(uri) == -1) {
                Log.e(TAG, "DtvkitRecordingSession onTune invalid uri = " + uri);
                notifyError(TvInputManager.RECORDING_ERROR_UNKNOWN);
                return;
            }
            mChannel = uri;
            Channel channel = getChannel(uri);
            if (recordingCheckAvailability(getChannelInternalDvbUri(channel))) {
                Log.i(TAG, "recording path available");
                notifyTuned(uri);
            } else {
                Log.i(TAG, "No recording path available");
                notifyError(TvInputManager.RECORDING_ERROR_UNKNOWN);
            }
        }

        @Override
        public void onStartRecording(@Nullable Uri uri) {
            Log.i(TAG, "onStartRecording " + uri);
            mProgram = uri;

            String dvbUri;
            long durationSecs = 0;
            Program program = getProgram(uri);
            if (program != null) {
                startRecordTimeMillis = program.getStartTimeUtcMillis();
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis > startRecordTimeMillis) {
                    startRecordTimeMillis = currentTimeMillis;
                }
                dvbUri = getProgramInternalDvbUri(program);
            } else {
                startRecordTimeMillis = System.currentTimeMillis();
                dvbUri = getChannelInternalDvbUri(getChannel(mChannel));
                durationSecs = 3 * 60 * 60; // 3 hours is maximum recording duration for Android
            }
            StringBuffer recordingResponse = new StringBuffer();
            if (!recordingAddRecording(dvbUri, false, durationSecs, recordingResponse)) {
                if (recordingResponse.toString().equals("May not be enough space on disk")) {
                    Log.i(TAG, "record error insufficient space");
                    notifyError(TvInputManager.RECORDING_ERROR_INSUFFICIENT_SPACE);
                }
                else {
                    Log.i(TAG, "record error unknown");
                    notifyError(TvInputManager.RECORDING_ERROR_UNKNOWN);
                }
            }
        }

        @Override
        public void onStopRecording() {
            Log.i(TAG, "onStopRecording");

            endRecordTimeMillis = System.currentTimeMillis();
            String recordingUri = getProgramInternalRecordingUri(recordingGetStatus());
            scheduleTimeshiftRecording = true;

            if (!recordingStopRecording(recordingUri)) {
                notifyError(TvInputManager.RECORDING_ERROR_UNKNOWN);
            } else {
                long recordingDurationMillis = endRecordTimeMillis - startRecordTimeMillis;
                RecordedProgram.Builder builder;
                Program program = getProgram(mProgram);
                if (program == null) {
                    program = getCurrentProgram(mChannel);
                    if (program == null) {
                        builder = new RecordedProgram.Builder()
                                .setStartTimeUtcMillis(startRecordTimeMillis)
                                .setEndTimeUtcMillis(endRecordTimeMillis);
                    } else {
                        builder = new RecordedProgram.Builder(program);
                    }
                } else {
                    builder = new RecordedProgram.Builder(program);
                }
                RecordedProgram recording = builder.setInputId(mInputId)
                        .setRecordingDataUri(recordingUri)
                        .setRecordingDurationMillis(recordingDurationMillis > 0 ? recordingDurationMillis : -1)
                        .build();
                notifyRecordingStopped(mContext.getContentResolver().insert(TvContract.RecordedPrograms.CONTENT_URI,
                        recording.toContentValues()));
            }
        }

        @Override
        public void onRelease() {
            Log.i(TAG, "onRelease");

            String uri = "";
            if (mProgram != null) {
                uri = getProgramInternalDvbUri(getProgram(mProgram));
            } else {
                uri = getChannelInternalDvbUri(getChannel(mChannel)) + ";0000";
            }

            JSONArray scheduledRecordings = recordingGetListOfScheduledRecordings();
            if (scheduledRecordings != null) {
                for (int i = 0; i < scheduledRecordings.length(); i++) {
                    try {
                        if (getScheduledRecordingUri(scheduledRecordings.getJSONObject(i)).equals(uri)) {
                            Log.i(TAG, "removing recording uri from schedule: " + uri);
                            recordingRemoveScheduledRecording(uri);
                            break;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }

        private Program getProgram(Uri uri) {
            Program program = null;
            if (uri != null) {
                Cursor cursor = mContext.getContentResolver().query(uri, Program.PROJECTION, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        program = Program.fromCursor(cursor);
                    }
                }
            }
            return program;
        }

        private Program getCurrentProgram(Uri channelUri) {
            return TvContractUtils.getCurrentProgram(mContext.getContentResolver(), channelUri);
        }
    }

    class DtvkitTvInputSession extends TvInputService.Session implements DtvkitDvbScan.ScannerEventListener {
        private static final String TAG = "DtvkitTvInputSession";
        private static final int ADEC_START_DECODE = 1;
        private static final int ADEC_PAUSE_DECODE = 2;
        private static final int ADEC_RESUME_DECODE = 3;
        private static final int ADEC_STOP_DECODE = 4;
        private static final int ADEC_SET_DECODE_AD = 5;
        private static final int ADEC_SET_VOLUME = 6;
        private static final int ADEC_SET_MUTE = 7;
        private static final int ADEC_SET_OUTPUT_MODE = 8;
        private static final int ADEC_SET_PRE_GAIN = 9;
        private static final int ADEC_SET_PRE_MUTE = 10;
        private boolean mhegTookKey = false;
        private Channel mTunedChannel = null;
        private List<TvTrackInfo> mTunedTracks = null;
        protected final Context mContext;
        private RecordedProgram recordedProgram = null;
        private long originalStartPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
        private long startPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
        private long currentPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
        private float playSpeed = 0;
        private PlayerState playerState = PlayerState.STOPPED;
        private boolean timeshiftAvailable = false;
        private int timeshiftBufferSizeMins = 60;
        DtvkitOverlayView mView = null;
        private long mCurrentDtvkitTvInputSessionIndex = 0;
        protected HandlerThread mHandlerThread = null;
        protected Handler mHandlerThreadHandle = null;
        private boolean mIsMain = false;

        DtvkitTvInputSession(Context context) {
            super(context);
            mContext = context;
            Log.i(TAG, "DtvkitTvInputSession creat");
            mDtvkitDvbScan.setScannerListener(this);
            setOverlayViewEnabled(true);
            numActiveRecordings = recordingGetNumActiveRecordings();
            Log.i(TAG, "numActiveRecordings: " + numActiveRecordings);

            if (numActiveRecordings < numRecorders) {
                timeshiftAvailable = true;
            } else {
                timeshiftAvailable = false;
            }

            timeshiftRecordRunnable = new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void run() {
                    Log.i(TAG, "timeshiftRecordRunnable running");
                    if (timeshiftAvailable) {
                        if (timeshiftRecorderState == RecorderState.STOPPED) {
                            if (playerStartTimeshiftRecording()) {
                                timeshiftRecorderState = RecorderState.STARTING;
                            } else {
                                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                            }
                        }
                    }
                }
            };

            playerSetTimeshiftBufferSize(timeshiftBufferSizeMins);
            recordingSetDefaultDisk("/data/data/org.dtvkit.inputsource");
            mDtvkitTvInputSessionCount++;
            mCurrentDtvkitTvInputSessionIndex = mDtvkitTvInputSessionCount;
            initWorkThread();
        }

        @Override
        public void onSetMain(boolean isMain) {
            Log.d(TAG, "onSetMain, isMain: " + isMain +" mCurrentDtvkitTvInputSessionIndex is " + mCurrentDtvkitTvInputSessionIndex);
            mIsMain = isMain;
        }

        @Override
        public void onRelease() {
            Log.i(TAG, "onRelease");
            releaseWorkThread();
        }

        public void doRelease() {
            Log.i(TAG, "doRelease");
            removeScheduleTimeshiftRecordingTask();
            scheduleTimeshiftRecording = false;
            mhegStop();
            playerStopTimeshiftRecording(false);
            playerStop();
            DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
            mDtvkitDvbScan.setScannerListener(null);
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            Log.i(TAG, "onSetSurface " + surface + ", mDtvkitTvInputSessionCount = " + mDtvkitTvInputSessionCount + ", mCurrentDtvkitTvInputSessionIndex = " + mCurrentDtvkitTvInputSessionIndex);

            if (null != mHardware && mConfigs.length > 0) {
                if (null == surface) {
                    if (mIsMain) {
                        doRelease();
                        setOverlayViewEnabled(false);
                        mHardware.setSurface(null, null);
                        Log.d(TAG, "onSetSurface null");
                        mSurface = null;
                    }
                } else {
                    if (mSurface != surface) {
                        Log.d(TAG, "TvView swithed,  onSetSurface null first");
                        doRelease();
                        mHardware.setSurface(null, null);
                    }
                    mHardware.setSurface(surface, mConfigs[0]);
                    surface = mSurface;
                    Log.d(TAG, "onSetSurface ok");
                }
            }
            return true;
        }

        @Override
        public void onSurfaceChanged(int format, int width, int height) {
            Log.i(TAG, "onSurfaceChanged " + format + ", " + width + ", " + height);
            playerSetRectangle(0, 0, width, height);
        }

        public View onCreateOverlayView() {
            if (mView == null) {
                mView = new DtvkitOverlayView(mContext);
            }
            return mView;
        }

        @Override
        public void onOverlayViewSizeChanged(int width, int height) {
            Log.i(TAG, "onOverlayViewSizeChanged " + width + ", " + height);
            if (mView == null) {
                mView = new DtvkitOverlayView(mContext);
            }
            //Platform platform = new Platform();
            //playerSetRectangle(platform.getSurfaceX(), platform.getSurfaceY(), width, height);
            mView.setSize(width, height);
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public boolean onTune(Uri channelUri) {
            Log.i(TAG, "onTune " + channelUri);
            if (ContentUris.parseId(channelUri) == -1) {
                Log.e(TAG, "DtvkitTvInputSession onTune invalid channelUri = " + channelUri);
                return false;
            }
            mHandlerThreadHandle.obtainMessage(MSG_ON_TUNE, 0, 0, channelUri).sendToTarget();
            mTunedChannel = getChannel(channelUri);

            Log.i(TAG, "onTune will be Done in onTuneByHandlerThreadHandle");
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
            if (true) {
                Log.i(TAG, "onSetCaptionEnabled no need again, start it in select track or gettracks in onsignal");
                return;
            }
            Log.i(TAG, "onSetCaptionEnabled " + enabled);
            // TODO CaptioningManager.getLocale()
            playerSetSubtitlesOn(enabled);//start it in select track or gettracks in onsignal
        }

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            Log.i(TAG, "onSelectTrack " + type + ", " + trackId);
            if (type == TvTrackInfo.TYPE_AUDIO) {
                if (playerSelectAudioTrack((null == trackId) ? 0xFFFF : Integer.parseInt(trackId))) {
                    notifyTrackSelected(type, trackId);
                    return true;
                }
            } else if (type == TvTrackInfo.TYPE_SUBTITLE) {
                String sourceTrackId = trackId;
                if (!TextUtils.isEmpty(trackId) && !TextUtils.isDigitsOnly(trackId)) {
                    String[] nameValuePairs = trackId.split("&");
                    if (nameValuePairs != null && nameValuePairs.length > 0) {
                        String[] nameValue = nameValuePairs[0].split("=");
                        if (nameValue != null && nameValue.length ==2 && TextUtils.equals(nameValue[0], "id") && TextUtils.isDigitsOnly(nameValue[1])) {
                            trackId = nameValue[1];//parse id
                        }
                    }
                    if (TextUtils.isEmpty(trackId) || !TextUtils.isDigitsOnly(trackId)) {
                        //notifyTrackSelected(type, sourceTrackId);
                        Log.d(TAG, "need trackId that only contains number sourceTrackId = " + sourceTrackId + ", trackId = " + trackId);
                        return false;
                    }
                }
                if (playerSelectSubtitleTrack(TextUtils.isEmpty(trackId) ? 0xFFFF : Integer.parseInt(trackId))) {
                    if (TextUtils.isEmpty(trackId)) {
                        if (playerGetSubtitlesOn()) {
                            playerSetSubtitlesOn(false);//close if opened
                        }
                    } else {
                        if (!playerGetSubtitlesOn()) {
                            playerSetSubtitlesOn(true);//open if closed
                        }
                    }
                    notifyTrackSelected(type, sourceTrackId);
                    return true;
                }
            }
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

        @Override
        public void onEvent(ScannerEvent event) {
            switch (event.type) {
                case DtvkitDvbScan.EVENT_SCAN_BEGIN:
                    Log.d(TAG, "scan start");
                    //mStatus = ScanStatusType.IN_PROG;
                   // updateStatus(mStatus);
                    break;
                case DtvkitDvbScan.EVENT_SCAN_END:
                    //mStatus = ScanStatusType.COMPLETE;
                    //updateStatus(mStatus);
                    Log.d(TAG, "scan end");
                    DtvkitStopSyncDb();
                    break;
                case DtvkitDvbScan.EVENT_SCAN_PROGRESS:
                    //mComplete = event.precent;
                    //mDtvCount = event.totalcount;
                    //updateChannelCount(ChannelType.DTV, mDtvCount);
                    //updateProgress(mComplete);
                    break;
                case DtvkitDvbScan.EVENT_STORE_BEGIN:
                    Log.d(TAG, "store begin");
                    //amlDtvkitStopScan(mProtocol);
                    DtvkitStartSyncDb(mInputId);
                    break;
                default:
                    Log.d(TAG, "unhandled scanner event: " + event.type);
                    break;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onTimeShiftPlay(Uri uri) {
            Log.i(TAG, "onTimeShiftPlay " + uri);

            recordedProgram = getRecordedProgram(uri);
            if (recordedProgram != null) {
                playerState = PlayerState.PLAYING;
                playerStop();
                playerPlay(recordedProgram.getRecordingDataUri());
            }
        }

        public void onTimeShiftPause() {
            Log.i(TAG, "onTimeShiftPause ");
            if (timeshiftRecorderState == RecorderState.RECORDING && !timeshifting) {
                Log.i(TAG, "starting pause playback ");
                timeshifting = true;
                playerPlayTimeshiftRecording(true, true);
            }
            else {
                Log.i(TAG, "player pause ");
                if (playerPause())
                {
                    playSpeed = 0;
                }
            }
        }

        public void onTimeShiftResume() {
            Log.i(TAG, "onTimeShiftResume ");
            playerState = PlayerState.PLAYING;
            if (playerResume())
            {
                playSpeed = 1;
            }
        }

        public void onTimeShiftSeekTo(long timeMs) {
            Log.i(TAG, "onTimeShiftSeekTo:  " + timeMs);
            if (timeshiftRecorderState == RecorderState.RECORDING && !timeshifting) /* Watching live tv while recording */ {
                timeshifting = true;
                boolean seekToBeginning = false;

                if (timeMs == startPosition) {
                    seekToBeginning = true;
                }
                playerPlayTimeshiftRecording(false, !seekToBeginning);
            } else if (timeshiftRecorderState == RecorderState.RECORDING && timeshifting) {
                playerSeekTo((timeMs - (originalStartPosition + PropSettingManager.getStreamTimeDiff())) / 1000);
            } else {
                playerSeekTo(timeMs / 1000);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onTimeShiftSetPlaybackParams(PlaybackParams params) {
            Log.i(TAG, "onTimeShiftSetPlaybackParams");

            float speed = params.getSpeed();
            Log.i(TAG, "speed: " + speed);
            if (speed != playSpeed) {
                if (timeshiftRecorderState == RecorderState.RECORDING && !timeshifting) {
                    timeshifting = true;
                    playerPlayTimeshiftRecording(false, true);
                }

                if (playerSetSpeed(speed))
                {
                    playSpeed = speed;
                }
            }
        }

        public long onTimeShiftGetStartPosition() {
            if (timeshiftRecorderState != RecorderState.STOPPED) {
                Log.i(TAG, "requesting timeshift recorder status");
                long length = 0;
                JSONObject timeshiftRecorderStatus = playerGetTimeshiftRecorderStatus();
                if (originalStartPosition != 0 && originalStartPosition != TvInputManager.TIME_SHIFT_INVALID_TIME) {
                    startPosition = originalStartPosition + PropSettingManager.getStreamTimeDiff();
                }
                if (timeshiftRecorderStatus != null) {
                    try {
                        length = timeshiftRecorderStatus.getLong("length");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }

                if (length > (timeshiftBufferSizeMins * 60)) {
                    startPosition = originalStartPosition + ((length - (timeshiftBufferSizeMins * 60)) * 1000);
                    Log.i(TAG, "new start position: " + startPosition);
                }
            }
            Log.i(TAG, "onTimeShiftGetStartPosition startPosition:" + startPosition + ", as date = " + ConvertSettingManager.convertLongToDate(startPosition));
            return startPosition;
        }

        public long onTimeShiftGetCurrentPosition() {
            if (startPosition == 0) /* Playing back recorded program */ {
                if (playerState == PlayerState.PLAYING) {
                    currentPosition = playerGetElapsed() * 1000;
                    Log.i(TAG, "playing back record program. current position: " + currentPosition);
                }
            } else if (timeshifting) {
                currentPosition = (playerGetElapsed() * 1000) + originalStartPosition + PropSettingManager.getStreamTimeDiff();
                Log.i(TAG, "timeshifting. current position: " + currentPosition);
            } else if (startPosition == TvInputManager.TIME_SHIFT_INVALID_TIME) {
                currentPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
                Log.i(TAG, "Invalid time. Current position: " + currentPosition);
            } else {
                currentPosition = /*System.currentTimeMillis()*/PropSettingManager.getCurrentStreamTime(true);
                Log.i(TAG, "live tv. current position: " + currentPosition);
            }
            Log.d(TAG, "onTimeShiftGetCurrentPosition currentPosition = " + currentPosition + ", as date = " + ConvertSettingManager.convertLongToDate(currentPosition));
            return currentPosition;
        }

        private RecordedProgram getRecordedProgram(Uri uri) {
            RecordedProgram recordedProgram = null;
            if (uri != null) {
                Cursor cursor = mContext.getContentResolver().query(uri, RecordedProgram.PROJECTION, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        recordedProgram = RecordedProgram.fromCursor(cursor);
                    }
                }
            }
            return recordedProgram;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            boolean used;

            Log.i(TAG, "onKeyDown " + keyCode);

            /* It's possible for a keypress to be registered before the overlay is created */
            if (mView == null) {
                used = super.onKeyDown(keyCode, event);
            }
            else {
                if (mView.handleKeyDown(keyCode, event)) {
                    used = true;
                } else {
                   used = super.onKeyDown(keyCode, event);
                }
            }

            return used;
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            boolean used;

            Log.i(TAG, "onKeyUp " + keyCode);

            /* It's possible for a keypress to be registered before the overlay is created */
            if (mView == null) {
                used = super.onKeyUp(keyCode, event);
            }
            else {
                if (mView.handleKeyUp(keyCode, event)) {
                    used = true;
                } else {
                    used = super.onKeyUp(keyCode, event);
                }
            }

            return used;
        }

        private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onSignal(String signal, JSONObject data) {
                Log.i(TAG, "onSignal: " + signal + " : " + data.toString());
                // TODO notifyTracksChanged(List<TvTrackInfo> tracks)
                if (signal.equals("PlayerStatusChanged")) {
                    String state = "off";
                    String dvbUri = "";
                    try {
                        state = data.getString("state");
                        dvbUri= data.getString("uri");
                    } catch (JSONException ignore) {
                    }
                    Log.i(TAG, "signal: "+state);
                    switch (state) {
                        case "playing":
                            String type = "dvblive";
                            try {
                                type = data.getString("type");
                            } catch (JSONException e) {
                                Log.e(TAG, e.getMessage());
                            }
                            if (type.equals("dvblive")) {
                                if (mTunedChannel.getServiceType().equals(TvContract.Channels.SERVICE_TYPE_AUDIO)) {
                                    notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY);
                                } else {
                                    notifyVideoAvailable();
                                }
                                List<TvTrackInfo> tracks = playerGetTracks();
                                if (!tracks.equals(mTunedTracks)) {
                                    mTunedTracks = tracks;
                                    // TODO Also for service changed event
                                    Log.d(TAG, "update new mTunedTracks");
                                }
                                notifyTracksChanged(mTunedTracks);

                                if (mTunedChannel.getServiceType().equals(TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO)) {
                                    if (mHandlerThreadHandle != null)
                                        mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CHECK_RESOLUTION, MSG_CHECK_RESOLUTION_PERIOD);//check resolution later
                                }
                                Log.i(TAG, "audio track selected: " + playerGetSelectedAudioTrack());
                                notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, Integer.toString(playerGetSelectedAudioTrack()));
                                if (playerGetSubtitlesOn()) {
                                    String selectId = playerGetSelectedSubtitleTrackId();
                                    Log.i(TAG, "subtitle track selected: " + selectId);
                                    if (TextUtils.isEmpty(selectId)) {
                                        playerSetSubtitlesOn(false);
                                    }
                                    notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, playerGetSelectedSubtitleTrackId());
                                } else {
                                    String selectId = playerGetSelectedSubtitleTrackId();
                                    Log.i(TAG, "subtitle track off selected = " + selectId);
                                    if (!TextUtils.isEmpty(selectId)) {
                                        playerSetSubtitlesOn(true);
                                        notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, selectId);
                                    } else {
                                        notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
                                    }
                                }

                                if (timeshiftRecorderState == RecorderState.STOPPED) {
                                    numActiveRecordings = recordingGetNumActiveRecordings();
                                    Log.i(TAG, "numActiveRecordings: " + numActiveRecordings);
                                    if (numActiveRecordings < numRecorders) {
                                        timeshiftAvailable = true;
                                    } else {
                                        timeshiftAvailable = false;
                                    }
                                }
                                Log.i(TAG, "timeshiftAvailable: " + timeshiftAvailable);
                                Log.i(TAG, "timeshiftRecorderState: " + timeshiftRecorderState);
                                if (timeshiftAvailable) {
                                    if (timeshiftRecorderState == RecorderState.STOPPED) {
                                        if (playerStartTimeshiftRecording()) {
                                            timeshiftRecorderState = RecorderState.STARTING;
                                        } else {
                                            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                                        }
                                    }
                                }
                            }
                            else if (type.equals("dvbrecording")) {
                                startPosition = originalStartPosition = 0; // start position is always 0 when playing back recorded program
                                currentPosition = playerGetElapsed(data) * 1000;
                                Log.i(TAG, "dvbrecording currentPosition = " + currentPosition + "as date = " + ConvertSettingManager.convertLongToDate(startPosition));
                                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
                            }
                            playerState = PlayerState.PLAYING;
                            break;
                        case "blocked":
                            notifyContentBlocked(TvContentRating.createRating("com.android.tv", "DVB", "DVB_18"));
                            break;
                        case "badsignal":
                            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL);
                            break;
                        case "off":
                            if (timeshiftRecorderState != RecorderState.STOPPED) {
                                removeScheduleTimeshiftRecordingTask();
                                scheduleTimeshiftRecording = false;
                                playerStopTimeshiftRecording(false);
                                timeshiftRecorderState = RecorderState.STOPPED;
                                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                            }
                            playerState = PlayerState.STOPPED;
                            if (recordedProgram != null) {
                                currentPosition = recordedProgram.getEndTimeUtcMillis();
                            }

                            break;
                        case "starting":
                           Log.i(TAG, "mhegStart " + dvbUri);
                           if (mhegStartService(dvbUri) != -1)
                           {
                              Log.i(TAG, "mhegStarted");
                           }
                           else
                           {
                              Log.i(TAG, "mheg failed to start");
                           }
                           break;
                        default:
                            Log.i(TAG, "Unhandled state: " + state);
                            break;
                    }
                } else if (signal.equals("AudioParamCB")) {
                    int cmd = 0, param1 = 0, param2 = 0;
                    AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                    try {
                        cmd = data.getInt("audio_status");
                        param1 = data.getInt("audio_param1");
                        param2 = data.getInt("audio_param2");
                    } catch (JSONException ignore) {
                        Log.e(TAG, ignore.getMessage());
                    }
                    Log.d(TAG, "cmd ="+cmd+" param1 ="+param1+" param2 ="+param2);
                    switch (cmd) {
                        case ADEC_START_DECODE:
                            audioManager.setParameters("fmt="+param1);
                            audioManager.setParameters("has_dtv_video="+param2);
                            audioManager.setParameters("cmd="+cmd);
                            break;
                        case ADEC_PAUSE_DECODE:
                            audioManager.setParameters("cmd="+cmd);
                            break;
                        case ADEC_RESUME_DECODE:
                            audioManager.setParameters("cmd="+cmd);
                            break;
                        case ADEC_STOP_DECODE:
                            audioManager.setParameters("cmd="+cmd);
                            break;
                        case ADEC_SET_DECODE_AD:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("fmt="+param1);
                            audioManager.setParameters("pid="+param2);
                            break;
                        case ADEC_SET_VOLUME:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("vol="+param1);
                            break;
                        case ADEC_SET_MUTE:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("mute="+param1);
                            break;
                        case ADEC_SET_OUTPUT_MODE:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("mode="+param1);
                            break;
                        case ADEC_SET_PRE_GAIN:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("gain="+param1);
                            break;
                        case ADEC_SET_PRE_MUTE:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("mute="+param1);
                            break;
                        default:
                            Log.i(TAG,"unkown audio cmd!");
                            break;
                    }
                } else if (signal.equals("PlayerTimeshiftRecorderStatusChanged")) {
                    switch (playerGetTimeshiftRecorderState(data)) {
                        case "recording":
                            timeshiftRecorderState = RecorderState.RECORDING;
                            startPosition = /*System.currentTimeMillis()*/PropSettingManager.getCurrentStreamTime(true);
                            originalStartPosition = PropSettingManager.getCurrentStreamTime(false);//keep the original time
                            Log.i(TAG, "recording originalStartPosition as date = " + ConvertSettingManager.convertLongToDate(originalStartPosition) + ", startPosition = " + ConvertSettingManager.convertLongToDate(startPosition));
                            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
                            break;
                        case "off":
                            timeshiftRecorderState = RecorderState.STOPPED;
                            startPosition = originalStartPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
                            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                            break;
                    }
                } else if (signal.equals("RecordingStatusChanged")) {
                    JSONArray activeRecordings = recordingGetActiveRecordings(data);
                    if (activeRecordings != null && activeRecordings.length() < numRecorders &&
                            timeshiftRecorderState == RecorderState.STOPPED && scheduleTimeshiftRecording) {
                        timeshiftAvailable = true;
                        scheduleTimeshiftRecordingTask();
                    }
                }
                else if (signal.equals("DvbUpdatedEventPeriods"))
                {
                    Log.i(TAG, "DvbUpdatedEventPeriods");
                    ComponentName sync = new ComponentName(mContext, DtvkitEpgSync.class);
                    EpgSyncJobService.requestImmediateSync(mContext, mInputId, false, sync);
                }
                else if (signal.equals("DvbUpdatedEventNow"))
                {
                    Log.i(TAG, "DvbUpdatedEventNow");
                    ComponentName sync = new ComponentName(mContext, DtvkitEpgSync.class);
                    EpgSyncJobService.requestImmediateSync(mContext, mInputId, true, sync);
                }
                else if (signal.equals("DvbUpdatedChannel"))
                {
                    Log.i(TAG, "DvbUpdatedChannel");
                    ComponentName sync = new ComponentName(mContext, DtvkitEpgSync.class);
                    EpgSyncJobService.requestImmediateSync(mContext, mInputId, false, true, sync);
                }
                else if (signal.equals("DvbUpdatedChannelData"))
                {
                    Log.i(TAG, "DvbUpdatedChannelData");
                    List<TvTrackInfo> tracks = playerGetTracks();
                    if (!tracks.equals(mTunedTracks)) {
                        mTunedTracks = tracks;
                        notifyTracksChanged(mTunedTracks);
                    }
                    Log.i(TAG, "audio track selected: " + playerGetSelectedAudioTrack());
                    notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, Integer.toString(playerGetSelectedAudioTrack()));
                    if (playerGetSubtitlesOn()) {
                        String selectId = playerGetSelectedSubtitleTrackId();
                        Log.i(TAG, "DvbUpdatedChannelData track selected: " + selectId);
                        if (TextUtils.isEmpty(selectId)) {
                            playerSetSubtitlesOn(false);
                        }
                        notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, playerGetSelectedSubtitleTrackId());
                    } else {
                        String selectId = playerGetSelectedSubtitleTrackId();
                        Log.i(TAG, "DvbUpdatedChannelData track off selected = " + selectId);
                        if (!TextUtils.isEmpty(selectId)) {
                            playerSetSubtitlesOn(true);
                            notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, selectId);
                        } else {
                            notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
                        }
                    }
                }
                else if (signal.equals("MhegAppStarted"))
                {
                   Log.i(TAG, "MhegAppStarted");
                   notifyVideoAvailable();
                }
                else if (signal.equals("AppVideoPosition"))
                {
                   Log.i(TAG, "AppVideoPosition");
                   int left,top,right,bottom;
                   left = 0;
                   top = 0;
                   right = 1920;
                   bottom = 1080;
                   try {
                      left = data.getInt("left");
                      top = data.getInt("top");
                      right = data.getInt("right");
                      bottom = data.getInt("bottom");
                   } catch (JSONException e) {
                      Log.e(TAG, e.getMessage());
                   }
                   //due to the incorrect Surface size passed in onSurfaceChanged(),
                   //close this feature temporarily, will affect all video layout requests.(eg. mheg, afd)
                   //layoutSurface(left,top,right,bottom);
                }
                else if (signal.equals("ServiceRetuned"))
                {
                   String dvbUri = "";
                   Channel channel;
                   Uri retuneUri;
                   boolean found = false;
                   int i;
                   long id=0;
                   try {
                      dvbUri= data.getString("uri");
                   } catch (JSONException ignore) {
                   }
                   Log.i(TAG, "ServiceRetuned " + dvbUri);
                   //find the channel URI that matches the dvb uri of the retune
                   for (i = 0;i < mChannels.size();i++)
                   {
                      channel = mChannels.get(mChannels.keyAt(i));
                      if (dvbUri.equals(getChannelInternalDvbUri(channel))) {
                         found = true;
                         id = mChannels.keyAt(i);
                         break;
                      }
                   }
                   if (found)
                   {
                      //rebuild the Channel URI from the current channel + the new ID
                      retuneUri = Uri.parse("content://android.media.tv/channel");
                      retuneUri = ContentUris.withAppendedId(retuneUri,id);
                      Log.i(TAG, "Retuning to " + retuneUri);

                      mHandlerThreadHandle.obtainMessage(MSG_ON_TUNE, 1/*mhegTune*/, 0, retuneUri).sendToTarget();
                   }
                   else
                   {
                      //if we couldn't find the channel uri for some reason,
                      // try restarting MHEG on the new service anyway
                      mhegSuspend();
                      mhegStartService(dvbUri);
                   }
                }
            }
        };

        protected static final int MSG_ON_TUNE = 1;
        protected static final int MSG_CHECK_RESOLUTION = 2;

        protected static final int MSG_CHECK_RESOLUTION_PERIOD = 1000;//MS

        protected void initWorkThread() {
            Log.d(TAG, "initWorkThread");
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread("DtvkitInputWorker");
                mHandlerThread.start();
                mHandlerThreadHandle = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        Log.d(TAG, "mHandlerThreadHandle handleMessage:"+msg.what);
                        switch (msg.what) {
                            case MSG_ON_TUNE:
                                Uri channelUri = (Uri)msg.obj;
                                boolean mhegTune = msg.arg1 == 0 ? false : true;
                                if (channelUri != null) {
                                    onTuneByHandlerThreadHandle(channelUri, mhegTune);
                                }
                                break;
                            case MSG_CHECK_RESOLUTION:
                                if (!checkRealTimeResolution()) {
                                    mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CHECK_RESOLUTION, MSG_CHECK_RESOLUTION_PERIOD);
                                }
                                break;
                            default:
                                Log.d(TAG, "initWorkThread default");
                                break;
                        }
                        return true;
                    }
                });
            }
        }

        protected void releaseWorkThread() {
            Log.d(TAG, "releaseWorkThread");
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeCallbacksAndMessages(null);
            }
            if (mHandlerThread != null) {
                mHandlerThread.quit();
            }
            mHandlerThread = null;
            mHandlerThreadHandle = null;
        }

        protected boolean onTuneByHandlerThreadHandle(Uri channelUri, boolean mhegTune) {
            Log.i(TAG, "onTuneByHandlerThreadHandle " + channelUri);
            if (ContentUris.parseId(channelUri) == -1) {
                Log.e(TAG, "onTuneByHandlerThreadHandle invalid channelUri = " + channelUri);
                return false;
            }
            removeScheduleTimeshiftRecordingTask();
            if (timeshiftRecorderState != RecorderState.STOPPED) {
                timeshiftRecorderState = RecorderState.STOPPED;
                timeshifting = false;
                scheduleTimeshiftRecording = false;
                playerStopTimeshiftRecording(false);
            }

            mTunedChannel = getChannel(channelUri);
            final String dvbUri = getChannelInternalDvbUri(mTunedChannel);

            if (mhegTune) {
                mhegSuspend();
                if (mhegGetNextTuneInfo(dvbUri) == 0)
                    notifyChannelRetuned(channelUri);
            } else {
                mhegStop();
            }

            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
            playerStop();
            if (playerPlay(dvbUri)) {
                DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
            } else {
                mTunedChannel = null;
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
            }
            Log.i(TAG, "onTuneByHandlerThreadHandle Done");
            return mTunedChannel != null;
        }

        private boolean checkRealTimeResolution() {
            boolean result = false;
            if (mTunedChannel == null) {
                return true;
            }
            String serviceType = mTunedChannel.getServiceType();
            //update video track resolution
            if (TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO.equals(serviceType)) {
                int[] videoSize = playerGetDTVKitVideoSize();
                String realtimeVideoFormat = mSysSettingManager.getVideoFormatFromSys();
                result = !TextUtils.isEmpty(realtimeVideoFormat);
                if (result) {
                    notifyTrackSelected(TvTrackInfo.TYPE_VIDEO, realtimeVideoFormat);//notify default video
                    notifyTrackSelected(TvTrackInfo.TYPE_VIDEO, null);//reset video trackid
                    Log.d(TAG, "checkRealTimeResolution notify realtimeVideoFormat = " + realtimeVideoFormat + ", videoSize width = " + videoSize[0] + ", height = " + videoSize[1]);
                }
            }
            return result;
        }
    }

    private void onChannelsChanged() {
        mChannels = TvContractUtils.buildChannelMap(mContentResolver, mInputId);
    }

    private Channel getChannel(Uri channelUri) {
        return mChannels.get(ContentUris.parseId(channelUri));
    }

    private String getChannelInternalDvbUri(Channel channel) {
        try {
            return channel.getInternalProviderData().get("dvbUri").toString();
        } catch (Exception e) {
            Log.e(TAG, "getChannelInternalDvbUri Exception = " + e.getMessage());
            return "dvb://0000.0000.0000";
        }
    }

    private String getProgramInternalDvbUri(Program program) {
        try {
            String uri = program.getInternalProviderData().get("dvbUri").toString();
            return uri;
        } catch (InternalProviderData.ParseException e) {
            Log.e(TAG, "getChannelInternalDvbUri ParseException = " + e.getMessage());
            return "dvb://current";
        }
    }

    private void playerSetVolume(int volume) {
        try {
            JSONArray args = new JSONArray();
            args.put(volume);
            DtvkitGlueClient.getInstance().request("Player.setVolume", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSetVolume = " + e.getMessage());
        }
    }

    private void playerSetSubtitlesOn(boolean on) {
        try {
            JSONArray args = new JSONArray();
            args.put(on);
            DtvkitGlueClient.getInstance().request("Player.setSubtitlesOn", args);
            Log.i(TAG, "playerSetSubtitlesOn on =  " + on);
        } catch (Exception e) {
            Log.e(TAG, "playerSetSubtitlesOn=  " + e.getMessage());
        }
    }

    private boolean playerPlay(String dvbUri) {
        try {
            JSONArray args = new JSONArray();
            args.put(dvbUri);
            AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            audioManager.setParameters("tuner_in=dtv");
            DtvkitGlueClient.getInstance().request("Player.play", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerPlay = " + e.getMessage());
            return false;
        }
    }

    private void playerStop() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.stop", args);
        } catch (Exception e) {
            Log.e(TAG, "playerStop = " + e.getMessage());
        }
    }

    private boolean playerPause() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.pause", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerPause = " + e.getMessage());
            return false;
        }
    }

    private boolean playerResume() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.resume", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerResume = " + e.getMessage());
            return false;
        }
    }

    private void playerFastForward() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.fastForward", args);
        } catch (Exception e) {
            Log.e(TAG, "playerFastForwards" + e.getMessage());
        }
    }

    private void playerFastRewind() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.fastRewind", args);
        } catch (Exception e) {
            Log.e(TAG, "playerFastRewind = " + e.getMessage());
        }
    }

    private boolean playerSetSpeed(float speed) {
        try {
            JSONArray args = new JSONArray();
            args.put((long)(speed * 100.0));
            DtvkitGlueClient.getInstance().request("Player.setPlaySpeed", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerSetSpeed = " + e.getMessage());
            return false;
        }
    }

    private boolean playerSeekTo(long positionSecs) {
        try {
            JSONArray args = new JSONArray();
            args.put(positionSecs);
            DtvkitGlueClient.getInstance().request("Player.seekTo", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerSeekTo = " + e.getMessage());
            return false;
        }
    }

    private boolean playerStartTimeshiftRecording() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.startTimeshiftRecording", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerStartTimeshiftRecording = " + e.getMessage());
            return false;
        }
    }

    private boolean playerStopTimeshiftRecording(boolean returnToLive) {
        try {
            JSONArray args = new JSONArray();
            args.put(returnToLive);
            DtvkitGlueClient.getInstance().request("Player.stopTimeshiftRecording", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    private boolean playerPlayTimeshiftRecording(boolean startPlaybackPaused, boolean playFromCurrent) {
        try {
            JSONArray args = new JSONArray();
            args.put(startPlaybackPaused);
            args.put(playFromCurrent);
            DtvkitGlueClient.getInstance().request("Player.playTimeshiftRecording", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerStopTimeshiftRecording = " + e.getMessage());
            return false;
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
            Log.e(TAG, "playerSetRectangle = " + e.getMessage());
        }
    }

    private List<TvTrackInfo> playerGetTracks() {
        List<TvTrackInfo> tracks = new ArrayList<>();
        try {
            JSONArray args = new JSONArray();
            JSONArray audioStreams = DtvkitGlueClient.getInstance().request("Player.getListOfAudioStreams", args).getJSONArray("data");
            for (int i = 0; i < audioStreams.length(); i++)
            {
                JSONObject audioStream = audioStreams.getJSONObject(i);
                Log.d(TAG, "getListOfAudioStreams audioStream = " + audioStream.toString());
                TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, Integer.toString(audioStream.getInt("index")));
                String audioLang = audioStream.getString("language");
                if (TextUtils.isEmpty(audioLang)) {
                    audioLang = "Audio" + (i + 1);
                } else if (ConstantManager.CONSTANT_QAA.equalsIgnoreCase(audioLang)) {
                    audioLang = ConstantManager.CONSTANT_ORIGINAL_AUDIO;
                }
                track.setLanguage(audioLang);
                if (audioStream.getBoolean("ad")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        track.setDescription("AD");
                    }
                }
                String codes = audioStream.getString("codec");
                if (!TextUtils.isEmpty(codes)) {
                    Bundle bundle = new Bundle();
                    bundle.putString(ConstantManager.KEY_AUDIO_CODES_DES, codes);
                    track.setExtra(bundle);
                }
                tracks.add(track.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "getListOfAudioStreams = " + e.getMessage());
        }
        try {
            JSONArray args = new JSONArray();
            JSONArray subtitleStreams = DtvkitGlueClient.getInstance().request("Player.getListOfSubtitleStreams", args).getJSONArray("data");
            for (int i = 0; i < subtitleStreams.length(); i++)
            {
                JSONObject subtitleStream = subtitleStreams.getJSONObject(i);
                Log.d(TAG, "getListOfSubtitleStreams subtitleStream = " + subtitleStream.toString());
                String trackId = null;
                if (subtitleStream.getBoolean("teletext")) {
                    trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "6";//TYPE_DTV_TELETEXT_IMG
                } else {
                    trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4";//TYPE_DTV_CC
                }
                TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_SUBTITLE, trackId);
                track.setLanguage(subtitleStream.getString("language"));
                tracks.add(track.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "getListOfSubtitleStreams = " + e.getMessage());
        }
        return tracks;
    }

    private boolean playerSelectAudioTrack(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.setAudioStream", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSelectAudioTrack = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerSelectSubtitleTrack(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.setSubtitleStream", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSelectSubtitleTrack = " + e.getMessage());
            return false;
        }
        return true;
    }

    private int[] playerGetDTVKitVideoSize() {
        int[] result = {0, 0};
        try {
            JSONArray args = new JSONArray();
            JSONObject videoStreams = DtvkitGlueClient.getInstance().request("Player.getDTVKitVideoResolution", args);
            if (videoStreams != null) {
                videoStreams = (JSONObject)videoStreams.get("data");
                if (!(videoStreams == null || videoStreams.length() == 0)) {
                    Log.d(TAG, "playerGetDTVKitVideoSize videoStreams = " + videoStreams.toString());
                    result[0] = (int)videoStreams.get("width");
                    result[1] = (int)videoStreams.get("height");
                }
            } else {
                Log.d(TAG, "playerGetDTVKitVideoSize then get null");
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetDTVKitVideoSize = " + e.getMessage());
        }
        return result;
    }

    private int playerGetSelectedSubtitleTrack() {
        int index = 0xFFFF;
        try {
            JSONArray args = new JSONArray();
            JSONArray subtitleStreams = DtvkitGlueClient.getInstance().request("Player.getListOfSubtitleStreams", args).getJSONArray("data");
            for (int i = 0; i < subtitleStreams.length(); i++)
            {
                JSONObject subtitleStream = subtitleStreams.getJSONObject(i);
                if (subtitleStream.getBoolean("selected")) {
                    index = subtitleStream.getInt("index");
                    Log.i(TAG, "playerGetSelectedSubtitleTrack index = " + index);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetSelectedSubtitleTrack = " + e.getMessage());
        }
        return index;
    }

    private String playerGetSelectedSubtitleTrackId() {
        String trackId = null;
        try {
            JSONArray args = new JSONArray();
            JSONArray subtitleStreams = DtvkitGlueClient.getInstance().request("Player.getListOfSubtitleStreams", args).getJSONArray("data");
            for (int i = 0; i < subtitleStreams.length(); i++)
            {
                JSONObject subtitleStream = subtitleStreams.getJSONObject(i);
                if (subtitleStream.getBoolean("selected")) {
                    if (subtitleStream.getBoolean("teletext")) {
                        trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "6";//TYPE_DTV_TELETEXT_IMG
                    } else {
                        trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4";//TYPE_DTV_CC
                    }
                    Log.i(TAG, "playerGetSelectedSubtitleTrack trackId = " + trackId);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetSelectedSubtitleTrackId = " + e.getMessage());
        }
        return trackId;
    }


    private int playerGetSelectedAudioTrack() {
        int index = 0xFFFF;
        try {
            JSONArray args = new JSONArray();
            JSONArray audioStreams = DtvkitGlueClient.getInstance().request("Player.getListOfAudioStreams", args).getJSONArray("data");
            for (int i = 0; i < audioStreams.length(); i++)
            {
                JSONObject audioStream = audioStreams.getJSONObject(i);
                if (audioStream.getBoolean("selected")) {
                    index = audioStream.getInt("index");
                    Log.i(TAG, "playerGetSelectedAudioTrack index = " + index);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetSelectedAudioTrack = " + e.getMessage());
        }
        return index;
    }

    private boolean playerGetSubtitlesOn() {
        boolean on = false;
        try {
            JSONArray args = new JSONArray();
            on = DtvkitGlueClient.getInstance().request("Player.getSubtitlesOn", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "playerGetSubtitlesOn = " + e.getMessage());
        }
        Log.i(TAG, "playerGetSubtitlesOn on = " + on);
        return on;
    }

    private void mhegSuspend() {
        Log.e(TAG, "Mheg suspending");
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Mheg.suspend", args);
            Log.e(TAG, "Mheg suspended");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private int mhegGetNextTuneInfo(String dvbUri) {
        int quiet = -1;
        try {
            JSONArray args = new JSONArray();
            args.put(dvbUri);
            quiet = DtvkitGlueClient.getInstance().request("Mheg.getTuneInfo", args).getInt("data");
            Log.e(TAG, "Tune info: "+ quiet);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return quiet;
    }

    private JSONObject playerGetStatus() {
        JSONObject response = null;
        try {
            JSONArray args = new JSONArray();
            response = DtvkitGlueClient.getInstance().request("Player.getStatus", args).getJSONObject("data");
        } catch (Exception e) {
            Log.e(TAG, "playerGetStatus = " + e.getMessage());
        }
        return response;
    }

    private long playerGetElapsed() {
        return playerGetElapsed(playerGetStatus());
    }

    private long playerGetElapsed(JSONObject playerStatus) {
        long elapsed = 0;
        if (playerStatus != null) {
            try {
                JSONObject content = playerStatus.getJSONObject("content");
                if (content.has("elapsed")) {
                    elapsed = content.getLong("elapsed");
                }
            } catch (JSONException e) {
                Log.e(TAG, "playerGetElapsed = " + e.getMessage());
            }
        }
        return elapsed;
    }

    private JSONObject playerGetTimeshiftRecorderStatus() {
        JSONObject response = null;
        try {
            JSONArray args = new JSONArray();
            response = DtvkitGlueClient.getInstance().request("Player.getTimeshiftRecorderStatus", args).getJSONObject("data");
        } catch (Exception e) {
            Log.e(TAG, "playerGetTimeshiftRecorderStatus = " + e.getMessage());
        }
        return response;
    }

    private int playerGetTimeshiftBufferSize() {
        int timeshiftBufferSize = 0;
        try {
            JSONArray args = new JSONArray();
            timeshiftBufferSize = DtvkitGlueClient.getInstance().request("Player.getTimeshiftBufferSize", args).getInt("data");
        } catch (Exception e) {
            Log.e(TAG, "playerGetTimeshiftBufferSize = " + e.getMessage());
        }
        return timeshiftBufferSize;
    }

    private boolean playerSetTimeshiftBufferSize(int timeshiftBufferSize) {
        try {
            JSONArray args = new JSONArray();
            args.put(timeshiftBufferSize);
            DtvkitGlueClient.getInstance().request("Player.setTimeshiftBufferSize", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerSetTimeshiftBufferSize = " + e.getMessage());
            return false;
        }
    }

    private boolean recordingSetDefaultDisk(String disk_mount_path) {
        try {
            Log.d(TAG, "setDefaultDisk: " + disk_mount_path);
            JSONArray args = new JSONArray();
            args.put(disk_mount_path);
            DtvkitGlueClient.getInstance().request("Recording.setDefaultDisk", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "recordingSetDefaultDisk = " + e.getMessage());
            return false;
        }
    }

    private String playerGetTimeshiftRecorderState(JSONObject playerTimeshiftRecorderStatus) {
        String timeshiftRecorderState = "off";
        if (playerTimeshiftRecorderStatus != null) {
            try {
                if (playerTimeshiftRecorderStatus.has("timeshiftrecorderstate")) {
                    timeshiftRecorderState = playerTimeshiftRecorderStatus.getString("timeshiftrecorderstate");
                }
            } catch (JSONException e) {
                Log.e(TAG, "playerGetTimeshiftRecorderState = " + e.getMessage());
            }
        }

        return timeshiftRecorderState;
    }

    private int mhegStartService(String dvbUri) {
        int quiet = -1;
        try {
            JSONArray args = new JSONArray();
            args.put(dvbUri);
            quiet = DtvkitGlueClient.getInstance().request("Mheg.start", args).getInt("data");
            Log.e(TAG, "Mheg started");
        } catch (Exception e) {
            Log.e(TAG, "mhegStartService = " + e.getMessage());
        }
        return quiet;
    }

    private void mhegStop() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Mheg.stop", args);
            Log.e(TAG, "Mheg stopped");
        } catch (Exception e) {
            Log.e(TAG, "mhegStop = " + e.getMessage());
        }
    }
    private boolean mhegKeypress(int keyCode) {
      boolean used=false;
        try {
            JSONArray args = new JSONArray();
            args.put(keyCode);
            used = DtvkitGlueClient.getInstance().request("Mheg.notifyKeypress", args).getBoolean("data");
            Log.e(TAG, "Mheg keypress, used:" + used);
        } catch (Exception e) {
            Log.e(TAG, "mhegKeypress = " + e.getMessage());
        }
        return used;
    }

    private final ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onChannelsChanged();
        }
    };

    public void cancelScan() {
        Log.d(TAG, "cancel scan");

        /*
        add dtvkit stop scan api, and unregist handle
         */
        DtvkitStopScan(mProtocol);
        //mStatus = ScanStatusType.USER_ABORT;
        //updateStatus(mStatus);
    }

    public int getScanStatus() {
        Log.d(TAG, "get scan status");

        //mCounts.put(ChannelType.ATV, mAtvCount);
        //mCounts.put(ChannelType.DTV, mDtvCount);
        //mCounts.put(ChannelType.IP, mIpCount);

        return 0;//new ScanStatus(mComplete, mStatus, mCounts);
    }

    private void reset() {
        Log.d(TAG, "reset");
        //mStatus = ScanStatusType.NO_SCAN;
        //mComplete = 0;
        //mAtvCount = 0;
        //mDtvCount = 0;
        //mIpCount = 0;
        //mCounts.clear();
    }

    public  class ScanType {
        public static final int SCAN_DTV_AUTO = 0x1;
        public static final int SCAN_DTV_MANUAL = 0x2;
        public static final int SCAN_DTV_ALLBAND = 0x3;
        public static final int SCAN_DTV_NONE = 0x7;

        public static final int SCAN_ATV_AUTO = 0x1;
        public static final int SCAN_ATV_MANUAL = 0x2;
        public static final int SCAN_ATV_FREQ = 0x3;
        public static final int SCAN_ATV_NONE = 0x7;

        public static final int SCAN_ATV_AUTO_FREQ_LIST = 0x0; /* 0: freq table list sacn mode */
        public static final int SCAN_ATV_AUTO_ALL_BAND = 0x1;  /* 1: all band sacn mode */

        public ScanType() {}
    }


   public enum ScanProtocol {
        AIR(0),
        CABLE(1),
        ALL(2);

        private int val;

        ScanProtocol(int val) {
            this.val = val;
        }

        public int toInt() {
            return this.val;
        }
    }

    public void startAutoScan(ScanProtocol protocol, ScanType type) {
        /*
        need add check dtvkit is playing or stoping, if true, we need retry
         */
        if (false) {
            Log.d(TAG, "startAutoScan block, wait for=" + retry_times + ", timeout is 200ms");
            if (retry_times > 0) {
                doStartAutoScaninHandle(protocol, type);
                retry_times--;
                return ;
            }
        } else {
            Log.d(TAG, "startAutoScan is ok");
            doStartAutoScan(protocol, type);
        }
    }

    private final class DtvScanHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            Log.d(TAG, "AmlTvScanHandler, msg.what=" + message.what);
            switch (message.what) {
                case MSG_DO_TRY_SCAN:
                    mDtvScanHandler.removeMessages(MSG_DO_TRY_SCAN);
                    SomeArgs args = (SomeArgs) message.obj;
                    startAutoScan((ScanProtocol)args.arg1, (ScanType)args.arg2);
                    break;
            }
        }
    }

    private void doStartAutoScan(ScanProtocol protocol, ScanType type) {
        reset();
        String scanProtocol;
        switch (protocol) {
            case AIR:
                scanProtocol = TvContract.Channels.TYPE_DVB_T;
                break;
            case CABLE:
                scanProtocol = TvContract.Channels.TYPE_DVB_S;
                break;
            case ALL:
                // TODO: scan all for real
                scanProtocol = TvContract.Channels.TYPE_DVB_T;
                break;
            default:
                scanProtocol = TvContract.Channels.TYPE_DVB_T;
                break;
        }

        // start scan
        /*
        add dtvkit scan api and regist handle
         */
        mProtocol = scanProtocol;
        Log.d(TAG, "doStartAutoScan");
        DtvkitStartScan(mProtocol, null);
        //mStatus = ScanStatusType.IN_PROG;
        //updateStatus(mStatus);
    }

    public final class SomeArgs {
         public Object arg1;
         public Object arg2;
         public SomeArgs() {

         }
         public void clear() {
             arg1 = null;
             arg2 = null;
         }
     }

    private void doStartAutoScaninHandle(ScanProtocol protocol, ScanType type) {
        Log.d(TAG, "doStartAutoScaninHandle");
        Message message = mDtvScanHandler.obtainMessage();
        message.what = MSG_DO_TRY_SCAN;
        SomeArgs args = new SomeArgs();
        synchronized(this) {
            args.arg1 = protocol;
            args.arg2 = type;
            message.obj = args;
        }
        mDtvScanHandler.sendMessageDelayed(message, 200);
    }
        /*
        add dtvkit scan api
     */
    /*
        protocol  : TvContract.Channels.TYPE_DVB_T or TvContract.Channels.TYPE_DVB_S
        scanParam : scan param for dtvt or dvbs.
     */
    private int DtvkitStartScan(String protocol, JSONObject scanParam) {
        int ret = 0;
        Log.d(TAG, "DtvkitStartScan:"+ " protocol" + protocol);
        //mDtvkitDvbScan.setScannerListener(this);
        mDtvkitDvbScan.startSearch(protocol, scanParam);
        return ret;
    }
     /*
        protocol  : TvContract.Channels.TYPE_DVB_T or TvContract.Channels.TYPE_DVB_S
        mHandler  : used to got scan event
     */
    private int DtvkitStopScan(String protocol) {
        int ret = 0;
        Log.d(TAG, "DtvkitStopScan:"+ " protocol" + protocol);
        mDtvkitDvbScan.stopScan(protocol);
        return ret;
    }
        /*
        inputId  : used for store channel to tv.db, can not null
     */
    public int DtvkitStartSyncDb(String inputId) {
        int ret = 0;
        Log.d(TAG, "DtvkitStartSyncDb:"+ " inputId" + inputId);
        ret = mDtvkitDvbScan.StartSyncDb(this, inputId);
        return ret;
    }
        /*
     */
    public int DtvkitStopSyncDb() {
        int ret = 0;
        ret = mDtvkitDvbScan.StopSyncDb(this);
        return ret;
    }

   private boolean recordingAddRecording(String dvbUri, boolean eventTriggered, long duration, StringBuffer response) {
       try {
           JSONArray args = new JSONArray();
           args.put(dvbUri);
           args.put(eventTriggered);
           args.put(duration);
           response.insert(0, DtvkitGlueClient.getInstance().request("Recording.addScheduledRecording", args).getString("data"));
           return true;
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           response.insert(0, e.getMessage());
           return false;
       }
   }

   private boolean checkActiveRecording() {
        return checkActiveRecording(recordingGetStatus());
   }

   private boolean checkActiveRecording(JSONObject recordingStatus) {
        boolean active = false;

        if (recordingStatus != null) {
            try {
                active = recordingStatus.getBoolean("active");
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return active;
   }

   private JSONObject recordingGetStatus() {
       JSONObject response = null;
       try {
           JSONArray args = new JSONArray();
           response = DtvkitGlueClient.getInstance().request("Recording.getStatus", args).getJSONObject("data");
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
       }
       return response;
   }

   private boolean recordingStopRecording(String dvbUri) {
       try {
           JSONArray args = new JSONArray();
           args.put(dvbUri);
           DtvkitGlueClient.getInstance().request("Recording.stopRecording", args);
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           return false;
       }
       return true;
   }

   private boolean recordingCheckAvailability(String dvbUri) {
       try {
           JSONArray args = new JSONArray();
           args.put(dvbUri);
           DtvkitGlueClient.getInstance().request("Recording.checkAvailability", args);
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           return false;
       }
       return true;
   }

   private String getProgramInternalRecordingUri() {
        return getProgramInternalRecordingUri(recordingGetStatus());
   }

   private String getProgramInternalRecordingUri(JSONObject recordingStatus) {
        String uri = "dvb://0000.0000.0000.0000;0000";
        if (recordingStatus != null) {
           try {
               JSONArray activeRecordings = recordingStatus.getJSONArray("activerecordings");
               if (activeRecordings.length() == 1)
               {
                   uri = activeRecordings.getJSONObject(0).getString("uri");
               }
           } catch (JSONException e) {
               Log.e(TAG, e.getMessage());
           }
       }
       return uri;
   }

   private JSONArray recordingGetActiveRecordings() {
       return recordingGetActiveRecordings(recordingGetStatus());
   }

   private JSONArray recordingGetActiveRecordings(JSONObject recordingStatus) {
       JSONArray activeRecordings = null;
       if (recordingStatus != null) {
            try {
                activeRecordings = recordingStatus.getJSONArray("activerecordings");
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
       }
       return activeRecordings;
   }

   private int recordingGetNumActiveRecordings() {
        int numRecordings = 0;
        JSONArray activeRecordings = recordingGetActiveRecordings();
        if (activeRecordings != null) {
            numRecordings = activeRecordings.length();
        }
        return numRecordings;
   }

   private int recordingGetNumRecorders() {
       int numRecorders = 0;
       try {
           JSONArray args = new JSONArray();
           numRecorders = DtvkitGlueClient.getInstance().request("Recording.getNumberOfRecorders", args).getInt("data");
           Log.i(TAG, "numRecorders: " + numRecorders);
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
       }
       return numRecorders;
   }

   private JSONArray recordingGetListOfRecordings() {
       JSONArray recordings = null;
       try {
           JSONArray args = new JSONArray();
           recordings = DtvkitGlueClient.getInstance().request("Recording.getListOfRecordings", args).getJSONArray("data");
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
       }
       return recordings;
   }

   private boolean recordingRemoveRecording(String uri) {
       try {
           JSONArray args = new JSONArray();
           args.put(uri);
           DtvkitGlueClient.getInstance().request("Recording.removeRecording", args);
           return true;
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           return false;
       }
   }

   private boolean checkRecordingExists(String uri, Cursor cursor) {
        boolean recordingExists = false;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                RecordedProgram recordedProgram = RecordedProgram.fromCursor(cursor);
                if (recordedProgram.getRecordingDataUri().equals(uri)) {
                    recordingExists = true;
                    break;
                }
            } while (cursor.moveToNext());
        }
        return recordingExists;
   }

   private JSONArray recordingGetListOfScheduledRecordings() {
       JSONArray scheduledRecordings = null;
       try {
           JSONArray args = new JSONArray();
           scheduledRecordings = DtvkitGlueClient.getInstance().request("Recording.getListOfScheduledRecordings", args).getJSONArray("data");
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
       }
       return scheduledRecordings;
   }

   private String getScheduledRecordingUri(JSONObject scheduledRecording) {
        String uri = "dvb://0000.0000.0000;0000";
        if (scheduledRecording != null) {
            try {
                uri = scheduledRecording.getString("uri");
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return uri;
   }

   private boolean recordingRemoveScheduledRecording(String uri) {
       try {
           JSONArray args = new JSONArray();
           args.put(uri);
           DtvkitGlueClient.getInstance().request("Recording.removeScheduledRecording", args);
           return true;
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           return false;
       }
   }

    private final ContentObserver mRecordingsContentObserver = new ContentObserver(new Handler()) {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onChange(boolean selfChange) {
            onRecordingsChanged();
        }
    };

   @RequiresApi(api = Build.VERSION_CODES.N)
   private void onRecordingsChanged() {
       Log.i(TAG, "onRecordingsChanged");

       new Thread(new Runnable() {
           @Override
           public void run() {
               Cursor cursor = mContentResolver.query(TvContract.RecordedPrograms.CONTENT_URI, RecordedProgram.PROJECTION, null, null, TvContract.RecordedPrograms._ID + " DESC");
               JSONArray recordings = recordingGetListOfRecordings();
               JSONArray activeRecordings = recordingGetActiveRecordings();

               if (recordings != null && cursor != null) {
                   for (int i = 0; i < recordings.length(); i++) {
                       try {
                           String uri = recordings.getJSONObject(i).getString("uri");

                           if (activeRecordings != null && activeRecordings.length() > 0) {
                               boolean activeRecording = false;
                               for (int j = 0; j < activeRecordings.length(); j++) {
                                   if (uri.equals(activeRecordings.getJSONObject(j).getString("uri"))) {
                                       activeRecording = true;
                                       break;
                                   }
                               }
                               if (activeRecording) {
                                   continue;
                               }
                           }

                           if (!checkRecordingExists(uri, cursor)) {
                               recordingRemoveRecording(uri);
                           }

                       } catch (JSONException e) {
                           Log.e(TAG, e.getMessage());
                       }
                   }
               }
           }
       }).start();
    }

    private void scheduleTimeshiftRecordingTask() {
       final long SCHEDULE_TIMESHIFT_RECORDING_DELAY_MILLIS = 1000 * 2;
       Log.i(TAG, "calling scheduleTimeshiftRecordingTask");
       if (scheduleTimeshiftRecordingHandler == null) {
            scheduleTimeshiftRecordingHandler = new Handler(Looper.getMainLooper());
       } else {
            scheduleTimeshiftRecordingHandler.removeCallbacks(timeshiftRecordRunnable);
       }
       scheduleTimeshiftRecordingHandler.postDelayed(timeshiftRecordRunnable, SCHEDULE_TIMESHIFT_RECORDING_DELAY_MILLIS);
    }

    private void removeScheduleTimeshiftRecordingTask() {
        Log.i(TAG, "calling removeScheduleTimeshiftRecordingTask");
        if (scheduleTimeshiftRecordingHandler != null) {
            scheduleTimeshiftRecordingHandler.removeCallbacks(timeshiftRecordRunnable);
        }
    }

    private HardwareCallback mHardwareCallback = new HardwareCallback(){
        @Override
        public void onReleased() {
            Log.d(TAG, "onReleased");
            mHardware = null;
        }

        @Override
        public void onStreamConfigChanged(TvStreamConfig[] configs) {
            Log.d(TAG, "onStreamConfigChanged");
            mConfigs = configs;
        }
    };

    public ResolveInfo getResolveInfo(String cls_name) {
        if (TextUtils.isEmpty(cls_name))
            return null;
        ResolveInfo ret_ri = null;
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(new Intent(TvInputService.SERVICE_INTERFACE),
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
        for (ResolveInfo ri : services) {
            ServiceInfo si = ri.serviceInfo;
            if (!android.Manifest.permission.BIND_TV_INPUT.equals(si.permission)) {
                continue;
            }
            Log.d(TAG, "cls_name = " + cls_name + ", si.name = " + si.name);
            if (cls_name.equals(si.name)) {
                ret_ri = ri;
                break;
            }
        }
        return ret_ri;
    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        Log.d(TAG, "onHardwareAdded ," + "DeviceId :" + hardwareInfo.getDeviceId());
        if (hardwareInfo.getDeviceId() != 19)
            return null;
        ResolveInfo rinfo = getResolveInfo(DtvkitTvInput.class.getName());
        if (rinfo != null) {
            try {
            mTvInputInfo = TvInputInfo.createTvInputInfo(getApplicationContext(), rinfo, hardwareInfo, null, null);
            } catch (XmlPullParserException e) {
                //TODO: handle exception
            } catch (IOException e) {
                //TODO: handle exception
            }
        }
        setInputId(mTvInputInfo.getId());
        mHardware = mTvInputManager.acquireTvInputHardware(19,mHardwareCallback,mTvInputInfo);
        return mTvInputInfo;
    }

    public String onHardwareRemoved(TvInputHardwareInfo hardwareInfo) {
        Log.d(TAG, "onHardwareRemoved");
        if (hardwareInfo.getDeviceId() != 19)
            return null;
        String id = null;
        if (mTvInputInfo != null) {
            id = mTvInputInfo.getId();
            mTvInputInfo = null;
        }
        return id;
    }

}
