package com.hello.ble;

import android.content.Context;
import android.util.Log;

public class HelloBle {
    // TODO: Refactor bluetooth library to use dependency injection.

    private static Context applicationContext;
    private static Logger logger;

    public static void init(Context context, Logger logger) {
        HelloBle.applicationContext = context.getApplicationContext();
        HelloBle.logger = logger;
    }


    //region Getters

    public static Context getApplicationContext() {
        return applicationContext;
    }

    //endregion


    //region Logging

    public static void logError(String tag, String message, Throwable e) {
        if (logger != null) {
            if (e != null) {
                logger.println(Log.ERROR, tag, message + "\n" + Log.getStackTraceString(e));
            } else {
                logger.println(Log.ERROR, tag, message);
            }
        } else {
            Log.e(tag, message, e);
        }
    }

    public static void logInfo(String tag, String message) {
        if (logger != null) {
            logger.println(Log.INFO, tag, message);
        } else {
            Log.i(tag, message);
        }
    }

    //endregion


    public interface Logger {
        void println(int priority, String tag, String message);
    }
}
