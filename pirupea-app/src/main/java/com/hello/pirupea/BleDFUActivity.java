package com.hello.pirupea;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.hello.scanner.ScannerFragment;


public class BleDFUActivity extends Activity implements ScannerFragment.OnDeviceSelectedListener{
    static final int REQUEST_ENABLE_BT = 2;
    private static final String TAG = "DFU";
    private Button btnDFUScan;
    private BluetoothDevice dfuTarget;
    @Override
    public void onDeviceSelected(final BluetoothDevice device, final String name) {
        Log.i(TAG, "Picked:"+name);
    }

    @Override
    public void onDialogCanceled() {

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
                Log.i("DFU", "Scan Button Pressed");
                if (isBLEEnabled()) {
                    showDeviceScanningDialog();
                } else {
                    showBLEDialog();
                }
            }
        });
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
