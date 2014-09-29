package com.hello.pirupea.dfu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.hello.pirupea.LoginActivity;

public class BleDFUNotificationActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If this activity is the root activity of the task, the app is not running
        if (isTaskRoot()) {
            // Start the app before finishing
            final Intent parentIntent = new Intent(this, LoginActivity.class);
            parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            final Intent startAppIntent = new Intent(this, BleDFUActivity.class);
            startAppIntent.putExtras(getIntent().getExtras());
            startActivities(new Intent[] { parentIntent, startAppIntent });
        }

        // Now finish, which will drop the user in to the activity that was at the top
        //  of the task stack
        finish();
    }
}