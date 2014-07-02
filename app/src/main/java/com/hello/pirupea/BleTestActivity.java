package com.hello.pirupea;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.hello.ble.Pill;
import com.hello.ble.PillDiscoveryCallback;

import java.util.List;


public class BleTestActivity extends ListActivity implements PillDiscoveryCallback {

    private ArrayAdapter<Pill> deviceArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_test);
        this.deviceArrayAdapter = new ArrayAdapter<Pill>(this, android.R.layout.simple_list_item_1);
        this.setListAdapter(this.deviceArrayAdapter);

        Pill.discover(this, this, 30000);


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
    public void onScanCompleted(final List<Pill> discoveredPills) {
        int debug = 0;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceArrayAdapter.clear();
                for(final Pill pill:discoveredPills){
                    deviceArrayAdapter.add(pill);
                }

                deviceArrayAdapter.notifyDataSetChanged();
            }
        });
    }
}
