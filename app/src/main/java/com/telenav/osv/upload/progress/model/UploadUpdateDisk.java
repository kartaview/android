package com.telenav.osv.upload.progress.model;

/**
 * Concrete implementation of {@code UploadUpdateBase} which is used to send all update related to disk size.
 * @see UploadUpdateBase
 */
public class UploadUpdateDisk extends UploadUpdateBase {

    /**
     * Default constructor for the current class.
     */
    public UploadUpdateDisk(long totalUnit) {
        super(totalUnit);
    }
}
