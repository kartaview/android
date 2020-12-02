package com.telenav.osv.upload.status;

import com.telenav.osv.common.service.KVBaseService;

/**
 * The interface for upload service which extends the {@link KVBaseService} interface. This will contain the {@link #setListener(UploadStatus)} method which is required for
 * controlling the service without exposing all public implementation behind in.
 * <p> The
 *
 * @see KVBaseService
 */
public interface ServiceUpload extends KVBaseService {
    void setListener(UploadStatus uploadStatus);
}
