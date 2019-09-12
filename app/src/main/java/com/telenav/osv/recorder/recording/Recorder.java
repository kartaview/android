package com.telenav.osv.recorder.recording;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;

/**
 * Interface containing all the available recording operations.
 * @author cameliao
 */
//TODO: Use this interface when the Recording in refactored.
public interface Recorder {

    /**
     * Starts the recording if the camera is open and the recording is not started yet. Notifies the registered subscribers that the recording started.
     * @return a {@code Completable} instance which will be notified when the recording session was started.
     */
    Completable startRecording();

    /**
     * Stops the recording if the recording was started previously and the camera is opened. Notifies the registered subscribers that the recording stopped.
     * @return a {@code Completable } instance which will be notified when the recording session was stopped.
     */
    Completable stopRecording();

    /**
     * @return {@code true} if the recording started, {@code false} otherwise.
     */
    boolean isRecordingStarted();

    /**
     * Takes a picture and notifies the registered subscribers when the picture was taken and saved.
     * @return a {@code Completable} instance which will be notified when a picture was successfully taken.
     */
    Completable takePicture();

    /**
     * Subscribes for the picture updates.
     * @param completableObserver the observer which will be notified when a picture was taken.
     */
    void subscribeForPictureUpdates(CompletableObserver completableObserver);
}
