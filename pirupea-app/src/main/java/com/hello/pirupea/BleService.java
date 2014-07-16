package com.hello.pirupea;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

import com.hello.ble.PillOperationCallback;
import com.hello.ble.devices.Pill;
import com.hello.pirupea.settings.LocalSettings;

import org.joda.time.DateTime;


public class BleService extends Service {

    private final static long ALARM_INTERVAL = 1 * 60 * 1000;
    private final static String ACTION_WAKEUP = BleService.class.getName() + ".action_wakeup";

    private Pill currentPill;
    private AlarmManager alarmManager;
    private AlarmReceiver alarmReceiver;

    private WakeLock cpuWakeLock;
    private Handler mainHandler;

    private PillOperationCallback<Void> connectionCallback = new PillOperationCallback<Void>() {
        @Override
        public void onCompleted(final Pill connectedPill, final Void data) {
            BleService.this.currentPill.getTime(BleService.this.getTimeCallback);
        }
    };

    private PillOperationCallback<Void> connectionTimeOutCallback = new PillOperationCallback<Void>() {
        @Override
        public void onCompleted(final Pill connectedPill, final Void data) {
            Log.w(BleService.class.getName(), "Connect to pill: " + connectedPill.getName() + " failed.");
        }
    };

    private PillOperationCallback<DateTime> getTimeCallback = new PillOperationCallback<DateTime>() {
        @Override
        public void onCompleted(final Pill connectedPill, final DateTime data) {
            //Toast.makeText(BleService.this, data.toString("MM/dd HH:mm:ss"), Toast.LENGTH_SHORT).show();
            BleService.this.mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    BleService.this.currentPill.disconnect();
                    BleService.this.currentPill = null;
                    setNextAlarm(BleService.this.alarmManager);
                    BleService.this.cpuWakeLock.release();
                }
            });

        }
    };

    private final class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if(ACTION_WAKEUP.equals(action)){
                // When calling context.startService(intent), it goes here.
                BleService.this.onWake();

            }
        }
    };


    private void onWake(){
        final PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        this.cpuWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BleServiceWakeLock");
        this.cpuWakeLock.acquire();


        if (this.currentPill == null) {
            final String address = LocalSettings.getPillAddress(this);
            if (address == null) {
                BleService.this.cpuWakeLock.release();

                return;
            }

            Pill.discover(address, new PillOperationCallback<Pill>() {
                @Override
                public void onCompleted(final Pill connectedPill, final Pill pill) {
                    BleService.this.currentPill = pill;

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(pill != null) {
                                pill.connect(BleService.this.connectionCallback, BleService.this.connectionTimeOutCallback);
                            }else{
                                setNextAlarm(BleService.this.alarmManager);
                                BleService.this.cpuWakeLock.release();
                            }
                        }
                    });


                }
            }, 20000);

        }

    }


    @Override
    public void onCreate() {
        super.onCreate();

        this.alarmReceiver = new AlarmReceiver();
        this.registerReceiver(this.alarmReceiver, new IntentFilter(ACTION_WAKEUP));

        this.alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        setNextAlarm(this.alarmManager);
        this.mainHandler = new Handler(getApplicationContext().getMainLooper());

        Toast.makeText(this, "Service created.", Toast.LENGTH_SHORT).show();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        if(this.currentPill != null){
            this.currentPill.disconnect();
        }
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
                System.currentTimeMillis() + ALARM_INTERVAL,
                alarmIntent);
    }
}
