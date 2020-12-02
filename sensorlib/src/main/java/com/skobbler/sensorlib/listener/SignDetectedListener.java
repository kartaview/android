package com.skobbler.sensorlib.listener;

import com.skobbler.sensorlib.sign.SignType;

/**
 * The listener class which receives the callback when a sign is detected, having the sign type as a parameter
 * Created by Kalman on 7/15/2015.
 */
public interface SignDetectedListener {
    void onSignDetected(SignType.enSignType type);
}
