package com.hello.pirupea;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.ble.BleOperationCallback;
import com.hello.ble.HelloBle;
import com.hello.ble.PillMotionData;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.devices.Pill;
import com.hello.ble.util.IO;
import com.hello.pirupea.datasource.InMemoryPillDataSource;
import com.hello.pirupea.settings.LocalSettings;
import com.hello.pirupea.settings.PillUserMap;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.event.SleepCycleAlgorithm;
import com.hello.suripu.android.SuripuClient;
import com.hello.suripu.core.db.models.TempTrackerData;
import com.hello.suripu.core.oauth.AccessToken;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class SmartAlarmTestService extends Service {

    private final static long WAKEUP_INTERVAL = 4 * 60 * 60 * 1000;
    private final static long FAST_WAKEUP_INTERVAL = 60 * 1000;

    private final static String ACTION_WAKEUP = SmartAlarmTestService.class.getName() + ".action_wakeup";


    private static class RetryInfo{
        public static final int MAX_RETRY_COUNTS = 5;

        public Pill pill;
        public int connectionRetryCounts;
        public int getTimeRetryCounts;
        public int getDataRetryCounts;
    }

    private HashMap<Pill, RetryInfo> pillRetryInfoHashMap = new HashMap<Pill, RetryInfo>();
    private SuripuClient suripuClient;

    private WakeLock cpuWakeLock;

    private static Handler handler;

    private BleOperationCallback<Void> connectionCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Void data) {
            final Pill pill = (Pill)sender;
            IO.log(pill.getName() + " connected.");

            // Get the time, make sure the pill hasn't crashed yet.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pill.getTime(SmartAlarmTestService.this.getTimeCallback);
                }
            }, 6000);

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
                    // Release the power lock and wait next wakeup.

                    final DateTime nextAlarmTime = new DateTime(LocalSettings.getAlarmTime()).plusDays(1);
                    SmartAlarmTestService.this.setNextDataCollection(nextAlarmTime.minusMinutes(20));
                    LocalSettings.setAlarmTime(nextAlarmTime.getMillis());

                    stopSelf();
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
                prepareForNextCollection();
                stopSelf();
            }
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            final Pill pill = (Pill)sender;
            IO.log(pill + " disconnect failed, " + reason + ": " + errorCode);
            // Retry ends here, if disconnect failed, go ahead.

            if(SmartAlarmTestService.this.pillRetryInfoHashMap.size() == 0){
                prepareForNextCollection();
                stopSelf();
            }
        }

        private void prepareForNextCollection(){
            final DateTime nextAlarmTime = new DateTime(LocalSettings.getAlarmTime()).plusDays(1);
            SmartAlarmTestService.this.setNextDataCollection(nextAlarmTime.minusMinutes(20));
            LocalSettings.setAlarmTime(nextAlarmTime.getMillis());
            IO.log("Next data collection starts at: " + nextAlarmTime.minusMinutes(20));
        }
    };

    private BleOperationCallback<DateTime> getTimeCallback = new BleOperationCallback<DateTime>() {
        @Override
        public void onCompleted(final HelloBleDevice connectedPill, final DateTime data) {
            final Pill pill = (Pill)connectedPill;
            IO.log("Time in " + pill.getName() + ": " + new DateTime(data.getMillis()));

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

            final ArrayList<TempTrackerData> pillData = new ArrayList<TempTrackerData>();
            for(final PillMotionData datum:data){
                stringBuilder.append(datum.timestamp.getMillis()).append(",")
                        .append(datum.maxAmplitude).append(",")
                        .append(new DateTime(datum.timestamp.getMillis()))
                        .append("\r\n");
                pillData.add(new TempTrackerData(datum.timestamp.getMillis(), datum.maxAmplitude, sender.getId()));
            }

            IO.appendStringToFile(csvFile, stringBuilder.toString());

            IO.log("Dump data from " + sender.getName() + "," + sender.getId() + " completed. data size: " + data.size());
            SmartAlarmTestService.this.pillRetryInfoHashMap.remove(pill);

            final DataSource<AmplitudeData> dataSource = new InMemoryPillDataSource(pillData);
            final List<Segment> segments = new SleepCycleAlgorithm(dataSource, 15).getCycles(DateTime.now());
            LocalSettings.setSleepCycles(segments);
            final File jsonFile = IO.getFileByDate(DateTime.now(), "cycles", "json");

            final DateTime smartAlarmTime = getSmartAlarmTimestamp(segments, LocalSettings.getAlarmTime());
            setRingTime(smartAlarmTime);

            try {
                final String jsonSegments = (new ObjectMapper()).writeValueAsString(segments);
                IO.appendStringToFile(jsonFile, jsonSegments);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }



            suripuClient.uploadPillData(pillData, new Callback<Void>() {
                @Override
                public void success(final Void aVoid, final Response response) {
                    IO.log("upload data for pill " + sender.getId() + " finished.");
                    pill.disconnect();
                }

                @Override
                public void failure(final RetrofitError error) {
                    if(error.isNetworkError()){
                        IO.log("upload data for pill " + sender.getId() + " failed: no network.");
                    }else {
                        IO.log("upload data for pill " + sender.getId() + " failed: " + error.getResponse().getReason());
                    }

                    pill.disconnect();
                }
            });
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


    private final BleOperationCallback<Set<Pill>> onDiscoverCompleted = new BleOperationCallback<Set<Pill>>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Set<Pill> data) {
            int pairedCount = 0;

            final String email = LocalSettings.getLastLoginUser();
            final String targetPillName = new PillUserMap().get(email);

            for(final Pill pill:data){
                if(targetPillName != null) {
                    if (pill.getName().equals(targetPillName)) {
                        IO.log("Paired pill detected: " + pill.getName());
                        final RetryInfo retryInfo = new RetryInfo();
                        retryInfo.pill = pill;
                        pairedCount++;

                        pill.setConnectedCallback(SmartAlarmTestService.this.connectionCallback);
                        pill.setDisconnectedCallback(SmartAlarmTestService.this.disconnectCallback);

                        SmartAlarmTestService.this.pillRetryInfoHashMap.put(pill, retryInfo);

                        pill.connect(SmartAlarmTestService.this.connectionCallback);
                        IO.log("Connecting to " + pill.getName() + " ....");
                    }
                }else{

                    // Is is just a quick fix for Ben.
                    IO.log("Paired pill detected: " + pill.getName());
                    final RetryInfo retryInfo = new RetryInfo();
                    retryInfo.pill = pill;
                    pairedCount++;

                    pill.setConnectedCallback(SmartAlarmTestService.this.connectionCallback);
                    pill.setDisconnectedCallback(SmartAlarmTestService.this.disconnectCallback);

                    SmartAlarmTestService.this.pillRetryInfoHashMap.put(pill, retryInfo);

                    pill.connect(SmartAlarmTestService.this.connectionCallback);
                    IO.log("Connecting to " + pill.getName() + " ....");
                }
            }

            setDefaultRing();

            if(pairedCount == 0){
                SmartAlarmTestService.this.setNextFastAlarm();
                IO.log("No paired pill discovered, sleep.");
                stopSelf();
            }
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            IO.log("Pill discovery failed, " + reason + ": " + errorCode);

            // Discovery failed for some reason, set next wakeup time and release the power lock.
            SmartAlarmTestService.this.setNextFastAlarm();
            setDefaultRing();

            stopSelf();
        }

        private void setDefaultRing(){
            DateTime defaultRingTime = new DateTime(LocalSettings.getAlarmTime());

            if(defaultRingTime.isBefore(DateTime.now())){
                defaultRingTime = defaultRingTime.plusDays(1);
                LocalSettings.setAlarmTime(defaultRingTime.getMillis());
            }
            SmartAlarmTestService.this.setRingTime(defaultRingTime);
            IO.log("Ring time set to default: " + defaultRingTime);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        IO.log(SmartAlarmTestService.class.getName() + " created.");

        final PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        this.cpuWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmartAlarmServiceWakeLock");
        this.cpuWakeLock.acquire();
        this.suripuClient = new SuripuClient();

        final ObjectMapper mapper = new ObjectMapper();
        final String accessTokenString = LocalSettings.getOAuthToken();


        final AccessToken accessToken;
        try {
            accessToken = mapper.readValue(accessTokenString, AccessToken.class);
            this.suripuClient.setAccessToken(accessToken);
        } catch (IOException e) {
            IO.log("Parse access token failed.");
            e.printStackTrace();
        }

        handler = new Handler(HelloBle.getApplicationContext().getMainLooper());

        // Scan for 20 seconds to pickup all pills around.
        IO.log("Discovering pills...");
        Pill.discover(onDiscoverCompleted, 20000);

    }

    @Override
    public void onDestroy(){
        IO.log(SmartAlarmTestService.class.getName() + " destroyed.");
        Toast.makeText(this, "Service stopped.", Toast.LENGTH_SHORT).show();

        if(this.cpuWakeLock != null){
            if(this.cpuWakeLock.isHeld()){
                this.cpuWakeLock.release();
            }
        }

        super.onDestroy();
    }


    public static DateTime getSmartAlarmTimestamp(final List<Segment> sleepCycles, long alarmDeadline){
        final Segment lastCycle = sleepCycles.get(sleepCycles.size() - 1);
        long deepSleepMoment = lastCycle.getEndTimestamp() + 20 * DateTimeConstants.MILLIS_PER_MINUTE;
        long dataCollectionMoment = DateTime.now().getMillis();
        DateTime smartAlarmTime = new DateTime(alarmDeadline);

        int possibleSpanInMinutes = (int)(deepSleepMoment - dataCollectionMoment) / DateTimeConstants.MILLIS_PER_MINUTE;
        final Random random = new Random();

        if(possibleSpanInMinutes > 0) {
            IO.log("User still in light sleep. Next deep sleep moment: " + new DateTime(deepSleepMoment));
            smartAlarmTime = smartAlarmTime.minusMinutes(20).plusMinutes(random.nextInt(possibleSpanInMinutes) + 1);
        }else{
            // User already in deep sleep.
            long sleepCycleLength = (long)(1.5 * DateTimeConstants.MILLIS_PER_HOUR);
            long cycleNumberInTheMiddle = (alarmDeadline - lastCycle.getEndTimestamp()) / sleepCycleLength;

            // It is possible that cycleNumberInTheMiddle > 0. In that case we need to guess the cycle.
            long nextLightSleepMoment = lastCycle.getEndTimestamp() + cycleNumberInTheMiddle * sleepCycleLength;
            IO.log("User already in deep sleep. Next light sleep moment: " + new DateTime(nextLightSleepMoment));

            if(nextLightSleepMoment > dataCollectionMoment && nextLightSleepMoment < alarmDeadline){
                smartAlarmTime = new DateTime(nextLightSleepMoment);
            }else {
                smartAlarmTime = smartAlarmTime.minusMinutes(5).plusMinutes(random.nextInt(5) + 1);
            }
        }

        IO.log("Smart alarm time: " + smartAlarmTime);

        return smartAlarmTime;

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static class RingService extends IntentService {
        // It looks like if we use the same IntentService for both
        // data collection and ring, they will conflict to each other on SOME phones.
        // It is a better idea to split them rather to register the same intent with different
        // extras.
        // This is Android.

        public RingService(){
            super("Ring Service");
        }

        @Override
        protected void onHandleIntent(final Intent intent) {
            ring();
        }

    }


    public static class AlarmService extends IntentService {
        public static final String EXTRA_TYPE = AlarmService.class.getName() + ".extra_type";


        public AlarmService(){
            super("Alarm Service");

        }

        @Override
        protected void onHandleIntent(final Intent intent) {
            startService(new Intent(this, SmartAlarmTestService.class));

        }
    }

    public static void ring(){
        final PowerManager powerManager = (PowerManager) HelloBle.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        final WakeLock cpuWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RingWakeLock");
        cpuWakeLock.acquire();

        try {
            if(handler == null) {
                handler = new Handler(HelloBle.getApplicationContext().getMainLooper());
            }

            final Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            final Ringtone ringtone = RingtoneManager.getRingtone(HelloBle.getApplicationContext(), notification);
            final AudioManager audioManager = (AudioManager) HelloBle.getApplicationContext().getSystemService(AUDIO_SERVICE);

            final int currentVolume = audioManager.getStreamVolume(ringtone.getStreamType());
            audioManager.setStreamVolume(ringtone.getStreamType(), audioManager.getStreamMaxVolume(ringtone.getStreamType()), 0);
            IO.log("Ring started.");
            ringtone.play();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    IO.log("Ring stop.");
                    ringtone.stop();
                    audioManager.setStreamVolume(ringtone.getStreamType(), currentVolume, 0);

                    cpuWakeLock.release();
                }
            }, 6000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setRingTime(final DateTime ringTime){
        cancelScheduledRing();

        final AlarmManager alarmManager = (AlarmManager) HelloBle.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        final Intent intent = new Intent(HelloBle.getApplicationContext(), RingService.class);

        final PendingIntent alarmIntent = PendingIntent.getService(HelloBle.getApplicationContext(), 0, intent, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                ringTime.getMillis(),
                alarmIntent);
        IO.log("Ring scheduled for " + ringTime);
    }

    public static void setNextDataCollection(final DateTime alarmTime){
        cancelScheduledDataCollection();

        final AlarmManager alarmManager = (AlarmManager) HelloBle.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        final Intent intent = new Intent(HelloBle.getApplicationContext(), AlarmService.class);
        intent.putExtra(AlarmService.EXTRA_TYPE, 0);

        final PendingIntent alarmIntent = PendingIntent.getService(HelloBle.getApplicationContext(), 0, intent, 0);

        if(LocalSettings.getAlarmTime() == 0){
            return;
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                alarmTime.getMillis(),
                alarmIntent);
    }

    private void setNextFastAlarm(){
        final AlarmManager alarmManager = (AlarmManager) HelloBle.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        final Intent intent = new Intent(HelloBle.getApplicationContext(), AlarmService.class);
        final PendingIntent alarmIntent = PendingIntent.getService(HelloBle.getApplicationContext(), 0, intent, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + FAST_WAKEUP_INTERVAL,
                alarmIntent);
    }

    public static void cancelScheduledRing(){
        final AlarmManager alarmManager = (AlarmManager) HelloBle.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        final Intent ringIntent = new Intent(HelloBle.getApplicationContext(), RingService.class);
        final PendingIntent pendingRingIntent = PendingIntent.getService(HelloBle.getApplicationContext(), 0, ringIntent, 0);
        alarmManager.cancel(pendingRingIntent);
    }

    public static void cancelScheduledDataCollection(){
        final AlarmManager alarmManager = (AlarmManager) HelloBle.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        final Intent intent = new Intent(HelloBle.getApplicationContext(), AlarmService.class);
        final PendingIntent alarmIntent = PendingIntent.getService(HelloBle.getApplicationContext(), 0, intent, 0);
        alarmManager.cancel(alarmIntent);


    }
}
