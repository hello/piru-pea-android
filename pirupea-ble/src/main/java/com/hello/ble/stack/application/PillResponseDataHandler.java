package com.hello.ble.stack.application;

import com.hello.ble.HelloBlePacket;
import com.hello.ble.PillCommand;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.util.BleUUID;

import java.util.UUID;

/**
 * Created by pangwu on 7/18/14.
 */
public class PillResponseDataHandler extends HelloDataHandler<PillCommand> {

    public PillResponseDataHandler(final HelloBleDevice helloBleDevice){
        super(helloBleDevice);
    }

    @Override
    public boolean shouldProcess(UUID charUUID) {
        if(BleUUID.CHAR_COMMAND_RESPONSE_UUID.equals(charUUID)){
            return true;
        }
        return false;
    }

    @Override
    public void onDataArrival(HelloBlePacket blePacket) {
        final PillCommand command = PillCommand.fromByte(blePacket.payload[1]);
        this.dataFinished(command);
    }
}
