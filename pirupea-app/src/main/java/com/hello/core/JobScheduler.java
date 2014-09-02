package com.hello.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager.WakeLock;

@SuppressLint("Wakelock")
public abstract class JobScheduler extends Widget {

	private Handler handler;
	private Runnable endWorkNotificationRunnable = new Runnable(){
		public void run() {
			// TODO Auto-generated method stub

			WakeLock wakeLock = acquireCPUWakeLock();  // Make sure the CPU is awake before all job has been finished.
			endWork();
			
			// After the job has been done, release the CPU wakelock and let the
			// phone fall asleep.
            if(wakeLock != null) {
                wakeLock.release();
            }
		}
	};


	
	public void onReceive(Context context, Intent intent){
		String action = intent.getAction();
		long wakeUpTimeInMS = intent.getLongExtra(AlarmScheduler.EXTRA_WAKEUP_DURATION, -1);
		
		if(getWakeUpAction().equals(action)){
			
			beginWork(wakeUpTimeInMS);
			handler.postDelayed(endWorkNotificationRunnable, wakeUpTimeInMS);
		}
		
	}


    @Override
    public void register(ContextWrapper contextWrapper){
        super.register(contextWrapper);

        this.handler = new Handler();
    }

    @Override
    public void unregister(){
        if(this.handler != null) {
            this.handler.removeCallbacks(this.endWorkNotificationRunnable);
        }

        super.unregister();
    }
	
	
	@Override
	protected String[] getSupportedActions() {
		// TODO Auto-generated method stub
		return new String[]{ getWakeUpAction() };
	}
	
	protected abstract void beginWork(long wakeUpDuration);
	protected abstract void endWork();
    protected String getWakeUpAction(){
        return getDefaultWakeUpAction();
    }

    protected final String getDefaultWakeUpAction(){
        return AlarmScheduler.ACTION_ALARM;
    }

}
