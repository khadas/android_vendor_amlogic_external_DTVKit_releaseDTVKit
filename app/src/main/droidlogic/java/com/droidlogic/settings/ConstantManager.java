package com.droidlogic.settings;

import android.util.Log;
import android.media.tv.TvTrackInfo;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


import com.droidlogic.app.SystemControlManager;

public class ConstantManager {

    private static final String TAG = "ConstantManager";
    private static final boolean DEBUG = true;

    public static final String PI_FORMAT_KEY = "pi_format";
    public static final String KEY_AUDIO_CODES_DES = "audio_codes";
    public static final String KEY_TRACK_PID = "pid";

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

    public static void ascendTrackInfoOderByPid(List<TvTrackInfo> list) {
        if (list != null) {
            Collections.sort(list, new PidAscendComparator());
        }
    }

    public static class PidAscendComparator implements Comparator<TvTrackInfo> {
        public int compare(TvTrackInfo o1, TvTrackInfo o2) {
            Integer pid1 = new Integer(o1.getExtra().getInt("pid", 0));
            Integer pid2 = new Integer(o2.getExtra().getInt("pid", 0));
            return pid1.compareTo(pid2);
        }
    }

    public static class PidDscendComparator implements Comparator<TvTrackInfo> {
        public int compare(TvTrackInfo o1, TvTrackInfo o2) {
            Integer pid1 = new Integer(o1.getExtra().getInt("pid", 0));
            Integer pid2 = new Integer(o2.getExtra().getInt("pid", 0));
            return pid2.compareTo(pid1);
        }
    }
}
