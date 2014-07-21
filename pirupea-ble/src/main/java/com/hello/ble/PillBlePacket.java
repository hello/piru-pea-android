package com.hello.ble;

/**
 * Created by pangwu on 7/14/14.
 */
public class PillBlePacket {
    public final int sequenceNumber;
    public final byte[] payload;

    public PillBlePacket(final int sequenceNumber, final byte[] payload){
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }
}
