package com.telenav.osv.network.payrate;

import com.telenav.osv.common.model.base.BaseInteractorImpl;
import com.telenav.osv.network.payrate.model.PayRate;
import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;

/**
 * The implementation class of {@link PayRateInteractor}.
 * Interactor used for handling the pay rate request.
 */
public class PayRateInteractorImpl extends BaseInteractorImpl implements PayRateInteractor {

    /**
     * The definition of pay rate api call.
     */
    private DriverPayRateApi driverPayRateApi;

    public PayRateInteractorImpl(int currentServer, String token) {
        super(currentServer, token);
        driverPayRateApi = retrofit.create(DriverPayRateApi.class);
    }

    @Override
    public Maybe<PayRate> getDriverPayRateDetails() {
        return driverPayRateApi.getDriverPayRateDetails().subscribeOn(Schedulers.io());
    }
}