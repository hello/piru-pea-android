package com.hello.ble.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.LibApplication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by pangwu on 7/22/14.
 */
public class ScanBasedPillDetector implements LeScanCallback {

    private static final byte[] PILL_SERVICE_UUID_BYTES = new byte[]{
            0x23, (byte)0xD1, (byte)0xBC, (byte)0xEA, 0x5F, 0x78,  //785FEABCD123
            0x23, 0x15,   // 1523
            (byte) 0xDE, (byte)0xEF,   // EFDE
            0x12, 0x12,   // 1212
            0x10, (byte)0xE1, 0x00, 0x00  // 0000E110
    };

    private final BluetoothAdapter bluetoothAdapter;
    private final String[] addresses;

    private final int maxScanTimeInMS;
    private final BleOperationCallback<Set<Pill>> discoveryCallback;

    private final Set<Pill> discoveredPills = new HashSet<>();
    private final Map<String, BluetoothDevice> pills = new HashMap<String, BluetoothDevice>();

    private final Handler scanHandler;

    private final Runnable stopScanRunnable = new Runnable() {
        @Override
        public void run() {
            bluetoothAdapter.stopLeScan(ScanBasedPillDetector.this);

            for(final String address:pills.keySet()){
                discoveredPills.add(new Pill(LibApplication.getAppContext(), pills.get(address)));
            }
            discoveryCallback.onCompleted(null, discoveredPills);
        }
    };

    public ScanBasedPillDetector(final String[] addresses,
                      final int maxScanTimeInMS,
                      final BleOperationCallback<Set<Pill>> discoveryCallback){

        final Context context = LibApplication.getAppContext();
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        this.bluetoothAdapter = bluetoothAdapter;
        this.addresses = addresses;
        this.discoveryCallback = discoveryCallback;
        this.maxScanTimeInMS = maxScanTimeInMS;
        this.scanHandler  = new Handler();

    }

    private boolean isPill(final byte[] scanResponse){
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

    @Override
    public void onLeScan(BluetoothDevice device, int i, byte[] scanRecord) {
        final String debugString = device.getName();

        if(!isPill(scanRecord)) {
            return;
        }

        if (this.pills.containsKey(device.getAddress())) {
            return;
        }


        if(this.addresses != null) {
            for (final String address : this.addresses) {
                if (!address.equals(device.getAddress())) {
                    continue;
                }

                this.pills.put(device.getAddress(), device);
            }

            if (this.pills.size() == this.addresses.length) {
                // We get the target pills, no need to wait until timeout.
                // Cancel the timeout callback and return.
                scanHandler.removeCallbacks(this.stopScanRunnable);
                this.stopScanRunnable.run();

            }
        }else{
            this.pills.put(device.getAddress(), device);
        }

    }

    public void beginDiscovery(){
        this.scanHandler.post(new Runnable() {
            @Override
            public void run() {
                ScanBasedPillDetector.this.bluetoothAdapter.startLeScan(ScanBasedPillDetector.this);
            }
        });

        scanHandler.postDelayed(this.stopScanRunnable, this.maxScanTimeInMS);
    }
}
