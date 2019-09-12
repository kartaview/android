package com.telenav.osv.recorder.score;

import android.location.Location;
import com.telenav.osv.event.OSVEvent;
import com.telenav.osv.network.payrate.model.PayRateData;
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
     * Sets the user type.
     * @param isUserByod20 a flag which should be {@code true} if the current logged in user is byod having payment model 2.0.
     */
    void setUserType(boolean isUserByod20);

    /**
     * @return the pay rate for a BYOD driver.
     */
    PayRateData getPayRateData();

    /**
     * Sets the pay rate data used for calculating the payment for a byod driver
     * @param payRateData the data containing the pay rate and the currency
     */
    void setPayRateData(PayRateData payRateData);

    /**
     * Use this method to register for the score or pay rate updates.
     * @return {@code Observable} instance on which the observer should subscribe for receiving the score updates.
     * If a pay rate couldn't be calculated for the current segment the return value will be {@link ScoreManager#UNKNOWN_VALUE}.
     */
    Observable<OSVEvent> getScoreUpdates();

    void setScoreValue(long score);

    /**
     * Releases all the resources for the {@code Score} component.
     */
    void release();
}
