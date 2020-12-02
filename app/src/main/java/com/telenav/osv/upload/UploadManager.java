package com.telenav.osv.upload;

import com.telenav.osv.upload.settings.SettingsUploadBase;
import com.telenav.osv.upload.status.UploadStatus;

/**
 * @author horatiuf
 */

public interface UploadManager {

    /**
     * Setup the uploader based on the settings provided. Currently there can be two types:
     * <ul>
     * <li>manual - {@link ServiceUploadImpl}</li>
     * <li>automatic - TBD</li>
     * </ul>
     * @param settings the settings which will set a specific type of upload.
     */
    void setup(SettingsUploadBase settings);

    /**
     * Signals the create view action of the fragment.
     */
    void onViewCreated();

    /**
     * Signals the destroy view action of the fragment.
     */
    void onViewDestroy();

    /**
     * Starts the upload start.
     */
    void start();

    /**
     * Pauses the upload service.
     */
    void pause();

    /**
     * Stops the upload service.
     */
    void stop();

    /**
     * @return {@code true} if the upload is in progress, {@code false} otherwise.
     */
    boolean isInProgress();

    /**
     * Add a upload status to be notified of upload changes.
     * @param uploadStatus interface which provides upload callbacks.
     */
    void addUploadStatusListener(UploadStatus uploadStatus);

    /**
     * Removes a upload status.
     */
    void removeUploadStatusListener();
}
