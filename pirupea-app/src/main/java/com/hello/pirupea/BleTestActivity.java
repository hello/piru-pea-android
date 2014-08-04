package com.hello.pirupea;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.hello.ble.PillMotionData;
import com.hello.ble.BleOperationCallback;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.devices.Pill;
import com.hello.pirupea.settings.LocalSettings;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Set;


public class BleTestActivity extends ListActivity implements
        BleOperationCallback<Set<Pill>> {

    private ArrayAdapter<Pill> deviceArrayAdapter;

    private final BleOperationCallback<Void> pillConnectedCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, Void data) {
            final Pill connectedPill = (Pill)sender;
            LocalSettings.setPillAddress(BleTestActivity.this, connectedPill.getAddress());
            Toast.makeText(BleTestActivity.this, "Pill: " + connectedPill.getName() + " connected.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            final Pill connectedPill = (Pill)sender;
            Toast.makeText(BleTestActivity.this,
                    "Connect to pill: " + connectedPill.getName() + " failed: " + reason + " " + errorCode,
                    Toast.LENGTH_SHORT).show();
        }
    };


    private final BleOperationCallback<Integer> pillDisconnectCallback = new BleOperationCallback<Integer>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Integer data) {
            final Pill disconnectedPill = (Pill)sender;
            Toast.makeText(BleTestActivity.this, disconnectedPill.getName() + " disconnected: " + data, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            final Pill disconnectedPill = (Pill)sender;
            Toast.makeText(BleTestActivity.this, disconnectedPill.getName() + " disconnect failed, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };


    private final BleOperationCallback<DateTime> getTimeCallback = new BleOperationCallback<DateTime>() {
        @Override
        public void onCompleted(final HelloBleDevice connectedPill, final DateTime data) {
            if(data != null) {
                final DateTime localTime = new DateTime(data.getMillis());
                Toast.makeText(BleTestActivity.this, "Pill time: " + localTime.toString("MM/dd HH:mm:ss"), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            Toast.makeText(BleTestActivity.this, "Get time error, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();

        }

    };


    private final BleOperationCallback<BluetoothGattCharacteristic> setTimeCallback = new BleOperationCallback<BluetoothGattCharacteristic>() {
        @Override
        public void onCompleted(final HelloBleDevice connectedPill, final BluetoothGattCharacteristic data) {
            final Pill selectedPill = (Pill)connectedPill;
            Toast.makeText(BleTestActivity.this,
                    "Time set to " + DateTime.now().toString("MM/dd/yyyy HH:mm:ss") + " in " + selectedPill.getName(),
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            Toast.makeText(BleTestActivity.this, "Set time error, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_ble_test);


        this.deviceArrayAdapter = new ArrayAdapter<Pill>(this, android.R.layout.simple_list_item_1);
        this.setListAdapter(this.deviceArrayAdapter);

        //this.startService(new Intent(this, BleService.class));
        setProgressBarIndeterminateVisibility(true);
        Pill.discover(this, 10000);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();

        for(int i = 0; i < this.deviceArrayAdapter.getCount(); i++){
            final Pill pill = this.deviceArrayAdapter.getItem(i);
            pill.disconnect();

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ble_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCompleted(final HelloBleDevice pill, final Set<Pill> discoveredPills) {
        int debug = 0;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setProgressBarIndeterminateVisibility(false);
                deviceArrayAdapter.clear();
                for (final Pill pill : discoveredPills) {
                    pill.setConnectedCallback(pillConnectedCallback);
                    pill.setDisconnectedCallback(pillDisconnectCallback);
                    deviceArrayAdapter.add(pill);
                }

                deviceArrayAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {

    }



    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final Pill selectedPill = this.deviceArrayAdapter.getItem(position);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a command.");
        if(!selectedPill.isConnected()){
            builder.setItems(new CharSequence[]{ "Connect to Pill" }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    selectedPill.connect(false);
                }
            });
        }else{
            builder.setItems(new CharSequence[]{
                    "Set Time",
                    "Get Time",
                    "Calibrate",
                    "Get Data",
                    "Stop Advertising",
                    "Start Advertising",
                    "Disconnect"
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case 0: // set time
                            final DateTime targetDateTime = DateTime.now();
                            selectedPill.setTime(targetDateTime, setTimeCallback);
                            break;
                        case 1: // get time
                            selectedPill.getTime(getTimeCallback);
                            break;
                        case 2:
                            selectedPill.calibrate(new BleOperationCallback<Void>() {
                                @Override
                                public void onCompleted(final HelloBleDevice connectedPill, final Void data) {
                                    Toast.makeText(BleTestActivity.this, selectedPill.getName() + " calibrated.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                                    Toast.makeText(BleTestActivity.this,
                                            selectedPill.getName() + " calibrat failed, " + reason + ": " + errorCode,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        case 3:
                            selectedPill.getData(new BleOperationCallback<List<PillMotionData>>() {
                                @Override
                                public void onCompleted(final HelloBleDevice connectedPill, final List<PillMotionData> data) {
                                    final Pill pill = (Pill)connectedPill;
                                    Toast.makeText(BleTestActivity.this, "Pill: " + pill.getName() + " get data completed.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                                    final Pill pill = (Pill)sender;
                                    Toast.makeText(BleTestActivity.this,
                                            "Pill: " + pill.getName() + " get data failed, " + reason + ": " + errorCode,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case 6:
                            selectedPill.disconnect();
                            break;
                        case 4:
                            selectedPill.stopAdvertising();
                            break;
                        case 5:
                            selectedPill.startAdvertising();
                            break;
                        default:
                            break;
                    }
                }
            });
        }

        builder.show();
        super.onListItemClick(l, v, position, id);
    }
}
