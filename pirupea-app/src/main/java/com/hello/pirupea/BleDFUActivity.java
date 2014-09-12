package com.hello.pirupea;

import no.nordicsemi.android.dfu.DfuBaseService;
import android.app.Activity;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hello.scanner.ScannerFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;



public class BleDFUActivity extends Activity implements ScannerFragment.OnDeviceSelectedListener{
    static final int REQUEST_ENABLE_BT = 2;
    private static final String TAG = "DFU";
    private Button btnDFUScan;
    private Button btnSelectFirmware;
    private TextView tvDFUTarget;
    private BluetoothDevice dfuTarget;
    private String filePath;
    private Button btnStartDFU;

    private final BroadcastReceiver dfuReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if(DfuBaseService.BROADCAST_PROGRESS.equals(action)){
                Log.i(TAG, "Progress");
            }else if(DfuBaseService.BROADCAST_ERROR.equals(action)){
                Log.i(TAG, "Error");
            }
        }
    };


    @Override
    public void onDeviceSelected(final BluetoothDevice device, final String name) {
        Log.i(TAG, "Picked:"+name);
        findViewById(R.id.btnSelectFirmware).setEnabled(true);
        dfuTarget = device;
        ((TextView)findViewById(R.id.tvDFUTarget)).setText(dfuTarget.getName());
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
                Log.i(TAG, "Click");
                //select firmware, but now just copy raw to file
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
                btnStartDFU.setEnabled(true);
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

    private void startDFU() {
        if(this.filePath != null && this.dfuTarget != null){

        }
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
