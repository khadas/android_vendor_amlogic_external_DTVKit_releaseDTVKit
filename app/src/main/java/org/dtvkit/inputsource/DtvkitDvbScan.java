package org.dtvkit.inputsource;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

import android.media.tv.TvContract;

public class DtvkitDvbScan {

    private static final String TAG = "DtvkitDvbScan";
    private ScannerEventListener mScannerListener = null;

    public final static int EVENT_SCAN_PROGRESS             = 0;
    public final static int EVENT_STORE_BEGIN               = 1;
    public final static int EVENT_STORE_END                 = 2;
    public final static int EVENT_SCAN_END                  = 3;
    public final static int EVENT_BLINDSCAN_PROGRESS        = 4;
    public final static int EVENT_BLINDSCAN_NEWCHANNEL      = 5;
    public final static int EVENT_BLINDSCAN_END             = 6;
    public final static int EVENT_ATV_PROG_DATA             = 7;
    public final static int EVENT_DTV_PROG_DATA             = 8;
    public final static int EVENT_SCAN_EXIT                 = 9;
    public final static int EVENT_SCAN_BEGIN                = 10;
    public final static int EVENT_LCN_INFO_DATA             = 11;

    public class ScannerEvent {
        public int type;
        public int precent;
        public int totalcount;
    }

    public interface ScannerEventListener {
        void onEvent(ScannerEvent ev);
    }
    //scanner
    public void setScannerListener(ScannerEventListener l) {
        mScannerListener = l;
    }
    /*
    used for receive scan event
     */
    private final DtvkitGlueClient.SignalHandler mSignalEventHandler = new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(String signal, JSONObject data) {

            if (signal.equals("DvbtStatusChanged")) {
                int progress = 0;
                try {
                    progress = data.getInt("progress");
                } catch (JSONException ignore) {

                }
                Log.d(TAG, "progress:" + progress);
                if (progress < 100) {
                    int found = 0;
                    try
                    {
                        JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getNumberOfServices", new JSONArray());
                        found = obj.getInt("data");
                    }
                    catch (Exception ignore)
                    {
                        Log.e(TAG, ignore.getMessage());
                    }
                    ScannerEvent scan_ev = new ScannerEvent();
                    scan_ev.type = EVENT_SCAN_PROGRESS;
                    scan_ev.precent = progress;
                    scan_ev.totalcount = found;
                    if (mScannerListener != null)
                        mScannerListener.onEvent(scan_ev);
                } else {
                    // scan end, need stop dtvkit scan
                    // Log.d(TAG, "scan start store db");
                    ScannerEvent scan_ev = new ScannerEvent();
                    scan_ev.type = EVENT_STORE_BEGIN;
                    scan_ev.precent = progress;
                    if (mScannerListener != null)
                        mScannerListener.onEvent(scan_ev);
                }
            }
        }
    };
     /*
    used for sync tv.db, receiver sync event
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String status = intent.getStringExtra(EpgSyncJobService.SYNC_STATUS);
            if (status == EpgSyncJobService.SYNC_FINISHED) {
                // need stop sync
                Log.d(TAG, "EpgSyncJobService:: SYNC_FINISHED");
                // set scan status
                ScannerEvent scan_ev = new ScannerEvent();
                scan_ev.type = EVENT_SCAN_END;
                if (mScannerListener != null)
                    mScannerListener.onEvent(scan_ev);
            }
        }
    };

    private int startDvbtSearch() {
        int ret = 0;
        try {
            JSONArray args = new JSONArray();
            args.put(false); // Commit
            DtvkitGlueClient.getInstance().request("Dvbt.finishSearch", args);
        } catch (Exception e) {
            ret = -1;
            return ret;
        }

        try {
            JSONArray args = new JSONArray();
            args.put(true); // retune
            DtvkitGlueClient.getInstance().request("Dvbt.startSearch", args);
        } catch (Exception e) {
            ret = -1;
            return ret;
        }
        return ret;
    }
    private int stopDvbtSearch() {
        int ret = 0;
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request("Dvbt.finishSearch", args);
        } catch (Exception e) {
            ret = -1;
            return ret;
        }
        return ret;
    }

    private int stopDvbsSearch() {
        int ret = 0;
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request("Dvbs.finishSearch", args);
        } catch (Exception e) {
            ret = -1;
            return ret;
        }
        return ret;
    }

    public int startSearch(String protocol, JSONObject scanParam) {
        int ret = 0;
        if (protocol == null || mSignalEventHandler == null) {
            ret = -1;
            Log.d(TAG, "startsearch error protocol or mHandler is null");
            return ret;
        }

        switch (protocol) {
            case TvContract.Channels.TYPE_DVB_S:
                startMonitoringSearch(mSignalEventHandler);
                ret = startDvbtSearch();
                break;
            case TvContract.Channels.TYPE_DVB_T:
                startMonitoringSearch(mSignalEventHandler);
                ret = startDvbtSearch();
                break;
            default:
                // TODO: scan all for real
                ret = -1;
                Log.d(TAG, "startsearch error protocol not found");
                break;
        }
        if (ret != 0) {
            Log.d(TAG, "startsearch error");
        }
        ScannerEvent scan_ev = new ScannerEvent();
        scan_ev.type = EVENT_SCAN_BEGIN;
        if (mScannerListener != null)
            mScannerListener.onEvent(scan_ev);
        return ret;
    }

    public int stopScan(String protocol) {
        int ret = 0;
        stopMonitoringSearch(mSignalEventHandler);
        switch (protocol) {
            case TvContract.Channels.TYPE_DVB_S:
                stopDvbsSearch();
                break;
            case TvContract.Channels.TYPE_DVB_T:
                stopDvbtSearch();
                break;
            default:
                // TODO: scan all for real
                ret = -1;
                Log.d(TAG, "stopScan error protocol not found");
                break;
        }
        return ret;
    }

    public int StartSyncDb(Context context, String inputId) {
        int ret = 0;
        if (inputId == null || context == null) {
            ret = -1;
            return ret;
        }
        startMonitoringSync(context);
        // By default, gets all channels and 1 hour of programs (DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS)
        EpgSyncJobService.cancelAllSyncRequests(context);
        Log.d(TAG, String.format("StartSyncDb inputId: %s", inputId));
        EpgSyncJobService.requestImmediateSync(context, inputId, true, new ComponentName(context, DtvkitEpgSync.class)); // 12 hours
        return ret;
    }

    public int StopSyncDb(Context context) {
        int ret = 0;
        if (context == null) {
            ret = -1;
            return ret;
        }
        stopMonitoringSync(context);
        EpgSyncJobService.cancelAllSyncRequests(context);
        return ret;
    }
    private int startMonitoringSearch(DtvkitGlueClient.SignalHandler mHandler) {
        int ret = 0;
        if (mHandler == null) {
            ret = -1;
            Log.d(TAG, "stopMonitoringSearch error, mHandler is null");
            return ret;
        }
        DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
        return ret;
    }

    private int stopMonitoringSearch(DtvkitGlueClient.SignalHandler mHandler) {
        int ret = 0;
        if (mHandler == null) {
            ret = -1;
            Log.d(TAG, "stopMonitoringSearch error, mHandler is null");
            return ret;
        }
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
        return ret;
    }

    private void startMonitoringSync(Context context) {
        LocalBroadcastManager.getInstance(context).registerReceiver(mReceiver,
                new IntentFilter(EpgSyncJobService.ACTION_SYNC_STATUS_CHANGED));
    }

    private void stopMonitoringSync(Context context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mReceiver);
    }
}
