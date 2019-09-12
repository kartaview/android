package com.telenav.osv.network.model;

import com.google.gson.annotations.SerializedName;
import com.telenav.osv.network.model.generic.ResponseModelStatus;
import com.telenav.osv.network.model.generic.ResponseNetworkBase;
import androidx.annotation.NonNull;

/**
 * @author horatiuf
 */
public class CreateSequenceResponse extends ResponseNetworkBase {

    @SerializedName("id")
    private String onlineId;

    public CreateSequenceResponse(String onlineId, @NonNull ResponseModelStatus responseModelStatus) {
        super(responseModelStatus);
        this.onlineId = onlineId;
    }

    public String getOnlineId() {
        return onlineId;
    }
}
