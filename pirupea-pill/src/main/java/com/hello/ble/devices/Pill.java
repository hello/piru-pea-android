package com.hello.ble.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.common.io.LittleEndianDataInputStream;
import com.hello.ble.PillCommand;
import com.hello.ble.PillData;
import com.hello.ble.PillOperationCallback;
import com.hello.ble.util.BleDateTimeConverter;

import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


    private enum ConnectionStatus{
        DISCONNECTED,
        CONNECTED,
        CONNECTING,
        DISCONNECTING
    }

    private static final UUID PILL_SERVICE_UUID = UUID.fromString("0000E110-1212-EFDE-1523-785FEABCD123");
    private static final UUID CHAR_COMMAND_UUID = UUID.fromString("0000deed-0000-1000-8000-00805f9b34fb");
    private static final UUID CHAR_COMMAND_RESPONSE_UUID = UUID.fromString("0000d00d-0000-1000-8000-00805f9b34fb");
    private static final UUID CHAR_DAY_DATETIME_UUID = UUID.fromString("00002A0A-0000-1000-8000-00805F9B34FB");
    private static final UUID CHAR_DATA_UUID = UUID.fromString("0000FEED-0000-1000-8000-00805F9B34FB");;

    private static final UUID DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static Handler scanHandler = new Handler();
    private BluetoothDevice bluetoothDevice;
    private BluetoothGattService pillService;
    private BluetoothGatt bluetoothGatt;

    private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;

    private PillOperationCallback<Void> connectedCallback;
    private PillOperationCallback<Void> commandWriteCallback;
    private PillOperationCallback<DateTime> getTimeCallback;
    private PillOperationCallback<List<PillData>> getDataCallback;

    private Map<UUID, PillOperationCallback<BluetoothGattDescriptor>> subscribeFinishedCallbacks = new HashMap<>();
    private Map<UUID, PillOperationCallback<BluetoothGattDescriptor>> unsubscribeFinishedCallbacks = new HashMap<>();

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        private static final int MINUTES_PER_DAY = 2;
        private byte[] pillDataInBytes;
        private int fillCount = 0;

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                Pill.this.bluetoothGatt = gatt;
                Pill.this.connectionStatus = ConnectionStatus.CONNECTING;
                gatt.discoverServices();
            }


            if(newState == BluetoothProfile.STATE_DISCONNECTED){
                Pill.this.bluetoothGatt = null;
                Pill.this.pillService = null;
                Pill.this.connectionStatus = ConnectionStatus.DISCONNECTED;
                Pill.this.subscribeFinishedCallbacks.clear();
                Pill.this.unsubscribeFinishedCallbacks.clear();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            final BluetoothGattService pillService = gatt.getService(PILL_SERVICE_UUID);
            Pill.this.pillService = pillService;
            Pill.this.connectionStatus = ConnectionStatus.CONNECTED;
            Pill.this.subscribeFinishedCallbacks.clear();
            Pill.this.unsubscribeFinishedCallbacks.clear();

            if(connectedCallback != null){
                connectedCallback.onCompleted(Pill.this, (Void)null);
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                if(CHAR_COMMAND_UUID.equals(characteristic.getUuid()) && Pill.this.commandWriteCallback != null){
                    Pill.this.commandWriteCallback.onCompleted(Pill.this, (Void)null);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final byte[] values = characteristic.getValue();


            if(CHAR_DAY_DATETIME_UUID.equals(characteristic.getUuid())){
                if(Pill.this.getTimeCallback != null){
                    Pill.this.unsubscribeNotification(characteristic.getUuid(),
                            new PillOperationCallback<BluetoothGattDescriptor>() {
                        @Override
                        public void onCompleted(Pill connectedPill, BluetoothGattDescriptor data) {
                            final DateTime dateTime = BleDateTimeConverter.bleTimeToDateTime(values);
                            Pill.this.getTimeCallback.onCompleted(Pill.this, dateTime);
                        }
                    });
                }

            }

            if(CHAR_DATA_UUID.equals(characteristic.getUuid())){

                final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(values);
                final LittleEndianDataInputStream inputStream = new LittleEndianDataInputStream(byteArrayInputStream);

                try {
                    byte sequenceNum = inputStream.readByte();
                    if (sequenceNum == 0) {
                        byte totalPackage = inputStream.readByte();
                        int version = inputStream.readUnsignedShort();

                        int structLength = inputStream.readUnsignedShort();

                        this.pillDataInBytes = new byte[structLength];
                        this.fillCount = 0;

                        for (int i = PillData.LEAD_BLE_PACKAGE_HEADER_LENGTH; i < values.length; i++) {
                            this.pillDataInBytes[fillCount++] = values[i];
                        }
                    } else {
                        for (int i = PillData.FOLLOWING_BLE_PACKAGE_HEADER_LENGTH; i < values.length; i++) {
                            this.pillDataInBytes[fillCount++] = values[i];
                        }
                    }
                }catch (IOException ioe){
                    ioe.printStackTrace();
                }

                if(fillCount == this.pillDataInBytes.length) {
                    final byte[] pillDataCopy = Arrays.copyOf(this.pillDataInBytes, this.pillDataInBytes.length);

                    Pill.this.unsubscribeNotification(characteristic.getUuid(), new PillOperationCallback<BluetoothGattDescriptor>() {
                        @Override
                        public void onCompleted(Pill connectedPill, BluetoothGattDescriptor data) {
                            final List<PillData> pillData = PillData.fromBytes(pillDataCopy);
                            if(Pill.this.getDataCallback != null){
                                Pill.this.getDataCallback.onCompleted(Pill.this, pillData);
                            }
                        }
                    });



                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                PillOperationCallback<BluetoothGattDescriptor> callback = null;

                if(Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    callback = Pill.this.subscribeFinishedCallbacks.get(descriptor.getCharacteristic().getUuid());
                }else if(Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    callback = Pill.this.unsubscribeFinishedCallbacks.get(descriptor.getCharacteristic().getUuid());
                }

                if(callback == null){
                    return;
                }
                callback.onCompleted(Pill.this, descriptor);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }
    };

    private boolean subscribeNotification(final UUID charUUID,
                                          final PillOperationCallback<BluetoothGattDescriptor> subscribeFinishedCallback){
        final BluetoothGattCharacteristic characteristic = pillService.getCharacteristic(charUUID);
        if(!this.bluetoothGatt.setCharacteristicNotification(characteristic, true)){
            Log.w(Pill.class.getName(), "Set notification for Characteristic: " + characteristic.getUuid() + " failed.");
            return false;
        }else {

            final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);  // This is the {0x01, 0x00} that shows up in the firmware
            if (!this.bluetoothGatt.writeDescriptor(descriptor)) {
                Log.w(Pill.class.getName(), "Set notification for descriptor: " + descriptor.getUuid() + " failed.");
                return false;
            }
        }

        if(subscribeFinishedCallback != null) {
            this.subscribeFinishedCallbacks.put(charUUID, subscribeFinishedCallback);
        }

        return true;
    }

    private boolean unsubscribeNotification(final UUID charUUID,
                                            final PillOperationCallback<BluetoothGattDescriptor> unsubscribeFinishedCallback){

        this.subscribeFinishedCallbacks.remove(charUUID);

        final BluetoothGattCharacteristic characteristic = pillService.getCharacteristic(charUUID);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG);
        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);  // This is the {0x01, 0x00} that shows up in the firmware
        if (!this.bluetoothGatt.writeDescriptor(descriptor)) {
            Log.w(Pill.class.getName(), "Set notification for descriptor: " + descriptor.getUuid() + " failed.");
            return false;
        }else{
            if(!this.bluetoothGatt.setCharacteristicNotification(characteristic, false)){
                Log.w(Pill.class.getName(), "Reset notification for Characteristic: " + characteristic.getUuid() + " failed.");
                return false;
            }

        }

        if(unsubscribeFinishedCallback != null) {
            this.unsubscribeFinishedCallbacks.put(charUUID, unsubscribeFinishedCallback);
        }

        return true;
    }

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

    public boolean setTime(final DateTime target){
        return setTime(target, null);
    }


    public boolean setTime(final DateTime target, final PillOperationCallback<Void> setTimeCallback){
        if(!isConnected()){
            throw new IllegalStateException("Pill not connected");
        }


        this.commandWriteCallback = setTimeCallback;

        final byte[] optionalBLETime = BleDateTimeConverter.dateTimeToBLETime(target);
        if(optionalBLETime == null){
            return false;
        }

        final BluetoothGattCharacteristic commandCharacteristic = this.pillService.getCharacteristic(CHAR_COMMAND_UUID);


        final byte[] commandData = new byte[1 + optionalBLETime.length];
        commandData[0] = PillCommand.SET_TIME.getValue();
        for(int i = 0; i < optionalBLETime.length; i++){
            commandData[i+1] = optionalBLETime[i];
        }
        commandCharacteristic.setValue(commandData);
        return this.bluetoothGatt.writeCharacteristic(commandCharacteristic);
    }


    public boolean getTime(final PillOperationCallback<DateTime> getTimeCallback){
        if(!isConnected()){
            throw new IllegalStateException("Pill not connected.");
        }

        if(getTimeCallback != null){
            this.getTimeCallback = getTimeCallback;
        }

        return this.subscribeNotification(CHAR_DAY_DATETIME_UUID, new PillOperationCallback<BluetoothGattDescriptor>() {
            @Override
            public void onCompleted(Pill connectedPill, BluetoothGattDescriptor data) {
                final byte[] pillCommandData = new byte[]{PillCommand.GET_TIME.getValue()};
                final BluetoothGattCharacteristic bluetoothGattCharacteristic = Pill.this.pillService.getCharacteristic(CHAR_COMMAND_UUID);
                bluetoothGattCharacteristic.setValue(pillCommandData);
                Pill.this.bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
            }
        });

    }


    public boolean getData(final PillOperationCallback<List<PillData>> getDataCallback){
        if(!isConnected()){
            throw new IllegalStateException("Pill not connected.");
        }

        if(getDataCallback != null){
            this.getDataCallback = getDataCallback;
        }


        return this.subscribeNotification(CHAR_DATA_UUID, new PillOperationCallback<BluetoothGattDescriptor>() {
            @Override
            public void onCompleted(final Pill connectedPill, final BluetoothGattDescriptor data) {
                final byte[] pillCommandData = new byte[]{PillCommand.SEND_DATA.getValue()};
                final BluetoothGattCharacteristic bluetoothGattCharacteristic = Pill.this.pillService.getCharacteristic(CHAR_COMMAND_UUID);
                bluetoothGattCharacteristic.setValue(pillCommandData);
                Pill.this.bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
            }
        });

    }

    public void connect(final Context context, final PillOperationCallback<Void> connectedCallback){
        //TODO: Remove the context param in the future, it's very bad taste
        checkNotNull(this.bluetoothDevice);
        if(isConnected() || isConnecting()){
            return;
        }

        this.connectedCallback = connectedCallback;
        this.bluetoothDevice.connectGatt(context, false, this.gattCallback);
        this.connectionStatus = ConnectionStatus.CONNECTING;
    }

    public void disconnect(){
        Pill.this.subscribeFinishedCallbacks.clear();
        Pill.this.unsubscribeFinishedCallbacks.clear();


        this.bluetoothGatt.disconnect();
        this.bluetoothGatt.close();

        this.connectionStatus = ConnectionStatus.DISCONNECTED;
    }

    public boolean isConnected(){
        return this.connectionStatus == ConnectionStatus.CONNECTED;
    }

    public boolean isConnecting(){
        return this.connectionStatus == ConnectionStatus.CONNECTING;
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


    public static boolean discover(final Context context, final PillOperationCallback<List<Pill>> onDiscoverCompleted, int maxScanTime){
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
                                     final PillOperationCallback<List<Pill>> discoveryCallback,
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
                discoveryCallback.onCompleted(null, discoveredPills);
            }
        }, maxScanTimeInMS);

        bluetoothAdapter.startLeScan(leScanCallback);
    }
}
