package com.telenav.osv.event.ui;

import com.telenav.osv.event.OSVEvent;

/**
 * Created by catalinj on 10/24/17.
 */
public class ByodDriverPayRateUpdatedEvent extends OSVEvent {

    public final String currency;

    public final float payRate;

    public ByodDriverPayRateUpdatedEvent(float payRate, String currency) {
        this.payRate = payRate;
        this.currency = currency;
    }
}
