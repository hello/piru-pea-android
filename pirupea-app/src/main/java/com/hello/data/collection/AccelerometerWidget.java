package com.hello.data.collection;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import com.hello.ble.util.IO;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by pangwu on 3/12/14.
 * Collect acceleration data for 15 seconds every minute.
 */
public class AccelerometerWidget extends SensorDataCollectionWidget {


    private static final String DATA_FILE_PREFIX = "accelerometer";
    private StringBuilder sensorData = new StringBuilder(8 * 1024);
    private double[] dataBuffer = new double[4];
    private long timestampBuffer = 0;

    private volatile boolean shouldEndPolling = false;

    private static final long POLLING_THREAD_SLEEP_TIME = 40;

    @Override
    protected void endWork(){
        shouldEndPolling = true;
        super.endWork();
    }

    @Override
    public String getDataFilePrefix() {
        return DATA_FILE_PREFIX;
    }

    @Override
    public long getMaxDataFileSize() {
        return 1024 * 1024;
    }

    @Override
    public int[] getSupportedSensorTypes() {
        return new int[] {Sensor.TYPE_ACCELEROMETER};
    }

    @Override
    public int getAndroidSamplingDelay() {
        return SensorManager.SENSOR_DELAY_NORMAL;
    }

    @Override
    public String getCSVHeaderLine() {
        return "timestamp,x,y,z,accuracy,activity,timezone_offset";
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        dataBuffer[0] = sensorEvent.values[0];
        dataBuffer[1] = sensorEvent.values[1];
        dataBuffer[2] = sensorEvent.values[2];
        dataBuffer[3] = sensorEvent.accuracy;

        timestampBuffer = DateTime.now(DateTimeZone.UTC).getMillis();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void beginWork(long wakeupTime){
        super.beginWork(wakeupTime);
        this.shouldEndPolling = false;
        final long timezoneOffsetFromUtc = DateTimeZone.getDefault().getOffset(new DateTime().getMillis());

        Thread pollingThread = new Thread(){
            public void run(){
                while(!shouldEndPolling){
                    sensorData.append(timestampBuffer).append(",")
                            .append(dataBuffer[0]).append(",")
                            .append(dataBuffer[1]).append(",")
                            .append(dataBuffer[2]).append(",")
                            .append(dataBuffer[3]).append(",")
                            .append(getCurrentActivity()).append(",")
                            .append(timezoneOffsetFromUtc)
                            .append("\r\n");
                    IO.appendStringToFile(getCSVDataFile(), sensorData.toString());
                    sensorData.setLength(0);
                    try {
                        sleep(POLLING_THREAD_SLEEP_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        pollingThread.start();

    }

}
