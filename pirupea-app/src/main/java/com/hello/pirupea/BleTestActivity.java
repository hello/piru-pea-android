package com.hello.pirupea;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.hello.ble.PillData;
import com.hello.ble.PillOperationCallback;
import com.hello.ble.devices.Pill;
import com.hello.pirupea.settings.LocalSettings;

import org.joda.time.DateTime;

import java.util.List;


public class BleTestActivity extends ListActivity implements
        PillOperationCallback<List<Pill>> {

    private ArrayAdapter<Pill> deviceArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_test);
        this.deviceArrayAdapter = new ArrayAdapter<Pill>(this, android.R.layout.simple_list_item_1);
        this.setListAdapter(this.deviceArrayAdapter);

        //this.startService(new Intent(this, BleService.class));

    }

    @Override
    protected void onResume(){
        super.onResume();

        Pill.discover(this, 3000);
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
    public void onCompleted(final Pill pill, final List<Pill> discoveredPills) {
        int debug = 0;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceArrayAdapter.clear();
                for (final Pill pill : discoveredPills) {
                    deviceArrayAdapter.add(pill);
                }

                deviceArrayAdapter.notifyDataSetChanged();
            }
        });
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
                    selectedPill.connect(new PillOperationCallback<Void>() {
                        @Override
                        public void onCompleted(final Pill connectedPill, final Void data) {

                            LocalSettings.setPillAddress(BleTestActivity.this, connectedPill.getAddress());
                            Toast.makeText(BleTestActivity.this, "Pill: " + connectedPill.getName() + " connected.", Toast.LENGTH_SHORT).show();

                        }

                    },
                    new PillOperationCallback<Void>() {
                        @Override
                        public void onCompleted(Pill connectedPill, Void data) {
                            Toast.makeText(BleTestActivity.this, "Connect to pill: " + connectedPill.getName() + " failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    );
                }
            });
        }else{
            builder.setItems(new CharSequence[]{
                    "Set Time",
                    "Get Time",
                    "Calibrate",
                    "Get Data",
                    "Disconnect"
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case 0: // set time
                            final DateTime targetDateTime = DateTime.now();
                            selectedPill.setTime(targetDateTime, new PillOperationCallback<BluetoothGattCharacteristic>() {
                                @Override
                                public void onCompleted(Pill connectedPill, BluetoothGattCharacteristic data) {
                                    Toast.makeText(BleTestActivity.this,
                                            "Time set to " + targetDateTime.toString("MM/dd HH:mm:ss") + " in " + selectedPill.getName(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case 1: // get time
                            selectedPill.getTime(new PillOperationCallback<DateTime>() {
                                @Override
                                public void onCompleted(Pill connectedPill, final DateTime data) {


                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(data != null) {
                                                final DateTime localTime = new DateTime(data.getMillis());
                                                Toast.makeText(BleTestActivity.this, "Pill time: " + localTime.toString("MM/dd HH:mm:ss"), Toast.LENGTH_SHORT).show();
                                            }else{
                                                Toast.makeText(BleTestActivity.this, "Get time error, timer may not init.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            });
                            break;
                        case 2:
                            selectedPill.calibrate(new PillOperationCallback<Void>() {
                                @Override
                                public void onCompleted(Pill connectedPill, Void data) {
                                    Toast.makeText(BleTestActivity.this, selectedPill.getName() + " calibrated.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        case 3:
                            selectedPill.getData(new PillOperationCallback<List<PillData>>() {
                                @Override
                                public void onCompleted(final Pill connectedPill, final List<PillData> data) {

                                }
                            });
                            break;
                        case 4:
                            selectedPill.disconnect(new PillOperationCallback<Void>() {
                                @Override
                                public void onCompleted(Pill connectedPill, Void data) {
                                    Toast.makeText(BleTestActivity.this, selectedPill.getName() + " disconnected.", Toast.LENGTH_SHORT).show();
                                }
                            });

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
