package com.hello.ble.stack;

import com.google.common.io.LittleEndianDataInputStream;
import com.hello.ble.HelloBlePacket;
import com.hello.ble.PillMotionData;
import com.hello.ble.devices.Pill;
import com.hello.ble.util.BleUUID;

import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created by pangwu on 7/14/14.
 */
public class MotionDataHandler extends HelloDataHandler<List<PillMotionData>> {

    private int totalPackets = 0;

    private DateTime startTime;
    private byte[] buffer;

    private int bufferOffsetIndex = 0;

    private LinkedList<HelloBlePacket> packets = new LinkedList<>();

    public MotionDataHandler(final Pill sender) {
        super(sender);
    }

    @Override
    public boolean shouldProcess(final UUID charUUID) {
        if(charUUID.equals(BleUUID.CHAR_DATA_UUID)){
            return true;
        }

        return false;
    }

    @Override
    public void onDataArrival(final HelloBlePacket blePacket) {
        if(blePacket.sequenceNumber == 0){
            // Assume the packets arrive in order.
            this.packets.clear();
            this.totalPackets = blePacket.payload[0];
            this.bufferOffsetIndex = 0;

            final HelloBlePacket headPacket = new HelloBlePacket(0, Arrays.copyOfRange(blePacket.payload, 1, blePacket.payload.length));
            this.packets.add(headPacket);

            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(headPacket.payload);
            final LittleEndianDataInputStream inputStream = new LittleEndianDataInputStream(byteArrayInputStream);

            try {
                final byte version = inputStream.readByte();
                final byte reserved1 = inputStream.readByte();
                final int structLength = inputStream.readUnsignedShort();

                this.buffer = new byte[structLength];
                inputStream.close();

            }catch (IOException ioe){
                ioe.printStackTrace();
            }

        }else{
            this.packets.add(blePacket);
        }

        final HelloBlePacket lastPacket = this.packets.getLast();
        for(int i = 0; (this.bufferOffsetIndex < this.buffer.length && i < lastPacket.payload.length); i++, this.bufferOffsetIndex++){
            this.buffer[this.bufferOffsetIndex] = lastPacket.payload[i];
        }

        if(this.packets.size() == this.totalPackets){
            final List<PillMotionData> data = PillMotionData.fromBytes(this.buffer);
            this.dataFinished(data);
        }
    }

}
