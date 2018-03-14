package org.dtvkit.inputsource;

import org.dtvkit.inputsource.R;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

public class DtvkitDvbsSetup extends Activity {
    private static final String TAG = "DtvkitDvbsSetup";

    private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(String signal, JSONObject data) {
            if (signal == "DvbtStatusChanged") {
                int progress = 0;
                try {
                    progress = data.getInt("progress");
                } catch (JSONException ignore) {
                }

                if (progress < 100) {
                    getProgressBar().setProgress(progress);
                    setSearchStatus(String.format(Locale.ENGLISH, "Searching (%d%%)", progress));
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
            if (status.equals(EpgSyncJobService.SYNC_FINISHED)) {
                setSearchStatus("Finished");
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup);

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
        setSearchStatus("Searching");
        getProgressBar().setIndeterminate(false);
        startMonitoringSearch();

        CheckBox network = (CheckBox)findViewById(R.id.network);
        EditText frequency = (EditText)findViewById(R.id.frequency);
        Spinner satellite = (Spinner)findViewById(R.id.satellite);
        Spinner polarity = (Spinner)findViewById(R.id.polarity);
        EditText symbolrate = (EditText)findViewById(R.id.symbolrate);
        Spinner fec = (Spinner)findViewById(R.id.fec);
        CheckBox dvbs2 = (CheckBox)findViewById(R.id.dvbs2);
        Spinner modulation = (Spinner)findViewById(R.id.modulation);

        try {
            JSONArray args = new JSONArray();
            args.put(network.isChecked());
            args.put(Integer.parseInt(frequency.getText().toString()));
            args.put(satellite.getSelectedItem().toString());
            args.put(polarity.getSelectedItem().toString());
            args.put(Integer.parseInt(symbolrate.getText().toString()));
            args.put(fec.getSelectedItem().toString());
            args.put(dvbs2.isChecked());
            args.put(modulation.getSelectedItem().toString());

            Log.i(TAG, args.toString());

            DtvkitGlueClient.getInstance().request("Dvbs.startManualSearch", args);
        } catch (Exception e) {
            stopMonitoringSearch();
            setSearchStatus(e.getMessage());
        }
    }

    private void onSearchFinished() {
        setSearchStatus("Finishing search");
        getProgressBar().setIndeterminate(true);
        stopMonitoringSearch();
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request("Dvbs.finishSearch", args);
        } catch (Exception e) {
            stopMonitoringSearch();
            setSearchStatus(e.getMessage());
            return;
        }

        setSearchStatus("Updating guide");
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

    private void setSearchStatus(String status) {
        Log.i(TAG, String.format("Search status \"%s\"", status));
        final TextView text = (TextView) findViewById(R.id.searchstatus);
        text.setText(status);
    }

    private ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.searchprogress);
    }
}
