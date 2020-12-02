package com.telenav.osv.common;

/**
 * Interface to be implemented by all base fragment to signal a back press action by the callback {@link #handleBackPressed()}.
 * @author horatiuf
 */

public interface OnBackPressed {

    /**
     * Handles the back press action from the user.
     * @return {@code true} if the back pressed was handled by the fragment, {@code false} otherwise.
     */
    boolean handleBackPressed();
}
