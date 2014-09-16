package com.hello.pirupea.dfu;

import android.app.Activity;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.hello.pirupea.R;
import com.hello.scanner.ScannerFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class BleDFUActivity extends Activity implements ScannerFragment.OnDeviceSelectedListener,
        HexSelectFragment.OnHexSelectedListener {
    static final int REQUEST_ENABLE_BT = 2;
    private static final String TAG = "DFU";
    private Button btnDFUScan;
    private Button btnSelectFirmware;
    private TextView tvDFUTarget;
    private BluetoothDevice dfuTarget;
    private String filePath;
    private Button btnStartDFU;
    private Uri fileStreamURI;
    private final BroadcastReceiver dfuReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if(BleDFUService.BROADCAST_PROGRESS.equals(action)){
                final int progress = intent.getIntExtra(BleDFUService.EXTRA_DATA,0);
                final int resolution = 20;
                char[] bar = new char[resolution];
                for(int i = 0; i < bar.length; i++){
                    if(progress > 0 && progress > i * (100/resolution)){
                        bar[i] = '*';
                    }else{
                        bar[i] = '-';
                    }
                }
                Log.i(TAG, "Progress");
                ((TextView)findViewById(R.id.tvProgress)).setText("[" + new String(bar) + "]");
            }else if(BleDFUService.BROADCAST_ERROR.equals(action)){
                Log.i(TAG, "Error");
            }
        }
    };


    private static IntentFilter makeDfuUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleDFUService.BROADCAST_PROGRESS);
        intentFilter.addAction(BleDFUService.BROADCAST_ERROR);
        intentFilter.addAction(BleDFUService.BROADCAST_LOG);
        return intentFilter;
    }
    @Override
    protected void onResume() {
        super.onResume();

        // We are using LocalBroadcastReceiver instead of normal BroadcastReceiver for optimization purposes
        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(dfuReceiver, makeDfuUpdateIntentFilter());
    }

    @Override
    public void onDeviceSelected(final BluetoothDevice device, final String name) {
        Log.i(TAG, "Picked:"+name);
        findViewById(R.id.btnSelectFirmware).setEnabled(true);
        dfuTarget = device;
        ((TextView)findViewById(R.id.tvDFUTarget)).setText(dfuTarget.getName());
    }

    @Override
    public void onHexSelected(Uri item) {
        Log.i(TAG, "Hex: "+item.toString());
        fileStreamURI = item;
        ((TextView)findViewById(R.id.tvFW)).setText(item.toString());
        (findViewById(R.id.btnStartDFU)).setEnabled(true);
    }

    @Override
    public void onHexCanceled() {

    }

    @Override
    public void onDialogCanceled() {
        dfuTarget = null;
    }

    private void showToast(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void showToast(final int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }
    private void isBLESupported() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showToast(R.string.no_ble);
            finish();
        }
    }
    private boolean isBLEEnabled() {
        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = manager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    private void showBLEDialog() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_dfu);
        isBLESupported();
        if(!isBLEEnabled()){
            showBLEDialog();
        }
        copyLinkedHex();
        this.btnDFUScan = (Button) findViewById(R.id.btnDFUScan);
        this.btnDFUScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                Log.i(TAG, "Scan Button Pressed");
                if (isBLEEnabled()) {
                    showDeviceScanningDialog();
                } else {
                    showBLEDialog();
                }
            }
        });
        //intiallaly hide the button
        findViewById(R.id.btnSelectFirmware).setEnabled(false);
        this.btnSelectFirmware = (Button) findViewById(R.id.btnSelectFirmware);
        this.btnSelectFirmware.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(final View view){
                showHexSelectionDialog();
            }
        });
        this.btnStartDFU = (Button)findViewById(R.id.btnStartDFU);
        this.btnStartDFU.setEnabled(false);
        this.btnStartDFU.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(final View view){
                Log.i(TAG, "Start Service");
                startDFU();
            }
        });
    }

    private void copyLinkedHex() {
        final File folder = new File(Environment.getExternalStorageDirectory(), "pirupea");
        if(!folder.exists()){
            folder.mkdir();
        }
        File f = new File(folder, "pill_default.hex");
        if(!f.exists()){
            copyRawResource(R.raw.pill_default,f);
            Log.i(TAG, "Copied");
        }else{
            Log.i(TAG, "Exists");
        }
        filePath = f.getPath();
    }


    private void startDFU() {
        final Intent service = new Intent(this, BleDFUService.class);
        service.putExtra(BleDFUService.EXTRA_DEVICE_ADDRESS, dfuTarget.getAddress());
        service.putExtra(BleDFUService.EXTRA_DEVICE_NAME, dfuTarget.getName());
        service.putExtra(BleDFUService.EXTRA_FILE_MIME_TYPE, BleDFUService.MIME_TYPE_HEX);
        service.putExtra(BleDFUService.EXTRA_FILE_TYPE, BleDFUService.TYPE_APPLICATION);
        service.putExtra(BleDFUService.EXTRA_FILE_URI, fileStreamURI);
        //service.putExtra(BleDFUService.EXTRA_FILE_PATH, filePath);

        startService(service);
    }

    private void copyRawResource(final int rawResId, final File dest) {
        try {
            final InputStream is = getResources().openRawResource(rawResId);
            final FileOutputStream fos = new FileOutputStream(dest);

            final byte[] buf = new byte[1024];
            int read = 0;
            try {
                while ((read = is.read(buf)) > 0)
                    fos.write(buf, 0, read);
            } finally {
                is.close();
                fos.close();
            }
        } catch (final IOException e) {
            Log.e(TAG, "Error while copying HEX file " + e.toString());
        }
    }
    private void showDeviceScanningDialog() {
        final FragmentManager fm = getFragmentManager();
        final ScannerFragment dialog = ScannerFragment.getInstance(BleDFUActivity.this, null, true);
        dialog.show(fm, "scan_fragment");
    }

    private void showHexSelectionDialog() {
        final FragmentManager fm = getFragmentManager();
        final HexSelectFragment dialog = HexSelectFragment.getInstance(BleDFUActivity.this);
        dialog.show(fm, "hex_fragment");
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ble_dfu, menu);
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
}
