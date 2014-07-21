package com.hello.ble.stack;

import android.bluetooth.BluetoothGatt;

import com.hello.ble.PillOperationCallback;
import com.hello.ble.devices.Pill;

/**
 * Created by pangwu on 7/15/14.
 */
class GattOperationTimeoutRunnable implements Runnable {

    private BluetoothGatt gatt;
    private PillOperationCallback<Void> disconnectCallback;
    private Pill sender;

    private GattOperationTimeoutRunnable(){}

    public GattOperationTimeoutRunnable(final Pill sender,
                                        final BluetoothGatt gatt,
                                        final PillOperationCallback<Void> disconnectCallback){
        this.gatt = gatt;
        this.disconnectCallback = disconnectCallback;
        this.sender = sender;
    }

    @Override
    public void run() {
        this.gatt.close();

        // Although this is a timeout, we still need to make sure the
        // callback set by user is still called.
        if(this.disconnectCallback != null && this.sender != null){
            this.disconnectCallback.onCompleted(this.sender, null);
        }
    }
}
