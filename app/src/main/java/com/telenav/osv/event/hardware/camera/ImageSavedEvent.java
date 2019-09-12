package com.telenav.osv.event.hardware.camera;

import android.location.Location;
import com.telenav.osv.event.OSVEvent;

/**
 * Event fired when a frame has been written to storage
 * @author unknown
 */
public class ImageSavedEvent extends OSVEvent {

    private double distance;

    private long diskSize;

    private int frameCount;

    private Location imageLocation;

    public ImageSavedEvent(double distance, long diskSize, int frameCount, Location imageLocation) {
        this.distance = distance;
        this.diskSize = diskSize;
        this.frameCount = frameCount;
        this.imageLocation = imageLocation;
    }

    public double getDistance() {
        return distance;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public Location getImageLocation() {
        return imageLocation;
    }
}
