package com.hello.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by pangwu on 7/1/14.
 */
public class Pill {

    private static final byte[] PILL_SERVICE_UUID_BYTES = new byte[]{
            0x23, (byte)0xD1, (byte)0xBC, (byte)0xEA, 0x5F, 0x78,  //785FEABCD123
            0x23, 0x15,   // 1523
            (byte) 0xDE, (byte)0xEF,   // EFDE
            0x12, 0x12,   // 1212
            0x10, (byte)0xE1, 0x00, 0x00  // 0000E110
    };

    private static Handler scanHandler = new Handler();
    private BluetoothDevice bluetoothDevice;

    protected Pill(final BluetoothDevice pillDevice){
        this.bluetoothDevice = pillDevice;
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
    public boolean equals(Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final Pill convertedObject = (Pill) other;
        return  com.google.common.base.Objects.equal(this.getAddress(), convertedObject.getAddress());
    }


    public static boolean discover(final Context context, final PillDiscoveryCallback onDiscoverCompleted, int maxScanTime){
        checkNotNull(context);
        checkNotNull(onDiscoverCompleted);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            return false;
        }

        scanLeDevice(bluetoothAdapter, onDiscoverCompleted, maxScanTime <= 0 ? 10000 : maxScanTime);
        return true;

    }

    private static boolean isPill(final byte[] scanResponse){
        if(scanResponse.length < PILL_SERVICE_UUID_BYTES.length) {
            return false;
        }

        boolean isPill = false;


        for (int i = 0; i < scanResponse.length - PILL_SERVICE_UUID_BYTES.length; i++) {

            final byte[] range = Arrays.copyOfRange(scanResponse, i, i + PILL_SERVICE_UUID_BYTES.length);
            if(Arrays.equals(range, PILL_SERVICE_UUID_BYTES)){
                isPill = true;
                break;
            }

        }

        return isPill;

    }

    private static void scanLeDevice(final BluetoothAdapter bluetoothAdapter,
                                     final PillDiscoveryCallback discoveryCallback,
                                     int maxScanTimeInMS) {


        final List<Pill> discoveredPills = new ArrayList<Pill>();

        final Map<String, BluetoothDevice> pills = new HashMap<String, BluetoothDevice>();
        final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if(isPill(scanRecord)) {
                    if (!pills.containsKey(device.getAddress())) {
                        pills.put(device.getAddress(), device);
                    }
                }

            }
        };

        // Stops scanning after a pre-defined scan period.
        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.stopLeScan(leScanCallback);

                for(final String address:pills.keySet()){
                    discoveredPills.add(new Pill(pills.get(address)));
                }
                discoveryCallback.onScanCompleted(discoveredPills);
            }
        }, maxScanTimeInMS);

        bluetoothAdapter.startLeScan(leScanCallback);
    }
}
