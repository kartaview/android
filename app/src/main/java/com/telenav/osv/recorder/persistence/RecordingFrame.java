package com.telenav.osv.recorder.persistence;

import android.location.Location;
import com.telenav.osv.recorder.camera.model.CameraFrame;


/**
 * Model object for the local persistence.
 * Created by cameliao on 2/6/18.
 */

public class RecordingFrame {

    /**
     * The camera frame containing the frame data received from the camera.
     */
    private CameraFrame frameData;

    /**
     * The location where the frame was taken.
     */
    private Location location;

    /**
     * The frame timestamp when was taken.
     */
    private long timestamp;

    /**
     * The distance between the last frame and the current one.
     */
    private double distance;


    public RecordingFrame(CameraFrame frameData, Location location, long timestamp, double distance) {
        this.frameData = frameData;
        this.location = location;
        this.timestamp = timestamp;
        this.distance = distance;
    }

    /**
     * @return an {@code CameraFrame} containing the frame data.
     */
    public CameraFrame getFrameData() {
        return frameData;
    }

    /**
     * @return the location where the frame was taken.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * @return the timestamp when the frame was taken.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return the distance between the previous frame and the current one.
     */
    public double getDistance() {
        return distance;
    }
}