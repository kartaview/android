package com.telenav.spherical.network;

/**
 * HTTP communication event listener class
 */
public interface HttpEventListener {
    /**
     * Notifies you of the device status check results
     * @param newStatus true:Update available, false;No update available
     */
    void onCheckStatus(boolean newStatus);

    /**
     * Notifies you when the file is saved
     * @param latestCapturedFileId ID of saved file
     */
    void onObjectChanged(String latestCapturedFileId);

    /**
     * Notify on completion of event
     */
    void onCompleted();

    /**
     * Notify in the event of an error
     */
    void onError(String errorMessage);
}
