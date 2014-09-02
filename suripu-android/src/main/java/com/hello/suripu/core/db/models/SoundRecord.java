package com.hello.suripu.core.db.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class SoundRecord {
    public static final float FLOAT_TO_INT_CONVERTER = 10000000f;

    @JsonProperty("device_id")
    @SerializedName("device_id")
    public final long deviceId;

    @JsonProperty("value")
    @SerializedName("value")
    public final int maxAmplitude;

    @JsonProperty("timestamp")
    @SerializedName("timestamp")
    public final long timestamp;

    @JsonCreator
    public SoundRecord(
            @JsonProperty("device_id") long deviceId,
            @JsonProperty("value") Integer maxAmplitude,
            @JsonProperty("timestamp") long timestamp
    ) {
        this.deviceId = deviceId;
        this.maxAmplitude = maxAmplitude;
        this.timestamp = timestamp;
    }

    public static float intToFloatValue(final int value){
        return value / FLOAT_TO_INT_CONVERTER;
    }
}
