package com.hello.ble;

/**
 * Created by pangwu on 7/2/14.
 */
public enum PillCommand {
    SET_TIME((byte)0x06),
    GET_TIME((byte)0x05),
    SEND_DATA((byte)0x04),
    CALIBRATE((byte)0x02),
    START_ADVERTISE((byte)0x07),
    STOP_ADVERTISE((byte)0x08);

    private byte value;
    private PillCommand(byte value){
        this.value = value;
    }

    public byte getValue(){
        return this.value;
    }

    public static PillCommand fromByte(final byte value){
        switch (value){
            case 0x02:
                return CALIBRATE;
            case 0x04:
                return SEND_DATA;
            case 0x05:
                return GET_TIME;
            case 0x06:
                return SET_TIME;
            case 0x08:
                return STOP_ADVERTISE;
            default:
                return CALIBRATE;
        }
    }
}
