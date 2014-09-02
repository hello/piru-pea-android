package com.hello.suripu.core.db.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class PillRegistration {

    @SerializedName("pill_id")
    public final String pillId;

    @JsonCreator
    public PillRegistration(@JsonProperty("pill_id") final String pillId) {
        this.pillId = pillId;
    }
}
