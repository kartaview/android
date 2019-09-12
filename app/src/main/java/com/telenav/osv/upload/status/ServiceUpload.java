package com.telenav.osv.upload.status;

import com.telenav.osv.common.service.OscBaseService;

/**
 * The interface for upload service which extends the {@link OscBaseService} interface. This will contain the {@link #setListener(UploadStatus)} method which is required for
 * controlling the service without exposing all public implementation behind in.
 * <p> The
 * @see OscBaseService
 */
public interface ServiceUpload extends OscBaseService {
    void setListener(UploadStatus uploadStatus);
}
