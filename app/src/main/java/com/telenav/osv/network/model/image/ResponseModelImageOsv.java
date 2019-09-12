package com.telenav.osv.network.model.image;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelImageOsv {

    @SerializedName("photo")
    @Expose
    public ResponseModelImage photo;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelImageOsv(ResponseModelImage photo) {
        this.photo = photo;
    }
}
