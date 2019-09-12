package com.telenav.osv.network.model.metadata;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelMetadataOsv {

    @SerializedName("sequence")
    @Expose
    public ResponseModelMetadataSequence sequence;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelMetadataOsv(ResponseModelMetadataSequence sequence) {
        this.sequence = sequence;
    }

}
