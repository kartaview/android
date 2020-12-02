package com.telenav.osv.network.model.video;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.telenav.osv.network.model.generic.ResponseModelStatus;
import com.telenav.osv.network.model.generic.ResponseNetworkBase;

public class ResponseModelUploadVideo extends ResponseNetworkBase {

    @SerializedName("osv")
    @Expose
    public ResponseModelVideoOsv osv;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelUploadVideo(ResponseModelStatus status, ResponseModelVideoOsv osv) {
        super(status);
        this.osv = osv;
    }
}
