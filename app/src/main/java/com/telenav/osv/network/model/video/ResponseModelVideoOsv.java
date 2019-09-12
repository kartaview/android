package com.telenav.osv.network.model.video;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelVideoOsv {

    @SerializedName("video")
    @Expose
    public ResponseModelVideo video;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelVideoOsv(ResponseModelVideo video) {
        this.video = video;
    }
}
