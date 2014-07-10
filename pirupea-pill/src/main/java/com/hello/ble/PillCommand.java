package com.hello.ble;

/**
 * Created by pangwu on 7/2/14.
 */
public enum PillCommand {
    SET_TIME((byte)0x06),
    GET_TIME((byte)0x05),
    SEND_DATA((byte)0x04);

    private byte value;
    private PillCommand(byte value){
        this.value = value;
    }

    public byte getValue(){
        return this.value;
    }
}
