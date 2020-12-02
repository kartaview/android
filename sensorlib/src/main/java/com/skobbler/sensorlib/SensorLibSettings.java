package com.skobbler.sensorlib;

/**
 * Created by Kalman on 7/15/2015.
 */
public class SensorLibSettings {

    public int maxLostTrackFrames = 20;

    public int minFramesForDetection = 20;

    public SensorLibSettings() {}

    public SensorLibSettings(int minFramesForDetection, int maxLostTrackFrames) {
        this.minFramesForDetection = minFramesForDetection;
        this.maxLostTrackFrames = maxLostTrackFrames;
    }

    public void setMaxLostTrackFrames(int maxLostTrackFrames) {
        this.maxLostTrackFrames = maxLostTrackFrames;
    }

    public void setMinFramesForDetection(int minFramesForDetection) {
        this.minFramesForDetection = minFramesForDetection;
    }
}
