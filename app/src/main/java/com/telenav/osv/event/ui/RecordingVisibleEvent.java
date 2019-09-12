package com.telenav.osv.event.ui;

import com.telenav.osv.event.OSVEvent;

/**
 * Event class used for listening to recording screen visibility.
 * Created by Kalman on 13/02/2017.
 */
public class RecordingVisibleEvent extends OSVEvent {
    /**
     * A boolean flag representing if the recording screen is visible, false otherwise.
     */
    private boolean isVisible;

    /**
     * The default constructor
     * @param isVisible a boolean flag representing if the recording screen is visible or not.
     */
    public RecordingVisibleEvent(boolean isVisible) {
        this.isVisible = isVisible;
    }

    /**
     * @return true if the recording screen is visible, false otherwise.
     */
    public boolean isVisible() {
        return isVisible;
    }
}
