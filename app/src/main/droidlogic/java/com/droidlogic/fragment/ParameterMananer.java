package com.droidlogic.fragment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.droidlogic.fragment.ItemAdapter.ItemDetail;
import com.droidlogic.fragment.dialog.CustomDialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import org.dtvkit.inputsource.DtvkitGlueClient;

public class ParameterMananer {

    private static final String TAG = "ParameterMananer";
    private Context mContext;
    private DtvkitGlueClient mDtvkitGlueClient;

    public static final String ITEM_SATALLITE              = "satellite";
    public static final String ITEM_TRANSPONDER            = "transponder";
    public static final String ITEM_DIRECTION              = "left";
    public static final String ITEM_SATALLITE_OPTION       = "satallite_option";
    public static final String ITEM_TRANSPONDER_OPTION     = "tansponder_option";
    public static final String ITEM_OPTION                 = "option";

    public static final String SAVE_SATELITE_POSITION = "satellite_position";
    public static final String SAVE_TRANSPONDER_POSITION = "transponder_position";
    public static final String SAVE_CURRENT_LIST_TYPE = "current_list_type";

    public static final String KEY_SATALLITE = "key_satallite";
    public static final String KEY_TRANSPONDER = "key_transponder";
    public static final String KEY_CURRENT_TYPE = "key_current_type";
    public static final String KEY_CURRENT_DIRECTION = "key_current_direction";
    public static final String KEY_LNB_TYPE = "key_lnb_type";
    public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE_LIST = {"5150", "9750/10600", "Customize"};
    //unicable
    public static final String KEY_UNICABLE = "key_unicable";
    public static final String KEY_UNICABLE_SWITCH = "key_unicable_switch";
    public static final String[] DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_SWITCH_LIST = {"off", "on"};
    public static final String[] DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_USER_BAND_LIST = {"0", "1", "2", "3", "4", "5", "6", "7"};
    public static final String[] DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_POSITION_LIST = {"off", "on"};
    public static final String KEY_USER_BAND = "key_user_band";
    public static final String KEY_UB_FREQUENCY = "key_ub_frequency";
    public static final String KEY_POSITION = "key_position";

    public static final String KEY_LNB_POWER = "key_lnb_power";
    public static final String KEY_22_KHZ = "key_22_khz";
    public static final String KEY_TONE_BURST = "key_tone_burst";
    public static final String KEY_DISEQC1_0 = "key_diseqc1_0";
    public static final String KEY_DISEQC1_1 = "key_diseqc1_1";
    public static final String KEY_MOTOR = "key_motor";
    public static final String KEY_DISEQC1_2 = "key_diseqc1_2";
    public static final String KEY_DISEQC1_2_DISH_LIMITS_STATUS = "key_dish_limit_status";
    public static final String KEY_DISEQC1_2_DISH_EAST_LIMITS = "key_dish_east_limit";
    public static final String KEY_DISEQC1_2_DISH_WEST_LIMITS = "key_dish_west_limit";
    public static final String KEY_DISEQC1_2_DISH_MOVE_DIRECTION = "key_dish_move_direction";
    public static final String KEY_DISEQC1_2_DISH_MOVE_STEP = "key_dish_move_step";
    public static final String KEY_DISEQC1_2_DISH_CURRENT_POSITION = "key_dish_current_position";
    public static final String KEY_DISEQC1_2_DISH_SAVE_POSITION = "key_dish_save_to_position";
    public static final String KEY_DISEQC1_2_DISH_MOVE_TO_POSITION = "key_dish_move_to_position";

    public static final String[] ID_DIALOG_KEY_COLLECTOR = {KEY_SATALLITE, KEY_TRANSPONDER, KEY_LNB_TYPE, KEY_UNICABLE_SWITCH/*KEY_UNICABLE*/, KEY_LNB_POWER,
            KEY_22_KHZ, KEY_TONE_BURST, KEY_DISEQC1_0, KEY_DISEQC1_1, KEY_MOTOR};
    public static final String KEY_LNB_CUSTOM = "key_lnb_custom";
    //default value
    public static final String KEY_SATALLITE_DEFAULT_VALUE = "null";
    public static final String KEY_TRANSPONDER_DEFAULT_VALUE = "null";
    public static final String KEY_LNB_TYPE_DEFAULT_VALUE = "9750/10600";
    //unicable
    public static final String KEY_UNICABLE_DEFAULT_VALUE = "off";
    public static final String KEY_UNICABLE_SWITCH_DEFAULT_VALUE = "off";
    public static final String KEY_USER_BAND_DEFAULT_VALUE = "0";
    public static final String KEY_UB_FREQUENCY_DEFAULT_VALUE = "0";
    public static final String KEY_POSITION_DEFAULT_VALUE = "false";

    public static final String KEY_LNB_POWER_DEFAULT_VALUE = "18V";
    public static final String KEY_22_KHZ_DEFAULT_VALUE = "auto";
    public static final String KEY_TONE_BURST_DEFAULT_VALUE = "none";
    public static final String KEY_DISEQC1_0_DEFAULT_VALUE = "none";
    public static final String KEY_DISEQC1_1_DEFAULT_VALUE = "none";
    public static final String KEY_MOTOR_DEFAULT_VALUE = "none";

    public static final String KEY_FUNCTION = "function";
    public static final String KEY_ADD_SATELLITE = "add_satellite";
    public static final String KEY_EDIT_SATELLITE = "edit_satellite";
    public static final String KEY_REMOVE_SATELLITE = "remove_satellite";
    public static final String KEY_ADD_TRANSPONDER = "add_transponder";
    public static final String KEY_EDIT_TRANSPONDER = "edit_transponder";
    public static final String KEY_REMOVE_TRANSPONDER = "remove_transponder";

    //default value that is save by index
    public static final int KEY_SATALLITE_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_TRANSPONDER_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_LNB_TYPE_DEFAULT_INDEX_INDEX = 1;
    //unicable
    public static final int KEY_UNICABLE_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_UNICABLE_SWITCH_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_USER_BAND_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_UB_FREQUENCY_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_POSITION_DEFAULT_VALUE_INDEX = 0;

    public static final int KEY_LNB_POWER_DEFAULT_VALUE_INDEX = 1;
    public static final int KEY_22_KHZ_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_TONE_BURST_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_DISEQC1_0_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_DISEQC1_1_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_MOTOR_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_DISEQC1_2_DISH_LIMITS_STATUS_DEFAULT_VALUE_INDEX = 0;

    public static final String[] DIALOG_SET_ITEM_UNICABLE_KEY_LIST = {KEY_UNICABLE_SWITCH, KEY_USER_BAND, KEY_UB_FREQUENCY, KEY_POSITION};

    public static final String KEY_DTVKIT_COUNTRY = "dtvkit_country";
    public static final String KEY_DTVKIT_MAIN_AUDIO_LANG = "main_audio_lang";
    public static final String KEY_DTVKIT_ASSIST_AUDIO_LANG = "assist_audio_lang";
    public static final String KEY_DTVKIT_MAIN_SUBTITLE_LANG = "main_subtitle_lang";
    public static final String KEY_DTVKIT_ASSIST_SUBTITLE_LANG = "assist_subtitle_lang";

    public ParameterMananer(Context context, DtvkitGlueClient client) {
        this.mContext = context;
        this.mDtvkitGlueClient = client;
    }

    public void setDtvkitGlueClient(DtvkitGlueClient client) {
        if (mDtvkitGlueClient == null) {
            mDtvkitGlueClient = client;
        }
    }

    public LinkedList<ItemDetail> getSatelliteList() {
        //need to add get function, debug as below
        LinkedList<ItemDetail> list = new LinkedList<ItemDetail>();
        try {
            JSONObject jsonObj = getSatellites();
            JSONArray data = null;
            if (jsonObj != null) {
                data = (JSONArray)jsonObj.get("data");
            }
            if (data == null || data.length() == 0) {
                return list;
            }
            String current = getCurrentSatellite();
            int type = 0;
            for (int i = 0; i < data.length(); i++) {
                Log.d(TAG, "getSatelliteList data:" + data.get(i));
                String satellitevalue = (String)(((JSONObject)(data.get(i))).get("name"));
                Log.d(TAG, "getSatelliteList satellitevalue = " + satellitevalue);
                if (TextUtils.equals(satellitevalue, current)) {
                    type = ItemDetail.SELECT_EDIT;
                } else {
                    type = ItemDetail.NOT_SELECT_EDIT;
                }
                list.add(new ItemDetail(type, satellitevalue, null, true));
            }
        } catch (Exception e) {
            Log.d(TAG, "getSatelliteList Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return list;
    }

    //private int mCurrentSatellite = -1;
    //private String mCurrentListType = null;
    /*private final String[] ALL_SATALLITE = {"001 013.OE Ku_HOTBIRD 6", "002 013.OE Ku_HOTBIRD 6", "003 013.OE Ku_HOTBIRD 6",
                                            "004 013.OE Ku_HOTBIRD 6", "005 013.OE Ku_HOTBIRD 6", "006 013.OE Ku_HOTBIRD 6",
                                            "007 013.OE Ku_HOTBIRD 6"};
    private int mCurrentTransponder = -1;
    private final String[] ALL_TRANSPONDER = {"001 10723 H 29900008", "002 10723 H 29900008", "003 10723 H 29900008",
                                              "004 10723 H 29900008", "005 10723 H 29900008", "006 10723 H 29900008",
                                              "007 10723 H 29900008"};*/

    public String getCurrentSatellite() {
        /*if (mCurrentSatellite == -1) {
            mCurrentSatellite = getIntParameters(SAVE_SATELITE_POSITION);
            if (mCurrentSatellite == -1) {
                saveIntParameters(SAVE_SATELITE_POSITION, 0);
                mCurrentSatellite = 0;
            }
        }
        return mCurrentSatellite;*/
        return getStringParameters(KEY_SATALLITE);
    }

    public int getCurrentSatelliteIndex() {
        LinkedList<ItemDetail> items = getSatelliteList();
        String current = getCurrentSatellite();
        Iterator iterator = null;//items.iterator();
        int i = -1;
        boolean found = false;
        if (items != null && items.size() > 0) {
            iterator = items.iterator();
            if (iterator != null) {
                while (iterator.hasNext()) {
                    i++;
                    String value = ((ItemDetail)iterator.next()).getFirstText();
                    if (TextUtils.equals(current, value)) {
                        found = true;
                        return i;
                    }
                }
            }
        }
        Log.d(TAG, "getCurrentSatelliteIndex can't match value");
        return 0;
    }

    public void setCurrentSatellite(String satellite) {
        /*mCurrentSatellite = position;
        saveIntParameters(SAVE_SATELITE_POSITION, position);*/
        saveStringParameters(KEY_SATALLITE, satellite);
    }

    public String getCurrentListType() {
        return getStringParameters(KEY_CURRENT_TYPE);
    }

    public void setCurrentListType(String type) {
        saveStringParameters(KEY_CURRENT_TYPE, type);
    }

    public void setCurrentListDirection(String type) {
        saveStringParameters(KEY_CURRENT_DIRECTION, type);
    }

    public String getCurrentListDirection() {
        return getStringParameters(KEY_CURRENT_DIRECTION);
    }

    public String getCurrentTransponder() {
        //return getIntParameters(SAVE_TRANSPONDER_POSITION);
        /*if (mCurrentTransponder == -1) {
            mCurrentTransponder = getIntParameters(SAVE_TRANSPONDER_POSITION);
            if (mCurrentTransponder == -1) {
                saveIntParameters(SAVE_TRANSPONDER_POSITION, 0);
                mCurrentTransponder = 0;
            }
        }*/
        return getStringParameters(KEY_TRANSPONDER);
    }

    public void setCurrentTransponder(String value) {
        //mCurrentTransponder = position;
        //saveIntParameters(SAVE_TRANSPONDER_POSITION, position);
        saveStringParameters(KEY_TRANSPONDER, value);
    }

    public int getCurrentTransponderIndex() {
        LinkedList<ItemDetail> items = getTransponderList();
        String current = getCurrentTransponder();
        Iterator iterator = null;//items.iterator();
        int i = -1;
        boolean found = false;
        if (items != null && items.size() > 0) {
            iterator = items.iterator();
            if (iterator != null) {
                while (iterator.hasNext()) {
                    i++;
                    String value = ((ItemDetail)iterator.next()).getFirstText();
                    if (TextUtils.equals(current, value)) {
                        found = true;
                        return i;
                    }
                }
            }
        }
        Log.d(TAG, "getCurrentTransponderIndex can match value");
        return 0;
    }

    public LinkedList<ItemDetail> getTransponderList() {
        LinkedList<ItemDetail> list = new LinkedList<ItemDetail>();
        try {
            JSONObject jsonObj = getTransponders(getCurrentSatellite());
            JSONArray data = null;
            if (jsonObj != null) {
                data = (JSONArray)jsonObj.get("data");
            }
            if (data == null || data.length() == 0) {
                return list;
            }
            String current = getCurrentTransponder();
            int type = 0;
            for (int i = 0; i < data.length(); i++) {
                String transpondervalue = null;
                transpondervalue = (int)(((JSONObject)(data.get(i))).get("frequency")) + "/";
                transpondervalue += (String)(((JSONObject)(data.get(i))).get("polarity")) + "/";
                transpondervalue += (int)(((JSONObject)(data.get(i))).get("symbol_rate"));
                if (TextUtils.equals(transpondervalue, current)) {
                    type = ItemDetail.SELECT_EDIT;
                } else {
                    type = ItemDetail.NOT_SELECT_EDIT;
                }
                list.add(new ItemDetail(type, transpondervalue, null, true));
            }
        } catch (Exception e) {
            Log.d(TAG, "getSatelliteList Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }

        return list;
    }

    public LinkedList<ItemDetail> getItemList(String type) {
        if (ITEM_SATALLITE.equals(type)) {
            return getSatelliteList();
        } else if (ITEM_TRANSPONDER.equals(type)) {
            return getTransponderList();
        } else {
            return getSatelliteList();
        }
    }

    /*public String getParameterListTitle(String type, int position) {
        String title = null;
        //need to add get function, debug as below
        title = "Ku_HOTBIRO6,7A,8";
        if (KEY_SATALLITE.equals(type)) {
            title = "Ku_NewSat2";
        } else if (KEY_TRANSPONDER.equals(type)) {
            title = "Ku_HOTBIRO6,7A,8";
        } else {
            return "";
        }
        return title;
    }*/

    //public static final String[] PARAMETER = {"LNB Type", "LNB Power", "22KHZ", "Toneburst", "DisEqC1.0", "DisEqC1.1", "Motor"};

    private List<String> getParameterListType(String type, int position) {
        List<String> list = new ArrayList<String>();
        //need to add get function, debug as below
        /*list.add("LNB Type");
        list.add("LNB Power");
        list.add("22KHZ");
        list.add("Toneburst");
        list.add("DisEqC1.0");
        list.add("DisEqC1.1");
        list.add("Motor");*/
        for (int i = 0; i < CustomDialog.ID_DIALOG_TITLE_COLLECTOR.length; i++) {
            list.add(mContext.getString(CustomDialog.ID_DIALOG_TITLE_COLLECTOR[i]));
        }
        return list;
    }

    private List<String> getParameterListValue(String type, int position) {
        List<String> list = new ArrayList<String>();
        //need to add get function, debug as below
        /*list.add("09750/10600");
        list.add("13/18V");
        list.add("Auto");
        list.add("BurstB");
        list.add("LNB2");
        list.add("LNB6");
        list.add("None");*/
        for (int i = 0; i < ParameterMananer.ID_DIALOG_KEY_COLLECTOR.length; i++) {
            String result = "null";
            switch (ParameterMananer.ID_DIALOG_KEY_COLLECTOR[i]) {
                case KEY_SATALLITE:
                case KEY_TRANSPONDER:
                    result = getStringParameters(ParameterMananer.ID_DIALOG_KEY_COLLECTOR[i]);
                    break;
                case KEY_LNB_TYPE:
                    result = mContext.getString(CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE_LIST[(getIntParameters(ParameterMananer.ID_DIALOG_KEY_COLLECTOR[i]))]);
                    break;
                case KEY_UNICABLE:
                    result = mContext.getString(CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE_LIST[(getIntParameters(ParameterMananer.ID_DIALOG_KEY_COLLECTOR[i]))]);
                    break;
                case KEY_UNICABLE_SWITCH:
                    result = mContext.getString(CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE_LIST[(getIntParameters(ParameterMananer.ID_DIALOG_KEY_COLLECTOR[i]))]);
                    break;
                case KEY_LNB_POWER:
                    result = CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_LNB_POWER_LIST[(getIntParameters(ParameterMananer.ID_DIALOG_KEY_COLLECTOR[i]))];
                    break;
                case KEY_22_KHZ:
                    result = mContext.getString(CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_22KHZ_LIST[(getIntParameters(ParameterMananer.ID_DIALOG_KEY_COLLECTOR[i]))]);
                    break;
                case KEY_TONE_BURST:
                    result = CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_TONE_BURST_LIST[(getIntParameters(ParameterMananer.ID_DIALOG_KEY_COLLECTOR[i]))];
                    break;
                case KEY_DISEQC1_0:
                    result = CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_0_LIST[(getIntParameters(ParameterMananer.ID_DIALOG_KEY_COLLECTOR[i]))];
                    break;
                case KEY_DISEQC1_1:
                    result = CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_1_LIST[(getIntParameters(ParameterMananer.ID_DIALOG_KEY_COLLECTOR[i]))];
                    break;
                case KEY_MOTOR:
                    result = CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_MOTOR_LIST[(getIntParameters(ParameterMananer.ID_DIALOG_KEY_COLLECTOR[i]))];
                    break;
            }
            list.add(result);
        }
        return list;
    }

    public LinkedList<ItemDetail> getCompleteParameterList(String type, int position) {
        List<String> parametertype = getParameterListType(type, position);
        List<String> parametervalue = getParameterListValue(type, position);
        LinkedList<ItemDetail> list = new LinkedList<ItemDetail>();
        if (parametertype.size() != parametervalue.size()) {
            return list;
        }

        int size = parametertype.size();
        for (int i = 0; i < parametertype.size(); i++) {
            Log.d(TAG, "parametertype " + parametertype.get(i) + ", parametervalue = " + parametervalue.get(i));
            if (i == 0 || i ==1) {
                list.add(new ItemDetail(ItemDetail.NONE_EDIT, parametertype.get(i), parametervalue.get(i), false));
            } else {
                list.add(new ItemDetail(ItemDetail.SWITCH_EDIT, parametertype.get(i), parametervalue.get(i), false));
            }
        }

        return list;
    }

    public void saveIntParameters(String key, int value) {
        /*SharedPreferences sp = mContext.getSharedPreferences("dish_parameter", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(key, value);
        editor.commit();*/
        Settings.Global.putInt(mContext.getContentResolver(), key, value);
    }

    public int getIntParameters(String key) {
        int defValue = 0;
        switch (key) {
            case KEY_LNB_TYPE:
                defValue = 1;
                break;
            case KEY_UNICABLE_SWITCH:
                defValue = KEY_UNICABLE_SWITCH_DEFAULT_VALUE_INDEX;
                break;
            case KEY_USER_BAND:
                defValue = KEY_USER_BAND_DEFAULT_VALUE_INDEX;
                break;
            case KEY_UB_FREQUENCY:
                defValue = KEY_UB_FREQUENCY_DEFAULT_VALUE_INDEX;
                break;
            case KEY_POSITION:
                defValue = KEY_POSITION_DEFAULT_VALUE_INDEX;
                break;
            case KEY_UNICABLE:
                defValue = KEY_UNICABLE_SWITCH_DEFAULT_VALUE_INDEX;
                break;
            case KEY_LNB_POWER:
                defValue = KEY_LNB_POWER_DEFAULT_VALUE_INDEX;
                break;
            case KEY_22_KHZ:
                defValue = KEY_22_KHZ_DEFAULT_VALUE_INDEX;
                break;
            case KEY_TONE_BURST:
                defValue = KEY_TONE_BURST_DEFAULT_VALUE_INDEX;
                break;
            case KEY_DISEQC1_0:
                defValue = KEY_DISEQC1_0_DEFAULT_VALUE_INDEX;
                break;
            case KEY_DISEQC1_1:
                defValue = KEY_DISEQC1_1_DEFAULT_VALUE_INDEX;
                break;
            case KEY_MOTOR:
                defValue = KEY_MOTOR_DEFAULT_VALUE_INDEX;
                break;
            case KEY_DISEQC1_2_DISH_LIMITS_STATUS:
                defValue = KEY_MOTOR_DEFAULT_VALUE_INDEX;
                break;
            case KEY_DISEQC1_2_DISH_EAST_LIMITS:
                defValue = 180;
                break;
            case KEY_DISEQC1_2_DISH_WEST_LIMITS:
                defValue = 180;
                break;
            case KEY_DISEQC1_2_DISH_MOVE_DIRECTION:
                defValue = 0;
                break;
            case KEY_DISEQC1_2_DISH_MOVE_STEP:
                defValue = 1;
                break;
            case KEY_DISEQC1_2_DISH_CURRENT_POSITION:
                defValue = 0;
                break;
            default:
                defValue = 0;
                break;
        }
        /*SharedPreferences sp = mContext.getSharedPreferences("dish_parameter", Context.MODE_PRIVATE);
        return sp.getInt(key, defValue);*/
        return Settings.Global.getInt(mContext.getContentResolver(), key, defValue);
    }

    public void saveStringParameters(String key, String value) {
        /*SharedPreferences sp = mContext.getSharedPreferences("dish_parameter", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.commit();*/
        Settings.Global.putString(mContext.getContentResolver(), key, value);
    }

    public String getStringParameters(String key) {
        String defValue = "";
        if (key == null) {
            return null;
        }
        switch (key) {
            case KEY_CURRENT_TYPE:
                defValue = ITEM_SATALLITE;
                break;
            case KEY_CURRENT_DIRECTION:
                defValue = ITEM_DIRECTION;
                break;
            case KEY_SATALLITE:
                defValue = KEY_SATALLITE_DEFAULT_VALUE;//ALL_SATALLITE[0];
                break;
            case KEY_TRANSPONDER:
                defValue = KEY_TRANSPONDER_DEFAULT_VALUE;//ALL_TRANSPONDER[0];
                break;
            case KEY_LNB_TYPE:
                defValue = KEY_LNB_TYPE_DEFAULT_VALUE;
                break;
            case KEY_UNICABLE_SWITCH:
                defValue = KEY_UNICABLE_SWITCH_DEFAULT_VALUE;
                break;
            case KEY_USER_BAND:
                defValue = KEY_USER_BAND_DEFAULT_VALUE;
                break;
            case KEY_UB_FREQUENCY:
                defValue = KEY_UB_FREQUENCY_DEFAULT_VALUE;
                break;
            case KEY_POSITION:
                defValue = KEY_POSITION_DEFAULT_VALUE;
                break;
            case KEY_UNICABLE:
                defValue = KEY_UNICABLE_SWITCH_DEFAULT_VALUE;
                break;
            case KEY_LNB_POWER:
                defValue = KEY_LNB_POWER_DEFAULT_VALUE;
                break;
            case KEY_22_KHZ:
                defValue = KEY_22_KHZ_DEFAULT_VALUE;
                break;
            case KEY_TONE_BURST:
                defValue = KEY_TONE_BURST_DEFAULT_VALUE;
                break;
            case KEY_DISEQC1_0:
                defValue = KEY_DISEQC1_0_DEFAULT_VALUE;
                break;
            case KEY_DISEQC1_1:
                defValue = KEY_DISEQC1_1_DEFAULT_VALUE;
                break;
            case KEY_MOTOR:
                defValue = KEY_MOTOR_DEFAULT_VALUE;
                break;
            case KEY_LNB_CUSTOM:
                defValue = "";
                break;
            default:
                defValue = "";
                break;
        }
        /*SharedPreferences sp = mContext.getSharedPreferences("dish_parameter", Context.MODE_PRIVATE);
        String result = sp.getString(key, defValue);*/
        String result = Settings.Global.getString(mContext.getContentResolver(), key);
        Log.d(TAG, "getStringParameters key = " + key + ", result = " + result);
        if (TextUtils.isEmpty(result)) {
            result = defValue;
        }
        return result;
    }

    /*public int getSelectSingleItemValueIndex(String title) {
        String key = switchTilteToKey(title);
        String value = getStringParameters(key);
        int result = 0;
        if (key == null) {
            return result;
        }
        switch (key) {
            case KEY_LNB_TYPE:
                result = getIndexFromArray(DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE_LIST, value);
                break;
            case KEY_UNICABLE_SWITCH:
                result = getIndexFromArray(DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_SWITCH_LIST, value);
                break;
            case KEY_USER_BAND:
                result = getIndexFromArray(DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_USER_BAND_LIST, value);
                break;
            case KEY_POSITION:
                result = getIndexFromArray(DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_POSITION_LIST, value);
                break;
        }
        Log.d(TAG, "getSelectSingleItemValueIndex title = " + title + ", result = " + result);
        return result;
    }*/

    /*public int getKeyValueIndex(String key, String value) {
        int result = 0;
        if (key == null) {
            return result;
        }
        switch (key) {
            case KEY_LNB_TYPE:
                result = getIndexFromArray(CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE_LIST, value);
                break;
            case KEY_UNICABLE_SWITCH:
                result = getIndexFromArray(CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_SWITCH_LIST, value);
                break;
            case KEY_USER_BAND:
                result = getIndexFromArray(CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_USER_BAND_LIST, value);
                break;
            case KEY_POSITION:
                result = getIndexFromArray(CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_POSITION_LIST, value);
                break;
        }
        Log.d(TAG, "getKeyValueIndex key = " + key + ", value = " + value + ", result = " + result);
        return result;
    }

    private int getIndexFromArray(int[] arrays, String value) {
        if (arrays != null && arrays.length > 0) {
            for (int i = 0; i < arrays.length; i++) {
                Log.d(TAG, "getIndexFromArray arrays[i] = " + arrays[i] + ", value = " + value);
                if (TextUtils.equals(value, mContext.getString(arrays[i]))) {
                    return i;
                }
            }
        }
        return 0;
    }*/

    /*public String switchTilteToKey(String title) {
        String result = null;
        if (title == null) {
            return result;
        }
        switch (title) {
            case CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE:
                result = KEY_LNB_TYPE;
                break;
            case CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_SWITCH:
                result = KEY_UNICABLE_SWITCH;
                break;
            case CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM_USER_BAND:
                result = KEY_USER_BAND;
                break;
            case CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM_UB_FREQUENCY:
                result = KEY_UB_FREQUENCY;
                break;
            case CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM_POSITION:
                result = KEY_POSITION;
                break;
        }
        Log.d(TAG, "switchTilteToKey title = " + title + ", result = " + result);
        return result;
    }*/

    public int getStrengthStatus() {
        int result = 0;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(0);
            JSONObject jsonObj = DtvkitGlueClient.getInstance().request("Dvb.getFrontend", args1);
            if (jsonObj != null) {
                Log.d(TAG, "getStrengthStatus resultObj:" + jsonObj.toString());
            } else {
                Log.d(TAG, "getStrengthStatus then get null");
            }
            JSONObject data = null;
            if (jsonObj != null) {
                data = (JSONObject)jsonObj.get("data");
            }
            if (data == null || data.length() == 0) {
                return result;
            } else {
                result = (int)(data.get("strength"));
                return result;
            }
        } catch (Exception e) {
            Log.d(TAG, "getStrengthStatus Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    public int getQualityStatus() {
        int result = 0;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(0);
            JSONObject jsonObj = DtvkitGlueClient.getInstance().request("Dvb.getFrontend", args1);
            if (jsonObj != null) {
                Log.d(TAG, "getQualityStatus resultObj:" + jsonObj.toString());
            } else {
                Log.d(TAG, "getQualityStatus then get null");
            }
            JSONObject data = null;
            if (jsonObj != null) {
                data = (JSONObject)jsonObj.get("data");
            }
            if (data == null || data.length() == 0) {
                return result;
            } else {
                result = (int)(data.get("integrity"));
                return result;
            }
        } catch (Exception e) {
            Log.d(TAG, "getQualityStatus Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    public void dishMove(int direction, int step) {
        String derection = null;
        switch (direction) {
            case 0:
                derection = "east";
                break;
            case 2:
                derection = "west";
                break;
            case 3:
                derection = "center";
                break;
            default:
                derection = "center";
                break;
        }
        try {
            JSONArray args1 = new JSONArray();
            args1.put(derection);
            args1.put(step);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.dishMove", args1);
            if (resultObj != null) {
                Log.d(TAG, "dishMove resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "dishMove then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "dishMove Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "dishMove " + derection + "->" + step);
    }

    public void stopDishMove() {
        try {
            JSONArray args1 = new JSONArray();
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.stopDishMove", args1);
            if (resultObj != null) {
                Log.d(TAG, "stopDishMove resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "stopDishMove then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "stopDishMove Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "stopDishMove");
    }

    public void storeDishPosition(int position) {
        try {
            JSONArray args1 = new JSONArray();
            args1.put(position);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.storeDishPosition", args1);
            if (resultObj != null) {
                Log.d(TAG, "storeDishPosition resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "storeDishPosition then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "storeDishPosition Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "storeDishPosition->" + position);
    }

    public void moveDishToPosition(int position) {
        try {
            JSONArray args1 = new JSONArray();
            args1.put(position);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.moveDishToPosition", args1);
            if (resultObj != null) {
                Log.d(TAG, "moveDishToPosition resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "moveDishToPosition then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "moveDishToPositions Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "moveDishToPosition->" + position);
    }

    public void enableDishLimits(boolean status) {
        try {
            JSONArray args1 = new JSONArray();
            args1.put(status);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.enableDishLimits", args1);
            if (resultObj != null) {
                Log.d(TAG, "enableDishLimits resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "enableDishLimits then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "enableDishLimits Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "enableDishLimits->" + status);
    }

    public void setDishLimits(int east, int west) {
        try {
            JSONArray args1 = new JSONArray();
            args1.put(east);
            args1.put(west);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.setDishLimits", args1);
            if (resultObj != null) {
                Log.d(TAG, "setDishLimits resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setDishLimits then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setDishLimits Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "setDishLimits east->" + east + ", west->" + west);
    }

    public String getSatelliteName(String name) {
        return name;
    }

    public String getSatelliteDirection(String name) {
        String result = "";
        try {
            JSONObject jsonObj = getSatellites();
            JSONArray data = null;
            if (jsonObj != null) {
                data = (JSONArray)jsonObj.get("data");
            }
            if (data == null || data.length() == 0) {
                return result;
            }
            String current = name;
            int type = 0;
            for (int i = 0; i < data.length(); i++) {
                //Log.d(TAG, "getSatelliteDirection data:" + data.get(i));
                String satellitevalue = (String)(((JSONObject)(data.get(i))).get("name"));
                //Log.d(TAG, "getSatelliteList satellitevalue = " + satellitevalue);
                if (TextUtils.equals(satellitevalue, current)) {
                    result = (boolean)(((JSONObject)(data.get(i))).get("east_west")) ? "east" : "west";
                    return result;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "getSatelliteDirection Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "getSatelliteDirection need to add " + result);
        return result;
    }

    public String getSatelliteLongitude(String name) {
        String result = "90";
        try {
            JSONObject jsonObj = getSatellites();
            JSONArray data = null;
            if (jsonObj != null) {
                data = (JSONArray)jsonObj.get("data");
            }
            if (data == null || data.length() == 0) {
                return result;
            }
            String current = name;
            int type = 0;
            for (int i = 0; i < data.length(); i++) {
                //Log.d(TAG, "getSatelliteDirection data:" + data.get(i));
                String satellitevalue = (String)(((JSONObject)(data.get(i))).get("name"));
                //Log.d(TAG, "getSatelliteList satellitevalue = " + satellitevalue);
                if (TextUtils.equals(satellitevalue, current)) {
                    result = String.valueOf((int)(((JSONObject)(data.get(i))).get("long_pos")));
                    return result;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "getSatelliteLongitude Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "getSatelliteLongitude need to add" + result);
        return result;
    }

    public JSONObject getSatellites() {
        JSONObject resultObj = null;
        try {
            resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getSatellites", new JSONArray());
            if (resultObj != null) {
                Log.d(TAG, "getSatellites resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "getSatellites then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getSatellites Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public int getSatelliteIndex(String satellite) {
        int result  = 0;
        try {
            JSONObject resultObj = getSatellites();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                String current = satellite;
                int type = 0;
                for (int i = 0; i < data.length(); i++) {
                    //Log.d(TAG, "getSatelliteIndex data:" + data.get(i));
                    String satellitevalue = (String)(((JSONObject)(data.get(i))).get("name"));
                    //Log.d(TAG, "getSatelliteIndex satellitevalue = " + satellitevalue);
                    if (TextUtils.equals(satellitevalue, current)) {
                        result = i;
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "getSatelliteIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    public JSONObject getTransponders(String satellite) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(satellite);
            resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getTransponders", args1);
            if (resultObj != null) {
                Log.d(TAG, "getTransponders resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "getTransponders then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getTransponders Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public void addSatellite(String name, boolean isEast, int position) {
        try {
            JSONArray args = new JSONArray();
            args.put(name);
            args.put(isEast);
            args.put(position);
            Log.d(TAG, "addSatallite->" + args.toString());
            DtvkitGlueClient.getInstance().request("Dvbs.addSatellite", args);
            /*JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getSatellites", new JSONArray());
            if (resultObj != null) {
                Log.d(TAG, "addSatallite resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "addSatallite then get null");
            }*/
        } catch (Exception e) {
            Log.d(TAG, "addSatallite Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
    }

    public void editSatellite(String name, boolean isEast, int position) {
        try {
            JSONArray args = new JSONArray();
            args.put(name);
            args.put(isEast);
            args.put(position);
            Log.d(TAG, "editSatellite->" + args.toString());
            DtvkitGlueClient.getInstance().request("Dvbs.addSatellite", args);
            /*JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getSatellites", new JSONArray());
            if (resultObj != null) {
                Log.d(TAG, "editSatellite resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "editSatellite then get null");
            }*/
        } catch (Exception e) {
            Log.d(TAG, "editSatellite Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
    }

    public void removeSatellite(String name) {
        try {
            JSONArray args = new JSONArray();
            args.put(name);
            Log.d(TAG, "removeSatellite->" + args.toString());
            DtvkitGlueClient.getInstance().request("Dvbs.deleteSatellite", args);
            /*JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getSatellites", args);
            if (resultObj != null) {
                Log.d(TAG, "removeSatellite resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "removeSatellites then get null");
            }*/
        } catch (Exception e) {
            Log.d(TAG, "removeSatellite Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
    }

    public void addTransponder(String satellite, int frequency, String polarity, int symbolrate) {
        try {
            JSONArray args = new JSONArray();
            args.put(satellite);
            args.put(frequency);
            args.put(polarity);
            args.put(symbolrate);
            Log.d(TAG, "addTransponder->" + args.toString());
            DtvkitGlueClient.getInstance().request("Dvbs.addTransponder", args);
            /*JSONArray args1 = new JSONArray();
            args1.put(satellite);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getTransponders", args1);
            if (resultObj != null) {
                Log.d(TAG, "addTransponder resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "addTransponder then get null");
            }*/
        } catch (Exception e) {
            Log.d(TAG, "addTransponder Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
    }

    public void editTransponder(String satellite, int frequency, String polarity, int symbolrate) {
        try {
            JSONArray args = new JSONArray();
            args.put(satellite);
            args.put(frequency);
            args.put(polarity);
            args.put(symbolrate);
            Log.d(TAG, "editTransponder->" + args.toString());
            DtvkitGlueClient.getInstance().request("Dvbs.addTransponder", args);
            /*JSONArray args1 = new JSONArray();
            args1.put(satellite);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getTransponders", args1);
            if (resultObj != null) {
                Log.d(TAG, "editTransponder resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "editTransponder then get null");
            }*/
        } catch (Exception e) {
            Log.d(TAG, "editTransponder Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
    }

    public void removeTransponder(String name, int frequency, String polarity, int symbolrate) {
        try {
            JSONArray args = new JSONArray();
            args.put(name);
            args.put(frequency);
            args.put(polarity);
            args.put(symbolrate);
            Log.d(TAG, "removeTransponder->" + args.toString());
            DtvkitGlueClient.getInstance().request("Dvbs.deleteTransponder", args);
            /*JSONArray args1 = new JSONArray();
            args1.put(name);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getTransponders", args1);
            if (resultObj != null) {
                Log.d(TAG, "removeTransponder resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "removeTransponder then get null");
            }*/
        } catch (Exception e) {
            Log.d(TAG, "removeTransponder Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
    }

    public List<String> getCountryList() {
        List<String> result = new ArrayList<String>();
        try {
            JSONObject resultObj = getCountrys();
            Locale[] allLocale = Locale.getAvailableLocales();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                for (int i = 0; i < data.length(); i++) {
                    String countryName = (String)(((JSONObject)(data.get(i))).get("country_name"));
                    if (allLocale != null && allLocale.length > 0) {
                        Log.d(TAG, "allLocale size = " + allLocale.length);
                        for (Locale one : allLocale) {
                            try {
                                String iso3Country = one.getISO3Country();
                                boolean isEqual = (countryName == null ? iso3Country == null :countryName.equalsIgnoreCase(iso3Country));
                                if (isEqual) {
                                    countryName = one.getDisplayCountry();
                                    break;
                                }
                            } catch (Exception e1) {
                                Log.e(TAG, "getCountryList ios3 country Exception " + e1.getMessage() + ", trace=" + e1.getStackTrace() + " continue");
                                continue;
                            }
                        }
                    }
                    result.add(countryName);
                }

            } else {
                Log.d(TAG, "getCountryList then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCountryList Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }

        return result;
    }

    private JSONObject getCountrys() {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.getCountrys", args1);
            if (resultObj != null) {
                Log.d(TAG, "getCountrys resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "getCountrys then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCountrys Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public List<String> getLangList() {
        List<String> result = new ArrayList<String>();
        try {
            JSONObject resultObj = getCountryLangs(getCountryCodeByIndex(getIntParameters(KEY_DTVKIT_COUNTRY)));
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                for (int i = 0; i < data.length(); i++) {
                    String langIds = (String)(((JSONObject)(data.get(i))).get("lang_ids"));
                    result.add(langIds);
                }

            } else {
                Log.d(TAG, "getLangList then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getLangList Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    private JSONObject getCountryLangs(int countryCode) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(countryCode);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.getCountryLangs", args1);
            if (resultObj != null) {
                Log.d(TAG, "getCountryLangs resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "getCountryLangs then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCountryLangs Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject setCountryCodeByIndex(int index) {
        JSONObject resultObj = null;
        try {
            resultObj = getCountrys();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return resultObj;
                }
                resultObj = (JSONObject)(data.get(index));
                if (resultObj != null) {
                    Log.d(TAG, "setCountryCodeByIndex resultObj = " + resultObj.toString());
                    setCountry((int)resultObj.get("country_code"));
                }
            } else {
                Log.d(TAG, "setCountryCodeByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setCountryCodeByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public int getCountryCodeByIndex(int index) {
        int countrycode = 0;
        JSONObject resultObj = null;
        try {
            resultObj = getCountrys();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return countrycode;
                }
                resultObj = (JSONObject)(data.get(index));
                if (resultObj != null) {
                    Log.d(TAG, "getCountryCodeByIndex resultObj = " + resultObj.toString());
                    countrycode = ((int)resultObj.get("country_code"));
                }
            } else {
                Log.d(TAG, "getCountryCodeByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCountryCodeByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return countrycode;
    }

    private JSONObject setCountry(int countryCode) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(countryCode);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setCountry", args1);
            if (resultObj != null) {
                Log.d(TAG, "setCountry resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setCountry then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setCountry Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public int getLangIndexCodeByIndex(int index) {
        int langIndex = 0;
        try {
            int currentCountryCode = getCountryCodeByIndex(getIntParameters(KEY_DTVKIT_COUNTRY));
            JSONObject resultObj = getCountryLangs(currentCountryCode);
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return langIndex;
                }
                resultObj = (JSONObject)(data.get(index));
                if (resultObj != null) {
                    Log.d(TAG, "getLangIndexCodeByIndex resultObj = " + resultObj.toString());
                    langIndex = ((int)resultObj.get("lang_index"));
                }
            } else {
                Log.d(TAG, "getLangIndexCodeByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getLangIndexCodeByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return langIndex;
    }

    public JSONObject setPrimaryAudioLangByIndex(int index) {
        JSONObject resultObj = null;
        try {
            int langIndex = getLangIndexCodeByIndex(index);
            resultObj = setPrimaryAudioLangId(langIndex);
            if (resultObj != null) {
                Log.d(TAG, "setPrimaryAudioLangByIndex resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setPrimaryAudioLangByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setPrimaryAudioLangByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    private JSONObject setPrimaryAudioLangId(int langIndex) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(langIndex);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setPrimaryAudioLangId", args1);
            if (resultObj != null) {
                Log.d(TAG, "setPrimaryAudioLangId resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setPrimaryAudioLangId then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setPrimaryAudioLangId Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject setSecondaryAudioLangByIndex(int index) {
        JSONObject resultObj = null;
        try {
            int langIndex = getLangIndexCodeByIndex(index);
            resultObj = setSecondaryAudioLangId(langIndex);
            if (resultObj != null) {
                Log.d(TAG, "setSecondaryAudioLangByIndex resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setSecondaryAudioLangByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setSecondaryAudioLangByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    private JSONObject setSecondaryAudioLangId(int langIndex) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(langIndex);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setSecondaryAudioLangId", args1);
            if (resultObj != null) {
                Log.d(TAG, "setSecondaryAudioLangId resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setSecondaryAudioLangId then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setSecondaryAudioLangId Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject setPrimaryTextLangByIndex(int index) {
        JSONObject resultObj = null;
        try {
            int langIndex = getLangIndexCodeByIndex(index);
            resultObj = setPrimaryTextLangId(langIndex);
            if (resultObj != null) {
                Log.d(TAG, "setPrimaryTextLangByIndex resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setPrimaryTextLangByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setPrimaryTextLangByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    private JSONObject setPrimaryTextLangId(int langIndex) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(langIndex);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setPrimaryTextLangId", args1);
            if (resultObj != null) {
                Log.d(TAG, "setPrimaryTextLangId resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setPrimaryTextLangId then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setPrimaryTextLangId Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject setSecondaryTextLangByIndex(int index) {
        JSONObject resultObj = null;
        try {
            int langIndex = getLangIndexCodeByIndex(index);
            resultObj = setSecondaryTextLangId(langIndex);
            if (resultObj != null) {
                Log.d(TAG, "setSecondaryTextLangByIndex resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setSecondaryTextLangByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setSecondaryTextLangByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    private JSONObject setSecondaryTextLangId(int langIndex) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(langIndex);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setSecondaryTextLangId", args1);
            if (resultObj != null) {
                Log.d(TAG, "setSecondaryTextLangId resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setSecondaryTextLangId then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setSecondaryTextLangId Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }
}
