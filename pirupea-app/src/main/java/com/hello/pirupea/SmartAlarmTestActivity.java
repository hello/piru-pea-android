package com.hello.pirupea;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SmartAlarmTestActivity extends Activity {

    private Button btnStartService;
    private Button btnStopService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_alarm_test);

        this.btnStartService = (Button) findViewById(R.id.btnStartAlarmService);
        this.btnStopService = (Button) findViewById(R.id.btnEndAlarmService);

        this.btnStartService.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(new Intent(SmartAlarmTestActivity.this, SmartAlarmTestService.class));
            }
        });

        this.btnStopService.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(SmartAlarmTestActivity.this, SmartAlarmTestService.class));
            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.smart_alarm_test, menu);
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
