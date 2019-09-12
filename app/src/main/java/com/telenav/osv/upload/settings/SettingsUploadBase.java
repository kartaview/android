package com.telenav.osv.upload.settings;

import android.content.Context;

/**
 * Base class for upload settings. This will be required for any type of upload settings to be implemented.
 */
public abstract class SettingsUploadBase {
    /**
     * Instance for the context required for various service related operations.
     */
    private Context context;

    /**
     * Default constructor for the current class.
     */
    public SettingsUploadBase(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }
}
