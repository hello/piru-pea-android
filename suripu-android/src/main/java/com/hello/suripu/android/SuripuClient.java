package com.hello.suripu.android;

import com.google.common.net.HttpHeaders;
import com.hello.suripu.core.db.models.PillRegistration;
import com.hello.suripu.core.db.models.TempTrackerData;
import com.hello.suripu.core.oauth.AccessToken;

import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Created by pangwu on 4/9/14.
 */
public class SuripuClient {
    public static final String SURIPU_API_ENDPOINT = "https://dev-api.hello.is/";
    //public static final String SURIPU_API_ENDPOINT = "http://192.168.128.52:9999";

    public static final String SURIPU_DATA_ENDPOINT = "https://dev-in.hello.is/";
    //public static final String SURIPU_DATA_ENDPOINT = "http://192.168.128.52:5555";

    private SuripuHTTPService apiService;
    private SuripuHTTPService dataService;

    private AccessToken token;

    public void setAccessToken(final AccessToken accessToken){
        this.token = accessToken;

        final RequestInterceptor dataRequestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader(HttpHeaders.AUTHORIZATION, accessToken.tokenType + " " + accessToken.token);
            }
        };

        final RestAdapter dataRestAdapter = new RestAdapter.Builder()
                .setEndpoint(SURIPU_DATA_ENDPOINT)
                .setRequestInterceptor(dataRequestInterceptor)
                .build();

        this.dataService = dataRestAdapter.create(SuripuHTTPService.class);

        final RequestInterceptor appRequestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader(HttpHeaders.AUTHORIZATION, accessToken.tokenType + " " + accessToken.token);
            }
        };

        final RestAdapter appRestAdapter = new RestAdapter.Builder()
                .setEndpoint(SURIPU_API_ENDPOINT)
                .setRequestInterceptor(appRequestInterceptor)
                .build();

        this.apiService = appRestAdapter.create(SuripuHTTPService.class);
    }

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

    public void uploadPillData(final List<TempTrackerData> pillData,
                                      final Callback<Void> uploadDataCallback){
        this.dataService.sendTempData(pillData, uploadDataCallback);
    }

    public void registerPill(final String pillId, final Callback<Void> callback){
        this.apiService.registerPill(new PillRegistration(pillId), callback);

    }



}
