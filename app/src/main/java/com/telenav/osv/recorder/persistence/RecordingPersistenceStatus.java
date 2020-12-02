package com.telenav.osv.recorder.persistence;

import androidx.annotation.StringDef;

/**
 * Interface representing the status of teh recording persistence.
 */
@StringDef
public @interface RecordingPersistenceStatus {

    /**
     * Video file couldn't be created.
     */
    String STATUS_ERROR_CREATE_VIDEO_FILE = "Failed to create video file.";

    /**
     * Frame location couldn't be saved.
     */
    String STATUS_ERROR_LOCATION_PERSISTENCE = "Failed to persist the location.";
}
