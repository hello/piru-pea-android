package com.hello.suripu.core.db.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class SleepLabel {
    @JsonProperty("date_utc")
    @SerializedName("date_utc")
    public long targetDateUTC;

    @JsonProperty("rating")
    @SerializedName("rating")
    public SleepRating rating;

    @JsonProperty("sleep_at_utc")
    @SerializedName("sleep_at_utc")
    public long sleepTimeUTC;

    @JsonProperty("wakeup_at_utc")
    @SerializedName("wakeup_at_utc")
    public long wakeUpTimeUTC;

    @JsonProperty("timezone_offset")
    @SerializedName("timezone_offset")
    public int timeZoneOffset;


    public SleepLabel(
            final long date,
            final SleepRating sleepRating,
            final long sleepTime,
            final long wakeUpTime,
            final int timeZoneOffset
    ){

        this.targetDateUTC = date;
        this.rating = sleepRating;
        this.sleepTimeUTC = sleepTime;
        this.wakeUpTimeUTC = wakeUpTime;
        this.timeZoneOffset = timeZoneOffset;

    }

}
