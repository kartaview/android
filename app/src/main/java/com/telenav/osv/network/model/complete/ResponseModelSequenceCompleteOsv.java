package com.telenav.osv.network.model.complete;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelSequenceCompleteOsv {

    @SerializedName("sequenceId")
    @Expose
    public String onlineSequenceId;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelSequenceCompleteOsv(String onlineSequenceId) {
        this.onlineSequenceId = onlineSequenceId;
    }
}
