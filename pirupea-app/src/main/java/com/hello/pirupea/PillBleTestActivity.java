package com.hello.pirupea;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.ble.BleOperationCallback;
import com.hello.ble.PillMotionData;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.devices.Pill;
import com.hello.ble.util.IO;
import com.hello.data.collection.ContinuesMotionWidget;
import com.hello.pirupea.settings.LocalSettings;
import com.hello.suripu.android.SuripuClient;
import com.hello.suripu.core.db.models.TempTrackerData;
import com.hello.suripu.core.oauth.AccessToken;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class PillBleTestActivity extends ListActivity implements
        BleOperationCallback<Set<Pill>> {

    private ArrayAdapter<Pill> deviceArrayAdapter;
    private SuripuClient suripuClient;

    private final BleOperationCallback<Void> pillConnectedCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, Void data) {
            final Pill connectedPill = (Pill)sender;
            LocalSettings.setPillAddress(connectedPill.getAddress());
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this, "Pill: " + connectedPill.getName() + " connected.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            final Pill connectedPill = (Pill)sender;
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this,
                    "Connect to pill: " + connectedPill.getName() + " failed: " + reason + " " + errorCode,
                    Toast.LENGTH_SHORT).show();
        }
    };


    private final BleOperationCallback<Integer> pillDisconnectCallback = new BleOperationCallback<Integer>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Integer data) {
            final Pill disconnectedPill = (Pill)sender;
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this, disconnectedPill.getName() + " disconnected: " + data, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            final Pill disconnectedPill = (Pill)sender;
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this, disconnectedPill.getName() + " disconnect failed, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };


    private final BleOperationCallback<DateTime> getTimeCallback = new BleOperationCallback<DateTime>() {
        @Override
        public void onCompleted(final HelloBleDevice connectedPill, final DateTime data) {
            if(data != null) {
                final DateTime localTime = new DateTime(data.getMillis());
                uiEndOperation();
                Toast.makeText(PillBleTestActivity.this, "Pill time: " + localTime.toString("MM/dd HH:mm:ss"), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this, "Get time error, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();

        }

    };


    private final BleOperationCallback<UUID> setTimeCallback = new BleOperationCallback<UUID>() {
        @Override
        public void onCompleted(final HelloBleDevice connectedPill, final UUID charUUID) {
            final Pill selectedPill = (Pill)connectedPill;
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this,
                    "Time set to " + DateTime.now().toString("MM/dd/yyyy HH:mm:ss") + " in " + selectedPill.getName(),
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this, "Set time error, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private final BleOperationCallback<List<PillMotionData>> dataCallback = new BleOperationCallback<List<PillMotionData>>() {
        @Override
        public void onCompleted(final HelloBleDevice connectedPill, final List<PillMotionData> data) {
            final Pill pill = (Pill)connectedPill;
            final StringBuilder stringBuilder = new StringBuilder(1000);
            stringBuilder.append("timestamp,amplitude,time_string\r\n");
            for(final PillMotionData datum:data){
                stringBuilder.append(datum.timestamp.getMillis()).append(",")
                        .append(datum.maxAmplitude).append(",")
                        .append(new DateTime(datum.timestamp.getMillis()))
                        .append("\r\n");
            }

            final File csvFile = IO.getFileByName(pill.getName(), "csv");
            IO.appendStringToFile(csvFile, stringBuilder.toString());


            final ArrayList<TempTrackerData> dataArrayList = new ArrayList<TempTrackerData>();
            for(final PillMotionData datum:data){
                dataArrayList.add(new TempTrackerData(datum.timestamp.getMillis(), datum.maxAmplitude, connectedPill.getId()));
            }

            suripuClient.registerPill(connectedPill.getId(), new Callback<Void>() {

                private void doUpload(){
                    suripuClient.uploadPillData(dataArrayList, new Callback<Void>() {
                        @Override
                        public void success(final Void aVoid, final Response response) {
                            uiEndOperation();
                            Toast.makeText(PillBleTestActivity.this, "Pill: " + pill.getName() + " get data completed.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void failure(final RetrofitError error) {
                            uiEndOperation();
                            Toast.makeText(PillBleTestActivity.this, "Pill: " + pill.getName() + " get data completed, but upload failed: " + error.getResponse().getReason(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void success(final Void aVoid, final Response response) {
                    doUpload();
                }

                @Override
                public void failure(final RetrofitError error) {
                    if(error.isNetworkError()){
                        uiEndOperation();
                        Toast.makeText(PillBleTestActivity.this, "Failed to upload data " + connectedPill.getId() + ", network error.", Toast.LENGTH_SHORT).show();
                    }else {
                        if (error.getResponse().getStatus() == 409) { // Pill already registered..
                            doUpload();
                        } else {
                            uiEndOperation();
                            Toast.makeText(PillBleTestActivity.this, "Failed to register device " + connectedPill.getId() + ": " + error.getResponse().getStatus(), Toast.LENGTH_SHORT).show();

                            if (error.getResponse().getStatus() == 401) { // token expired
                                connectedPill.disconnect();
                                goLoginScreen();
                                finish();
                            }
                        }
                    }
                }
            });





        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            final Pill pill = (Pill)sender;
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this,
                    "Pill: " + pill.getName() + " get data failed, " + reason + ": " + errorCode,
                    Toast.LENGTH_SHORT).show();
        }
    };


    private final BleOperationCallback<Void> startStreamOperationalCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(HelloBleDevice sender, Void data) {
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this, "Streaming started.", Toast.LENGTH_SHORT);
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this, "Start streaming failed.", Toast.LENGTH_SHORT);
        }
    };


    private final BleOperationCallback<Void> stopStreamOperationalCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(HelloBleDevice sender, Void data) {
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this, "Streaming stopped.", Toast.LENGTH_SHORT);
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this, "Stop streaming failed.", Toast.LENGTH_SHORT);
        }
    };




    private final BleOperationCallback<Long[]> streamDataCallback = new BleOperationCallback<Long[]>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Long[] data) {
            final Pill pill = (Pill)sender;
            final File csvFile = IO.getFileByName(pill.getName(), "stream.csv");
            final DateTime dateTime = DateTime.now();

            if(!csvFile.exists()){
                IO.appendStringToFile(csvFile, "timestamp,x,y,z,aggregate,readable_time\r\n");
            }

            final StringBuilder builder = new StringBuilder();
            builder.append(dateTime.getMillis()).append(",")
                    .append(data[0]).append(",")
                    .append(data[1]).append(",")
                    .append(data[2]).append(",")
                    .append(data[3]).append(",")
                    .append(dateTime.toString()).append(",")
                    .append("\r\n");
            IO.appendStringToFile(csvFile, builder.toString());
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            Log.w("Streaming error", "Stream data error.");
        }
    };


    private final BleOperationCallback<Integer> batteryLevelCallback = new BleOperationCallback<Integer>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Integer data) {
            uiEndOperation();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PillBleTestActivity.this, "Battery voltage: " + data + " milli volt", Toast.LENGTH_LONG);
                }
            });

        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();
            Toast.makeText(PillBleTestActivity.this, "Get battery voltage failed," + reason + ": " + errorCode, Toast.LENGTH_SHORT);
        }
    };


    final BleOperationCallback<Void> calibrateOperationCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice connectedPill, final Void data) {
            uiEndOperation();
            final Pill selectedPill = (Pill)connectedPill;
            Toast.makeText(PillBleTestActivity.this, selectedPill.getName() + " calibrated.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
            uiEndOperation();
            final Pill selectedPill = (Pill)sender;
            Toast.makeText(PillBleTestActivity.this,
                    selectedPill.getName() + " calibrate failed, " + reason + ": " + errorCode,
                    Toast.LENGTH_SHORT).show();
        }
    };

    final BleOperationCallback<Void> pairingCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Void data) {
            uiEndOperation();

            Toast.makeText(PillBleTestActivity.this, "Paired " + sender.getName(), Toast.LENGTH_SHORT);
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();

            Toast.makeText(PillBleTestActivity.this,
                    sender.getName() + " pair failed, " + reason + ": " + errorCode,
                    Toast.LENGTH_SHORT).show();
        }
    };


    private ContinuesMotionWidget continuesMotionWidget = new ContinuesMotionWidget();


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


        this.deviceArrayAdapter = new ArrayAdapter<Pill>(this, android.R.layout.simple_list_item_1);
        this.setListAdapter(this.deviceArrayAdapter);

        //this.startService(new Intent(this, BleService.class));

        this.suripuClient = new SuripuClient();
        setProgressBarIndeterminateVisibility(true);
        Pill.discover(this, 10000);
    }

    @Override
    protected void onResume(){
        super.onResume();

        final String accessTokenString = LocalSettings.getOAuthToken();
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final AccessToken token = mapper.readValue(accessTokenString, AccessToken.class);
            this.suripuClient.setAccessToken(token);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        if(R.id.action_scan == id){
            setProgressBarIndeterminateVisibility(true);
            Pill.discover(this, 10000);
            this.deviceArrayAdapter.clear();
            this.deviceArrayAdapter.notifyDataSetChanged();
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
                    uiBeginOperation();
                    selectedPill.connect();
                }
            });
        }else{
            builder.setTitle("Pill Id: " + selectedPill.getId());
            builder.setItems(new CharSequence[]{
                    "Set Time",             //0
                    "Get Time",             //1
                    "Calibrate",            //2
                    //"Get Data, 16bit",             //3
                    "Get Data, 32bit",             //3
                    "Start Streaming",  //4
                    "Stop Streaming",  //5
                    "Get Battery Voltage",   //6
                    "Pair",  //7
                    "Disconnect"            //8
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    uiBeginOperation();
                    switch (which){
                        case 0: // set time
                            final DateTime targetDateTime = DateTime.now();
                            selectedPill.setTime(targetDateTime, setTimeCallback);
                            break;
                        case 1: // get time
                            selectedPill.getTime(getTimeCallback);
                            break;
                        case 2:
                            selectedPill.calibrate(calibrateOperationCallback);
                            break;
                        /*case 3:
                            selectedPill.getData(16, dataCallback);
                            break;*/
                        case 3:
                            selectedPill.getData(32, dataCallback);
                            break;
                        case 4:
                            selectedPill.startStream(startStreamOperationalCallback, streamDataCallback);
                            continuesMotionWidget.register(PillBleTestActivity.this);
                            continuesMotionWidget.beginWork(0);
                            break;
                        case 5:
                            selectedPill.stopStream(stopStreamOperationalCallback);
                            continuesMotionWidget.unregister();
                            break;
                        case 6:
                            selectedPill.getBatteryLevel(batteryLevelCallback);
                            break;
                        case 7:
                            selectedPill.pair(pairingCallback);
                            break;
                        case 8:
                            selectedPill.disconnect();
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

    private void goLoginScreen(){
        final Intent bleActivityIntent = new Intent(this, LoginActivity.class);
        bleActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(bleActivityIntent);


    }
}
