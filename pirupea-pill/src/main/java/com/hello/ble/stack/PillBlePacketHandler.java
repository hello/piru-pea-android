package com.hello.ble.stack;

import com.hello.ble.PillBlePacket;
import com.hello.ble.PillOperationCallback;
import com.hello.ble.devices.Pill;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by pangwu on 7/14/14.
 */
public abstract class PillBlePacketHandler<T> {
    private PillOperationCallback<T> dataCallback;
    private Pill sender;

    public PillBlePacketHandler(final Pill sender){
        checkNotNull(sender);
        this.sender = sender;
    }

    protected abstract boolean shouldProcess(final UUID charUUID);
    protected abstract void onDataArrival(final PillBlePacket blePacket);

    public final void setDataCallback(final PillOperationCallback<T> pillOperationCallback){
        this.dataCallback = pillOperationCallback;
    }

    public final PillOperationCallback<T> getDataCallback(){
        return this.dataCallback;
    }

    protected void dataFinished(final T data){
        if(this.getDataCallback() != null){
            this.getDataCallback().onCompleted(this.sender, data);
        }
    }
}
