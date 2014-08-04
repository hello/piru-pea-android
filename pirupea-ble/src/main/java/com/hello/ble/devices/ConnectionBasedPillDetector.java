package com.hello.ble.devices;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;

import com.hello.ble.LibApplication;
import com.hello.ble.BleOperationCallback;
import com.hello.ble.util.BleUUID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by pangwu on 7/22/14.
 */
public class ConnectionBasedPillDetector {

    private final Handler handler;
    private final Set<BluetoothDevice> devices;
    private final BleOperationCallback<Set<Pill>> discoveryCallback;
    private int index = 0;
    private final Set<Pill> pills = new HashSet<Pill>();

    private final BroadcastReceiver serviceUUIDScanReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_UUID.equals(action)) {
                context.unregisterReceiver(this);

                final BluetoothDevice scanningDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

                if(uuids != null){
                    for (final Parcelable uuidString:uuids){
                        if(UUID.fromString(uuidString.toString()).equals(BleUUID.PILL_SERVICE_UUID)){
                            pills.add(new Pill(LibApplication.getAppContext(), scanningDevice));
                        }
                    }
                }

                ConnectionBasedPillDetector.this.handler.sendEmptyMessage(++index);

            }
        }
    };

    public ConnectionBasedPillDetector(final Set<BluetoothDevice> devices, final BleOperationCallback<Set<Pill>> discoveryCallback){
        final List<BluetoothDevice> deviceList = new ArrayList<>(devices);
        final Set<Pill> pills = new HashSet<>();
        this.devices = devices;
        this.discoveryCallback = discoveryCallback;

        this.handler = new Handler(){

            @Override
            public void handleMessage(final Message msg) {
                super.handleMessage(msg);
                final int index = msg.what;
                if(index == deviceList.size()){
                    discoveryCallback.onCompleted(null, pills);
                    return;
                }


                final BluetoothDevice device = deviceList.get(index);
                LibApplication.getAppContext().registerReceiver(serviceUUIDScanReceiver, new IntentFilter(BluetoothDevice.ACTION_UUID));
                if(!device.fetchUuidsWithSdp()){
                    LibApplication.getAppContext().unregisterReceiver(serviceUUIDScanReceiver);
                    this.sendEmptyMessage(index + 1);
                }


                /*
                final Pill possiblePill = new Pill(LibApplication.getAppContext(), device);
                final PillOperationCallback<Void> succeed = new PillOperationCallback<Void>() {
                    @Override
                    public void onCompleted(Pill connectedPill, Void data) {
                        connectedPill.disconnect(new PillOperationCallback<Void>() {
                            @Override
                            public void onCompleted(final Pill disconnectedPill, final Void data) {
                                pills.add(disconnectedPill);

                                if(index == deviceList.size() - 1){
                                    discoveryCallback.onCompleted(null, pills);
                                }else{

                                    boolean ret = handler.sendEmptyMessage(index+1);
                                    if(!ret){
                                        throw new RuntimeException("You got fucked.");
                                    }
                                }
                            }
                        });
                    }
                };

                final PillOperationCallback<Void> timeOut = new PillOperationCallback<Void>() {
                    @Override
                    public void onCompleted(final Pill timeOutPill, final Void data) {

                        if(index == deviceList.size() - 1){
                            discoveryCallback.onCompleted(null, pills);
                        }else{

                            boolean ret = handler.sendEmptyMessage(index+1);
                            if(!ret){
                                throw new RuntimeException("You got fucked.");
                            }
                        }
                        Log.i(ConnectionBasedPillDetector.class.getName(), timeOutPill.toString() + " is not a pill.");
                    }
                };

                possiblePill.connect(succeed, timeOut, false);
                */
            }
        };
    }

    public void beginDiscovery(){
        if(this.devices.size() == 0){
            if(this.discoveryCallback != null){
                this.discoveryCallback.onCompleted(null, new HashSet<Pill>());
            }
        }else {

            this.handler.sendEmptyMessage(index);
        }
    }
}
