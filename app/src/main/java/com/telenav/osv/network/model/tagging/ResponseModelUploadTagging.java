package com.telenav.osv.network.model.tagging;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.telenav.osv.network.model.generic.ResponseModelStatus;
import com.telenav.osv.network.model.generic.ResponseNetworkBase;

public class ResponseModelUploadTagging extends ResponseNetworkBase {

    @SerializedName("result")
    @Expose
    public ResponseModelTaggingResult responseModelTaggingResult;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelUploadTagging(ResponseModelStatus status, ResponseModelTaggingResult responseModelTaggingResult) {
        super(status);
        this.responseModelTaggingResult = responseModelTaggingResult;
    }

}
