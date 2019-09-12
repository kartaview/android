package com.telenav.osv.recorder.score.event;

import com.telenav.osv.event.OSVEvent;

/**
 * Model class representing the event received when the pay rate for a BYOD driver is updated.
 * Created by catalinj on 10/24/17.
 */
public class ByodDriverPayRateUpdatedEvent extends OSVEvent {

    /**
     * The currency of the pay rate(e.g USD).
     */
    private String currency;

    /**
     * The actual value of the pay rate calculated as currency/km.
     */
    private float payRateValue;

    public ByodDriverPayRateUpdatedEvent(float payRate, String currency) {
        this.payRateValue = payRate;
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public float getPayRateValue() {
        return payRateValue;
    }
}
