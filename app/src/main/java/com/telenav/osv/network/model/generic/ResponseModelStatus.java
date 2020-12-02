package com.telenav.osv.network.model.generic;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelStatus {

    @SerializedName("httpCode")
    @Expose
    public int httpCode;

    @SerializedName("httpMessage")
    @Expose
    public String httpMessage;

    @SerializedName("apiCode")
    @Expose
    public int code;

    @SerializedName("apiMessage")
    @Expose
    public String message;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelStatus(int httpCode, String httpMessage, int code, String message) {
        this.httpCode = httpCode;
        this.httpMessage = httpMessage;
        this.code = code;
        this.message = message;
    }
}
