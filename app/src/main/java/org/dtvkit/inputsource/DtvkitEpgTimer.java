package org.dtvkit.inputsource;

import android.content.ComponentName;
import android.content.Context;
import android.media.tv.TvContract;
import android.util.Log;

import org.dtvkit.companionlibrary.EpgSyncJobService;

import java.util.Timer;
import java.util.TimerTask;

public class DtvkitEpgTimer {
    private static final String TAG = "DtvkitEpgTimer";

    private static final long INITIAL_DECODING_MILLIS = 1000 * 60 * 5; // 5 minutes
    private static final long BUFFER_MILLIS = 1000 * 15; // 15 seconds
    private static final long PERIOD_MILLIS = 1000 * 60 * 15; // 15 minutes

    private Context mContext;
    private Timer mTimer = null;
    private boolean mInitialDecodingCompleted = false;
    private long mDecodingStartedUtc = 0;
    private long mDecodingMillis = 0;

    DtvkitEpgTimer(Context context)
    {
        mContext = context;
    }

    public void notifyDecodingStarted() {
        Log.i(TAG, "notifyDecodingStarted");
        if (!mInitialDecodingCompleted) {
            if (mDecodingStartedUtc == 0) {
                mDecodingStartedUtc = System.currentTimeMillis();
            }
            long delay = Math.max(0, INITIAL_DECODING_MILLIS - mDecodingMillis) + BUFFER_MILLIS;
            Log.i(TAG, "Decoding started. Scheduling/rescheduling task with delay " + delay);
            rescheduleTask(delay, PERIOD_MILLIS);
        }
    }

    public void notifyDecodingStopped() {
        Log.i(TAG, "notifyDecodingStopped");
        if (!mInitialDecodingCompleted) {
            if (mDecodingStartedUtc != 0) {
                long millis = System.currentTimeMillis() - mDecodingStartedUtc;
                if (millis >= INITIAL_DECODING_MILLIS) {
                    Log.i(TAG, "Added " + millis + " millis");
                    mDecodingMillis += millis;
                }
                mDecodingStartedUtc = 0;
            }
        }
    }

    public void cancel() {
    }

    private void rescheduleTask(long delay, long period) {
        if (mTimer != null) {
            mTimer.cancel();
        }

        TimerTask task = new TimerTask() {
            public void run() {
                if (!mInitialDecodingCompleted) {
                    if (mDecodingStartedUtc != 0) {
                        long millis = System.currentTimeMillis() - mDecodingStartedUtc;
                        if (millis >= INITIAL_DECODING_MILLIS) {
                            Log.i(TAG, "Added " + millis + " millis");
                            mDecodingMillis += millis;
                        }
                        mDecodingStartedUtc = System.currentTimeMillis();
                    }

                    if (mDecodingMillis >= INITIAL_DECODING_MILLIS) {
                        mInitialDecodingCompleted = true;
                    } else {
                        long delay = Math.max(0, INITIAL_DECODING_MILLIS - mDecodingMillis) + BUFFER_MILLIS;
                        Log.i(TAG, "Initial decoding not completed. Rescheduling task with delay " + delay);
                        rescheduleTask(delay, PERIOD_MILLIS);
                        return;
                    }
                }

                Log.i(TAG, "Requesting sync!");
                String inputId = TvContract.buildInputId(new ComponentName(mContext, DtvkitTvInput.class));
                ComponentName sync = new ComponentName(mContext, DtvkitEpgSync.class);
                EpgSyncJobService.requestImmediateSync(mContext, inputId, 1000 * 60 * 60 * 2, false, sync);
            }
        };

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(task, delay, period);
    }
}
