package com.droidlogic.settings;

import android.util.Log;

import java.lang.reflect.Method;

public class PropSettingManager {

    private static final String TAG = "PropSettingManager";
    private static final boolean DEBUG = true;

    public static final String TV_STREAM_TIME = "tv.stream.realtime";//sync with TvTime.java

    public static long getLong(String key, long def) {
        long result = def;
        try {
            Class clz = Class.forName("android.os.SystemProperties");
            Method method = clz.getMethod("getLong", String.class, long.class);
            result = (long)method.invoke(clz, key, def);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getLong Exception = " + e.getMessage());
        }
        if (DEBUG) {
            Log.i(TAG, "getLong key = " + key + ", result = " + result);
        }
        return result;
    }

    public static int getInt(String key, int def) {
        int result = def;
        try {
            Class clz = Class.forName("android.os.SystemProperties");
            Method method = clz.getMethod("getInt", String.class, int.class);
            result = (int)method.invoke(clz, key, def);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "getLInt Exception = " + e.getMessage());
        }
        if (DEBUG) {
            Log.i(TAG, "getInt key = " + key + ", result = " + result);
        }
        return result;
    }

    public static String getString(String key, String def) {
        String result = def;
        try {
            Class clz = Class.forName("android.os.SystemProperties");
            Method method = clz.getMethod("get", String.class, String.class);
            result = (String)method.invoke(clz, key, def);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "getString Exception = " + e.getMessage());
        }
        if (DEBUG) {
            Log.i(TAG, "getString key = " + key + ", result = " + result);
        }
        return result;
    }

    public static boolean getBoolean(String key, boolean def) {
        boolean result = def;
        try {
            Class clz = Class.forName("android.os.SystemProperties");
            Method method = clz.getMethod("getBoolean", String.class, boolean.class);
            result = (boolean)method.invoke(clz, key, def);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "getBoolean Exception = " + e.getMessage());
        }
        if (DEBUG) {
            Log.i(TAG, "getBoolean key = " + key + ", result = " + result);
        }
        return result;
    }

    public static long getCurrentStreamTime(boolean streamtime) {
        long result = System.currentTimeMillis();
        if (streamtime) {
            result = result + getLong(TV_STREAM_TIME, 0);
        }
        return result;
    }

    public static long getStreamTimeDiff() {
        long result = 0;
        result = getLong(TV_STREAM_TIME, 0);
        return result;
    }
}
