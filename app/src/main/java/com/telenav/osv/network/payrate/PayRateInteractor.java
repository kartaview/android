package com.telenav.osv.network.payrate;

import com.telenav.osv.network.payrate.model.PayRate;
import io.reactivex.Maybe;

/**
 * Interactor interface which is used for a pay rate request.
 */
public interface PayRateInteractor {

    /**
     * @return the response of a pay rate request.
     * The request is called on a background thread, therefore the received response will be also on background thread.
     * In order to receive the response on the main thread use {@code AndroidSchedulers}.
     */
    Maybe<PayRate> getDriverPayRateDetails();
}