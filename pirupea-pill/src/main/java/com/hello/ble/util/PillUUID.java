package com.hello.ble.util;

import java.util.UUID;

/**
 * Created by pangwu on 7/14/14.
 */
public class PillUUID {
    public static final UUID PILL_SERVICE_UUID = UUID.fromString("0000E110-1212-EFDE-1523-785FEABCD123");
    public static final UUID CHAR_COMMAND_UUID = UUID.fromString("0000DEED-0000-1000-8000-00805F9B34FB");
    public static final UUID CHAR_COMMAND_RESPONSE_UUID = UUID.fromString("0000D00D-0000-1000-8000-00805F9B34FB");
    public static final UUID CHAR_DAY_DATETIME_UUID = UUID.fromString("00002A0A-0000-1000-8000-00805F9B34FB");
    public static final UUID CHAR_DATA_UUID = UUID.fromString("0000FEED-0000-1000-8000-00805F9B34FB");

    public static final UUID DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
}
