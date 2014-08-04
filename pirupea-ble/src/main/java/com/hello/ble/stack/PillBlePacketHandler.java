package com.hello.ble.stack;

import com.hello.ble.HelloBlePacket;

import java.util.Arrays;

/**
 * Created by pangwu on 7/31/14.
 */
public class PillBlePacketHandler extends BlePacketHandler {
    @Override
    public HelloBlePacket getHelloBlePacket(byte[] blePacket) {
        int sequenceNumber = blePacket[0];
        return new HelloBlePacket(sequenceNumber, Arrays.copyOfRange(blePacket, 1, blePacket.length));
    }
}
