package com.hello.suripu.core.db.models;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * THIS IS A TEMPORARY CLASS FOR ACCEPTING DATA GENERATED BY THE STUPID BLUE ACCELEROMETERS
 * DO NOT EVEN THINK ABOUT RE-USING THIS IN **ANY** WAY
 * THIS IS TEMPORARY
 */
@Deprecated
public class TempTrackerData {

    public final Long timestamp;
    public final Long value;
    public final String trackerId;

    @JsonCreator
    public TempTrackerData(
            @JsonProperty("timestamp") Long timestamp,
            @JsonProperty("value") Long value,
            @JsonProperty("tracker_id") String trackerId) {

        this.timestamp = timestamp;
        this.value = value;
        this.trackerId = trackerId;
    }
}
