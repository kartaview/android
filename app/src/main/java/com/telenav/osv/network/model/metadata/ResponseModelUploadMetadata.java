package com.telenav.osv.network.model.metadata;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.telenav.osv.network.model.generic.ResponseModelStatus;
import com.telenav.osv.network.model.generic.ResponseNetworkBase;

public class ResponseModelUploadMetadata extends ResponseNetworkBase {

    @SerializedName("osv")
    @Expose
    public ResponseModelMetadataOsv osv;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelUploadMetadata(ResponseModelStatus status, ResponseModelMetadataOsv osv) {
        super(status);
        this.osv = osv;
    }

}
