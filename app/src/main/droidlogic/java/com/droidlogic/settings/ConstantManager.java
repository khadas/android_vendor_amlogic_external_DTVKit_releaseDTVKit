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
    public static final String KEY_AUDIO_CODES_DES = "audio_codes";

    public static final String EVENT_STREAM_PI_FORMAT = "event_pi_format";

    public static final String CONSTANT_QAA = "qaa";//Original Audio flag
    public static final String CONSTANT_ORIGINAL_AUDIO = "Original Audio";
    public static final String CONSTANT_UND_FLAG = "und";//undefined flag
    public static final String CONSTANT_UND_VALUE = "Undefined";

    public static final Map<String, String> PI_TO_VIDEO_FORMAT_MAP = new HashMap<>();
    static {
        PI_TO_VIDEO_FORMAT_MAP.put("interlace", "I");
        PI_TO_VIDEO_FORMAT_MAP.put("progressive", "P");
    }

    public static final String SYS_HEIGHT_PATH = "/sys/class/video/frame_height";
    public static final String SYS_PI_PATH = "/sys/class/deinterlace/di0/frame_format";
}
