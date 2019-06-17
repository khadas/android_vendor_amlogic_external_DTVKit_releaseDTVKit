package com.droidlogic.settings;

import android.util.Log;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.droidlogic.fragment.ParameterMananer;

import org.dtvkit.inputsource.DtvkitGlueClient;
import org.dtvkit.inputsource.R;

import android.content.ComponentName;
import android.media.tv.TvInputInfo;
import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.dtvkit.inputsource.DtvkitEpgSync;

public class DtvkitDvbSettings extends Activity {

    private static final String TAG = "DtvkitDvbSettings";
    private static final boolean DEBUG = true;

    private DtvkitGlueClient mDtvkitGlueClient = DtvkitGlueClient.getInstance();
    private ParameterMananer mParameterMananer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lanuage_settings);
        mParameterMananer = new ParameterMananer(this, mDtvkitGlueClient);
        initLayout(false);
    }

    private void updatingGuide() {
        EpgSyncJobService.cancelAllSyncRequests(this);
        String inputId = this.getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        Log.i(TAG, String.format("inputId: %s", inputId));
        EpgSyncJobService.requestImmediateSync(this, inputId, true, new ComponentName(this, DtvkitEpgSync.class));
    }

    private void initLayout(boolean update) {
        Spinner country = (Spinner)findViewById(R.id.country_spinner);
        Spinner main_audio = (Spinner)findViewById(R.id.main_audio_spinner);
        Spinner assist_audio = (Spinner)findViewById(R.id.assist_audio_spinner);
        Spinner main_subtitle = (Spinner)findViewById(R.id.main_subtitle_spinner);
        Spinner assist_subtitle = (Spinner)findViewById(R.id.assist_subtitle_spinner);
        initSpinnerParameter();
        if (update) {
            Log.d(TAG, "initLayout update");
            return;
        }
        country.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "country onItemSelected position = " + position);
                int saveCountryCode = mParameterMananer.getCurrentCountryCode();
                int selectCountryCode = mParameterMananer.getCountryCodeByIndex(position);
                if (saveCountryCode == selectCountryCode) {
                    Log.d(TAG, "country onItemSelected same position");
                    return;
                }
                mParameterMananer.setCountryCodeByIndex(position);
                updatingGuide();
                initLayout(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        main_audio.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "main_audio onItemSelected position = " + position);
                int currentMainAudio = mParameterMananer.getCurrentMainAudioLangId();
                int savedMainAudio = mParameterMananer.getLangIndexCodeByPosition(position);
                if (currentMainAudio == savedMainAudio) {
                    Log.d(TAG, "main_audio onItemSelected same position");
                    return;
                }
                mParameterMananer.setPrimaryAudioLangByPosition(position);
                updatingGuide();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        assist_audio.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "assist_audio onItemSelected position = " + position);
                int currentAssistAudio = mParameterMananer.getCurrentSecondAudioLangId();
                int savedAssistAudio = mParameterMananer.getSecondLangIndexCodeByPosition(position);
                if (currentAssistAudio == savedAssistAudio) {
                    Log.d(TAG, "assist_audio onItemSelected same position");
                    return;
                }
                mParameterMananer.setSecondaryAudioLangByPosition(position);
                updatingGuide();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        main_subtitle.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "main_subtitle onItemSelected position = " + position);
                int currentMainSub = mParameterMananer.getCurrentMainSubLangId();
                int savedMainSub = mParameterMananer.getLangIndexCodeByPosition(position);
                if (currentMainSub == savedMainSub) {
                    Log.d(TAG, "main_subtitle onItemSelected same position");
                    return;
                }
                mParameterMananer.setPrimaryTextLangByPosition(position);
                updatingGuide();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        assist_subtitle.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "assist_subtitle onItemSelected position = " + position);
                int currentAssistSub = mParameterMananer.getCurrentSecondSubLangId();
                int savedAssistSub = mParameterMananer.getSecondLangIndexCodeByPosition(position);
                if (currentAssistSub == savedAssistSub) {
                    Log.d(TAG, "assist_subtitle onItemSelected same position");
                    return;
                }
                mParameterMananer.setSecondaryTextLangByPosition(position);
                updatingGuide();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
    }

    private void initSpinnerParameter() {
        Spinner country = (Spinner)findViewById(R.id.country_spinner);
        Spinner main_audio = (Spinner)findViewById(R.id.main_audio_spinner);
        Spinner assist_audio = (Spinner)findViewById(R.id.assist_audio_spinner);
        Spinner main_subtitle = (Spinner)findViewById(R.id.main_subtitle_spinner);
        Spinner assist_subtitle = (Spinner)findViewById(R.id.assist_subtitle_spinner);
        List<String> list = null;
        ArrayAdapter<String> adapter = null;
        int select = 0;
        //add country
        list = mParameterMananer.getCountryDisplayList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        country.setAdapter(adapter);
        select = mParameterMananer.getCurrentCountryIndex();
        country.setSelection(select);
        //add main audio
        list = mParameterMananer.getCurrentLangNameList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        main_audio.setAdapter(adapter);
        select = mParameterMananer.getCurrentMainAudioLangId();
        main_audio.setSelection(select);
        //add second audio
        list = mParameterMananer.getCurrentSecondLangNameList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        assist_audio.setAdapter(adapter);
        select = mParameterMananer.getCurrentSecondAudioLangId();
        assist_audio.setSelection(select);
        //add main subtitle
        list = mParameterMananer.getCurrentLangNameList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        main_subtitle.setAdapter(adapter);
        select = mParameterMananer.getCurrentMainSubLangId();
        main_subtitle.setSelection(select);
        //add second subtitle
        list = mParameterMananer.getCurrentSecondLangNameList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        assist_subtitle.setAdapter(adapter);
        select = mParameterMananer.getCurrentSecondSubLangId();
        assist_subtitle.setSelection(select);
    }
}
