package com.hello.data.collection;

import android.content.ContextWrapper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import com.hello.ble.util.IO;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by pangwu on 4/23/14.
 */
public class ContinuesMotionWidget extends SensorDataCollectionWidget {
    public static final String ACTION_WAKEUP = ContinuesMotionWidget.class.getName() + ".action_wakeup";
    private static final long POLLING_THREAD_SLEEP_TIME = 40;
    private double[] motionBuffer = new double[4];
    private long timestampBuffer = 0;
    private volatile boolean shouldPollData = false;
    private volatile boolean shouldExitPollingThread = false;

    private Thread pollingThread;

    @Override
    public String getDataFilePrefix() {
        return "Android";
    }

    @Override
    public long getMaxDataFileSize() {
        return 1024 * 1024;
    }

    @Override
    public int[] getSupportedSensorTypes() {
        return new int[]{ Sensor.TYPE_ACCELEROMETER };
    }

    @Override
    public int getAndroidSamplingDelay() {
        return SensorManager.SENSOR_DELAY_NORMAL;
    }

    @Override
    public String getCSVHeaderLine() {
        return "timestamp,x,y,z,accuracy,timezone_offset";
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        motionBuffer[0] = sensorEvent.values[0];
        motionBuffer[1] = sensorEvent.values[1];
        motionBuffer[2] = sensorEvent.values[2];
        motionBuffer[3] = sensorEvent.accuracy;

        timestampBuffer = new DateTime(DateTimeZone.UTC).getMillis();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void beginWork(long wakeUpTime){
        super.beginWork(wakeUpTime);

        this.shouldPollData = true;

    }


    @Override
    public void endWork(){
        this.shouldPollData = false;
        super.endWork();
    }

    @Override
    protected String getWakeUpAction(){
        return ACTION_WAKEUP;
    }

    @Override
    public void register(ContextWrapper contextWrapper){
        super.register(contextWrapper);

        this.shouldExitPollingThread = false;
        final long timezoneOffsetFromUtc = DateTimeZone.getDefault().getOffset(new DateTime().getMillis());

        this.pollingThread = new Thread(){


            private StringBuilder buffer = new StringBuilder(1024 * 8);
            public void run(){
                while(!shouldExitPollingThread){
                    if(shouldPollData){
                        buffer.append(timestampBuffer).append(",")
                                .append(motionBuffer[0]).append(",")
                                .append(motionBuffer[1]).append(",")
                                .append(motionBuffer[2]).append(",")
                                .append(motionBuffer[3]).append(",")
                                .append(timezoneOffsetFromUtc)
                                .append("\r\n");
                    }

                    try {
                        sleep(POLLING_THREAD_SLEEP_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                IO.appendStringToFile(getCSVDataFile(), buffer.toString());
                buffer = new StringBuilder(1024 * 8);
            }
        };

        this.pollingThread.start();

    }

    @Override
    public void unregister(){

        this.shouldPollData = false;

        this.shouldExitPollingThread = true;
        super.unregister();

    }
}
