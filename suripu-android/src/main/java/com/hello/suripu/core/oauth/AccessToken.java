package com.hello.suripu.core.oauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class AccessToken {

    @SerializedName("access_token")
    @JsonProperty("access_token")
    public final String token;

    @SerializedName("refresh_token")
    @JsonProperty("refresh_token")
    public final String refreshToken;

    @SerializedName("token_type")
    @JsonProperty("token_type")
    public final String tokenType = "Bearer";

    @SerializedName("expires_in")
    @JsonProperty("expires_in")
    public final Long expiresIn;


    @Expose(serialize = false, deserialize = false)
    @JsonIgnore
    public final OAuthScope[] scopes;

    @JsonCreator
    public AccessToken(
            @JsonProperty("access_token") final String token,
            @JsonProperty("refresh_token") final String refreshToken,
            @JsonProperty("expires_in") final Long expiresIn
            ) {

        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;

        this.scopes = new OAuthScope[0];
    }

}