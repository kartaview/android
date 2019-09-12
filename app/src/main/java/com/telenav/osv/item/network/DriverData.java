package com.telenav.osv.item.network;

/**
 * Data holder class for driver profile info
 * Created by kalmanb on 7/6/17.
 */
public class DriverData extends ApiResponse {

    private double totalAcceptedDistance;

    private double totalRejectedDistance;

    private double totalObdDistance;

    private double totalPhotos;

    private double totalTracks;

    private String displayName;

    private String currency;

    private double currentAcceptedDistance;

    private double currentPayRate;

    private double currentPaymentValue;

    private double totalPaidValue;

    private String paymentModelVersion;

    @Override
    public String toString() {
        return "DriverData{" + "totalAcceptedDistance=" + totalAcceptedDistance + ", totalRejectedDistance=" + totalRejectedDistance +
                ", obdDistance=" + totalObdDistance + ", totalPhotos=" + totalPhotos + ", totalTracks=" + totalTracks + ", displayName='" +
                displayName + '\'' + ", currency='" + currency + '\'' + ", currentAcceptedDistance=" + currentAcceptedDistance +
                ", currentPayRate=" + currentPayRate + ", currentPaymentValue=" + currentPaymentValue + ", totalPaidValue=" + totalPaidValue + '}';
    }

    public double getTotalPaidValue() {
        return totalPaidValue;
    }

    public void setTotalPaidValue(double totalValue) {
        this.totalPaidValue = totalValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getCurrentPayRate() {
        return currentPayRate;
    }

    public void setCurrentPayRate(double currentPayRate) {
        this.currentPayRate = currentPayRate;
    }

    public double getCurrentPaymentValue() {
        return currentPaymentValue;
    }

    public void setCurrentPaymentValue(double currentPaymentValue) {
        this.currentPaymentValue = currentPaymentValue;
    }

    public double getTotalAcceptedDistance() {
        return totalAcceptedDistance;
    }

    public void setTotalAcceptedDistance(double totalAcceptedDistance) {
        this.totalAcceptedDistance = totalAcceptedDistance;
    }

    public double getTotalRejectedDistance() {
        return totalRejectedDistance;
    }

    public void setTotalRejectedDistance(double totalRejectedDistance) {
        this.totalRejectedDistance = totalRejectedDistance;
    }

    public double getTotalObdDistance() {
        return totalObdDistance;
    }

    public void setTotalObdDistance(double totalObdDistance) {
        this.totalObdDistance = totalObdDistance;
    }

    public double getTotalPhotos() {
        return totalPhotos;
    }

    public void setTotalPhotos(double totalPhotos) {
        this.totalPhotos = totalPhotos;
    }

    public double getTotalTracks() {
        return totalTracks;
    }

    public void setTotalTracks(double totalTracks) {
        this.totalTracks = totalTracks;
    }

    public double getCurrentAcceptedDistance() {
        return currentAcceptedDistance;
    }

    public void setCurrentAcceptedDistance(double currentAcceptedDistance) {
        this.currentAcceptedDistance = currentAcceptedDistance;
    }

    public String getPaymentModelVersion() {
        return paymentModelVersion;
    }

    public void setPaymentModelVersion(String paymentModelVersion) {
        this.paymentModelVersion = paymentModelVersion;
    }
}
