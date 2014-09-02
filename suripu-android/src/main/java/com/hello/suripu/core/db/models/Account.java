package com.hello.suripu.core.db.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

public class Account {

    // TODO: add age or DoB

    @NotNull
    @JsonIgnore
    public final Optional<Long> id;

    @JsonProperty("id")
    @SerializedName("id")
    public final String externalID;

    @JsonProperty("email")
    @SerializedName("email")
    public final String email;

    @JsonProperty("tz")
    @SerializedName("tz")
    public final Integer tzOffsetMillis;

    @JsonProperty("name")
    @SerializedName("name")
    public final String name;

    @JsonProperty("gender")
    @SerializedName("gender")
    public final Gender gender;

    @JsonProperty("height")
    @SerializedName("height")
    public final Integer height;

    @JsonProperty("weight")
    @SerializedName("weight")
    public final Integer weight;


    /**
     *
     * @param id
     * @param email
     * @param password
     * @param tzOffsetMillis
     * @param name
     * @param gender
     * @param height
     * @param weight
     */
    private Account(final Optional<Long> id,
                   final String externalID,
                   final String email,
                   final String password,
                   final Integer tzOffsetMillis,
                   final String name,
                   final Gender gender,
                   final Integer height,
                   final Integer weight,
                   final DateTime created) {

        this.id = id;
        this.externalID = externalID;
        this.email = email;
        this.tzOffsetMillis = tzOffsetMillis;

        this.name = name;
        this.gender = gender;
        this.height = height;
        this.weight = weight;


    }


}
