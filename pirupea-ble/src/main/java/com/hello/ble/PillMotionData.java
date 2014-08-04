package com.hello.ble;

import android.util.Log;

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
public class PillMotionData {

    public final static int STRUCT_HEADER_SIZE = 1 + 1 + 2 + 8 + 2;

    public final DateTime timestamp;
    public final Integer maxAmplitude;

    public PillMotionData(final DateTime timestamp, final Integer maxAmplitude){
        this.timestamp = timestamp;
        this.maxAmplitude = maxAmplitude;
    }


    public static List<PillMotionData> fromBytes(final byte[] payload){
        final List<PillMotionData> list = new ArrayList<>();
        final ByteArrayInputStream pillByteArrayInputStream = new ByteArrayInputStream(payload);
        final LittleEndianDataInputStream pillInputStream = new LittleEndianDataInputStream(pillByteArrayInputStream);


        try {

            byte version = pillInputStream.readByte();
            byte reserved1 = pillInputStream.readByte();

            int structLength = pillInputStream.readUnsignedShort();


            long timestamp = pillInputStream.readLong();

            int validIndex = pillInputStream.readUnsignedShort();
            int currentIndex = validIndex == 0xFFFF ? 0 : validIndex + 1;
            final int[] valueList = new int[(structLength - STRUCT_HEADER_SIZE) / 2];
            for (int i = 0; i < valueList.length; i++) {
                valueList[i] = pillInputStream.readShort();

            }

            pillInputStream.close();

            if(validIndex != 0xFFFF) {

                final DateTime startTime = new DateTime(timestamp, DateTimeZone.UTC);

                if(currentIndex > valueList.length - 1){
                    currentIndex = 0;
                }

                DateTime currentDataTime = startTime;

                if (validIndex > valueList.length - 1 || payload.length != structLength) {
                    throw new IllegalArgumentException("Corrupted data");
                }

                int index = validIndex;
                while (list.size() < valueList.length - 1) {
                    if(index != currentIndex) {
                        int value = valueList[index] - 1;
                        final PillMotionData pillMotionData = new PillMotionData(currentDataTime, value);
                        list.add(0, pillMotionData);
                        currentDataTime = currentDataTime.minusMinutes(1);
                        Log.i("IMU DATA", pillMotionData.timestamp + ", " + pillMotionData.maxAmplitude);
                    }

                    index--;

                    if (index == -1) {
                        index = valueList.length - 1;
                    }
                }

            }

            Log.i("Current max at index " + currentIndex, String.valueOf(valueList[currentIndex]));

        }catch (IOException ioe){
            ioe.printStackTrace();
        }catch (IllegalFieldValueException ifvEx){
            ifvEx.printStackTrace();
        }





        return ImmutableList.<PillMotionData>copyOf(list);
    }
}
