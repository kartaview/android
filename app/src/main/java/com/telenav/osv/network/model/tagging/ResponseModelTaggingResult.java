package com.telenav.osv.network.model.tagging;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelTaggingResult {

    @SerializedName("data")
    @Expose
    public ResponseModelTaggingData responseModelTaggingData;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelTaggingResult(ResponseModelTaggingData responseModelTaggingData) {
        super();
        this.responseModelTaggingData = responseModelTaggingData;
    }

}
