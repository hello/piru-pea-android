package com.hello.ble.devices;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.google.common.base.Objects;
import com.hello.ble.BleOperationCallback;
import com.hello.ble.LibApplication;
import com.hello.ble.stack.HelloGattLayer;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by pangwu on 7/31/14.
 */
public abstract class HelloBleDevice {
    public static final int DEFAULT_SCAN_INTERVAL_MS = 10000;

    protected Context context;
    protected BluetoothDevice bluetoothDevice;
    protected HelloGattLayer gattLayer;

    protected BleOperationCallback<Void> connectedCallback;
    protected BleOperationCallback<Integer> disconnectedCallback;

    protected BleOperationCallback<Void> pairedCallback;
    protected BleOperationCallback<Void> unpairCallback;


    private final BroadcastReceiver pairingReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                final BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(HelloBleDevice.this.bluetoothDevice == null){
                    return;
                }

                if(!HelloBleDevice.this.bluetoothDevice.getAddress().equals(bondedDevice.getAddress())){
                    return;
                }

                if (state == BluetoothDevice.BOND_BONDED) {
                    context.unregisterReceiver(this);
                    if(HelloBleDevice.this.pairedCallback != null){
                        HelloBleDevice.this.pairedCallback.onCompleted(HelloBleDevice.this, null);
                    }
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    context.unregisterReceiver(this);

                    if(HelloBleDevice.this.unpairCallback != null){
                        HelloBleDevice.this.unpairCallback.onCompleted(HelloBleDevice.this, null);
                    }
                }

            }
        }
    };

    public void connect(final BleOperationCallback<Void> connectedCallback){
        this.connectedCallback = connectedCallback;
        this.gattLayer.connect();
    }

    public void connect(final BleOperationCallback<Void> connectedCallback, final boolean autoBond){
        checkNotNull(this.bluetoothDevice);
        if(isConnected()){
            return;
        }

        if(this.bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED && autoBond){
            final BleOperationCallback<Void> pairedCallback = new BleOperationCallback<Void>() {
                @Override
                public void onCompleted(final HelloBleDevice connectedPill, final Void data) {
                    HelloBleDevice.this.connect(connectedCallback);
                }

                @Override
                public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                    if(HelloBleDevice.this.connectedCallback != null){
                        HelloBleDevice.this.connectedCallback.onFailed(sender, reason, errorCode);
                    }
                }
            };
            pair(pairedCallback);
        }else{
            this.connect(connectedCallback);
        }
    }

    public void connect(){
        checkNotNull(this.bluetoothDevice);
        if(isConnected()){
            return;
        }

        HelloBleDevice.this.gattLayer.connect();
    }

    public void connect(boolean autoBond){
        checkNotNull(this.bluetoothDevice);
        if(isConnected()){
            return;
        }

        if(this.bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED && autoBond){
            final BleOperationCallback<Void> pairedCallback = new BleOperationCallback<Void>() {
                @Override
                public void onCompleted(final HelloBleDevice connectedPill, final Void data) {
                    HelloBleDevice.this.gattLayer.connect();
                }

                @Override
                public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                    if(HelloBleDevice.this.connectedCallback != null){
                        HelloBleDevice.this.connectedCallback.onFailed(sender, reason, errorCode);
                    }
                }
            };
            pair(pairedCallback);
        }else{
            this.gattLayer.connect();
        }
    }


    public void disconnect()
    {
        if(this.gattLayer != null) {
            this.gattLayer.disconnect();
        }

        try {
            LibApplication.getAppContext().unregisterReceiver(this.pairingReceiver);
        }catch (IllegalArgumentException iae){
            Log.w(Pill.class.getName(), "Disconnect without paired.");
        }
    }

    private HelloBleDevice(){}

    protected HelloBleDevice(final Context context, final BluetoothDevice bluetoothDevice){
        this.context = context;
        this.bluetoothDevice = bluetoothDevice;

    }

    public String getAddress(){
        checkNotNull(this.bluetoothDevice);
        return this.bluetoothDevice.getAddress();
    }

    public String getName(){
        checkNotNull(this.bluetoothDevice);
        return this.bluetoothDevice.getName();
    }

    @Override
    public String toString(){
        return getName() + "@" + getAddress();
    }

    @Override
    public int hashCode(){
        return this.getAddress().hashCode();
    }

    @Override
    public boolean equals(Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final HelloBleDevice convertedObject = (HelloBleDevice) other;
        return  Objects.equal(this.getAddress(), convertedObject.getAddress());
    }

    public void pair(final BleOperationCallback<Void> pairedCallback) {
        try {

            this.pairedCallback = pairedCallback;
            final IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

            LibApplication.getAppContext().registerReceiver(this.pairingReceiver, filter);

            Method method = this.bluetoothDevice.getClass().getMethod("createBond", (Class[]) null);  // this is shit!
            method.invoke(this.bluetoothDevice, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unpair(final BleOperationCallback<Void> unpairCallback) {
        try {
            this.unpairCallback = unpairCallback;
            final IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

            LibApplication.getAppContext().registerReceiver(this.pairingReceiver, filter);

            Method method = this.bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(this.bluetoothDevice, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected(){
        return this.gattLayer.getConnectionStatus() == BluetoothProfile.STATE_CONNECTED;
    }
}
