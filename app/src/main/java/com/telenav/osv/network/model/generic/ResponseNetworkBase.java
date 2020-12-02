package com.telenav.osv.network.model.generic;

import com.google.gson.annotations.SerializedName;

/**
 * @author horatiuf
 */
public class ResponseNetworkBase {

    @SerializedName("status")
    private ResponseModelStatus status;

    public ResponseNetworkBase(ResponseModelStatus status) {
        this.status = status;
    }

    public ResponseModelStatus getStatus() {
        return status;
    }
}
