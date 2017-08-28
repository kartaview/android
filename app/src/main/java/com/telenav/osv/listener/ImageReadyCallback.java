package com.telenav.osv.listener;

import android.location.Location;

/**
 * interface for image saved callback
 * Created by Kalman on 24/04/2017.
 */
public interface ImageReadyCallback {

  void onPictureTaken(final byte[] jpegData, long timestamp, int sequenceId, String folderPath, Location location);
}
