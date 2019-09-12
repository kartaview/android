package com.telenav.osv.item;

import java.util.Date;

/**
 * Holder class for payment item details
 * Created by kalmanb on 7/12/17.
 */
public class Payment {

    private final int mId;

    private final Date mDate;

    private final double mDistance;

    private final double mValue;

    private final String mCurrency;

    public Payment(int id, Date date, double distance, double value, String currency) {
        this.mId = id;
        this.mDate = date;
        this.mDistance = distance;
        this.mValue = value;
        this.mCurrency = currency;
    }

    public int getId() {
        return mId;
    }

    public Date getDate() {
        return mDate;
    }

    public double getDistance() {
        return mDistance;
    }

    public double getValue() {
        return mValue;
    }

    public String getCurrency() {
        return mCurrency;
    }
}
