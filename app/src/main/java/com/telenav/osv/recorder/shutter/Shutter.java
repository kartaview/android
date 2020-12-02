package com.telenav.osv.recorder.shutter;

import android.location.Location;
import android.util.Pair;
import io.reactivex.Observable;

/**
 * Interface that holds the available methods for the Shutter logic functionality.
 * The logic priorities for taking a photo:
 * <ul>
 * <li>1. OBD logic: OBD speed is received and is valid (timeout in 5 sec if the speed is not valid)</li>
 * <li>2. GPS logic: a location is received and the accuracy is good or medium</li>
 * <li>3. Auto logic: if a location is received(could have bad accuracy too), take a photo every 5 seconds</li>
 * <li>4. Idle logic: no photos are taken</li>
 * </ul>
 * Created by cameliao on 2/5/18.
 */

public interface Shutter {

    /**
     * This method is used to notify the shutter when the recording started.
     * @param isRecording true if recording started, false otherwise.
     */
    void onRecordingStateChanged(boolean isRecording);

    /**
     * Destroys the instance of the Shutter.
     */
    void destroy();

    /**
     * This methods should be used to subscribe for receiving the request for taking a photo.
     * @return the observable which will be notify when to take the photo.
     */
    Observable<Pair<Float, Location>> getTakeImageObservable();

    /**
     * This methods should be used to get the current location
     * @return the observable which will be notify when to take the photo.
     */
    Location getCurrentLocationForManualTakeImage();

    /**
     * @return a {@code Observable} in order to subscribe for receiving location accuracy updates.
     */
    Observable<Integer> getAccuracyType();
}
