package com.hello.ble.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;

import com.google.common.base.Objects;
import com.hello.ble.LibApplication;
import com.hello.ble.PillCommand;
import com.hello.ble.PillData;
import com.hello.ble.PillOperationCallback;
import com.hello.ble.stack.BleTimePacketHandler;
import com.hello.ble.stack.MotionPacketHandler;
import com.hello.ble.stack.PillGattLayer;
import com.hello.ble.util.BleDateTimeConverter;
import com.hello.ble.util.PillUUID;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    private enum ConnectionStatus{
        DISCONNECTED,
        CONNECTED,
        CONNECTING,
        DISCONNECTING
    }


    private static final Handler scanHandler = new Handler();

    protected Context context;
    private BluetoothDevice bluetoothDevice;
    private PillGattLayer gattLayer;

    private BleTimePacketHandler bleTimePacketHandler;
    private MotionPacketHandler motionPacketHandler;


    private Pill(){

    }


    protected Pill(final Context context, final BluetoothDevice pillDevice){
        checkNotNull(context);

        this.bluetoothDevice = pillDevice;
        this.context = context;
        this.gattLayer = new PillGattLayer(this);
        this.bleTimePacketHandler = new BleTimePacketHandler(this);
        this.motionPacketHandler = new MotionPacketHandler(this);


        this.gattLayer.registerDataHandler(this.bleTimePacketHandler);
        this.gattLayer.registerDataHandler(this.motionPacketHandler);
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

    public void setTime(final DateTime target){
        setTime(target, null);
    }


    public void setTime(final DateTime target, final PillOperationCallback<BluetoothGattCharacteristic> setTimeFinishedCallback){
        if(!isConnected()){
            throw new IllegalStateException("Pill not connected");
        }


        this.gattLayer.setCommandWriteCallback(setTimeFinishedCallback);

        final byte[] optionalBLETime = BleDateTimeConverter.dateTimeToBLETime(target);
        if(optionalBLETime == null){
            return;
        }

        final byte[] commandData = new byte[1 + optionalBLETime.length];
        commandData[0] = PillCommand.SET_TIME.getValue();
        for(int i = 0; i < optionalBLETime.length; i++){
            commandData[i+1] = optionalBLETime[i];
        }

        this.gattLayer.writeCommand(commandData);
    }


    public void getTime(final PillOperationCallback<DateTime> getTimeCallback){
        if(!isConnected()){
            throw new IllegalStateException("Pill not connected.");
        }

        this.bleTimePacketHandler.setDataCallback(new PillOperationCallback<DateTime>() {
            @Override
            public void onCompleted(final Pill connectedPill, final DateTime data) {
                Pill.this.gattLayer.unsubscribeNotification(PillUUID.CHAR_DAY_DATETIME_UUID, null);
                if(getTimeCallback != null){
                    getTimeCallback.onCompleted(Pill.this, data);
                }
            }
        });


        this.gattLayer.subscribeNotification(PillUUID.CHAR_DAY_DATETIME_UUID, new PillOperationCallback<BluetoothGattDescriptor>() {
            @Override
            public void onCompleted(Pill connectedPill, BluetoothGattDescriptor data) {
                final byte[] pillCommandData = new byte[]{PillCommand.GET_TIME.getValue()};
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }
        });

    }


    public void getData(final PillOperationCallback<List<PillData>> getDataCallback){
        if(!isConnected()){
            throw new IllegalStateException("Pill not connected.");
        }

        this.motionPacketHandler.setDataCallback(new PillOperationCallback<List<PillData>>() {
            @Override
            public void onCompleted(final Pill connectedPill, final List<PillData> data) {
                Pill.this.gattLayer.unsubscribeNotification(PillUUID.CHAR_DATA_UUID, null);
                if(getDataCallback != null){
                    getDataCallback.onCompleted(Pill.this, data);
                }
            }
        });


        this.gattLayer.subscribeNotification(PillUUID.CHAR_DATA_UUID, new PillOperationCallback<BluetoothGattDescriptor>() {
            @Override
            public void onCompleted(final Pill connectedPill, final BluetoothGattDescriptor data) {
                final byte[] pillCommandData = new byte[]{PillCommand.SEND_DATA.getValue()};
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }
        });

    }

    public void connect(final PillOperationCallback<Void> connectedCallback){
        //TODO: Remove the context param in the future, it's very bad taste
        checkNotNull(this.bluetoothDevice);
        if(isConnected()){
            return;
        }

        this.gattLayer.setGattConnectedCallback(connectedCallback);

        final Handler handler = new Handler(LibApplication.getAppContext().getMainLooper()); // Fuck you Android!
        handler.post(new Runnable() {
            @Override
            public void run() {
                Pill.this.bluetoothDevice.connectGatt(context, false, Pill.this.gattLayer);
            }
        });

    }

    public void disconnect(){
        this.gattLayer.disconnect();
    }

    public boolean isConnected(){
        return this.gattLayer.getConnectionStatus() == BluetoothProfile.STATE_CONNECTED;
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
        return  Objects.equal(this.getAddress(), convertedObject.getAddress());
    }


    public static boolean discover(final String address, final PillOperationCallback<Pill> onDiscoverCompleted, int maxScanTime){
        //checkNotNull(context);
        checkNotNull(LibApplication.getAppContext());
        checkNotNull(onDiscoverCompleted);

        final Context context = LibApplication.getAppContext();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            return false;
        }

        scanLeDevice(bluetoothAdapter, new PillOperationCallback<List<Pill>>() {
            @Override
            public void onCompleted(Pill connectedPill, List<Pill> data) {
                final Pill targetPill = data.size() > 0 ? data.get(0) : null;
                onDiscoverCompleted.onCompleted(connectedPill, targetPill);
            }

        }, maxScanTime <= 0 ? 10000 : maxScanTime, new String[]{ address });
        return true;

    }

    public static boolean discover(final PillOperationCallback<List<Pill>> onDiscoverCompleted, int maxScanTime){
        //checkNotNull(context);
        checkNotNull(LibApplication.getAppContext());
        checkNotNull(onDiscoverCompleted);

        final Context context = LibApplication.getAppContext();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            return false;
        }

        scanLeDevice(bluetoothAdapter, onDiscoverCompleted, maxScanTime <= 0 ? 10000 : maxScanTime, null);
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
                                     final PillOperationCallback<List<Pill>> discoveryCallback,
                                     int maxScanTimeInMS,
                                     final String[] addresses) {


        final List<Pill> discoveredPills = new ArrayList<Pill>();

        final Map<String, BluetoothDevice> pills = new HashMap<String, BluetoothDevice>();
        final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if(isPill(scanRecord)) {
                    if (!pills.containsKey(device.getAddress())) {
                        if(addresses == null) {
                            pills.put(device.getAddress(), device);
                        } else {
                            for(final String address:addresses){
                                if(address.equals(device.getAddress())){
                                    pills.put(device.getAddress(), device);
                                }
                            }
                        }
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
                    discoveredPills.add(new Pill(LibApplication.getAppContext(), pills.get(address)));
                }
                discoveryCallback.onCompleted(null, discoveredPills);
            }
        }, maxScanTimeInMS);

        bluetoothAdapter.startLeScan(leScanCallback);
    }
}
