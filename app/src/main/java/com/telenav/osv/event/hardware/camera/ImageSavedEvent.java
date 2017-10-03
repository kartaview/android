package com.telenav.osv.event.hardware.camera;

import com.telenav.osv.event.OSVEvent;
import com.telenav.osv.item.LocalSequence;

/**
 * Event fired when a frame has been written to storage
 * Created by Kalman on 16/11/2016.
 */
public class ImageSavedEvent extends OSVEvent {

    public final LocalSequence sequence;

    private boolean saved;

    public ImageSavedEvent(LocalSequence sequence, boolean saved) {
        this.sequence = sequence;
        this.saved = saved;
    }
}
