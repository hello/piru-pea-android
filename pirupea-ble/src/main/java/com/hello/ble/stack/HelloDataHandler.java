package com.hello.ble.stack;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.HelloBlePacket;
import com.hello.ble.devices.HelloBleDevice;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by pangwu on 7/14/14.
 */
public abstract class HelloDataHandler<T> {
    private BleOperationCallback<T> dataCallback;
    private HelloBleDevice sender;

    public HelloDataHandler(final HelloBleDevice sender){
        checkNotNull(sender);
        this.sender = sender;
    }

    protected abstract boolean shouldProcess(final UUID charUUID);
    protected abstract void onDataArrival(final HelloBlePacket blePacket);

    public final void setDataCallback(final BleOperationCallback<T> bleOperationCallback){
        this.dataCallback = bleOperationCallback;
    }

    public final BleOperationCallback<T> getDataCallback(){
        return this.dataCallback;
    }

    protected void dataFinished(final T data){
        if(this.getDataCallback() != null){
            this.getDataCallback().onCompleted(this.sender, data);
        }
    }
}
