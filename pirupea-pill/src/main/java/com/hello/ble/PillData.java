package com.hello.ble;

import com.google.common.collect.ImmutableList;
import com.google.common.io.LittleEndianDataInputStream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.IllegalFieldValueException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 7/7/14.
 */
public class PillData {

    public final static int STRUCT_HEADER_SIZE = 1 + 1 + 2 + 5 + 1 + 2 + 2;

    public final DateTime timestamp;
    public final Integer maxAmplitude;

    public PillData(final DateTime timestamp, final Integer maxAmplitude){
        this.timestamp = timestamp;
        this.maxAmplitude = maxAmplitude;
    }


    public static List<PillData> fromBytes(final byte[] payload){
        final List<PillData> list = new ArrayList<>();
        final ByteArrayInputStream pillByteArrayInputStream = new ByteArrayInputStream(payload);
        final LittleEndianDataInputStream pillInputStream = new LittleEndianDataInputStream(pillByteArrayInputStream);


        try {

            byte version = pillInputStream.readByte();
            byte reserved1 = pillInputStream.readByte();

            int structLength = pillInputStream.readUnsignedShort();

            byte second = pillInputStream.readByte();
            byte minute = pillInputStream.readByte();
            byte hour = pillInputStream.readByte();
            byte day = pillInputStream.readByte();
            byte month = pillInputStream.readByte();
            byte reserved2 = pillInputStream.readByte();

            int year = pillInputStream.readUnsignedShort();

            int validIndex = pillInputStream.readUnsignedShort();

            final DateTime startTime = new DateTime(year, month, day, hour, minute, second, DateTimeZone.UTC);
            final int[] valueList = new int[(payload.length - STRUCT_HEADER_SIZE) / 2];

            DateTime currentDataTime = startTime;

            if(validIndex > valueList.length - 1 || payload.length != structLength){
                throw new IllegalArgumentException("Corrupted data");
            }

            for(int i = 0; i < valueList.length; i++){
                valueList[i] = pillInputStream.readUnsignedShort();
            }

            for(int i = validIndex; i >= 0; i--){
                int value = valueList[i] - 1;

                // Shall we insert missing values?
                final PillData pillData = new PillData(currentDataTime, value);
                list.add(0, pillData);
                currentDataTime = currentDataTime.minusMinutes(1);
            }

            pillInputStream.close();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }catch (IllegalFieldValueException ifvEx){
            ifvEx.printStackTrace();
        }

        return ImmutableList.<PillData>copyOf(list);
    }
}
