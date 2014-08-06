package com.hello.ble.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.google.common.base.Objects;
import com.hello.ble.BleOperationCallback;
import com.hello.ble.LibApplication;
import com.hello.ble.PillCommand;
import com.hello.ble.PillMotionData;
import com.hello.ble.stack.TimeDataHandler;
import com.hello.ble.stack.CommandResponseDataHandler;
import com.hello.ble.stack.HelloGattLayer;
import com.hello.ble.stack.MotionDataHandler;
import com.hello.ble.stack.PillBlePacketHandler;
import com.hello.ble.util.BleDateTimeConverter;
import com.hello.ble.util.BleUUID;

import org.joda.time.DateTime;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by pangwu on 7/1/14.
 */
public class Pill implements HelloBleDevice {

    private static final int DEFAULT_SCAN_INTERVAL_MS = 10000;


    protected Context context;
    private BluetoothDevice bluetoothDevice;
    private HelloGattLayer gattLayer;

    private TimeDataHandler bleTimeDataHandler;
    private MotionDataHandler motionPacketHandler;
    private CommandResponseDataHandler commandResponsePacketHandler;
    private BleOperationCallback<Void> pairedCallback;
    private BleOperationCallback<Void> unpairCallback;

    private BleOperationCallback<Void> connectedCallback;
    private BleOperationCallback<Integer> disconnectedCallback;

    private final BroadcastReceiver pairingReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                final BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(Pill.this.bluetoothDevice == null){
                    return;
                }

                if(!Pill.this.bluetoothDevice.getAddress().equals(bondedDevice.getAddress())){
                    return;
                }

                if (state == BluetoothDevice.BOND_BONDED) {
                    context.unregisterReceiver(this);
                    if(Pill.this.pairedCallback != null){
                        Pill.this.pairedCallback.onCompleted(Pill.this, null);
                    }
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    context.unregisterReceiver(this);

                    if(Pill.this.unpairCallback != null){
                        Pill.this.unpairCallback.onCompleted(Pill.this, null);
                    }
                }

            }
        }
    };


    private Pill(){

    }

    protected Pill(final Context context, final BluetoothDevice pillDevice){
        checkNotNull(context);

        this.bluetoothDevice = pillDevice;
        this.context = context;

        this.bleTimeDataHandler = new TimeDataHandler(this);
        this.motionPacketHandler = new MotionDataHandler(this);
        this.commandResponsePacketHandler = new CommandResponseDataHandler(this);

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
        final PillBlePacketHandler transmissionLayer = new PillBlePacketHandler();

        // attach application layer on top of transmission layer
        transmissionLayer.registerDataHandler(this.bleTimeDataHandler);
        transmissionLayer.registerDataHandler(this.bleTimeDataHandler);
        transmissionLayer.registerDataHandler(this.motionPacketHandler);
        transmissionLayer.registerDataHandler(this.commandResponsePacketHandler);

        // attach the link layer to transmission layer
        this.gattLayer = new HelloGattLayer(this, this.bluetoothDevice,
                transmissionLayer,
                connectedCallback,
                disconnectCallback);



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


    public void stopAdvertising(){
        saveAndResetPreviousCommandWriteCallback();

        final byte[] pillCommandData = new byte[]{PillCommand.STOP_ADVERTISE.getValue()};
        Pill.this.gattLayer.writeCommand(pillCommandData);
    }

    public void startAdvertising(){

        saveAndResetPreviousCommandWriteCallback();
        final byte[] pillCommandData = new byte[]{ PillCommand.START_ADVERTISE.getValue() };
        this.gattLayer.writeCommand(pillCommandData);
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


    public void getData(final BleOperationCallback<List<PillMotionData>> getDataCallback){

        saveAndResetPreviousCommandWriteCallback();

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

    public void setConnectedCallback(final BleOperationCallback<Void> connectedCallback){
        this.connectedCallback = connectedCallback;
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
                    Pill.this.connect(connectedCallback);
                }

                @Override
                public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                    if(Pill.this.connectedCallback != null){
                        Pill.this.connectedCallback.onFailed(sender, reason, errorCode);
                    }
                }
            };
            pair(pairedCallback);
        }else{
            this.connect(connectedCallback);
        }
    }

    @Override
    public void connect(final BleOperationCallback<Void> connectedCallback) {
        setConnectedCallback(connectedCallback);
        Pill.this.gattLayer.connect();
    }

    public void connect(){
        checkNotNull(this.bluetoothDevice);
        if(isConnected()){
            return;
        }

        Pill.this.gattLayer.connect();
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
                    Pill.this.gattLayer.connect();
                }

                @Override
                public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                    if(Pill.this.connectedCallback != null){
                        Pill.this.connectedCallback.onFailed(sender, reason, errorCode);
                    }
                }
            };
            pair(pairedCallback);
        }else{
            this.gattLayer.connect();
        }
    }

    public void setDisconnectedCallback(final BleOperationCallback<Integer> disconnectCallback){
        this.disconnectedCallback = disconnectCallback;
    }

    public void disconnect(){
        if(this.gattLayer != null) {
            this.gattLayer.disconnect();
        }

        try {
            LibApplication.getAppContext().unregisterReceiver(this.pairingReceiver);
        }catch (IllegalArgumentException iae){
            Log.w(Pill.class.getName(), "Disconnect without paired.");
        }
        //IO.log("Try to disconnect pill " + this.getName() + "@" + this.getAddress());
    }

    public boolean isConnected(){
        return this.gattLayer.getConnectionStatus() == BluetoothProfile.STATE_CONNECTED;
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

    @Override
    public int hashCode(){
        return this.getAddress().hashCode();
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

        /*
        final PillOperationCallback<Set<Pill>> bondDiscoveryCallback = new PillOperationCallback<Set<Pill>>() {
            @Override
            public void onCompleted(final Pill connectedPill, final Set<Pill> bondedPills) {
                pills.addAll(bondedPills);
                Pill targetPill = null;

                for(final Pill pill:pills){
                    if(pill.getAddress().equals(address)){
                        targetPill = pill;
                    }
                }

                onDiscoverCompleted.onCompleted(null, targetPill);
            }
        };

        final ConnectionBasedPillDetector bondScanner =
                new ConnectionBasedPillDetector(bluetoothAdapter.getBondedDevices(), bondDiscoveryCallback);
        */

        final BleOperationCallback<Set<Pill>> scanDiscoveryCallback = new BleOperationCallback<Set<Pill>>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final Set<Pill> advertisingPills) {
                pills.addAll(advertisingPills);
                //bondScanner.beginDiscovery();

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


        final ScanBasedPillDetector scanner = new ScanBasedPillDetector(new String[]{ address },
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

        /*
        final PillOperationCallback<Set<Pill>> bondDiscoveryCallback = new PillOperationCallback<Set<Pill>>() {
            @Override
            public void onCompleted(final Pill connectedPill, final Set<Pill> bondedPills) {
                finalPills.addAll(bondedPills);
                //scanner.beginDiscovery();
                onDiscoverCompleted.onCompleted(null, finalPills);
            }
        };

        final ConnectionBasedPillDetector bondScanner = new ConnectionBasedPillDetector(bluetoothAdapter.getBondedDevices(), bondDiscoveryCallback);
        */

        final BleOperationCallback<Set<Pill>> scanDiscoveryCallback = new BleOperationCallback<Set<Pill>>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final Set<Pill> advertisingPills) {
                finalPills.addAll(advertisingPills);
                onDiscoverCompleted.onCompleted(null, finalPills);
                //bondScanner.beginDiscovery();
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                // This will never be called.
            }
        };

        final ScanBasedPillDetector scanner = new ScanBasedPillDetector(null,
                maxScanTime <= 0 ? DEFAULT_SCAN_INTERVAL_MS : maxScanTime,
                scanDiscoveryCallback);

        scanner.beginDiscovery();

        return true;

    }

}
