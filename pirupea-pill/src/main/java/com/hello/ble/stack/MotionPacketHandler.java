package com.hello.ble.stack;

import com.google.common.io.LittleEndianDataInputStream;
import com.hello.ble.PillBlePacket;
import com.hello.ble.PillData;
import com.hello.ble.devices.Pill;
import com.hello.ble.util.PillUUID;

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
public class MotionPacketHandler extends PillBlePacketHandler<List<PillData>> {

    private int totalPackets = 0;

    private DateTime startTime;
    private byte[] buffer;

    private int bufferOffsetIndex = 0;

    private LinkedList<PillBlePacket> packets = new LinkedList<>();

    public MotionPacketHandler(final Pill sender) {
        super(sender);
    }

    @Override
    public boolean shouldProcess(final UUID charUUID) {
        if(charUUID.equals(PillUUID.CHAR_DATA_UUID)){
            return true;
        }

        return false;
    }

    @Override
    public void onDataArrival(final PillBlePacket blePacket) {
        if(blePacket.sequenceNumber == 0){
            // Assume the packets arrive in order.
            this.packets.clear();
            totalPackets = blePacket.payload[0];
            final PillBlePacket headPacket = new PillBlePacket(0, Arrays.copyOfRange(blePacket.payload, 1, blePacket.payload.length));
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

        final PillBlePacket lastPacket = this.packets.getLast();
        for(int i = 0; i < lastPacket.payload.length; i++, this.bufferOffsetIndex++){
            this.buffer[this.bufferOffsetIndex] = lastPacket.payload[i];
        }

        if(this.packets.size() == this.totalPackets){
            final List<PillData> data = PillData.fromBytes(this.buffer);
            this.dataFinished(data);
        }
    }

}