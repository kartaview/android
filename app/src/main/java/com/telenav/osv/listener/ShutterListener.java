package com.telenav.osv.listener;

/**
 * interface to take a photo after the shutter logic decides it is time
 * Created by Kalman on 17/05/2017.
 */
public interface ShutterListener {

    void requestTakeSnapshot(float distance);
}
