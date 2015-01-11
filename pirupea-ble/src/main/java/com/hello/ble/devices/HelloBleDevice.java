package com.hello.ble.devices;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.BleOperationCallback.OperationFailReason;
import com.hello.ble.HelloBle;
import com.hello.ble.stack.HelloGattLayer;

import java.lang.reflect.Method;

/**
 * Created by pangwu on 7/31/14.
 */
public abstract class HelloBleDevice {
    public static final int DEFAULT_SCAN_INTERVAL_MS = 10000;

    protected Context context;
    protected BluetoothDevice bluetoothDevice;
    protected int scanTimeRssi;
    protected HelloGattLayer gattLayer;

    protected String id;

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

                if (HelloBleDevice.this.bluetoothDevice == null) {
                    return;
                }

                if (!HelloBleDevice.this.bluetoothDevice.getAddress().equals(bondedDevice.getAddress())) {
                    return;
                }

                if (state == BluetoothDevice.BOND_BONDED) {
                    context.unregisterReceiver(this);
                    if (HelloBleDevice.this.pairedCallback != null) {
                        HelloBleDevice.this.pairedCallback.onCompleted(HelloBleDevice.this, null);
                    }
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    context.unregisterReceiver(this);

                    if (HelloBleDevice.this.unpairCallback != null) {
                        HelloBleDevice.this.unpairCallback.onCompleted(HelloBleDevice.this, null);
                    }
                } else if (state == BluetoothDevice.ERROR) {
                    context.unregisterReceiver(this);
                    if (HelloBleDevice.this.pairedCallback != null) {
                        HelloBleDevice.this.pairedCallback.onFailed(HelloBleDevice.this, OperationFailReason.INTERNAL_ERROR, state);
                    }
                }

            }
        }
    };

    public void connect(final BleOperationCallback<Void> connectedCallback) {
        this.connectedCallback = connectedCallback;
        connect();
    }


    protected void setId(final String deviceId) {
        this.id = deviceId;
    }

    public String getId() {
        return this.id;
    }

    public void connect() {
        if (this.bluetoothDevice == null)
            throw new IllegalArgumentException();

        if (this.gattLayer == null) {
            if (connectedCallback != null) {
                connectedCallback.onFailed(this, OperationFailReason.GATT_NOT_INITIALIZED, 0);
            }

            return;
        }

        HelloBleDevice.this.gattLayer.connect();
    }


    public void disconnect() {
        if (this.gattLayer != null) {
            this.gattLayer.disconnect();
        }

        try {
            HelloBle.getApplicationContext().unregisterReceiver(this.pairingReceiver);
        } catch (IllegalArgumentException iae) {
            HelloBle.logInfo(Pill.class.getName(), "Disconnect without paired.");
        }
    }

    private HelloBleDevice() {
    }

    protected HelloBleDevice(final Context context, final BluetoothDevice bluetoothDevice, final int scanTimeRssi) {
        this.context = context;
        this.bluetoothDevice = bluetoothDevice;
        this.scanTimeRssi = scanTimeRssi;
    }

    public Context getContext() {
        return context;
    }

    public int getScanTimeRssi() {
        return scanTimeRssi;
    }

    public String getAddress() {
        if (this.bluetoothDevice == null)
            throw new IllegalArgumentException();

        return this.bluetoothDevice.getAddress();
    }

    public String getName() {
        if (this.bluetoothDevice == null)
            throw new IllegalArgumentException();

        return this.bluetoothDevice.getName();
    }

    public int getBondState() {
        return this.bluetoothDevice.getBondState();
    }

    @Override
    public String toString() {
        return getName() + "@" + getAddress();
    }

    @Override
    public int hashCode() {
        return this.getAddress().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (getClass() != other.getClass()) {
            return false;
        }

        final HelloBleDevice convertedObject = (HelloBleDevice) other;
        return getAddress().equals(convertedObject.getAddress());
    }

    public void pair(final BleOperationCallback<Void> pairedCallback) {
        try {

            this.pairedCallback = pairedCallback;
            final IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

            HelloBle.getApplicationContext().registerReceiver(this.pairingReceiver, filter);

            Method method = this.bluetoothDevice.getClass().getMethod("createBond", (Class[]) null);  // this is shit!
            method.invoke(this.bluetoothDevice, (Object[]) null);
        } catch (Exception e) {
            HelloBle.logError(getClass().getSimpleName(), "Could not pair.", e);
            if (pairedCallback != null) {
                pairedCallback.onFailed(this, OperationFailReason.INTERNAL_ERROR, -1);
            }
        }
    }

    public void listenForPairing(final BleOperationCallback<Void> pairedCallback) {
        try {

            this.pairedCallback = pairedCallback;
            final IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

            HelloBle.getApplicationContext().registerReceiver(this.pairingReceiver, filter);
        } catch (Exception e) {
            HelloBle.logError(getClass().getSimpleName(), "Could not listen for pairing.", e);
            if (pairedCallback != null) {
                pairedCallback.onFailed(this, OperationFailReason.INTERNAL_ERROR, -1);
            }
        }
    }

    public void unpair(final BleOperationCallback<Void> unpairCallback) {
        try {
            this.unpairCallback = unpairCallback;
            final IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

            HelloBle.getApplicationContext().registerReceiver(this.pairingReceiver, filter);

            Method method = this.bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(this.bluetoothDevice, (Object[]) null);

        } catch (Exception e) {
            HelloBle.logError(getClass().getSimpleName(), "Could not unpair.", e);
        }
    }

    public boolean isConnected() {
        if (this.gattLayer != null) {
            return this.gattLayer.getConnectionStatus() == BluetoothProfile.STATE_CONNECTED;
        } else {
            return false;
        }
    }
}
