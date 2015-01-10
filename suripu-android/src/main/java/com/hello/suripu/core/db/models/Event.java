package com.hello.suripu.core.db.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Created by pangwu on 5/8/14.
 */
public class Event {
    public enum Type {
        MOTION(0),
        NOISE(1),
        SNORING(2),
        SLEEP_TALK(3),
        LIGHT(4),
        SLEEP_MOTION(5);

        private int value;

        private Type(int value) {
            this.value = value;
        }

        public int getValue(){
            return this.value;
        }

        public static Type fromInteger(int value){
            switch (value){
                case 0:
                    return MOTION;
                case 1:
                    return NOISE;
                case 2:
                    return SNORING;
                case 3:
                    return SLEEP_TALK;
                case 4:
                    return LIGHT;
                case 5:
                    return SLEEP_MOTION;
                default:
                    return MOTION;
            }
        }
    }

    @JsonProperty("event_type")
    @SerializedName("event_type")
    public final Type type;

    @JsonProperty("start_timestamp")
    @SerializedName("start_timestamp")
    public final long startTimestamp;


    @JsonProperty("end_timestamp")
    @SerializedName("end_timestamp")
    public final long endTimestamp;

    @JsonProperty("timezone_offset")
    @SerializedName("timezone_offset")
    public final int timezoneOffset;

    @JsonCreator
    public Event(

            @JsonProperty("event_type") final Type type,
            @JsonProperty("start_timestamp") final long startTimestamp,
            @JsonProperty("end_timestamp") final long endTimestamp,
            @JsonProperty("timezone_offset") final int timezoneOffset

    ){
        this.type = type;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.timezoneOffset = timezoneOffset;
    }

    @Override
    public boolean equals(Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final Event convertedObject = (Event) other;

        return  this.type.equals(convertedObject.type)
                && this.startTimestamp == convertedObject.startTimestamp
                && this.endTimestamp == convertedObject.endTimestamp
                && this.timezoneOffset == convertedObject.timezoneOffset;
    }
}
