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

public class DtvkitDvbtSetup extends Activity {
    private static final String TAG = "DtvkitDvbtSetup";

    private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(String signal, JSONObject data) {

            if (signal.equals("DvbtStatusChanged")) {
                int progress = 0;
                try {
                    progress = data.getInt("progress");
                } catch (JSONException ignore) {
                }

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


                    setSearchProgress(progress);
                    setSearchStatus(String.format(Locale.ENGLISH, "Searching (%d%%)", progress), String.format(Locale.ENGLISH, "Found %d services", found));
                } else {
                    onSearchFinished();
                }
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String status = intent.getStringExtra(EpgSyncJobService.SYNC_STATUS);
            if (status == EpgSyncJobService.SYNC_FINISHED) {
                setSearchStatus("Finished", "");
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.autosetup);

        findViewById(R.id.startsearch).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startSearch();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoringSearch();
        stopMonitoringSync();
    }

    private void startSearch() {
        setSearchStatus("Searching", "");
        setSearchProgressIndeterminate(false);
        startMonitoringSearch();

        try {
            JSONArray args = new JSONArray();
            args.put(false); // Commit
            DtvkitGlueClient.getInstance().request("Dvbt.finishSearch", args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return;
        }

        try {
            JSONArray args = new JSONArray();
            args.put(true); // retune
            DtvkitGlueClient.getInstance().request("Dvbt.startSearch", args);
        } catch (Exception e) {
            stopMonitoringSearch();
            setSearchStatus("Failed to start search", e.getMessage());
        }
    }

    private void onSearchFinished() {
        setSearchStatus("Finishing search", "");
        setSearchProgressIndeterminate(true);
        stopMonitoringSearch();
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request("Dvbt.finishSearch", args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return;
        }

        setSearchStatus("Updating guide", "");
        startMonitoringSync();
        // By default, gets all channels and 1 hour of programs (DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS)
        EpgSyncJobService.cancelAllSyncRequests(this);

        String inputId = this.getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        Log.i(TAG, String.format("inputId: %s", inputId));
        EpgSyncJobService.requestImmediateSync(this, inputId, 1000 * 60 * 60 * 12, true, new ComponentName(this, DtvkitEpgSync.class)); // 12 hours
    }

    private void startMonitoringSearch() {
        DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
    }

    private void stopMonitoringSearch() {
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
    }

    private void startMonitoringSync() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(EpgSyncJobService.ACTION_SYNC_STATUS_CHANGED));
    }

    private void stopMonitoringSync() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    private void setSearchStatus(final String status, final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, String.format("Search status \"%s\"", status));
                final TextView text = (TextView) findViewById(R.id.searchstatus);
                text.setText(status);

                final TextView text2 = (TextView) findViewById(R.id.description);
                text2.setText(description);
            }
        });
    }

    private void setSearchProgress(final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ProgressBar bar = (ProgressBar) findViewById(R.id.searchprogress);
                bar.setProgress(progress);
            }
        });
    }

    private void setSearchProgressIndeterminate(final Boolean indeterminate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ProgressBar bar = (ProgressBar) findViewById(R.id.searchprogress);
                bar.setIndeterminate(indeterminate);
            }
        });
    }
}
