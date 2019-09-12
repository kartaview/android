package com.telenav.osv.network.payrate.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents one pay rate data item returned by the OSC endpoint. The value expressed here applies to an interval of passes over the same track.
 * <p>
 * The passes interval to which this applies to is specified through the {@link PayRateItem#payRateCoverageInterval} field.
 * <p>
 * Objects of this class are immutable.
 * <p>
 * Created by catalinj on 10/18/17.
 */
public class PayRateItem {
    /**
     * The track coverage interval on which the value of this pay rate item applies.
     */
    @SerializedName("coverage")
    public final PayRateCoverageInterval payRateCoverageInterval;

    /**
     * Monetary value associated with this pay rate item, when the user provides OBD-specific meta-data. The currency i which this value is expressed is specified in the
     * {@link PayRateData} which will ultimately store this {@link PayRateItem}.
     */
    @SerializedName("obdValue")
    public final float obdPayRateValue;

    /**
     * Monetary value associated with this pay rate item, when the user does not provide OBD-specific meta-data. The currency i which this value is expressed is specified in the
     * {@link PayRateData} which will ultimately store this {@link PayRateItem}.
     */
    @SerializedName("nonObdValue")
    public final float nonObdPayRateValue;

    /**
     * Creates a new immutable {@link PayRateItem}.
     * @param payRateCoverageInterval the track coverage passes interval on which the value of this {@link PayRateItem} applies.
     * @param obdPayRateValue monetary value which applies to the coverage interval of this instance, when the user provides OBD metadata.
     * @param nonObdPayRateValue monetary value which applies to the coverage interval of this instance, when the user does not provide OBD metadata.
     */
    public PayRateItem(PayRateCoverageInterval payRateCoverageInterval, float obdPayRateValue, float nonObdPayRateValue) {
        this.payRateCoverageInterval = payRateCoverageInterval;
        this.obdPayRateValue = obdPayRateValue;
        this.nonObdPayRateValue = nonObdPayRateValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PayRateItem that = (PayRateItem) o;

        if (Float.compare(that.obdPayRateValue, obdPayRateValue) != 0) return false;
        if (Float.compare(that.nonObdPayRateValue, nonObdPayRateValue) != 0) return false;
        return payRateCoverageInterval != null ? payRateCoverageInterval.equals(that.payRateCoverageInterval) : that.payRateCoverageInterval == null;
    }

    @Override
    public int hashCode() {
        int result = payRateCoverageInterval != null ? payRateCoverageInterval.hashCode() : 0;
        result = 31 * result + (obdPayRateValue != +0.0f ? Float.floatToIntBits(obdPayRateValue) : 0);
        result = 31 * result + (nonObdPayRateValue != +0.0f ? Float.floatToIntBits(nonObdPayRateValue) : 0);
        return result;
    }
}
