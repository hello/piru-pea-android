package com.hello.pirupea;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.PillMotionData;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.devices.Pill;
import com.hello.ble.util.IO;

import org.joda.time.DateTime;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


public class SmartAlarmTestService extends Service {

    private final static long WAKEUP_INTERVAL = 40 * 1000;
    private final static String ACTION_WAKEUP = SmartAlarmTestService.class.getName() + ".action_wakeup";


    private static class RetryInfo{
        public static final int MAX_RETRY_COUNTS = 5;

        public Pill pill;
        public int connectionRetryCounts;
        public int getTimeRetryCounts;
        public int getDataRetryCounts;
    }

    private HashMap<Pill, RetryInfo> pillRetryInfoHashMap = new HashMap<Pill, RetryInfo>();

    private AlarmManager alarmManager;
    private AlarmReceiver alarmReceiver;

    private WakeLock cpuWakeLock;

    private BleOperationCallback<Void> connectionCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Void data) {
            final Pill pill = (Pill)sender;
            IO.log(pill.getName() + " connected.");

            // Get the time, make sure the pill hasn't crashed yet.
            pill.getTime(SmartAlarmTestService.this.getTimeCallback);
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            final Pill pill = (Pill)sender;
            Log.w(SmartAlarmTestService.class.getName(), pill.getName() + " connection failed, " + reason + ": " + errorCode);
            IO.log(pill.getName() + " connection failed, " + reason + ": " + errorCode);

            IO.log("Retry connect to " + pill.getName());
            final RetryInfo retryInfo = SmartAlarmTestService.this.pillRetryInfoHashMap.get(pill);
            if(retryInfo == null){
                IO.log("Failed to get retry info for " + pill.getName());
                return;
            }

            retryInfo.connectionRetryCounts++;


            if(retryInfo.connectionRetryCounts < RetryInfo.MAX_RETRY_COUNTS){
                pill.connect(this);
            }else{
                IO.log("Retry connect to " + pill.getName() + " " + RetryInfo.MAX_RETRY_COUNTS + " times. Give up.");
                SmartAlarmTestService.this.pillRetryInfoHashMap.remove(pill);
                if(SmartAlarmTestService.this.pillRetryInfoHashMap.size() == 0){
                    // We all failed or the last pending connection failed.
                    // Release teh power lock and wait next wakeup.
                    SmartAlarmTestService.this.setNextAlarm(SmartAlarmTestService.this.alarmManager);
                    SmartAlarmTestService.this.cpuWakeLock.release();
                }
            }
        }
    };

    private BleOperationCallback<Integer> disconnectCallback = new BleOperationCallback<Integer>() {
        @Override
        public void onCompleted(final HelloBleDevice connectedPill, final Integer data) {
            final Pill pill = (Pill)connectedPill;
            Log.w(SmartAlarmTestService.class.getName(), pill.getName() + " disconnected: " + data);
            IO.log(pill.getName() + " disconnected: " + data);


            if(SmartAlarmTestService.this.pillRetryInfoHashMap.size() == 0){
                SmartAlarmTestService.this.setNextAlarm(SmartAlarmTestService.this.alarmManager);
                SmartAlarmTestService.this.cpuWakeLock.release();
            }
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            final Pill pill = (Pill)sender;
            IO.log(pill + " disconnect failed, " + reason + ": " + errorCode);
            // Retry ends here, if disconnect failed, go ahead.

            if(SmartAlarmTestService.this.pillRetryInfoHashMap.size() == 0){
                SmartAlarmTestService.this.setNextAlarm(SmartAlarmTestService.this.alarmManager);
                SmartAlarmTestService.this.cpuWakeLock.release();
            }
        }
    };

    private BleOperationCallback<DateTime> getTimeCallback = new BleOperationCallback<DateTime>() {
        @Override
        public void onCompleted(final HelloBleDevice connectedPill, final DateTime data) {
            final Pill pill = (Pill)connectedPill;
            IO.log("Time in " + pill.getName() + ": " + data);

            if(DateTime.now().minusMinutes(5).isAfter(data)){
                IO.log("Time is way too off in " + pill.getName() + ", pill may crashed.");
                // Don't quit, let's still try to get data.
            }

            pill.getData(32, SmartAlarmTestService.this.getDataCallback);

        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            final Pill pill = (Pill)sender;
            IO.log("Get time failed for " + pill.getName() + ", " + reason + ", error: " + errorCode);

            final RetryInfo retryInfo = SmartAlarmTestService.this.pillRetryInfoHashMap.get(pill);
            if(retryInfo == null){
                IO.log("Get retry info for " + pill.getName() + " failed in get time.");
                return;
            }

            if(retryInfo.getTimeRetryCounts == RetryInfo.MAX_RETRY_COUNTS){
                SmartAlarmTestService.this.pillRetryInfoHashMap.remove(pill);
                IO.log("Get time retry for " + pill.getName() + " reach " + RetryInfo.MAX_RETRY_COUNTS + " times. Give up.");
                pill.disconnect();
                IO.log("Disconnecting " + pill.getName() + " because too many failures in get time.");
            }else {
                retryInfo.getTimeRetryCounts++;
                pill.getTime(this);
                IO.log("Retry get time for the " + retryInfo.getTimeRetryCounts + " times.");
            }


        }
    };


    private final BleOperationCallback<List<PillMotionData>> getDataCallback = new BleOperationCallback<List<PillMotionData>>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final List<PillMotionData> data) {
            final Pill pill = (Pill)sender;

            final StringBuilder stringBuilder = new StringBuilder(1000);
            final File csvFile = IO.getFileByName(pill.getName(), "csv");

            if(!csvFile.exists()) {
                stringBuilder.append("timestamp,amplitude,time_string\r\n");
            }

            for(final PillMotionData datum:data){
                stringBuilder.append(datum.timestamp.getMillis()).append(",")
                        .append(datum.maxAmplitude).append(",")
                        .append(new DateTime(datum.timestamp.getMillis()))
                        .append("\r\n");
            }

            IO.appendStringToFile(csvFile, stringBuilder.toString());

            IO.log("Dump data completed. data size: " + data.size());
            SmartAlarmTestService.this.pillRetryInfoHashMap.remove(pill);
            pill.disconnect();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            final Pill pill = (Pill)sender;

            IO.log("Get data failed for " + pill.getName() + ", " + reason + ", error: " + errorCode);

            final RetryInfo retryInfo = SmartAlarmTestService.this.pillRetryInfoHashMap.get(pill);
            if(retryInfo == null){
                IO.log("Get retry info for " + pill.getName() + " failed in get data.");
                return;
            }

            if(retryInfo.getDataRetryCounts == RetryInfo.MAX_RETRY_COUNTS){
                SmartAlarmTestService.this.pillRetryInfoHashMap.remove(pill);
                IO.log("Get data retry for " + pill.getName() + " reach " + RetryInfo.MAX_RETRY_COUNTS + " times. Give up.");
                pill.disconnect();
                IO.log("Disconnecting " + pill.getName() + " because too many failures in get data.");
            }else{
                retryInfo.getDataRetryCounts++;
                pill.getData(32, this);
                IO.log("Retry get data for the " + retryInfo.getDataRetryCounts + " times.");
            }
        }
    };

    private final class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if(ACTION_WAKEUP.equals(action)){
                // When calling context.startService(intent), it goes here.
                SmartAlarmTestService.this.onWake();

            }
        }
    };


    private final BleOperationCallback<Set<Pill>> onDiscoverCompleted = new BleOperationCallback<Set<Pill>>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Set<Pill> data) {

            for(final Pill pill:data){
                if(pill.isPaired()){
                    IO.log("Paired pill detected: " + pill.getName());
                    final RetryInfo retryInfo = new RetryInfo();
                    retryInfo.pill = pill;

                    pill.setConnectedCallback(SmartAlarmTestService.this.connectionCallback);
                    pill.setDisconnectedCallback(SmartAlarmTestService.this.disconnectCallback);

                    SmartAlarmTestService.this.pillRetryInfoHashMap.put(pill, retryInfo);

                    pill.connect(SmartAlarmTestService.this.connectionCallback);
                    IO.log("Connecting to " + pill.getName() + " ....");
                }
            }
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            IO.log("Pill discovery failed, " + reason + ": " + errorCode);

            // Discovery failed for some reason, set next wakeup time and release the power lock.
            SmartAlarmTestService.this.setNextAlarm(SmartAlarmTestService.this.alarmManager);
            SmartAlarmTestService.this.cpuWakeLock.release();
        }
    };


    private void onWake(){
        final PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        this.cpuWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmartAlarmServiceWakeLock");
        this.cpuWakeLock.acquire();

        // Scan for 20 seconds to pickup all pills around.
        IO.log("Discovering pills...");
        Pill.discover(onDiscoverCompleted, 20000);
    }


    @Override
    public void onCreate() {
        super.onCreate();

        this.alarmReceiver = new AlarmReceiver();
        this.registerReceiver(this.alarmReceiver, new IntentFilter(ACTION_WAKEUP));

        this.alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        setNextAlarm(this.alarmManager);

        Toast.makeText(this, "Service created.", Toast.LENGTH_SHORT).show();
        IO.log(SmartAlarmTestService.class.getName() + " created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If we get killed, after returning from here, restart
        Toast.makeText(this, "Service running.", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        IO.log(SmartAlarmTestService.class.getName() + " destroyed.");
        Toast.makeText(this, "Service stopped.", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }



    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }



    private void setNextAlarm(final AlarmManager alarmManager){
        final Intent intent = new Intent(ACTION_WAKEUP);
        final PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + WAKEUP_INTERVAL,
                alarmIntent);
    }
}
