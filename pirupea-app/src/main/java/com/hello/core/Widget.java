package com.hello.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.telephony.TelephonyManager;

import com.hello.pirupea.core.IO;

import java.util.Iterator;

public abstract class Widget extends BroadcastReceiver {
	private final String TAG = this.getClass().getName();
	
	private ContextWrapper context = null;
	protected IntentFilter filter = null;
	
	/*
	The events that the handler is going to consume.
	 */
	protected abstract String[] getSupportedActions();
	
	protected void beforeRegistered(ContextWrapper context){
		
	}
	
	protected void setFilter(IntentFilter filter){
		if(this.filter == null)
			this.filter = filter;
		
		if(filter == null)
			return;
		
		if(filter != this.filter){
			Iterator<String> actions = filter.actionsIterator();

			while(actions.hasNext()){
				String action = actions.next();
				this.filter.addAction(action);
			}
		}
	}

    protected final PowerManager.WakeLock acquireCPUWakeLock(){
        try {
            PowerManager powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock cpuWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    this.getClass().getName());
            cpuWakeLock.acquire();
            return cpuWakeLock;
        }catch (Exception ex){
            ex.printStackTrace();
            IO.log(ex);
            return null;
        }
    }

    public final String getDeviceId(){
        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        if(telephonyManager == null){
            return null;
        }

        return telephonyManager.getDeviceId();
    }
	

	public void register(ContextWrapper contextWrapper){
		if(contextWrapper == null)
			return;
		
		if(this.getContext() != null)
			this.unregister();
		this.setContext(contextWrapper);
		
		if(this.filter == null)
			this.filter = new IntentFilter();
		
		this.beforeRegistered(contextWrapper);
		
		String[] actions = this.getSupportedActions();
		for(String action:actions){
			this.filter.addAction(action);
		}
		
		this.getContext().registerReceiver(this, filter);
        IO.log(this.getClass().getName() + " registered.");
		
		
	}
	
	public void unregister(){
		if(this.getContext() == null)
			return;

		this.getContext().unregisterReceiver(this);
		
		this.setContext(null);
		
		this.filter = null;

        IO.log(this.getClass().getName() + " unregistered.");
		
	}
	
	public boolean hasRegistered(){
		return this.getContext() != null;
	}
	

	public abstract void onReceive(Context context, Intent intent);

	protected void setContext(ContextWrapper context) {
		this.context = context;
	}

    protected final String[] mergeSupportedActions(String[] actions1, String[] actions2){
        String[] supportedActions = new String[actions1.length + actions2.length];
        for(int i = 0; i < actions1.length; i++){
            supportedActions[i] = actions1[i];
        }

        for(int i = 0; i < actions2.length; i++){
            supportedActions[i + actions1.length] = actions2[i];
        }

        return supportedActions;
    }

	public ContextWrapper getContext() {
		return context;
	}
}
