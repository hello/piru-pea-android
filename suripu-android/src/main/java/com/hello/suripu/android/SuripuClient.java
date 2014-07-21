package com.hello.suripu.android;

import com.hello.suripu.core.oauth.AccessToken;

import java.util.HashMap;

import retrofit.Callback;
import retrofit.RestAdapter;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Created by pangwu on 4/9/14.
 */
public class SuripuClient {
    public static final String SURIPU_API_ENDPOINT = "http://api.skeletor.com";
    //public static final String SURIPU_API_ENDPOINT = "http://192.168.128.52:9999";

    public static final String SURIPU_DATA_ENDPOINT = "http://in.skeletor.com";
    //public static final String SURIPU_DATA_ENDPOINT = "http://192.168.128.52:5555";

    public static void getToken(final String userName, final String password,
                                final ClientApplication application,
                                final Callback<AccessToken> accessTokenCallback){

        checkNotNull(application.getClientId());
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(SuripuClient.SURIPU_API_ENDPOINT)
                .build();

        SuripuHTTPService suripuService = restAdapter.create(SuripuHTTPService.class);

        HashMap<String, String> additionParameters = new HashMap<String, String>();
        additionParameters.put("grant_type", "password");
        additionParameters.put("client_id", application.getClientId());
        suripuService.getToken(userName, password, additionParameters, accessTokenCallback);
    }



}
