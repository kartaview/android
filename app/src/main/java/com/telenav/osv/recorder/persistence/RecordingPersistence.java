package com.telenav.osv.recorder.persistence;

import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.utils.Size;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

/**
 * Interface that holds all the functionality of the local recording persistence.
 * Created by cameliao on 2/6/18.
 */

public interface RecordingPersistence {

    /**
     * Start method which handles all the setup for recording persistence.
     * This method should be called when a new sequence of frame is provided to the persistence.
     * The method runs by default on {@link Schedulers#single()} which is the same thread for all the operations,
     * in order to prevent concurrency issues. Using this thread the sequence is synchronized across all the streams.
     * @param sequence the new sequence which will hold all the frame.
     * @param formatSize the size of teh videos.
     * @param imageFormat the frame format received for encoding.
     * @return {@code Completable} which notifies its the observers when the initialization was finished.
     */
    Completable start(LocalSequence sequence, Size formatSize, int imageFormat);

    /**
     * Saves a frame in a sequence to the local storage.
     * The method runs by default on {@link Schedulers#single()} which is the same thread for all the operations,
     * in order to prevent concurrency issues. Using this thread the sequence is synchronized across all the streams.
     * @return {@code Completable} object to notify when the frame was saved.
     */
    Completable save(RecordingFrame frame);

    /**
     * Stop method which is responsible to release all the resources.
     * This method should be called when a sequence recording is finished.
     * The method runs by default on {@link Schedulers#single()} which is the same thread for all the operations,
     * in order to prevent concurrency issues. Using this thread the sequence is synchronized across all the streams.
     */
    Completable stop();
}
