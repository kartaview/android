package com.telenav.osv.listener;

import android.location.Location;

/**
 * interface to take a photo after the shutter logic decides it is time
 * Created by Kalman on 17/05/2017.
 */
public interface ShutterListener {

    void requestTakeSnapshot(float distance, Location gpsPhoto);

    void logObdData(long timeStamp, int speed);
}
