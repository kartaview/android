package com.telenav.osv.upload.settings;

import android.content.Context;

/**
 * Concrete implementation of {@link SettingsUploadBase} for manual upload.
 * <p> This will include the context which is required in order to start a foreground service and the identifiers for the sequences.
 */
public class SettingsUploadManual extends SettingsUploadBase {

    /**
     * Default constructor for the current class.
     */
    public SettingsUploadManual(Context context) {
        super(context);
    }
}
