package com.enkeli;

import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class LogUtil {
    private static final boolean TIMING_ENABLED = true;
    private static final boolean DEBUG_ENABLED = true;
    private static final boolean INFO_ENABLED = true;

    private static final String TIMING = "Timing";
    private static final String DB_URL = "DATABASE URL: ";

    private static Map<String, Long> timings = new HashMap<String, Long>();

    private static boolean isDebugEnabled() {
        return DEBUG_ENABLED;
    }

    public static void logTimeStart(String tag, String operation) {
        if (TIMING_ENABLED) {
            timings.put(tag + operation, new Date().getTime());
            Log.i(TIMING, tag + ": " + operation + " started");
        }
    }

    public static void logTimeStop(String tag, String operation) {
        if (TIMING_ENABLED) {
            if (timings.containsKey(tag + operation)) {
                Log.i(TIMING, tag + ": " + operation + " finished for "
                        + (new Date().getTime() - timings.get(tag + operation)) / 1000 + "sec");
            }
        }
    }

    public static void logDebug(String tag, String message) {
        if (isDebugEnabled()) {
            Log.d(tag, message);
        }
    }

    public static void logWarn(String tag, String message) {
        Log.w(tag, message);
    }

    public static void logInfo(String tag, String message) {
        if (INFO_ENABLED) {
            Log.i(tag, message);
        }
    }

    public static void logError(String tag, String message, Exception e) {
        Log.e(tag, message, e);
    }

    public static void logError(String tag, String message) {
        Log.e(tag, message);
    }

}
