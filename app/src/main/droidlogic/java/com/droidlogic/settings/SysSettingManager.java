package com.droidlogic.settings;

import android.util.Log;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

import com.droidlogic.app.SystemControlManager;

public class SysSettingManager {

    private static final String TAG = "SysSettingManager";
    private static final boolean DEBUG = true;

    protected SystemControlManager mSystemControlManager;

    public SysSettingManager() {
        mSystemControlManager = SystemControlManager.getInstance();
    }

    public String readSysFs(String sys) {
        String result = mSystemControlManager.readSysFs(sys);
        if (DEBUG) {
            Log.d(TAG, "readSysFs sys = " + sys + ", result = " + result);
        }
        return result;
    }

    public String getVideoFormatFromSys() {
        String result = "";
        String height = readSysFs(ConstantManager.SYS_HEIGHT_PATH);
        String pi = readSysFs(ConstantManager.SYS_PI_PATH);
        if (!TextUtils.isEmpty(height) && !"NA".equals(height) && !TextUtils.isEmpty(pi) && !"null".equals(pi)) {
            result = height + ConstantManager.PI_TO_VIDEO_FORMAT_MAP.get(pi);
        }
        if (DEBUG) {
            Log.d(TAG, "getVideoFormatFromSys result = " + result);
        }
        return result;
    }
}
