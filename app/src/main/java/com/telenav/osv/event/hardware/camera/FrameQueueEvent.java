package com.telenav.osv.event.hardware.camera;

import com.telenav.osv.event.OSVEvent;

/**
 * event setn when a frame gets added to encoding queue
 * Created by Kalman on 19/05/2017.
 */
public class FrameQueueEvent extends OSVEvent {

    public final int queueSize;

    public FrameQueueEvent(int size) {
        this.queueSize = size;
    }
}
