package com.hello.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;

public class AlarmScheduler extends Widget {
	private static final String CLASS_PREFIX = AlarmScheduler.class.getName();
	public static final String ACTION_ALARM = CLASS_PREFIX + ".action_wakeup";
	public static final String EXTRA_WAKEUP_DURATION = CLASS_PREFIX + ".extra_wakeup_duration";
	
	
	public static final long WAKEUP_DURATION_MS = 15000;
	public static final long SLEEP_DURATION_MS = 45000;

	private WakeLock cpuWakeLock = null;
	
	private Handler releaseHandler;
	private Runnable releaseRunnable = new Runnable(){

		public void run() {
			
			set(getContext(), getSleepDurationMS());
			
			beforeFallAsleep();
			
			// TODO Auto-generated method stub
			if(cpuWakeLock != null){
				if(cpuWakeLock.isHeld()){
					cpuWakeLock.release();
				}
			}
		}
		
	};

    public long getWakeUpDurationMS() {
        return WAKEUP_DURATION_MS;
    }

    public long getSleepDurationMS() {
        return SLEEP_DURATION_MS;
    }



    @Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		
		if(getWakeUpAction().equals(action)){
			this.cpuWakeLock = acquireCPUWakeLock();

            if(this.cpuWakeLock != null) {

                this.onWakeUp();
                // Wait sometime to get things done, then set Alarm and release the wakelock.
                this.releaseHandler.postDelayed(releaseRunnable, getWakeUpDurationMS());
            }
			
		}
		
	}
	
	public void register(ContextWrapper context){
		super.register(context);

        this.releaseHandler = new Handler();
		set(context, getSleepDurationMS());
	}
	
	public void unregister(){
		if(this.cpuWakeLock != null){
			if(this.cpuWakeLock.isHeld())
				this.cpuWakeLock.release();
		}

        if(this.releaseHandler != null) {
            this.releaseHandler.removeCallbacks(releaseRunnable);
        }
		
		super.unregister();
	}
	
	public void onWakeUp(){
		
	}
	
	public void beforeFallAsleep(){
		
	}
	
	
	private void set(Context context, long delayMillionSec){
		if(this.getContext() == null)
			return;
		
		AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent alarmIntent = new Intent(getWakeUpAction());
		alarmIntent.putExtra(EXTRA_WAKEUP_DURATION, getWakeUpDurationMS());
		
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);

		mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayMillionSec, pi);

	}

    protected String getWakeUpAction(){
        return getDefaultWakeUpAction();
    }

    protected final String getDefaultWakeUpAction(){
        return ACTION_ALARM;
    }

	@Override
	protected String[] getSupportedActions() {
		// TODO Auto-generated method stub
		return new String[]{ getWakeUpAction() };
	}
	
	
}
