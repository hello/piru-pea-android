package com.hello.ble;

import android.app.Application;
import android.content.Context;

/**
 * Created by pangwu on 7/11/14.
 */
public class LibApplication extends Application {
    private static Context context;

    public void onCreate(){
        super.onCreate();
        LibApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return LibApplication.context;
    }
}
