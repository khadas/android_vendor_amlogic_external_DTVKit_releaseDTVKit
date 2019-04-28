package org.dtvkit.inputsource;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.tv.TvContract;
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
import android.widget.ArrayAdapter;
import android.view.KeyEvent;

import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import com.droidlogic.fragment.ParameterMananer;

public class DtvkitDvbtSetup extends Activity {
    private static final String TAG = "DtvkitDvbtSetup";

    private boolean mIsDvbt = false;
    private DataMananer mDataMananer;
    private ParameterMananer mParameterMananer = null;

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
            if (status.equals(EpgSyncJobService.SYNC_FINISHED)) {
                setSearchStatus("Finished", "");
                finish();
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            stopMonitoringSearch();
            stopSearch();
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.autosetup);
        mParameterMananer = new ParameterMananer(this, DtvkitGlueClient.getInstance());
        final View startSearch = findViewById(R.id.terrestrialstartsearch);
        final View stopSearch = findViewById(R.id.terrestrialstopsearch);

        startSearch.setEnabled(true);
        startSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startSearch.setEnabled(false);
                stopSearch.setEnabled(true);
                stopSearch.requestFocus();
                startSearch();
            }
        });
        startSearch.requestFocus();
        Intent intent = getIntent();
        if (intent != null) {
            mIsDvbt = intent.getBooleanExtra(DataMananer.KEY_IS_DVBT, false);
        }
        ((TextView)findViewById(R.id.description)).setText(mIsDvbt ? R.string.strSearchDvbtDescription : R.string.strSearchDvbcDescription);

        stopSearch.setEnabled(false);
        stopSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSearchFinished();
            }
        });

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
        LinearLayout public_search_channel_name_containner = (LinearLayout)findViewById(R.id.public_search_channel_containner);
        Spinner public_search_channel_name_spinner = (Spinner)findViewById(R.id.public_search_channel_spinner);
        Button search = (Button)findViewById(R.id.terrestrialstartsearch);

        int isFrequencyMode = mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY);
        if (isFrequencyMode == DataMananer.VALUE_FREQUENCY_MODE) {
            public_type_in.setText(R.string.search_frequency);
            public_type_edit.setHint(R.string.search_frequency_hint);
            public_typein_containner.setVisibility(View.VISIBLE);
            public_search_channel_name_containner.setVisibility(View.GONE);
        } else {
            //public_type_in.setText(R.string.search_number);
            //public_type_edit.setHint(R.string.search_number_hint);//not needed
            public_typein_containner.setVisibility(View.GONE);
            public_search_channel_name_containner.setVisibility(View.VISIBLE);
            updateChannelNameContainer();
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
            public_search_channel_name_containner.setVisibility(View.GONE);
            search.setText(R.string.strAutoSearch);
        } else {
            search.setText(R.string.strManualSearch);
            if (isFrequencyMode == DataMananer.VALUE_FREQUENCY_MODE) {
                public_typein_containner.setVisibility(View.VISIBLE);
                public_search_channel_name_containner.setVisibility(View.GONE);
            } else {
                public_typein_containner.setVisibility(View.GONE);
                public_search_channel_name_containner.setVisibility(View.VISIBLE);
            }
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
                    if (mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE) == position) {
                        Log.d(TAG, "dvbt_type_spinner same position = " + position);
                        return;
                    }
                    Log.d(TAG, "dvbt_type_spinner onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBT_TYPE, position);
                    initOrUpdateView(false);
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
            public_search_channel_name_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "public_search_channel_name_spinner onItemSelected position = " + position);
                    if (mIsDvbt) {
                        mDataMananer.saveIntParameters(DataMananer.KEY_SEARCH_DVBT_CHANNEL_NAME, position);
                    } else {
                        mDataMananer.saveIntParameters(DataMananer.KEY_SEARCH_DVBC_CHANNEL_NAME, position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
        }
    }

    private void updateChannelNameContainer() {
        LinearLayout public_search_channel_name_containner = (LinearLayout)findViewById(R.id.public_search_channel_containner);
        Spinner public_search_channel_name_spinner = (Spinner)findViewById(R.id.public_search_channel_spinner);

        List<String> list = null;
        List<String> newlist = new ArrayList<String>();
        ArrayAdapter<String> adapter = null;
        int select = mIsDvbt ? mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_DVBT_CHANNEL_NAME) :
                mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_DVBC_CHANNEL_NAME);
        list = mParameterMananer.getChannelTable(mParameterMananer.getCurrentCountryCode(), mIsDvbt, mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE) == 1);
        for (String one : list) {
            String[] parameter = one.split(",");//first number, second string, third number
            if (parameter != null && parameter.length == 3) {
                String result = "NO." + parameter[0] + "  " + parameter[1] + "  " + parameter[2] + "Hz";
                newlist.add(result);
            }
        }
        if (list == null) {
            Log.d(TAG, "updateChannelNameContainer can't find channel freq table");
            return;
        }
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, newlist);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        public_search_channel_name_spinner.setAdapter(adapter);
        select = (select < list.size()) ? select : 0;
        public_search_channel_name_spinner.setSelection(select);
    }

    private JSONArray initSearchParameter(JSONArray args) {
        if (args != null) {
            if (!(DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO == mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE))) {
                String parameter = getParameter();
                if (!TextUtils.isEmpty(parameter)) {
                    int isfrequencysearch = mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY);
                    if (mIsDvbt) {
                        if (DataMananer.VALUE_FREQUENCY_MODE == isfrequencysearch) {
                            args.put(Integer.valueOf(parameter) * 1000000);//mhz
                            args.put(DataMananer.VALUE_DVBT_BANDWIDTH_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_BANDWIDTH)]);
                            args.put(DataMananer.VALUE_DVBT_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_MODE)]);
                            args.put(DataMananer.VALUE_DVBT_TYPE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE)]);
                        } else {
                            parameter = getChannelIndex();
                            if (parameter == null) {
                                Log.d(TAG, "initSearchParameter dvbt search can't find channel index");
                                return null;
                            }
                            args.put(Integer.valueOf(parameter));
                        }
                    } else {
                        if (DataMananer.VALUE_FREQUENCY_MODE == isfrequencysearch) {
                            args.put(Integer.valueOf(parameter) * 1000000);//mhz
                            args.put(DataMananer.VALUE_DVBC_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBC_MODE)]);
                            args.put(getUpdatedDvbcSymbolRate());
                        } else {
                            parameter = getChannelIndex();
                            if (parameter == null) {
                                Log.d(TAG, "initSearchParameter dvbc search can't find channel index");
                                return null;
                            }
                            args.put(Integer.valueOf(parameter));
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
        String parameter = null;
        EditText public_type_edit = (EditText)findViewById(R.id.public_typein_edit);
        Editable editable = public_type_edit.getText();
        int isfrequencysearch = mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY);
        if (DataMananer.VALUE_FREQUENCY_MODE != isfrequencysearch) {
            parameter = getChannelIndex();
        } else if (editable != null) {
            String value = editable.toString();
            if (!TextUtils.isEmpty(value) && TextUtils.isDigitsOnly(value)) {
                parameter = value;
            }
        }

        return parameter;
    }

    private int getUpdatedDvbcSymbolRate() {
        int parameter = DataMananer.VALUE_DVBC_SYMBOL_RATE;
        EditText symbolRate = (EditText)findViewById(R.id.dvbc_symbol_edit);
        Editable editable = symbolRate.getText();
        int isfrequencysearch = mDataMananer.getIntParameters(DataMananer.KEY_DVBC_SYMBOL_RATE);
        if (editable != null) {
            String value = editable.toString();
            if (!TextUtils.isEmpty(value) && TextUtils.isDigitsOnly(value)) {
                parameter = Integer.valueOf(value);
                mDataMananer.saveIntParameters(DataMananer.KEY_DVBC_SYMBOL_RATE, parameter);
            }
        } else {
            mDataMananer.saveIntParameters(DataMananer.KEY_DVBC_SYMBOL_RATE, DataMananer.VALUE_DVBC_SYMBOL_RATE);
        }

        return parameter;
    }

    private String getChannelIndex() {
        String result = null;
        int index = mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_DVBT_CHANNEL_NAME);
        List<String> list = mParameterMananer.getChannelTable(mParameterMananer.getCurrentCountryCode(), mIsDvbt, mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE) == 1);
        String channelInfo = (index < list.size()) ? list.get(index) : null;
        if (channelInfo != null) {
            String[] parameter = channelInfo.split(",");//first number, second string, third number
            if (parameter != null && parameter.length == 3 && TextUtils.isDigitsOnly(parameter[0])) {
                result = parameter[0];
                Log.d(TAG, "getChannelIndex channel index = " + parameter[0] + ", name = " + parameter[1] + ", freq = " + parameter[2]);
            }
        }
        return result;
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
        findViewById(R.id.terrestrialstartsearch).setEnabled(true);
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
        disableStopSearchButton();
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

        // If the intent that started this activity is from Live Channels app
        String inputId = this.getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        Log.i(TAG, String.format("inputId: %s", inputId));
        EpgSyncJobService.requestImmediateSync(this, inputId, true, new ComponentName(this, DtvkitEpgSync.class)); // 12 hours
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

    private void disableStopSearchButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.terrestrialstopsearch).setEnabled(false);
            }
        });
    }
}
