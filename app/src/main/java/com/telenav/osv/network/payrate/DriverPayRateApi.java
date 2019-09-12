package com.telenav.osv.network.payrate;

import com.telenav.osv.network.payrate.model.PayRate;
import io.reactivex.Maybe;
import retrofit2.http.POST;

/**
 * Interface containing the request call for drive pay rates details.
 */
public interface DriverPayRateApi {

    @POST("user/byod/payrates")
    Maybe<PayRate> getDriverPayRateDetails();
}