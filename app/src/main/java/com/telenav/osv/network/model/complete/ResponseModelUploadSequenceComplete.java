package com.telenav.osv.network.model.complete;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.telenav.osv.network.model.generic.ResponseModelStatus;
import com.telenav.osv.network.model.generic.ResponseNetworkBase;
import com.telenav.osv.network.model.metadata.ResponseModelMetadataOsv;

public class ResponseModelUploadSequenceComplete extends ResponseNetworkBase {

    @SerializedName("osv")
    @Expose
    public ResponseModelMetadataOsv osv;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelUploadSequenceComplete(ResponseModelStatus status, ResponseModelMetadataOsv osv) {
        super(status);
        this.osv = osv;
    }
}
