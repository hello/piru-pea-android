package com.hello.ble.stack;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.hello.ble.LibApplication;
import com.hello.ble.PillBlePacket;
import com.hello.ble.PillOperationCallback;
import com.hello.ble.devices.Pill;
import com.hello.ble.util.PillUUID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by pangwu on 7/14/14.
 */
public class PillGattLayer extends BluetoothGattCallback {

    private Pill sender;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService bluetoothGattService;
    private int connectionStatus = BluetoothProfile.STATE_DISCONNECTED;

    private PillOperationCallback<Void> connectedCallback;
    private PillOperationCallback<BluetoothGattCharacteristic> commandWriteCallback;

    private Map<UUID, PillOperationCallback<BluetoothGattDescriptor>> subscribeFinishedCallbacks = new HashMap<>();
    private Map<UUID, PillOperationCallback<BluetoothGattDescriptor>> unsubscribeFinishedCallbacks = new HashMap<>();

    private Set<PillBlePacketHandler> dataHanlders = new HashSet<>();

    private Handler messageHandler;
    private HandlerThread lopperThread = new HandlerThread("PillHandler");

    public synchronized void waitUntilReady() {
        messageHandler = new Handler(lopperThread.getLooper());
    }

    public int getConnectionStatus(){
        return this.connectionStatus;
    }

    public void registerDataHandler(final PillBlePacketHandler handler){

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                PillGattLayer.this.dataHanlders.add(handler);
            }
        });

    }

    public void unregisterDataHandler(final PillBlePacketHandler handler){
        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                PillGattLayer.this.dataHanlders.remove(handler);
            }
        });
    }

    public void setCommandWriteCallback(final PillOperationCallback<BluetoothGattCharacteristic> pillOperationCallback){
        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                PillGattLayer.this.commandWriteCallback = pillOperationCallback;
            }
        });
    }

    public void setGattConnectedCallback(final PillOperationCallback<Void> pillOperationCallback){
        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                PillGattLayer.this.connectedCallback = pillOperationCallback;
            }
        });

    }

    public PillGattLayer(final Pill pill){
        this.sender = pill;
        this.lopperThread.start();
        this.waitUntilReady();
    }

    public void writeCommand(final byte[] commandData){
        writeCommand(PillUUID.CHAR_COMMAND_UUID, commandData);
    }

    public void writeCommand(final UUID commandInterfaceUUID, final byte[] commandData){
        final byte[] commandCopy = Arrays.copyOf(commandData, commandData.length);
        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                final BluetoothGattCharacteristic commandCharacteristic = PillGattLayer.this.bluetoothGattService.getCharacteristic(commandInterfaceUUID);
                commandCharacteristic.setValue(commandCopy);
                PillGattLayer.this.bluetoothGatt.writeCharacteristic(commandCharacteristic);
            }
        });

    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    PillGattLayer.this.connectionStatus = BluetoothProfile.STATE_CONNECTING;
                    PillGattLayer.this.bluetoothGatt = gatt;
                    gatt.discoverServices();
                }


                if(newState == BluetoothProfile.STATE_DISCONNECTED){
                    PillGattLayer.this.connectionStatus = BluetoothProfile.STATE_DISCONNECTED;
                    PillGattLayer.this.subscribeFinishedCallbacks.clear();
                    PillGattLayer.this.unsubscribeFinishedCallbacks.clear();
                }
            }
        });

    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                PillGattLayer.this.bluetoothGattService = gatt.getService(PillUUID.PILL_SERVICE_UUID);
                PillGattLayer.this.connectionStatus = BluetoothProfile.STATE_CONNECTED;
                PillGattLayer.this.subscribeFinishedCallbacks.clear();
                PillGattLayer.this.unsubscribeFinishedCallbacks.clear();

                if(connectedCallback != null){
                    connectedCallback.onCompleted(PillGattLayer.this.sender, (Void)null);
                }
            }
        });


    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        if(status == BluetoothGatt.GATT_SUCCESS){
            this.messageHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(PillUUID.CHAR_COMMAND_UUID.equals(characteristic.getUuid()) && PillGattLayer.this.commandWriteCallback != null){
                        PillGattLayer.this.commandWriteCallback.onCompleted(PillGattLayer.this.sender, characteristic);
                    }
                }
            });

        }
    }

    @Override
    public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        final byte[] values = characteristic.getValue();
        final int sequenceNumber = values[0];
        final PillBlePacket packet = new PillBlePacket(sequenceNumber, Arrays.copyOfRange(values, 1, values.length));

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                for (final PillBlePacketHandler handler : PillGattLayer.this.dataHanlders) {
                    if(!handler.shouldProcess(characteristic.getUuid())){
                        continue;
                    }

                    handler.onDataArrival(packet);
                }
            }
        });


    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, int status) {
        if(status == BluetoothGatt.GATT_SUCCESS) {

            this.messageHandler.post(new Runnable() {
                @Override
                public void run() {
                    PillOperationCallback<BluetoothGattDescriptor> callback = null;

                    if(Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                        callback = PillGattLayer.this.subscribeFinishedCallbacks.get(descriptor.getCharacteristic().getUuid());
                    }else if(Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                        callback = PillGattLayer.this.unsubscribeFinishedCallbacks.get(descriptor.getCharacteristic().getUuid());
                    }

                    if(callback == null){
                        return;
                    }
                    callback.onCompleted(PillGattLayer.this.sender, descriptor);
                }
            });

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

    public void subscribeNotification(final UUID charUUID,
                                          final PillOperationCallback<BluetoothGattDescriptor> subscribeFinishedCallback){

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                final BluetoothGattCharacteristic characteristic = PillGattLayer.this.bluetoothGattService.getCharacteristic(charUUID);
                if(!PillGattLayer.this.bluetoothGatt.setCharacteristicNotification(characteristic, true)){
                    Log.w(Pill.class.getName(), "Set notification for Characteristic: " + characteristic.getUuid() + " failed.");
                    return;
                }else {
                    final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(PillUUID.DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);  // This is the {0x01, 0x00} that shows up in the firmware
                    if (!PillGattLayer.this.bluetoothGatt.writeDescriptor(descriptor)) {
                        Log.w(Pill.class.getName(), "Set notification for descriptor: " + descriptor.getUuid() + " failed.");
                        return;
                    }
                }

                if(subscribeFinishedCallback != null) {
                    PillGattLayer.this.subscribeFinishedCallbacks.put(charUUID, subscribeFinishedCallback);
                }
            }
        });
    }

    public void unsubscribeNotification(final UUID charUUID,
                                            final PillOperationCallback<BluetoothGattDescriptor> unsubscribeFinishedCallback){

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                PillGattLayer.this.subscribeFinishedCallbacks.remove(charUUID);

                final BluetoothGattCharacteristic characteristic = PillGattLayer.this.bluetoothGattService.getCharacteristic(charUUID);
                final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(PillUUID.DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG);
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);  // This is the {0x01, 0x00} that shows up in the firmware
                if (!PillGattLayer.this.bluetoothGatt.writeDescriptor(descriptor)) {
                    Log.w(Pill.class.getName(), "Set notification for descriptor: " + descriptor.getUuid() + " failed.");
                    return;
                }else{
                    if(!PillGattLayer.this.bluetoothGatt.setCharacteristicNotification(characteristic, false)){
                        Log.w(Pill.class.getName(), "Reset notification for Characteristic: " + characteristic.getUuid() + " failed.");
                        return;
                    }

                }

                if(unsubscribeFinishedCallback != null) {
                    PillGattLayer.this.unsubscribeFinishedCallbacks.put(charUUID, unsubscribeFinishedCallback);
                }
            }
        });

    }

    public void disconnect(){

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                PillGattLayer.this.subscribeFinishedCallbacks.clear();
                PillGattLayer.this.unsubscribeFinishedCallbacks.clear();
                PillGattLayer.this.dataHanlders.clear();

                final Handler mainHandler = new Handler(LibApplication.getAppContext().getMainLooper());

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(PillGattLayer.this.bluetoothGatt != null) {
                            PillGattLayer.this.bluetoothGatt.disconnect();
                        }
                    }
                });


                PillGattLayer.this.connectionStatus = BluetoothProfile.STATE_DISCONNECTED;
            }
        });

    }
}
