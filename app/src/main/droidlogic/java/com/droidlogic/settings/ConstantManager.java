package com.droidlogic.settings;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

import com.droidlogic.app.SystemControlManager;

public class ConstantManager {

    private static final String TAG = "ConstantManager";
    private static final boolean DEBUG = true;

    public static final String PI_FORMAT_KEY = "pi_format";

    public static final String EVENT_STREAM_PI_FORMAT  = "event_pi_format";

    public static final Map<String, String> PI_TO_VIDEO_FORMAT_MAP = new HashMap<>();
    static {
        PI_TO_VIDEO_FORMAT_MAP.put("interlace", "I");
        PI_TO_VIDEO_FORMAT_MAP.put("progressive", "P");
    }

    public static final String SYS_HEIGHT_PATH = "/sys/class/video/frame_height";
    public static final String SYS_PI_PATH = "/sys/class/deinterlace/di0/frame_format";
}
