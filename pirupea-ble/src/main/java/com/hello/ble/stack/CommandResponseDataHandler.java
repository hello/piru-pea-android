package com.hello.ble.stack;

import com.hello.ble.HelloBlePacket;
import com.hello.ble.PillCommand;
import com.hello.ble.devices.Pill;
import com.hello.ble.util.BleUUID;

import java.util.UUID;

/**
 * Created by pangwu on 7/18/14.
 */
public class CommandResponseDataHandler extends HelloDataHandler<PillCommand> {

    public CommandResponseDataHandler(final Pill pill){
        super(pill);
    }

    @Override
    protected boolean shouldProcess(UUID charUUID) {
        if(BleUUID.CHAR_COMMAND_RESPONSE_UUID.equals(charUUID)){
            return true;
        }
        return false;
    }

    @Override
    protected void onDataArrival(HelloBlePacket blePacket) {
        final PillCommand command = PillCommand.fromByte(blePacket.payload[1]);
        this.dataFinished(command);
    }
}
