package com.telenav.osv.upload.progress.model;

/**
 * Base class for all the upload updates which will be sent.
 */
public abstract class UploadUpdateBase {
    protected long totalUnit;

    /**
     * Default constructor for the current class.
     * @param totalUnit
     */
    public UploadUpdateBase(long totalUnit) {
        this.totalUnit = totalUnit;
    }

    public long getTotalUnit() {
        return totalUnit;
    }

    public void setTotalUnit(long totalUnit) {
        this.totalUnit = totalUnit;
    }
}
