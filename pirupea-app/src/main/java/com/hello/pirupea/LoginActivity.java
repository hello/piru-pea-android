package com.hello.pirupea;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.ble.util.IO;
import com.hello.pirupea.settings.LocalSettings;
import com.hello.suripu.android.BleTestApplication;
import com.hello.suripu.android.SuripuClient;
import com.hello.suripu.core.oauth.AccessToken;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by pangwu on 4/1/14.
 */
public class LoginActivity
        extends Activity
        implements Callback<AccessToken> {

    private EditText edUserName;
    private EditText edPassword;
    private TextView txtError;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        this.edUserName = (EditText)this.findViewById(R.id.edUserName);
        this.edPassword = (EditText)this.findViewById(R.id.edPassword);
        this.txtError = (TextView)this.findViewById(R.id.txtError);

    }

    public void onSignInClicked(View sender){
        final String userName = this.edUserName.getText().toString();
        final String passWord = this.edPassword.getText().toString();

        SuripuClient.getToken(userName, passWord, new BleTestApplication(), this);

        //OAuth.getAccessToken(userName, passWord, this);

    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();

        finish();
    }


    @Override
    public void success(AccessToken accessToken, Response response) {
        LoginActivity.this.txtError.setVisibility(View.GONE);

        try {
            String accessTokenJSONString = new ObjectMapper().writeValueAsString(accessToken);
            LocalSettings.saveOAuthToken(LoginActivity.this, accessTokenJSONString);
            Intent bleActivityIntent = new Intent(LoginActivity.this, BleTestActivity.class);
            bleActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(bleActivityIntent);
            finish();
        }catch (Exception ex){
            ex.printStackTrace();
            IO.log(ex);


            LoginActivity.this.txtError.setVisibility(View.VISIBLE);
            LoginActivity.this.txtError.setText("Error! Contact Pang please.");
        }
    }

    @Override
    public void failure(RetrofitError error) {
        Response r = error.getResponse();
        if (r != null && r.getStatus() == 401) {
            LoginActivity.this.txtError.setVisibility(View.VISIBLE);
            LoginActivity.this.txtError.setText("Login failed.");
        }
    }
}
