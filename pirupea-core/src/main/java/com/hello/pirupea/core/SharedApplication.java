package com.hello.pirupea.core;

import android.app.Application;
import android.content.Context;

/**
 * Created by pangwu on 7/11/14.
 */
public class SharedApplication extends Application {
    private static Context context;

    public void onCreate(){
        super.onCreate();
        SharedApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return SharedApplication.context;
    }
}
