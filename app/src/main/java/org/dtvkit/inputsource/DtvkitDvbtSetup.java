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
import android.text.Editable;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

public class DtvkitDvbtSetup extends Activity {
    private static final String TAG = "DtvkitDvbtSetup";

    private boolean mIsDvbt = false;
    private DataMananer mDataMananer;

    private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(String signal, JSONObject data) {

            if ((mIsDvbt && signal.equals("DvbtStatusChanged")) || (!mIsDvbt && signal.equals("DvbcStatusChanged"))) {
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

        Button search = (Button)findViewById(R.id.startsearch);
        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                findViewById(R.id.startsearch).setEnabled(false);
                startSearch();
            }
        });
        search.requestFocus();
        Intent intent = getIntent();
        if (intent != null) {
            mIsDvbt = intent.getBooleanExtra(DataMananer.KEY_IS_DVBT, false);
        }
        ((TextView)findViewById(R.id.description)).setText(mIsDvbt ? R.string.strSearchDvbtDescription : R.string.strSearchDvbcDescription);
        mDataMananer = new DataMananer(this);
        initOrUpdateView(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoringSearch();
        stopMonitoringSync();
    }

    private void initOrUpdateView(boolean init) {
        LinearLayout public_typein_containner = (LinearLayout)findViewById(R.id.public_typein_containner);
        TextView public_type_in = (TextView)findViewById(R.id.public_typein_text);
        EditText public_type_edit = (EditText)findViewById(R.id.public_typein_edit);
        LinearLayout dvbt_bandwidth_containner = (LinearLayout)findViewById(R.id.dvbt_bandwidth_containner);
        Spinner dvbt_bandwidth_spinner = (Spinner)findViewById(R.id.dvbt_bandwidth_spinner);
        LinearLayout dvbt_mode_containner = (LinearLayout)findViewById(R.id.dvbt_mode_containner);
        Spinner dvbt_mode_spinner = (Spinner)findViewById(R.id.dvbt_mode_spinner);
        LinearLayout dvbt_type_containner = (LinearLayout)findViewById(R.id.dvbt_type_containner);
        Spinner dvbt_type_spinner = (Spinner)findViewById(R.id.dvbt_type_spinner);
        LinearLayout dvbc_mode_containner = (LinearLayout)findViewById(R.id.dvbc_mode_containner);
        Spinner dvbc_mode_spinner = (Spinner)findViewById(R.id.dvbc_mode_spinner);
        LinearLayout dvbc_symbol_containner = (LinearLayout)findViewById(R.id.dvbc_symbol_containner);
        EditText dvbc_symbol_edit = (EditText)findViewById(R.id.dvbc_symbol_edit);
        LinearLayout public_search_mode_containner = (LinearLayout)findViewById(R.id.public_search_mode_containner);
        Spinner public_search_mode_spinner = (Spinner)findViewById(R.id.public_search_mode_spinner);
        LinearLayout frequency_channel_container = (LinearLayout)findViewById(R.id.frequency_channel_container);
        Spinner frequency_channel_spinner = (Spinner)findViewById(R.id.frequency_channel_spinner);
        Button search = (Button)findViewById(R.id.startsearch);

        int isFrequencyMode = mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY);
        if (isFrequencyMode == DataMananer.VALUE_FREQUENCY_MODE) {
            public_type_in.setText(R.string.search_frequency);
            public_type_edit.setHint(R.string.search_frequency_hint);
        } else {
            public_type_in.setText(R.string.search_number);
            public_type_edit.setHint(R.string.search_number_hint);
        }
        public_type_edit.setText("");
        int value = mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE);
        public_search_mode_spinner.setSelection(value);
        if (DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO == value) {
            public_typein_containner.setVisibility(View.GONE);
            dvbt_bandwidth_containner.setVisibility(View.GONE);
            dvbt_mode_containner.setVisibility(View.GONE);
            dvbt_type_containner.setVisibility(View.GONE);
            dvbc_mode_containner.setVisibility(View.GONE);
            dvbc_symbol_containner.setVisibility(View.GONE);
            frequency_channel_container.setVisibility(View.GONE);
            search.setText(R.string.strAutoSearch);
        } else {
            search.setText(R.string.strManualSearch);
            public_typein_containner.setVisibility(View.VISIBLE);
            frequency_channel_container.setVisibility(View.VISIBLE);
            frequency_channel_spinner.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY));
            if (mIsDvbt) {
                dvbt_bandwidth_containner.setVisibility(View.VISIBLE);
                dvbt_mode_containner.setVisibility(View.VISIBLE);
                dvbt_type_containner.setVisibility(View.VISIBLE);
                dvbc_symbol_containner.setVisibility(View.GONE);
                dvbc_mode_containner.setVisibility(View.GONE);
                dvbt_bandwidth_spinner.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_DVBT_BANDWIDTH));
                dvbt_mode_spinner.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_DVBT_MODE));
                dvbt_type_spinner.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE));
            } else {
                dvbt_bandwidth_containner.setVisibility(View.GONE);
                dvbt_mode_containner.setVisibility(View.GONE);
                dvbt_type_containner.setVisibility(View.GONE);
                dvbc_symbol_containner.setVisibility(View.VISIBLE);
                dvbc_mode_containner.setVisibility(View.VISIBLE);
                dvbc_mode_spinner.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_DVBC_MODE));
                dvbc_symbol_edit.setText(mDataMananer.getIntParameters(DataMananer.KEY_DVBC_SYMBOL_RATE) + "");
            }
        }
        if (init) {//init one time
            dvbt_bandwidth_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "dvbt_bandwidth_spinner onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBT_BANDWIDTH, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            dvbt_mode_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "dvbt_mode_spinner onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBT_MODE, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            dvbt_type_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "dvbt_type_spinner onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBT_TYPE, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            dvbc_mode_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "dvbc_mode_spinner onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBC_MODE, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            public_search_mode_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE)) {
                        Log.d(TAG, "public_search_mode_spinner select same position = " + position);
                        return;
                    }
                    Log.d(TAG, "public_search_mode_spinner onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE, position);
                    initOrUpdateView(false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            frequency_channel_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY)) {
                        Log.d(TAG, "frequency_channel_container select same position = " + position);
                        return;
                    }
                    Log.d(TAG, "frequency_channel_container onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_IS_FREQUENCY, position);
                    initOrUpdateView(false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
        }
    }

    private JSONArray initSearchParameter(JSONArray args) {
        if (args != null) {
            if (!(DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO == mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE))) {
                String parameter = getParameter();
                if (!TextUtils.isEmpty(parameter)) {
                    int isfrequencysearch = mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY);
                    if (mIsDvbt) {
                        args.put(Integer.valueOf(parameter));
                        if (DataMananer.VALUE_FREQUENCY_MODE == isfrequencysearch) {
                            args.put(DataMananer.VALUE_DVBT_BANDWIDTH_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_BANDWIDTH)]);
                            args.put(DataMananer.VALUE_DVBT_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_MODE)]);
                            args.put(DataMananer.VALUE_DVBT_TYPE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE)]);
                        }
                    } else {
                        args.put(Integer.valueOf(parameter));
                        if (DataMananer.VALUE_FREQUENCY_MODE == isfrequencysearch) {
                            args.put(DataMananer.VALUE_DVBC_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBC_MODE)]);
                            args.put(mDataMananer.getIntParameters(DataMananer.KEY_DVBC_SYMBOL_RATE));
                        }
                    }
                    return args;
                } else {
                    return null;
                }
            } else {
                return args;
            }
        } else {
            return null;
        }
    }

    private String getParameter() {
        EditText public_type_edit = (EditText)findViewById(R.id.public_typein_edit);
        Editable editable = public_type_edit.getText();
        if (editable != null) {
            String value = editable.toString();
            if (!TextUtils.isEmpty(value) && TextUtils.isDigitsOnly(value)) {
                return value;
            }
        }
        return null;
    }

    private void startSearch() {
        setSearchStatus("Searching", "");
        setSearchProgressIndeterminate(false);
        startMonitoringSearch();

        try {
            JSONArray args = new JSONArray();
            args.put(false); // Commit
            DtvkitGlueClient.getInstance().request(mIsDvbt ? "Dvbt.finishSearch" : "Dvbc.finishSearch", args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return;
        }

        try {
            JSONArray args = new JSONArray();
            args.put(true); // retune
            args = initSearchParameter(args);
            if (args != null) {
                String command = null;
                int searchmode = mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE);
                int isfrequencysearch = mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY);
                if (!(DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO == searchmode)) {
                    if (isfrequencysearch == DataMananer.VALUE_FREQUENCY_MODE) {
                        command = (mIsDvbt ? "Dvbt.startManualSearchByFreq" : "Dvbc.startManualSearchByFreq");
                    } else {
                        command = (mIsDvbt ? "Dvbt.startManualSearchById" : "Dvbc.startManualSearchById");
                    }
                } else {
                    command = (mIsDvbt ? "Dvbt.startSearch" : "Dvbc.startSearch");
                }
                Log.d(TAG, "command = " + command + ", args = " + args.toString());
                DtvkitGlueClient.getInstance().request(command, args);
            } else {
                stopMonitoringSearch();
                setSearchStatus("parameter not complete", "");
                stopSearch();
            }
        } catch (Exception e) {
            stopMonitoringSearch();
            setSearchStatus("Failed to start search", e.getMessage());
            stopSearch();
        }
    }

    private void stopSearch() {
        findViewById(R.id.startsearch).setEnabled(true);
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request(mIsDvbt ? "Dvbt.finishSearch" : "Dvbc.finishSearch", args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return;
        }
    }

    private void onSearchFinished() {
        setSearchStatus("Finishing search", "");
        setSearchProgressIndeterminate(true);
        stopMonitoringSearch();
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request(mIsDvbt ? "Dvbt.finishSearch" : "Dvbc.finishSearch", args);
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
