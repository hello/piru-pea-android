package com.hello.pirupea;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.hello.pirupea.settings.LocalSettings;

public class BleTestSelectionActivity extends Activity {

    private Button btnPillTest;
    private Button btnMorpheus;
    private Button btnSmartAlarm;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_test_selection);

        this.btnMorpheus = (Button) findViewById(R.id.btnMorpheusTest);
        this.btnMorpheus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                Intent bleActivityIntent = new Intent(BleTestSelectionActivity.this, MorpheusBleTestActivity.class);
                bleActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(bleActivityIntent);
            }
        });


        this.btnPillTest = (Button) findViewById(R.id.btnPillTest);
        this.btnPillTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                Intent bleActivityIntent = new Intent(BleTestSelectionActivity.this, PillBleTestActivity.class);
                bleActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(bleActivityIntent);
            }
        });


        this.btnSmartAlarm = (Button) findViewById(R.id.btnSmartAlarmTest);
        this.btnSmartAlarm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bleActivityIntent = new Intent(BleTestSelectionActivity.this, SmartAlarmTestActivity.class);
                bleActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(bleActivityIntent);
            }
        });

        this.btnLogout = (Button) findViewById(R.id.btnLogout);
        this.btnLogout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                LocalSettings.saveOAuthToken("");
                final Intent bleActivityIntent = new Intent(BleTestSelectionActivity.this, LoginActivity.class);
                bleActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(bleActivityIntent);

                finish();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ble_test_selection, menu);
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
