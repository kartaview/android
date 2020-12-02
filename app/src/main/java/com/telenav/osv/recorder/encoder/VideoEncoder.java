package com.telenav.osv.recorder.encoder;

import com.telenav.osv.utils.Size;
import io.reactivex.Completable;

/**
 * Video encoder from a given frame data into a video file.
 */
public interface VideoEncoder {

    /**
     * Starts and initializes the encoder for creating a new video file.
     * @param videoPath the video path for saving the encoded video.
     * @param size the size of the video.
     * @param colorFormat the color format for encoding the frames in a video.
     * @return a  {@code Completable} which notifies the observer when the encoder was started successfully
     * or when an error occurred.
     */
    Completable startEncoder(String videoPath, Size size, int colorFormat);

    /**
     * Stops the encoder when the video was finished to encode.
     */
    void stopEncoder();

    /**
     * Sends the frame data which should be encoded in the video.
     * @param frameData the frame data to encode which should have the color format
     * given when the encoder was started.
     * The observer will be notified on a background thread, other than the one defined in the {@code onSubscribe} operator.
     * @return a {@code Completable} which notifies the observer when the frame was encoded successfully
     * or when an error occurred.
     */
    Completable encode(byte[] frameData);
}