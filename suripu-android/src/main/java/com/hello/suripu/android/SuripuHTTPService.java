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
interface SuripuHTTPService {
    @FormUrlEncoded
    @POST("/oauth2/token")
    void getToken(@Field("username") final String userName,
                  @Field("password") final String password,
                  @Field("grant_type") final String grantType,
                  @Field("client_id") final String clientId,
                  final Callback<AccessToken> accessTokenCallback);

    @FormUrlEncoded
    @POST("/oauth2/token")
    void getToken(@FieldMap final HashMap<String, String> params,
                  final Callback<AccessToken> accessTokenCallback);

    @FormUrlEncoded
    @POST("/oauth2/token")
    void getToken(@Field("username") final String userName,
                  @Field("password") final String password,
                  @FieldMap final HashMap<String, String> optionalParams,
                  final Callback<AccessToken> accessTokenCallback);
}
