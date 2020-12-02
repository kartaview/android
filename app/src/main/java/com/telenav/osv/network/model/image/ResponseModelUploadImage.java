package com.telenav.osv.network.model.image;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.telenav.osv.network.model.generic.ResponseModelStatus;
import com.telenav.osv.network.model.generic.ResponseNetworkBase;
import com.telenav.osv.network.model.video.ResponseModelVideoOsv;

public class ResponseModelUploadImage extends ResponseNetworkBase {

    @SerializedName("osv")
    @Expose
    public ResponseModelVideoOsv osv;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelUploadImage(ResponseModelStatus status, ResponseModelVideoOsv osv) {
        super(status);
        this.osv = osv;
    }

}
