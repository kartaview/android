package com.telenav.osv.network.payrate.model;

import com.google.gson.annotations.SerializedName;
import com.telenav.osv.item.network.ApiResponse;

/**
 * Model class for a pay rate request.
 */
public class PayRate {

    /**
     * The status of the pay rate request.
     */
    private ApiResponse status;

    /**
     * The actual data of the pay rate request.
     */
    @SerializedName("osv")
    private PayRateData payRateData;

    public PayRate(ApiResponse status, PayRateData payRateData) {
        this.status = status;
        this.payRateData = payRateData;
    }

    public ApiResponse getStatus() {
        return status;
    }

    public PayRateData getPayRateData() {
        return payRateData;
    }
}