package com.hello.pirupea;

import android.app.Application;

import com.hello.ble.HelloBle;

public class PiruPeaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        HelloBle.init(this, null);
    }
}
