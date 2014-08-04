package com.hello.ble.stack;

import com.hello.ble.HelloBlePacket;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by pangwu on 7/31/14.
 */
public abstract class BlePacketHandler {
    private final Set<HelloDataHandler> dataHanlders = new HashSet<>();

    protected abstract HelloBlePacket getHelloBlePacket(final byte[] blePacket);

    public final void dispatch(final UUID charUUID, final byte[] blePacket){
        final HelloBlePacket helloBlePacket = getHelloBlePacket(blePacket);

        for (final HelloDataHandler handler : this.dataHanlders) {
            if(!handler.shouldProcess(charUUID)){
                continue;
            }

            handler.onDataArrival(helloBlePacket);
        }
    }

    public final void registerDataHandler(final HelloDataHandler handler){
        this.dataHanlders.add(handler);
    }

    public final void unregisterDataHandler(final HelloDataHandler handler) {
        this.dataHanlders.remove(handler);
    }
}
