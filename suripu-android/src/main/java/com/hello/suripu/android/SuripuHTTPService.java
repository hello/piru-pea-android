package com.hello.suripu.android;

import com.hello.suripu.core.oauth.AccessToken;

import java.util.HashMap;

import retrofit.Callback;
import retrofit.http.Field;
import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;

/**
 * Created by pangwu on 7/15/14.
 */
public interface SuripuHTTPService {
    @FormUrlEncoded
    @POST("/oauth2/token")
    void getToken(@Field("username") String userName,
                  @Field("password") String password,
                  @Field("grant_type") String grantType,
                  @Field("client_id") String clientId,
                  Callback<AccessToken> accessTokenCallback);

    @FormUrlEncoded
    @POST("/oauth2/token")
    void getToken(@FieldMap HashMap<String, String> params,
                  Callback<AccessToken> accessTokenCallback);

    @FormUrlEncoded
    @POST("/oauth2/token")
    void getToken(@Field("username") String userName,
                  @Field("password") String password,
                  @FieldMap HashMap<String, String> optionalParams,
                  Callback<AccessToken> accessTokenCallback);
}
