package com.hello.suripu.core.db.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by jchen on 9/16/14.
 */
public class FirmwareUpdate {

    @SerializedName("name")
    public String name;

    @SerializedName("url")
    public String url;

    @SerializedName("created")
    public String created;


}