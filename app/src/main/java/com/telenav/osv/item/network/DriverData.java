package com.telenav.osv.item.network;

/**
 * Data holder class for driver profile info
 * Created by kalmanb on 7/6/17.
 */
public class DriverData extends ApiResponse {

    /**
     * the value displayed in the scrollable track list header
     */
    private double totalAcceptedDistance;

    /**
     * the value displayed in the scrollable track list header
     */
    private double totalRejectedDistance;

    /**
     * the value displayed in the scrollable track list header
     */
    private double totalObdDistance;

    /**
     * the value displayed in the scrollable track list header
     */
    private double totalPhotos;

    /**
     * the value displayed in the scrollable track list header
     */
    private double totalTracks;

    private String displayName;

    private String currency;

    /**
     * the value displayed in the profile extensible header in the appbar
     */
    private double currentAcceptedDistance;

    /**
     * the value displayed in the profile extensible header in the appbar
     */
    private double currentPayRate;

    /**
     * the value displayed in the profile extensible header in the appbar
     */
    private double currentPaymentValue;

    /**
     * the total value displayed in byod profile screen in the payments history tab under the Payments tab
     */
    private double totalPaidValue;

    @Override
    public String toString() {
        return "DriverData{" + "totalAcceptedDistance=" + totalAcceptedDistance + ", totalRejectedDistance=" + totalRejectedDistance +
                ", totalObdDistance=" + totalObdDistance + ", totalPhotos=" + totalPhotos + ", totalTracks=" + totalTracks + ", displayName='" +
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
}
