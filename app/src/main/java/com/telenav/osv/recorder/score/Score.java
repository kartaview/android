package com.telenav.osv.recorder.score;

import android.location.Location;
import com.telenav.osv.event.OSVEvent;
import io.reactivex.Observable;

/**
 * Interface that holds all the functionality for score.
 * Created by cameliao on 2/6/18.
 */

public interface Score {

    /**
     * This method should be called to notify the {@code Score} when a new photo was taken
     * @param location the location where the photo was taken
     */
    void onPictureTaken(Location location);

    /**
     * This method should be used to notify the {@code Score} when recording started or stopped
     * @param isRecording true if recording started, false if recording stopped
     * @param sequenceId the {@code identifier} of the current recording sequence. If the recording is stopped it will be cleared.
     */
    void onRecordingStateChanged(boolean isRecording, String sequenceId);

    /**
     * Use this method to register for the score updates.
     * @return {@code Observable} instance on which the observer should subscribe for receiving the score updates.
     */
    Observable<OSVEvent> getScoreUpdates();

    void setScoreValue(long score);

    /**
     * Releases all the resources for the {@code Score} component.
     */
    void release();
}
