package com.telenav.osv.network.payrate.model;

import java.util.List;
import com.google.gson.annotations.SerializedName;

/**
 * Stores the pay rate information for a BYOD user.
 * <p>
 * Created by catalinj on 10/18/17.
 */
public class PayRateData {

    /**
     * Currency in which the pay rate monetary values are expressed.
     */
    private String currency;

    /**
     * List holding the pay rate data for the case when the user does not also provide OBD data.
     */
    @SerializedName("payrates")
    private List<PayRateItem> payRates;

    /**
     * Returns the currency in which the pay rates are specified.
     * @return a {@link String} representing the currency.
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Set the currency in which the pay rates are specified.
     * @param currency a {@link String} representing the currency.
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Return the pay rate values when recording is performed without an OBD device connected.
     * @return a {@link List}<{@link PayRateItem}> which holds the pay rate intervals & associated values for each interval, when recoding without an OBD device connected.
     */
    public List<PayRateItem> getPayRates() {
        return payRates;
    }

    /**
     * Set the pay rate values when recording is performed without an OBD device connected.
     * @param payRates a {@link List}<{@link PayRateItem}> which holds the pay rate intervals & associated values for each interval, when recoding without an OBD
     * connection.
     */
    public void setPayRates(List<PayRateItem> payRates) {
        this.payRates = payRates;
    }

}
