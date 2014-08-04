package com.hello.ble.devices;

import com.hello.ble.BleOperationCallback;

/**
 * Created by pangwu on 7/31/14.
 */
public interface HelloBleDevice {
    void connect(final BleOperationCallback<Void> connectedCallback);
    void disconnect();
    boolean isConnected();
}
