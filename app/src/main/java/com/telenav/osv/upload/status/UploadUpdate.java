package com.telenav.osv.upload.status;

public class UploadUpdate {
    private double percentage;

    private long eta;

    private long bandwidth;

    private long currentUnit;

    private long totalUnit;

    public UploadUpdate(double percentage, long eta, long bandwidth, long currentUnit, long totalUnit) {
        this.percentage = percentage;
        this.eta = eta;
        this.bandwidth = bandwidth;
        this.currentUnit = currentUnit;
        this.totalUnit = totalUnit;
    }

    public long getEta() {
        return eta;
    }

    public long getBandwidth() {
        return bandwidth;
    }

    public long getCurrentUnit() {
        return currentUnit;
    }

    public long getTotalUnit() {
        return totalUnit;
    }

    public double getPercentage() {
        return percentage;
    }
}
