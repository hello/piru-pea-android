package com.hello.suripu.android;

import com.hello.suripu.core.db.models.Account;
import com.hello.suripu.core.db.models.FirmwareUpdate;
import com.hello.suripu.core.db.models.PillRegistration;
import com.hello.suripu.core.db.models.TempTrackerData;
import com.hello.suripu.core.oauth.AccessToken;

import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by pangwu on 7/15/14.
 */
interface SuripuHTTPService {
    @FormUrlEncoded
    @POST("/v1/oauth2/token")
    void getToken(@Field("username") final String userName,
                  @Field("password") final String password,
                  @Field("grant_type") final String grantType,
                  @Field("client_id") final String clientId,
                  final Callback<AccessToken> accessTokenCallback);

    @FormUrlEncoded
    @POST("/v1/oauth2/token")
    void getToken(@FieldMap final HashMap<String, String> params,
                  final Callback<AccessToken> accessTokenCallback);

    @FormUrlEncoded
    @POST("/v1/oauth2/token")
    void getToken(@Field("username") final String userName,
                  @Field("password") final String password,
                  @FieldMap final HashMap<String, String> optionalParams,
                  final Callback<AccessToken> accessTokenCallback);


    @POST("/in/temp/tracker")
    @Deprecated
    public void sendTempData(
            @Body final List<TempTrackerData> trackerData, final Callback<Void> sendCallback);

    @POST("/v1/devices/pill")
    public void registerPill(
            @Body final PillRegistration pillRegistration, final Callback<Void> sendCallback);

    @GET("/v1/account")
    public void getCurrentAccount(final Callback<Account> sendCallback);

    @GET("/download/pill/manifest")
    public void getPillFirmwareUpdates(final Callback<List<FirmwareUpdate>> getFirmwareUpdateCallback);

}
