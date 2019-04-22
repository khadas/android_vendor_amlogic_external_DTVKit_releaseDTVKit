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
        updateFirstSettings();
        initLayout(false);
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
                if (mParameterMananer.getIntParameters(ParameterMananer.KEY_DTVKIT_COUNTRY) == position) {
                    Log.d(TAG, "country onItemSelected same position");
                    return;
                }
                mParameterMananer.saveIntParameters(ParameterMananer.KEY_DTVKIT_COUNTRY, position);
                mParameterMananer.setCountryCodeByIndex(position);
                //reset prevoius setting as country changed
                mParameterMananer.saveIntParameters(ParameterMananer.KEY_DTVKIT_MAIN_AUDIO_LANG, 0);
                mParameterMananer.saveIntParameters(ParameterMananer.KEY_DTVKIT_ASSIST_AUDIO_LANG, 0);
                mParameterMananer.saveIntParameters(ParameterMananer.KEY_DTVKIT_MAIN_SUBTITLE_LANG, 0);
                mParameterMananer.saveIntParameters(ParameterMananer.KEY_DTVKIT_ASSIST_SUBTITLE_LANG, 0);
                mParameterMananer.setPrimaryAudioLangByIndex(0);
                mParameterMananer.setSecondaryAudioLangByIndex(0);
                mParameterMananer.setPrimaryTextLangByIndex(0);
                mParameterMananer.setSecondaryTextLangByIndex(0);
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
                if (mParameterMananer.getIntParameters(ParameterMananer.KEY_DTVKIT_MAIN_AUDIO_LANG) == position) {
                    Log.d(TAG, "main_audio onItemSelected same position");
                    return;
                }
                mParameterMananer.saveIntParameters(ParameterMananer.KEY_DTVKIT_MAIN_AUDIO_LANG, position);
                mParameterMananer.setPrimaryAudioLangByIndex(position);
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
                if (mParameterMananer.getIntParameters(ParameterMananer.KEY_DTVKIT_ASSIST_AUDIO_LANG) == position) {
                    Log.d(TAG, "assist_audio onItemSelected same position");
                    return;
                }
                mParameterMananer.saveIntParameters(ParameterMananer.KEY_DTVKIT_ASSIST_AUDIO_LANG, position);
                mParameterMananer.setSecondaryAudioLangByIndex(position);
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
                if (mParameterMananer.getIntParameters(ParameterMananer.KEY_DTVKIT_MAIN_SUBTITLE_LANG) == position) {
                    Log.d(TAG, "main_subtitle onItemSelected same position");
                    return;
                }
                mParameterMananer.saveIntParameters(ParameterMananer.KEY_DTVKIT_MAIN_SUBTITLE_LANG, position);
                mParameterMananer.setPrimaryTextLangByIndex(position);
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
                if (mParameterMananer.getIntParameters(ParameterMananer.KEY_DTVKIT_ASSIST_SUBTITLE_LANG) == position) {
                    Log.d(TAG, "assist_subtitle onItemSelected same position");
                    return;
                }
                mParameterMananer.saveIntParameters(ParameterMananer.KEY_DTVKIT_ASSIST_SUBTITLE_LANG, position);
                mParameterMananer.setSecondaryTextLangByIndex(position);
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
        select = mParameterMananer.getIntParameters(ParameterMananer.KEY_DTVKIT_COUNTRY);
        country.setSelection(select);
        //add main audio
        list = mParameterMananer.getCurrentLangNameList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice );
        main_audio.setAdapter(adapter);
        select = mParameterMananer.getIntParameters(ParameterMananer.KEY_DTVKIT_MAIN_AUDIO_LANG);
        main_audio.setSelection(select);
        //add second audio
        assist_audio.setAdapter(adapter);
        select = mParameterMananer.getIntParameters(ParameterMananer.KEY_DTVKIT_ASSIST_AUDIO_LANG);
        assist_audio.setSelection(select);
        //add main subtitle
        main_subtitle.setAdapter(adapter);
        select = mParameterMananer.getIntParameters(ParameterMananer.KEY_DTVKIT_MAIN_SUBTITLE_LANG);
        main_subtitle.setSelection(select);
        //add second subtitle
        assist_subtitle.setAdapter(adapter);
        select = mParameterMananer.getIntParameters(ParameterMananer.KEY_DTVKIT_ASSIST_SUBTITLE_LANG);
        assist_subtitle.setSelection(select);
    }

    private void updateFirstSettings() {
        int[] allLang = mParameterMananer.getCurrentLangParameter();
        int[] value_list = new int[5];
        List<Integer> countryCodeList = mParameterMananer.getCountryCodeList();
        /*int currentCountryCode*/
        value_list[0] = mParameterMananer.getCurrentCountryCode();
        List<Integer> langIndexList = mParameterMananer.getLangIdList(value_list[0]);
        /*int currentMainAudioId*/
        value_list[1] = mParameterMananer.getCurrentMainAudioLangId();
        /*int currentSecondAudioId*/
        value_list[2] = mParameterMananer.getCurrentSecondAudioLangId();
        /*int currentMainSubId*/
        value_list[3] = mParameterMananer.getCurrentMainSubLangId();
        /*int currentSecondSubId*/
        value_list[4] = mParameterMananer.getCurrentSecondSubLangId();
        final String[] KEY_LIST = {ParameterMananer.KEY_DTVKIT_COUNTRY, ParameterMananer.KEY_DTVKIT_MAIN_AUDIO_LANG, ParameterMananer.KEY_DTVKIT_ASSIST_AUDIO_LANG,
                ParameterMananer.KEY_DTVKIT_MAIN_SUBTITLE_LANG, ParameterMananer.KEY_DTVKIT_ASSIST_SUBTITLE_LANG};
        final String[] PRINT_TNFO_LIST = {"currentCountryCode", "currentMainAudioId", "currentSecondAudioId",
                "currentMainSubId", "currentSecondSubId"};

        for (int k = 0; k < 5; k++) {//deal five parameters in for
            if (allLang[k] != -1 && allLang[k] != value_list[k] && countryCodeList != null && countryCodeList.size() > 0) {
                for (int i = 0; i < countryCodeList.size(); i++) {
                    if (countryCodeList.get(i) == allLang[k]) {
                        Log.d(TAG, "updateFirstSettings find " + PRINT_TNFO_LIST[k] + " by index i = " + i);
                        value_list[k] = allLang[k];
                        mParameterMananer.saveIntParameters(KEY_LIST[i], i);
                        break;
                    }
                }
            } else {
                Log.d(TAG, "updateFirstSettings use saved " + PRINT_TNFO_LIST[k] + " = " + value_list[k]);
            }
        }
    }
}
