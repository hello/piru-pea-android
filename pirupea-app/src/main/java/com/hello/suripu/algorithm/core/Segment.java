package com.hello.suripu.algorithm.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by pangwu on 6/10/14.
 */
public class Segment {
    private long startTimestamp = 0;
    private long endTimestamp = 0;
    private int offsetMillis = 0;

    @JsonProperty("start_timestamp")
    public long getStartTimestamp() {
        return startTimestamp;
    }

    @JsonProperty("start_timestamp")
    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    @JsonProperty("end_timestamp")
    public long getEndTimestamp() {
        return endTimestamp;
    }

    @JsonProperty("end_timestamp")
    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    @JsonProperty("offset_millis")
    public int getOffsetMillis() {
        return offsetMillis;
    }

    @JsonProperty("offset_millis")
    public void setOffsetMillis(int offsetMillis) {
        this.offsetMillis = offsetMillis;
    }

    @JsonIgnore
    public long getDuration(){
        return getEndTimestamp() - getStartTimestamp();
    }

}
