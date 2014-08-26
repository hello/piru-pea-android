package com.hello.ble.stack.application;

import com.google.common.io.LittleEndianDataInputStream;
import com.hello.ble.HelloBlePacket;
import com.hello.ble.devices.Pill;
import com.hello.ble.util.BleUUID;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by pangwu on 8/11/14.
 */
public class MotionStreamDataHandler extends HelloDataHandler<Integer[]> {

    public MotionStreamDataHandler(final Pill sender) {
        super(sender);
    }

    @Override
    public boolean shouldProcess(final UUID charUUID) {
        return BleUUID.CHAR_DATA_UUID.equals(charUUID);
    }

    @Override
    public void onDataArrival(final HelloBlePacket blePacket) {
        final byte[] bytes = Arrays.copyOfRange(blePacket.payload, 1, blePacket.payload.length);

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        final LittleEndianDataInputStream littleEndianDataInputStream = new LittleEndianDataInputStream(byteArrayInputStream);
        final Integer[] xyz = new Integer[4];

        try {
            xyz[0] = (int)littleEndianDataInputStream.readShort();
            xyz[1] = (int)littleEndianDataInputStream.readShort();
            xyz[2] = (int)littleEndianDataInputStream.readShort();
            xyz[3] = littleEndianDataInputStream.readInt();

            littleEndianDataInputStream.close();
            byteArrayInputStream.close();

            this.dataFinished(xyz);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }


    }
}
