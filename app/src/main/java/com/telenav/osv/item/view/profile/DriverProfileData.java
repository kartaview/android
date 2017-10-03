package com.telenav.osv.item.view.profile;

/**
 * Data presented on the DriverProfile screen extended appbar
 * Created by kalmanb on 8/30/17.
 */
public class DriverProfileData extends ProfileData {

    private double currentAccepted;

    private double rate;

    private double value;

    private double paymentValue;

    private String currency;

    public double getCurrentAccepted() {
        return currentAccepted;
    }

    public void setCurrentAccepted(double currentAccepted) {
        this.currentAccepted = currentAccepted;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getPaymentValue() {
        return paymentValue;
    }

    public void setPaymentValue(double paymentValue) {
        this.paymentValue = paymentValue;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
