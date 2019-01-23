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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.ActivityNotFoundException;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

public class DtvkitDvbsSetup extends Activity {
    private static final String TAG = "DtvkitDvbsSetup";
    private static final int REQUEST_CODE_SET_UP_SETTINGS = 1;
    private DataMananer mDataMananer;

    private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(String signal, JSONObject data) {
            if (signal.equals("DvbsStatusChanged")) {
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

        mDataMananer = new DataMananer(this);

        Button search = (Button)findViewById(R.id.startsearch);
        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startSearch();
            }
        });
        search.requestFocus();
        Button setup = (Button)findViewById(R.id.setup);
        setup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setUp();
            }
        });

        CheckBox nit = (CheckBox)findViewById(R.id.network);
        CheckBox clear = (CheckBox)findViewById(R.id.clear_old);
        CheckBox dvbs2 = (CheckBox)findViewById(R.id.dvbs2);
        Spinner searchmode = (Spinner)findViewById(R.id.search_mode);
        Spinner fecmode = (Spinner)findViewById(R.id.fec_mode);
        Spinner modulationmode = (Spinner)findViewById(R.id.modulation_mode);

        nit.setChecked(mDataMananer.getIntParameters(DataMananer.KEY_NIT) == 1 ? true : false);
        clear.setChecked(mDataMananer.getIntParameters(DataMananer.KEY_CLEAR) == 1 ? true : false);
        dvbs2.setChecked(mDataMananer.getIntParameters(DataMananer.KEY_DVBS2) == 1 ? true : false);
        nit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (nit.isChecked()) {
                    mDataMananer.saveIntParameters(DataMananer.KEY_NIT, 1);
                } else {
                    mDataMananer.saveIntParameters(DataMananer.KEY_NIT, 0);
                }
            }
        });
        clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (clear.isChecked()) {
                    mDataMananer.saveIntParameters(DataMananer.KEY_CLEAR, 1);
                } else {
                    mDataMananer.saveIntParameters(DataMananer.KEY_CLEAR, 0);
                }
            }
        });
        dvbs2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (dvbs2.isChecked()) {
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBS2, 1);
                } else {
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBS2, 0);
                }
            }
        });
        searchmode.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "searchmode onItemSelected position = " + position);
                mDataMananer.saveIntParameters(DataMananer.KEY_SEARCH_MODE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        fecmode.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "fecmode onItemSelected position = " + position);
                mDataMananer.saveIntParameters(DataMananer.KEY_FEC_MODE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        modulationmode.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "modulationmode onItemSelected position = " + position);
                mDataMananer.saveIntParameters(DataMananer.KEY_MODULATION_MODE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        searchmode.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_MODE));
        fecmode.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_FEC_MODE));
        modulationmode.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_MODULATION_MODE));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoringSearch();
        stopMonitoringSync();
    }

    private void setUp() {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.droidlogic.fragment", "com.droidlogic.fragment.ScanMainActivity");
            String inputId = this.getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
            if (inputId != null) {
                intent.putExtra(TvInputInfo.EXTRA_INPUT_ID, inputId);
            }
            startActivityForResult(intent, REQUEST_CODE_SET_UP_SETTINGS);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.strSetUpNotFound), Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private JSONArray initSearchParameter() {
        JSONArray args = new JSONArray();
        boolean needclear = mDataMananer.getIntParameters(DataMananer.KEY_CLEAR) == 1 ? true : false;
        String searchmode = DataMananer.KEY_SEARCH_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_MODE)];
        args.put(needclear);
        args.put(searchmode);
        switch (searchmode) {
            case "blind":
                Log.d(TAG, "initSearchParameter blind");
                args = initBlindSearch(args);
                break;
            case "satellite":
                Log.d(TAG, "initSearchParameter satellite");
                args = initSatelliteSearch(args);
                break;
            case "transponder":
                Log.d(TAG, "initSearchParameter transponder");
                args = initTransponderSearch(args);
                break;
            default:
                Log.d(TAG, "initSearchParameter not surported mode!");
                break;
        }
        return args;
    }

    private JSONArray initBlindSearch(JSONArray args) {
        return args;
    }

    private JSONArray initSatelliteSearch(JSONArray args) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("nit", mDataMananer.getIntParameters(DataMananer.KEY_NIT) == 1 ? true : false);
            obj.put("satellite", mDataMananer.getStringParameters(DataMananer.KEY_SATALLITE));
            obj.put("unicable", mDataMananer.getIntParameters(DataMananer.KEY_UNICABLE_SWITCH) == 1);
            obj.put("unicable_chan", mDataMananer.getIntParameters(DataMananer.KEY_USER_BAND));
            obj.put("unicable_if", mDataMananer.getIntParameters(DataMananer.KEY_UB_FREQUENCY));
            obj.put("unicable_position_b", mDataMananer.getIntParameters(DataMananer.KEY_POSITION) == 1);
            obj.put("tone_burst", DataMananer.DIALOG_SET_SELECT_SINGLE_ITEM_TONE_BURST_LIST[mDataMananer.getIntParameters(DataMananer.KEY_TONE_BURST)]);
            obj.put("c_switch", mDataMananer.getIntParameters(DataMananer.KEY_DISEQC1_0));
            obj.put("u_switch", mDataMananer.getIntParameters(DataMananer.KEY_DISEQC1_1));
            obj.put("dish_pos", mDataMananer.getIntParameters(DataMananer.KEY_DISEQC1_2_DISH_CURRENT_POSITION));

            JSONObject lnbobj = new JSONObject();
            JSONObject lowband_obj = new JSONObject();
            JSONObject highband_obj = new JSONObject();
            int lnbtype = mDataMananer.getIntParameters(DataMananer.KEY_LNB_TYPE);
            int lowlnb = 0;
            int highlnb = 0;
            switch (lnbtype) {
                case 0:
                    lowlnb = 5150;
                    break;
                case 1:
                    lowlnb = 9750;
                    highlnb = 10600;
                    break;
                case 2:
                    String customlnb = mDataMananer.getStringParameters(DataMananer.KEY_LNB_CUSTOM);
                    String[] customlnbvalue = null;
                    if (customlnb != null) {
                        customlnbvalue = customlnb.split(",");
                    }
                    if (customlnbvalue != null && customlnbvalue.length == 1) {
                        lowlnb = Integer.valueOf(customlnbvalue[0]);
                    } else if (customlnbvalue != null && customlnbvalue.length == 2) {
                        lowlnb = Integer.valueOf(customlnbvalue[0]);
                        highlnb = Integer.valueOf(customlnbvalue[1]);
                    } else {
                        Log.d(TAG, "null lnb customized data!");
                    }
            }
            lowband_obj.put("min_freq", lowlnb);
            lowband_obj.put("max_freq", lowlnb);
            lowband_obj.put("local_oscillator_frequency", lowlnb);
            lowband_obj.put("lnb_voltage", DataMananer.DIALOG_SET_SELECT_SINGLE_ITEM_LNB_POWER_LIST[mDataMananer.getIntParameters(DataMananer.KEY_LNB_POWER)]);
            lowband_obj.put("tone_22k", mDataMananer.getIntParameters(DataMananer.KEY_22_KHZ) == 1);
            highband_obj.put("min_freq", lowlnb);
            highband_obj.put("max_freq", highlnb);
            highband_obj.put("local_oscillator_frequency", highlnb);
            highband_obj.put("lnb_voltage", DataMananer.DIALOG_SET_SELECT_SINGLE_ITEM_LNB_POWER_LIST[mDataMananer.getIntParameters(DataMananer.KEY_LNB_POWER)]);
            highband_obj.put("tone_22k", mDataMananer.getIntParameters(DataMananer.KEY_22_KHZ) == 1);

            lnbobj.put("low_band", lowband_obj);
            if (highlnb > 0) {
                lnbobj.put("high_band", highband_obj);
            }
            obj.put("lnb", lnbobj);

            args.put(obj.toString());
        } catch (Exception e) {
            args = null;
            Log.d(TAG, "initSatelliteSearch Exception " + e.getMessage());
        }
        return args;
    }

    private JSONArray initTransponderSearch(JSONArray args) {
        try {
            args.put(mDataMananer.getIntParameters(DataMananer.KEY_NIT) == 1);
            args.put(mDataMananer.getStringParameters(DataMananer.KEY_SATALLITE));
            String[] singleParameter = null;
            String parameter = mDataMananer.getStringParameters(DataMananer.KEY_TRANSPONDER);
            if (parameter != null) {
                singleParameter = parameter.split("/");
                if (singleParameter != null && singleParameter.length == 3) {
                    args.put(Integer.valueOf(singleParameter[0]));
                    args.put(singleParameter[1]);
                    args.put(Integer.valueOf(singleParameter[2]));
                } else {
                    return null;
                }
            } else {
                return null;
            }
            args.put(DataMananer.KEY_FEC_ARRAY_VALUE[mDataMananer.getIntParameters(DataMananer.KEY_FEC_MODE)]);
            args.put(mDataMananer.getIntParameters(DataMananer.KEY_DVBS2) == 1);
            args.put(DataMananer.KEY_MODULATION_ARRAY_VALUE[mDataMananer.getIntParameters(DataMananer.KEY_MODULATION_MODE)]);
        } catch (Exception e) {
            args = null;
            Log.d(TAG, "initTransponderSearch Exception " + e.getMessage());
        }
        return args;
    }

     private void testAddSatallite() {
        try {
            JSONArray args = new JSONArray();
            args.put("test1");
            args.put(true);
            args.put(1200);
            Log.d(TAG, "addSatallite->" + args.toString());
            DtvkitGlueClient.getInstance().request("Dvbs.addSatellite", args);
            JSONArray args1 = new JSONArray();
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getSatellites", args1);
            if (resultObj != null) {
                Log.d(TAG, "addSatallite resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "addSatallite then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "addSatallite Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
    }

    private void testAddTransponder() {
        try {
            JSONArray args = new JSONArray();
            args.put("test1");
            args.put(498);
            args.put("H");
            args.put(6875);
            Log.d(TAG, "addTransponder->" + args.toString());
            DtvkitGlueClient.getInstance().request("Dvbs.addTransponder", args);
            JSONArray args1 = new JSONArray();
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getSatellites", args1);
            if (resultObj != null) {
                Log.d(TAG, "addTransponder resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "addTransponder then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "addTransponder Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
    }

    private void startSearch() {
        setSearchStatus("Searching");
        getProgressBar().setIndeterminate(false);
        startMonitoringSearch();

        /*CheckBox network = (CheckBox)findViewById(R.id.network);
        EditText frequency = (EditText)findViewById(R.id.frequency);
        Spinner satellite = (Spinner)findViewById(R.id.satellite);
        Spinner polarity = (Spinner)findViewById(R.id.polarity);
        EditText symbolrate = (EditText)findViewById(R.id.symbolrate);
        Spinner fec = (Spinner)findViewById(R.id.fec);
        CheckBox dvbs2 = (CheckBox)findViewById(R.id.dvbs2);
        Spinner modulation = (Spinner)findViewById(R.id.modulation);*/

        try {
            /*JSONArray args = new JSONArray();
            args.put(network.isChecked());
            args.put(Integer.parseInt(frequency.getText().toString()));
            args.put(satellite.getSelectedItem().toString());
            args.put(polarity.getSelectedItem().toString());
            args.put(Integer.parseInt(symbolrate.getText().toString()));
            args.put(fec.getSelectedItem().toString());
            args.put(dvbs2.isChecked());
            args.put(modulation.getSelectedItem().toString());

            Log.i(TAG, args.toString());

            DtvkitGlueClient.getInstance().request("Dvbs.startManualSearch", args);*/
            JSONArray args = initSearchParameter();
            if (args != null) {
                DtvkitGlueClient.getInstance().request("Dvbs.startSearch", args);
            } else {
                stopMonitoringSearch();
                setSearchStatus(getString(R.string.invalid_parameter));
                return;
            }
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
