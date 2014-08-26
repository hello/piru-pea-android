package com.hello.pirupea;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.devices.Morpheus;
import com.hello.pirupea.settings.LocalSettings;

import java.util.Set;


public class MorpheusBleTestActivity extends ListActivity implements
        BleOperationCallback<Set<Morpheus>> {

    private ArrayAdapter<Morpheus> deviceArrayAdapter;

    private final BleOperationCallback<Void> connectedCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, Void data) {
            final Morpheus connectedDevice = (Morpheus)sender;
            LocalSettings.setPillAddress(connectedDevice.getAddress());
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, connectedDevice.getName() + " connected.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            final Morpheus connectedDevice = (Morpheus)sender;
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this,
                    "Connect to " + connectedDevice.getName() + " failed: " + reason + " " + errorCode,
                    Toast.LENGTH_SHORT).show();
        }
    };


    private final BleOperationCallback<Integer> disconnectCallback = new BleOperationCallback<Integer>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Integer data) {
            final Morpheus disconnectedDevice = (Morpheus)sender;
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, disconnectedDevice.getName() + " disconnected: " + data, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            final Morpheus disconnectedDevice = (Morpheus)sender;
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, disconnectedDevice.getName() + " disconnect failed, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private final BleOperationCallback<Void> modeSwitchCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(HelloBleDevice sender, Void data) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " mode switched.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " mode switched failed, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };


    private final BleOperationCallback<String> getDeviceIdOperationCallback = new BleOperationCallback<String>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final String data) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " device id: " + data, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " get device id failed, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };


    private void uiBeginOperation(){
        setProgressBarIndeterminateVisibility(true);
        setProgressBarIndeterminate(true);
    }

    private void uiEndOperation(){
        setProgressBarIndeterminate(false);
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_ble_test);


        this.deviceArrayAdapter = new ArrayAdapter<Morpheus>(this, android.R.layout.simple_list_item_1);
        this.setListAdapter(this.deviceArrayAdapter);

        //this.startService(new Intent(this, BleService.class));
        setProgressBarIndeterminateVisibility(true);
        Morpheus.discover(this, 10000);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();

        for(int i = 0; i < this.deviceArrayAdapter.getCount(); i++){
            final Morpheus morpheus = this.deviceArrayAdapter.getItem(i);
            morpheus.disconnect();

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
    public void onCompleted(final HelloBleDevice pill, final Set<Morpheus> discoveredMorpheus) {
        int debug = 0;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setProgressBarIndeterminateVisibility(false);
                deviceArrayAdapter.clear();
                for (final Morpheus morpheus : discoveredMorpheus) {
                    morpheus.setConnectedCallback(connectedCallback);
                    morpheus.setDisconnectedCallback(disconnectCallback);
                    deviceArrayAdapter.add(morpheus);
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
        final Morpheus selectedDevice = this.deviceArrayAdapter.getItem(position);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a command.");
        if(!selectedDevice.isConnected()){
            builder.setItems(new CharSequence[]{ "Connect to Sense" }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    uiBeginOperation();
                    selectedDevice.connect(true);
                }
            });
        }else{
            builder.setItems(new CharSequence[]{
                    "Pairing Mode",//0
                    "Normal Mode",//1
                    "Get Device ID",//2
                    "Start WIFI Scan",
                    "Stop WIFI Scan",
                    "Set WIFI End Point",
                    "Disconnect"//3
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    uiBeginOperation();
                    switch (which){
                        case 0:
                            selectedDevice.switchToPairingMode(modeSwitchCallback);
                            break;
                        case 1:
                            selectedDevice.switchToNormalMode(modeSwitchCallback);
                            break;

                        case 2:
                            selectedDevice.getDeviceId(getDeviceIdOperationCallback);
                            break;
                        case 6:
                            selectedDevice.disconnect();
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