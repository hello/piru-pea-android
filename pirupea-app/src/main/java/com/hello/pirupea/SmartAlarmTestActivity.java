package com.hello.pirupea;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.echo.holographlibrary.Bar;
import com.echo.holographlibrary.BarGraph;
import com.hello.pirupea.settings.LocalSettings;
import com.hello.pirupea.settings.PillUserMap;
import com.hello.suripu.algorithm.core.Segment;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;
import com.sleepbot.datetimepicker.time.TimePickerDialog.OnTimeSetListener;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SmartAlarmTestActivity extends FragmentActivity {

    private Button cancelAlarm;
    private Button btnSetAlarm;
    private Button btnRingtoneTest;

    private TimePickerDialog timePickerDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_alarm_test);

        final String email = LocalSettings.getLastLoginUser();
        final String targetPillName = new PillUserMap().get(email);

        this.setTitle("Target: " + targetPillName);

        this.cancelAlarm = (Button) findViewById(R.id.btnEndAlarmService);
        this.btnSetAlarm = (Button) findViewById(R.id.btnSetAlarmTime);

        this.btnRingtoneTest = (Button) findViewById(R.id.btnRingtoneTest);

        this.btnRingtoneTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                SmartAlarmTestService.setRingTime(DateTime.now().plusSeconds(3));
            }
        });

        this.cancelAlarm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                LocalSettings.setAlarmTime(0);
                final TextView txtAlarmTime = (TextView) findViewById(R.id.txtAlarmTime);
                txtAlarmTime.setText(String.format("%02d", 0) + ":" + String.format("%02d", 0));
                SmartAlarmTestService.cancelAlarm();
            }
        });

        Calendar calendar = Calendar.getInstance();
        this.timePickerDialog = TimePickerDialog.newInstance(new OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialPickerLayout radialPickerLayout, int hourOfDay, int minute) {
                SmartAlarmTestService.cancelAlarm();
                final TextView txtAlarmTime = (TextView)findViewById(R.id.txtAlarmTime);
                txtAlarmTime.setText(String.format("%02d", hourOfDay) + ":" + String.format("%02d", minute));

                DateTime nextAlarm = DateTime.now().withTimeAtStartOfDay().plusHours(hourOfDay).plusMinutes(minute);
                if(nextAlarm.isBeforeNow()){
                    nextAlarm = nextAlarm.plusDays(1);

                }

                Toast.makeText(SmartAlarmTestActivity.this, "Alarm set to: " + nextAlarm.toString(), Toast.LENGTH_SHORT).show();

                LocalSettings.setAlarmTime(nextAlarm.getMillis());
                SmartAlarmTestService.setNextDataCollection(nextAlarm.minusMinutes(20));
            }
        }, calendar.get(Calendar.HOUR_OF_DAY) ,calendar.get(Calendar.MINUTE), false, false);


        this.btnSetAlarm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                timePickerDialog.show(getSupportFragmentManager(), "Select Time");
            }
        });


        long alarmMillis = LocalSettings.getAlarmTime();

        if(alarmMillis > 0) {
            DateTime dateTime = new DateTime(alarmMillis);
            final TextView txtAlarmTime = (TextView) findViewById(R.id.txtAlarmTime);
            txtAlarmTime.setText(String.format("%02d", dateTime.getHourOfDay()) + ":" + String.format("%02d", dateTime.getMinuteOfHour()));
        }

        /*
        final DataSource<AmplitudeData> dataSource = new CSVPillTestDataSource("Pill-EC_working.csv");
        final List<Segment> segments = new SleepCycleAlgorithm(dataSource, 15)
                .getCycles(DateTime.parse("09/07/2014", DateTimeFormat.forPattern("MM/dd/yyyy")));
        LocalSettings.setSleepCycles(segments);
        */

    }


    protected void onResume(){
        super.onResume();

        final BarGraph barGraph = (BarGraph) findViewById(R.id.bgSleepCycle);

        final List<Segment> segments = LocalSettings.getSleepCycles();
        if(segments.size() == 0){
            barGraph.setVisibility(View.INVISIBLE);
            return;
        }

        int index = 0;

        final DateTime alarmTime = new DateTime(LocalSettings.getAlarmTime()).plusMinutes(20);
        final int hours = alarmTime.getHourOfDay();
        final int minute = alarmTime.getMinuteOfHour();
        final DateTime alarmTimeAtThatDay = new DateTime(segments.get(segments.size() - 1).getEndTimestamp())
                .withTimeAtStartOfDay()
                .plusHours(hours)
                .plusMinutes(minute);

        DateTime startTime = alarmTimeAtThatDay.minusHours(9);
        final ArrayList<Bar> bars = new ArrayList<Bar>();

        while (startTime.isBefore(alarmTimeAtThatDay)){
            Segment segment = null;
            if(index < segments.size()) {
                segment = segments.get(index);
            }

            final Bar bar = new Bar();
            bar.setValue(0);



            if(segment != null) {
                final DateTime segmentStartTime = new DateTime(segment.getStartTimestamp());
                final DateTime segmentEndTime = new DateTime(segment.getEndTimestamp());

                if (startTime.isBefore(segmentStartTime)) {
                    bar.setValue(0);
                    bar.setColor(Color.parseColor("#00FFFFFF"));
                } else if (startTime.isAfter(segmentStartTime) && startTime.isBefore(segmentEndTime)) {
                    bar.setValue(1);
                    bar.setColor(Color.parseColor("#AA00FF00"));
                } else if (startTime.isAfter(segmentEndTime)) {
                    bar.setValue(0);
                    bar.setColor(Color.parseColor("#00FFFFFF"));
                    index++;
                }
            }

            bars.add(bar);
            if(startTime.getMinuteOfHour() == 0){
                bar.setName(String.valueOf(startTime.getHourOfDay()));
                bar.setLabelColor(Color.GRAY);
            }

            startTime = startTime.plusMinutes(1);
        }

        barGraph.setShowBarText(false);
        barGraph.setBars(bars);
        barGraph.setShowAxis(true);

        barGraph.setVisibility(View.VISIBLE);
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
