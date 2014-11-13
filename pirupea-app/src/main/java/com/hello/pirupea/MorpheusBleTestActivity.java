package com.hello.pirupea;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.ble.BleOperationCallback;
import com.hello.ble.HelloBle;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.devices.Morpheus;
import com.hello.ble.protobuf.MorpheusBle.MorpheusCommand;
import com.hello.ble.protobuf.MorpheusBle.wifi_endpoint;
import com.hello.ble.protobuf.MorpheusBle.wifi_endpoint.sec_type;
import com.hello.pirupea.settings.LocalSettings;
import com.hello.suripu.core.oauth.AccessToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class MorpheusBleTestActivity extends ListActivity implements
        BleOperationCallback<Set<Morpheus>> {

    private ArrayAdapter<Morpheus> deviceArrayAdapter;
    private String wifiPassword = "";

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

    private final BleOperationCallback<Void> erasePairedUsersCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Void data) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " erase apired users.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " erase paired user failed, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };


    private final BleOperationCallback<String> unpairPillCallback = new BleOperationCallback<String>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final String data) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " pill " + data + " unpaired.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " unpair pill failed, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private final BleOperationCallback<String> pairPillCallback = new BleOperationCallback<String>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final String data) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " pill " + data + " paired.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " pair pill failed, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private final BleOperationCallback<Void> linkAccountCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Void data) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " account linked.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " link account failed, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };


    private final BleOperationCallback<Void> wifiConnectionCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Void data) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " wifi set.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " set wifi failed, " + reason + ": " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private final BleOperationCallback<Void> factoryResetCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Void data) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " factory reset success.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " factory reset failed, " + reason + " code: " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private final BleOperationCallback<Void> ledCallback = new BleOperationCallback<Void>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final Void data) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " led command sent.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " led command failed, " + reason + " code: " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };


    private final BleOperationCallback<MorpheusCommand> getWifiCallback = new BleOperationCallback<MorpheusCommand>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final MorpheusCommand data) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() +
                    " connected to " + data.getWifiSSID() + " status: " + data.getWifiConnectionState(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, int errorCode) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " Get wifi failed, " + reason + " code: " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private final BleOperationCallback<List<wifi_endpoint>> wifiScanCallback = new BleOperationCallback<List<wifi_endpoint>>() {
        @Override
        public void onCompleted(final HelloBleDevice sender, final List<wifi_endpoint> data) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MorpheusBleTestActivity.this)
                    .setTitle("Select WIFI");



            final ArrayList<String> wifiEndPointString = new ArrayList<String>();

            for(final wifi_endpoint wifi_ep:data){
                String hexMac = "";
                final byte[] mac = wifi_ep.getBssid().toByteArray();
                for(final byte b:mac){

                    if(hexMac.length() > 0){
                        hexMac += ":" + Byte.toString(b);
                    }else{
                        hexMac += Byte.toString(b);
                    }
                }
                wifiEndPointString.add(wifi_ep.getSsid() + ", rssi: " + wifi_ep.getRssi() + ", mac: " + hexMac);
            }

            builder.setItems(wifiEndPointString.toArray(new CharSequence[0]), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    final wifi_endpoint endPoint = data.get(i);
                    password((Morpheus)sender, endPoint.getSsid(), endPoint.getBssid().toString(), endPoint.getSecurityType());
                }
            });

            builder.show();
        }

        @Override
        public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
            uiEndOperation();
            Toast.makeText(MorpheusBleTestActivity.this, sender.getName() + " wifi scan failed, " + reason + " code: " + errorCode, Toast.LENGTH_SHORT).show();
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

        if(R.id.action_scan == id){
            setProgressBarIndeterminateVisibility(true);
            Morpheus.discover(this, 10000);
            this.deviceArrayAdapter.clear();
            this.deviceArrayAdapter.notifyDataSetChanged();
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
                    selectedDevice.connect();
                }
            });
        }else{
            builder.setItems(new CharSequence[]{
                    "Disconnect", //0
                    "Pairing Mode",//1
                    "Normal Mode",//2
                    "Get Device ID",//3
                    "Erase Paired Users", // 4
                    "Set WIFI End Point", // 5
                    "Pair Pill", // 6
                    "Link Account",
                    "Unpair Pill",
                    "Wipe Firmware",
                    "Factory Reset",
                    "Get Wifi Endpoint",
                    "LED Fade In",
                    "LED Fade Out",
                    "LED Rainbow"
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    uiBeginOperation();
                    switch (which){
                        case 0:
                            selectedDevice.disconnect();
                            break;
                        case 1:
                            selectedDevice.switchToPairingMode(modeSwitchCallback);
                            break;
                        case 2:
                            selectedDevice.switchToNormalMode(modeSwitchCallback);
                            break;

                        case 3:
                            selectedDevice.getDeviceId(getDeviceIdOperationCallback);
                            break;
                        case 4:
                            selectedDevice.clearPairedUser(erasePairedUsersCallback);
                            break;
                        case 5:
                            selectedDevice.scanSupportedWIFIAP(wifiScanCallback);
                            break;

                        case 6: {
                            final String accessTokenString = LocalSettings.getOAuthToken();
                            final ObjectMapper mapper = new ObjectMapper();
                            try {
                                final AccessToken accessToken = mapper.readValue(accessTokenString, AccessToken.class);
                                selectedDevice.pairPill(accessToken.token, pairPillCallback);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                        case 7: {
                            final String accessTokenString = LocalSettings.getOAuthToken();
                            final ObjectMapper mapper = new ObjectMapper();
                            try {
                                final AccessToken accessToken = mapper.readValue(accessTokenString, AccessToken.class);
                                selectedDevice.linkAccount(accessToken.token, linkAccountCallback);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                        break;

                        case 8:
                            selectedDevice.unpairPill("55614E945A95CA03", unpairPillCallback);
                            break;
                        case 9:
                            selectedDevice.wipeFirmware(new BleOperationCallback<Void>() {
                                @Override
                                public void onCompleted(HelloBleDevice sender, Void data) {
                                    uiEndOperation();
                                    Toast.makeText(HelloBle.getApplicationContext(), "wipe done!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                                    uiEndOperation();
                                    Toast.makeText(HelloBle.getApplicationContext(), "Wipe failed, error: " + reason, Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case 10:
                            selectedDevice.factoryReset(factoryResetCallback);
                            break;
                        case 11:
                            selectedDevice.getWIFI(getWifiCallback);
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

    private void password(final Morpheus connectedDevice, final String SSID, final String BSSID, final sec_type securityType) {
        final String convertedSSID = SSID;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        // Set an EditText view to get user input
        final EditText txtPassword = new EditText(this);

        alert.setTitle("Input Password")
                .setMessage("Please Input the password for WIFI \"" + SSID + "\"")
                .setView(txtPassword)
                .setPositiveButton("Done", new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, int i) {
                        final String password = txtPassword.getText().toString();
                        connectedDevice.setWIFIConnection(BSSID, convertedSSID, securityType, password, wifiConnectionCallback);
                    }
                }).show();

    }
}
