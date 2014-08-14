package com.hello.ble.stack.transmission;

import com.hello.ble.HelloBlePacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by pangwu on 7/31/14.
 */
public class PillBlePacketHandler extends BlePacketHandler {
    @Override
    public HelloBlePacket getHelloBlePacket(byte[] blePacket) {
        int sequenceNumber = blePacket[0];
        return new HelloBlePacket(sequenceNumber, Arrays.copyOfRange(blePacket, 1, blePacket.length));
    }

    @Override
    public List<byte[]> prepareBlePacket(byte[] applicationData) {
        final ArrayList<byte[]> packets = new ArrayList<>();
        packets.add(applicationData);
        return packets;
    }
}
