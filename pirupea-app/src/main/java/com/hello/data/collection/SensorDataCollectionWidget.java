package com.hello.data.collection;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.hello.pirupea.core.IO;
import com.hello.core.JobScheduler;

import java.io.File;
import java.util.HashMap;

/**
 * Created by pangwu on 3/12/14.
 */
public abstract class SensorDataCollectionWidget extends JobScheduler implements SensorEventListener {

    private File csvFile = null;
    private SensorManager sensorManager;
    private HashMap<Integer, Sensor> sensors = new HashMap<Integer, Sensor>();



    private String currentActivity = "unknown";

    public static final long MAX_DATA_FILE_SIZE = 1024 * 1024;

    @Override
    protected void beginWork(long wakeUpDuration) {
        csvFile = getCSVDataFile();
        registerSensors();
    }

    protected File getCSVDataFile(){
        csvFile = IO.getCSVFileforToday(getDataFilePrefix(), getCSVHeaderLine());

        return csvFile;
    }

    @Override
    protected void endWork() {
        unregisterSensors();

    }

    protected String getCurrentActivity() {
        return currentActivity;
    }

    private void setCurrentActivity(String currentActivity) {
        this.currentActivity = currentActivity;
    }

    public abstract String getDataFilePrefix();
    public abstract long getMaxDataFileSize();


    public abstract int[] getSupportedSensorTypes();
    public abstract int getAndroidSamplingDelay();
    public abstract String getCSVHeaderLine();

    protected HashMap<String, Sensor> getRegisteredSensors(){
        return (HashMap<String, Sensor>) sensors.clone();
    }

    protected void registerSensors(){
        unregisterSensors();
        int[] supportedSensorTypes = this.getSupportedSensorTypes();
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);

        if(sensorManager == null){
            IO.log("SensorManager is null!");
            return;
        }

        for(int supportedSensorType:supportedSensorTypes){
            Sensor sensor = sensorManager.getDefaultSensor(supportedSensorType);
            if(sensor != null){
                this.sensors.put(supportedSensorType, sensor);
                sensorManager.registerListener(this, sensor, getAndroidSamplingDelay());
                IO.log("Sensor: " + sensor.getName() + " registered.");
            }
        }


    }

    protected void unregisterSensors(){
        if(sensorManager != null){
            sensorManager.unregisterListener(this);
            this.sensors.clear();
            sensorManager = null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);

        String action = intent.getAction();


    }

    @Override
    protected String[] getSupportedActions(){

        return super.getSupportedActions();
    }

}
