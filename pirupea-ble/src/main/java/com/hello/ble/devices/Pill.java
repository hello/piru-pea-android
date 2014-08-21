package com.hello.ble.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.LibApplication;
import com.hello.ble.PillCommand;
import com.hello.ble.PillMotionData;
import com.hello.ble.stack.application.MotionXYZDataHandler;
import com.hello.ble.stack.application.PillBatteryVoltageDataHandler;
import com.hello.ble.stack.application.PillResponseDataHandler;
import com.hello.ble.stack.HelloGattLayer;
import com.hello.ble.stack.application.MotionDataHandler;
import com.hello.ble.stack.transmission.PillBlePacketHandler;
import com.hello.ble.stack.application.TimeDataHandler;
import com.hello.ble.util.BleDateTimeConverter;
import com.hello.ble.util.BleUUID;
import com.hello.ble.util.HelloBleDeviceScanner;
import com.hello.ble.util.PillScanner;

import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by pangwu on 7/1/14.
 */
public class Pill extends HelloBleDevice {
    private TimeDataHandler bleTimeDataHandler;
    private MotionDataHandler motionPacketHandler;
    private MotionXYZDataHandler motionXYZDataHandler;
    private PillResponseDataHandler commandResponsePacketHandler;

    private PillBatteryVoltageDataHandler pillBatteryVoltageDataHandler;

    private PillBlePacketHandler transmissionLayer;


    public Pill(final Context context, final BluetoothDevice pillDevice){
        super(context, pillDevice);
        checkNotNull(context);

        this.bleTimeDataHandler = new TimeDataHandler(this);
        this.motionPacketHandler = new MotionDataHandler(this);
        this.commandResponsePacketHandler = new PillResponseDataHandler(this);
        this.motionXYZDataHandler = new MotionXYZDataHandler(this);
        this.pillBatteryVoltageDataHandler = new PillBatteryVoltageDataHandler(this);

        final BleOperationCallback<Void> connectedCallback = new BleOperationCallback<Void>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final Void data) {
                if(Pill.this.connectedCallback != null){
                    Pill.this.connectedCallback.onCompleted(sender, data);
                }
            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                if(Pill.this.connectedCallback != null){
                    Pill.this.connectedCallback.onFailed(sender, reason, errorCode);
                }
            }
        };

        final BleOperationCallback<Integer> disconnectCallback = new BleOperationCallback<Integer>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final Integer reason) {
                if(Pill.this.disconnectedCallback != null){
                    Pill.this.disconnectedCallback.onCompleted(sender, reason);
                }
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if(Pill.this.disconnectedCallback != null){
                    Pill.this.disconnectedCallback.onFailed(sender, reason, errorCode);
                }
            }
        };

        // this is the transmission layer
        this.transmissionLayer = new PillBlePacketHandler();

        // attach application layer on top of transmission layer
        transmissionLayer.registerDataHandler(this.bleTimeDataHandler);
        transmissionLayer.registerDataHandler(this.bleTimeDataHandler);
        transmissionLayer.registerDataHandler(this.motionPacketHandler);
        transmissionLayer.registerDataHandler(this.commandResponsePacketHandler);

        // attach the link layer to transmission layer
        this.gattLayer = new HelloGattLayer(this, this.bluetoothDevice,
                BleUUID.PILL_SERVICE_UUID,
                transmissionLayer,
                connectedCallback,
                disconnectCallback);

    }


    public void setTime(final DateTime target){
        setTime(target, null);
    }


    public void setTime(final DateTime target, final BleOperationCallback<BluetoothGattCharacteristic> setTimeFinishedCallback){

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

    private void saveAndResetPreviousCommandWriteCallback(){
        final BleOperationCallback<BluetoothGattCharacteristic> previousCommandWriteCallback = this.gattLayer.getCommandWriteCallback();

        // When the current callback has been executed, set the previous callback to place.
        this.gattLayer.setCommandWriteCallback(new BleOperationCallback<BluetoothGattCharacteristic>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final BluetoothGattCharacteristic data) {
                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
            }
        });
    }


    public void getTime(final BleOperationCallback<DateTime> getTimeCallback){

        saveAndResetPreviousCommandWriteCallback();

        this.bleTimeDataHandler.setDataCallback(new BleOperationCallback<DateTime>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final DateTime data) {
                // We don't care whether the unsubscription is successful or not
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DAY_DATETIME_UUID, null);

                if(getTimeCallback != null){
                    getTimeCallback.onCompleted(Pill.this, data);
                }
            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DAY_DATETIME_UUID, null);
                if(getTimeCallback != null){
                    getTimeCallback.onFailed(sender, reason, errorCode);
                }
            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_DAY_DATETIME_UUID, new BleOperationCallback<BluetoothGattDescriptor>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final BluetoothGattDescriptor data) {
                final byte[] pillCommandData = new byte[]{PillCommand.GET_TIME.getValue()};
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if(getTimeCallback != null){
                    getTimeCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

    }

    public void calibrate(final BleOperationCallback<Void> calibrateCallback){
        saveAndResetPreviousCommandWriteCallback();

        this.commandResponsePacketHandler.setDataCallback(new BleOperationCallback<PillCommand>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final PillCommand data) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, null);
                if(calibrateCallback != null){
                    calibrateCallback.onCompleted(Pill.this, null);
                }
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, null);
                if(calibrateCallback != null){
                    calibrateCallback.onFailed(sender, reason, errorCode);
                }
            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, new BleOperationCallback<BluetoothGattDescriptor>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, BluetoothGattDescriptor data) {
                final byte[] pillCommandData = new byte[]{PillCommand.CALIBRATE.getValue()};
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if(calibrateCallback != null){
                    calibrateCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

    }


    @Deprecated
    public void startStream(final BleOperationCallback<Void> operationCallback, final BleOperationCallback<Short[]> dataCallback){
        this.gattLayer.setCommandWriteCallback(null);

        this.gattLayer.subscribeNotification(BleUUID.CHAR_DATA_UUID, new BleOperationCallback<BluetoothGattDescriptor>() {

            @Override
            public void onCompleted(final HelloBleDevice sender, final BluetoothGattDescriptor data) {
                Pill.this.gattLayer.setCommandWriteCallback(new BleOperationCallback<BluetoothGattCharacteristic>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final BluetoothGattCharacteristic data) {
                        Pill.this.transmissionLayer.unregisterDataHandler(Pill.this.motionPacketHandler);
                        Pill.this.transmissionLayer.registerDataHandler(Pill.this.motionXYZDataHandler);
                        Pill.this.motionXYZDataHandler.setDataCallback(dataCallback);

                        if(operationCallback != null){
                            operationCallback.onCompleted(sender, null);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });

                final byte[] pillCommandData = new byte[]{PillCommand.START_STREAM.getValue()};
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                if(operationCallback != null){
                    operationCallback.onFailed(sender, reason, errorCode);
                }
            }
        });
    }

    @Deprecated
    public void stopStream(final BleOperationCallback<Void> operationCallback){

        this.transmissionLayer.unregisterDataHandler(this.motionXYZDataHandler);
        this.transmissionLayer.registerDataHandler(this.motionPacketHandler);

        this.gattLayer.setCommandWriteCallback(new BleOperationCallback<BluetoothGattCharacteristic>() {
            @Override
            public void onCompleted(HelloBleDevice sender, BluetoothGattCharacteristic data) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DATA_UUID, new BleOperationCallback<BluetoothGattDescriptor>() {
                    @Override
                    public void onCompleted(HelloBleDevice sender, BluetoothGattDescriptor data) {
                        if(operationCallback != null){
                            operationCallback.onCompleted(sender, null);
                        }
                    }

                    @Override
                    public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if(operationCallback != null){
                    operationCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

        final byte[] pillCommandData = new byte[]{PillCommand.STOP_STREAM.getValue()};
        Pill.this.gattLayer.writeCommand(pillCommandData);

    }


    public void getData(final BleOperationCallback<List<PillMotionData>> getDataCallback){

        saveAndResetPreviousCommandWriteCallback();
        this.transmissionLayer.registerDataHandler(this.motionPacketHandler);

        this.motionPacketHandler.setDataCallback(new BleOperationCallback<List<PillMotionData>>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final List<PillMotionData> data) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DATA_UUID, null);
                if(getDataCallback != null){
                    getDataCallback.onCompleted(Pill.this, data);
                }
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DATA_UUID, null);
                if(getDataCallback != null){
                    getDataCallback.onFailed(sender, reason, errorCode);
                }
            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_DATA_UUID, new BleOperationCallback<BluetoothGattDescriptor>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final BluetoothGattDescriptor data) {
                final byte[] pillCommandData = new byte[]{PillCommand.GET_DATA.getValue()};
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if(getDataCallback != null){
                    getDataCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

    }


    public void getData(final int unitLength, final BleOperationCallback<List<PillMotionData>> getDataCallback){

        saveAndResetPreviousCommandWriteCallback();
        final MotionDataHandler motionDataHandler32Bit = new MotionDataHandler(unitLength, this);

        this.transmissionLayer.unregisterDataHandler(this.motionPacketHandler);  // avoid conflict
        this.transmissionLayer.registerDataHandler(motionDataHandler32Bit);

        motionDataHandler32Bit.setDataCallback(new BleOperationCallback<List<PillMotionData>>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final List<PillMotionData> data) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DATA_UUID, null);
                Pill.this.transmissionLayer.unregisterDataHandler(motionDataHandler32Bit);
                if(getDataCallback != null){
                    getDataCallback.onCompleted(Pill.this, data);
                }
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DATA_UUID, null);
                Pill.this.transmissionLayer.unregisterDataHandler(motionDataHandler32Bit);
                if(getDataCallback != null){
                    getDataCallback.onFailed(sender, reason, errorCode);
                }
            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_DATA_UUID, new BleOperationCallback<BluetoothGattDescriptor>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final BluetoothGattDescriptor data) {
                final byte[] pillCommandData = new byte[]{PillCommand.GET_DATA.getValue()};
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.transmissionLayer.unregisterDataHandler(motionDataHandler32Bit);
                if(getDataCallback != null){
                    getDataCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

    }

    public void getBatteryLevel(final BleOperationCallback<Integer> getBatteryLevelCallback){
        this.transmissionLayer.unregisterDataHandler(this.commandResponsePacketHandler);
        this.transmissionLayer.registerDataHandler(this.pillBatteryVoltageDataHandler);
        final BleOperationCallback<BluetoothGattCharacteristic> previousCommandWriteCallback = this.gattLayer.getCommandWriteCallback();

        this.pillBatteryVoltageDataHandler.setDataCallback(new BleOperationCallback<Integer>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final Integer data) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, null);

                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
                Pill.this.transmissionLayer.registerDataHandler(Pill.this.commandResponsePacketHandler);
                Pill.this.transmissionLayer.unregisterDataHandler(Pill.this.pillBatteryVoltageDataHandler);

                if(getBatteryLevelCallback != null){
                    getBatteryLevelCallback.onCompleted(Pill.this, data);
                }
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, null);

                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
                Pill.this.transmissionLayer.registerDataHandler(Pill.this.commandResponsePacketHandler);
                Pill.this.transmissionLayer.unregisterDataHandler(Pill.this.pillBatteryVoltageDataHandler);

                if(getBatteryLevelCallback != null){
                    getBatteryLevelCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

        final BleOperationCallback<BluetoothGattCharacteristic> commandWriteCallback = new BleOperationCallback<BluetoothGattCharacteristic>() {
            @Override
            public void onCompleted(HelloBleDevice sender, BluetoothGattCharacteristic data) {
                // Do nothing, write to command interface succeed.
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, null);

                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
                Pill.this.transmissionLayer.registerDataHandler(Pill.this.commandResponsePacketHandler);
                Pill.this.transmissionLayer.unregisterDataHandler(Pill.this.pillBatteryVoltageDataHandler);

                if(getBatteryLevelCallback != null){
                    getBatteryLevelCallback.onFailed(sender, reason, errorCode);
                }
            }
        };


        this.gattLayer.subscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, new BleOperationCallback<BluetoothGattDescriptor>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final BluetoothGattDescriptor data) {
                final byte[] pillCommandData = new byte[]{PillCommand.GET_BATTERY_VOLT.getValue()};
                Pill.this.gattLayer.setCommandWriteCallback(commandWriteCallback);
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
                Pill.this.transmissionLayer.registerDataHandler(Pill.this.commandResponsePacketHandler);
                Pill.this.transmissionLayer.unregisterDataHandler(Pill.this.pillBatteryVoltageDataHandler);

                if(getBatteryLevelCallback != null){
                    getBatteryLevelCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

    }

    public void setConnectedCallback(final BleOperationCallback<Void> connectedCallback){
        this.connectedCallback = connectedCallback;
    }

    public void setDisconnectedCallback(final BleOperationCallback<Integer> disconnectCallback){
        this.disconnectedCallback = disconnectCallback;
    }


    public boolean isPaired(){
        if(this.bluetoothDevice == null){
            return false;
        }

        return this.bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED;
    }


    public static boolean discover(final String address, final BleOperationCallback<Pill> onDiscoverCompleted, final int maxScanTime){
        //checkNotNull(context);
        checkNotNull(LibApplication.getAppContext());
        checkNotNull(onDiscoverCompleted);

        final Context context = LibApplication.getAppContext();
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            return false;
        }


        final Set<Pill> pills = new HashSet<Pill>();

        final BleOperationCallback<Set<HelloBleDevice>> scanDiscoveryCallback = new BleOperationCallback<Set<HelloBleDevice>>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final Set<HelloBleDevice> advertisingPills) {
                for(final HelloBleDevice device:advertisingPills){
                    pills.add((Pill)device);
                }

                Pill targetPill = null;

                for(final Pill pill:pills){
                    if(pill.getAddress().equals(address)){
                        targetPill = pill;
                    }
                }

                onDiscoverCompleted.onCompleted(null, targetPill);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                // This will never be called.
            }
        };


        final HelloBleDeviceScanner scanner = new PillScanner(new String[]{ address },
                maxScanTime <= 0 ? DEFAULT_SCAN_INTERVAL_MS : maxScanTime,
                scanDiscoveryCallback);

        scanner.beginDiscovery();

        return true;

    }

    public static boolean discover(final BleOperationCallback<Set<Pill>> onDiscoverCompleted, final int maxScanTime){
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

        final Set<Pill> finalPills = new HashSet<Pill>();

        final BleOperationCallback<Set<HelloBleDevice>> scanDiscoveryCallback = new BleOperationCallback<Set<HelloBleDevice>>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final Set<HelloBleDevice> advertisingDevices) {

                for(final HelloBleDevice device:advertisingDevices){
                   finalPills.add((Pill)device);
                }
                onDiscoverCompleted.onCompleted(null, finalPills);
                //bondScanner.beginDiscovery();
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                // This will never be called.
            }
        };

        final HelloBleDeviceScanner scanner = new PillScanner(null,
                maxScanTime <= 0 ? DEFAULT_SCAN_INTERVAL_MS : maxScanTime,
                scanDiscoveryCallback);

        scanner.beginDiscovery();

        return true;

    }

}
