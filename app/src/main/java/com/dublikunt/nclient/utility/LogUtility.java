package com.dublikunt.nclient.utility;

import android.util.Log;

import java.util.Arrays;

public class LogUtility {
    public static final String LogTag = "NCLIENT";

    public static void d(Object... message) {
        if (message == null) return;
        if (message.length == 1) Log.d(LogUtility.LogTag, "" + message[0]);
        else Log.d(LogUtility.LogTag, Arrays.toString(message));
    }

    public static void d(Object message, Throwable throwable) {
        if (message == null) message = "";
        Log.d(LogUtility.LogTag, message.toString(), throwable);
    }

    public static void i(Object... message) {
        if (message == null) return;
        if (message.length == 1) Log.i(LogUtility.LogTag, "" + message[0]);
        else Log.i(LogUtility.LogTag, Arrays.toString(message));
    }

    public static void i(Object message, Throwable throwable) {
        if (message == null) message = "";
        Log.i(LogUtility.LogTag, message.toString(), throwable);
    }

    public static void e(Object... message) {
        if (message == null) return;
        if (message.length == 1) Log.e(LogUtility.LogTag, "" + message[0]);
        else Log.e(LogUtility.LogTag, Arrays.toString(message));
    }

    public static void e(Object message, Throwable throwable) {
        if (message == null) message = "";
        Log.e(LogUtility.LogTag, message.toString(), throwable);
    }
}
