package com.telenav.osv.recorder.encoder;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;

/**
 * The video encoder implementation which is responsible to encode multiple frames into a video file.
 */
public class VideoEncoderManager extends MediaCodec.Callback implements VideoEncoder {

    private static final String TAG = VideoEncoderManager.class.getSimpleName();

    /**
     * The frame rate per second for encoding the frames.
     * For a frame rate of 5/second, a video having 10 seconds will contain 50 frames.
     */
    private static final int VIDEO_FRAME_RATE = 5;

    /**
     * The frame rate from which the bit rate should change.
     */
    private static final int FRAME_MP = 5;

    /**
     * The multiplier for bitrate when is a higher resolution for frame.
     */
    private static final int BITRATE_MULTIPLIER_HIGH_RESOLUTION = 3;

    /**
     * The multiplier for bitrate when is a lower resolution for frame.
     */
    private static final int BITRATE_MULTIPLIER_LOW_RESOLUTION = 4;

    /**
     * The interval in seconds for a video key frames.
     */
    private static final int VIDEO_KEY_FRAME_INTERVAL = 5;

    /**
     * The presentation interval between the video frames in microseconds .
     */
    private static final int FRAME_PRESENTATION_INTERVAL = 200000;

    /**
     * The maximum capacity for the input buffers queue.
     * The input buffers number is depending on the device type, therefore it will be used the highest value.
     */
    private static final int INPUT_BUFFERS_QUEUE_CAPACITY = 10;

    /**
     * The value used to double the value or to divided it in half.
     */
    private static final int TWO_OPERATIONAL_VALUE = 2;

    /**
     * The queue containing all the available input buffers for encoding operation.
     */
    private final LinkedBlockingQueue<Integer> inputBuffersQueue = new LinkedBlockingQueue<>(INPUT_BUFFERS_QUEUE_CAPACITY);

    /**
     * The codec for encoding the frames in a H.264 format.
     */
    private MediaCodec mediaCodec;

    /**
     * The muxer for writing the H.264 frames into an mp4 video file.
     */
    private MediaMuxer mediaMuxer;

    /**
     * The index of the current video file to identify the file in order to write the frames to it.
     */
    private int outputVideoIndex;

    /**
     * The video path where to store the video file.
     */
    private String videoPath;

    /**
     * The presentation time for the current frame which is constantly increased with {@link #FRAME_PRESENTATION_INTERVAL}.
     */
    private long framePresentationTime;

    /**
     * The emitter to notify the observer when a frame encoding was finished.
     */
    private CompletableEmitter encodeCompleteEmitter;

    /**
     * A flag which is {@code true} when the encoding is started, {@code false} otherwise.
     */
    private boolean isMediaCodecStarted;

    /**
     * The frame width which is the same as video width.
     */
    private int width;

    /**
     * The frame height which is the same as video height.
     */
    private int height;

    public VideoEncoderManager() {
    }

    @Override
    public Completable startEncoder(String videoPath, Size videoSize, int colorFormat) {
        return Completable.create(emitter -> {
            try {
                this.width = videoSize.getWidth();
                this.height = videoSize.getHeight();
                this.videoPath = videoPath;
                MediaFormat format = createOutputFormat(videoSize, colorFormat);
                mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                mediaCodec.setCallback(VideoEncoderManager.this);
                mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                framePresentationTime = 0;
                mediaCodec.start();
                isMediaCodecStarted = true;
                Log.d(TAG, String.format("startEncoder. Status: completed. Video path: %s", VideoEncoderManager.this.videoPath));
                emitter.onComplete();
            } catch (IOException e) {
                Log.d(TAG, String.format("startEncoder. Status: error. Message: %s", e.getMessage()));
                emitter.onError(e);
            } catch (Exception e) {
                Log.d(TAG, String.format("startEncoder. Status: error. Exception message: %s", e.getMessage()));
                emitter.onError(e);
            }
        });
    }

    @Override
    public void stopEncoder() {
        Log.d(TAG, "stopEncoder");
        outputVideoIndex = 0;
        if (mediaCodec != null && isMediaCodecStarted) {
            try {
                mediaCodec.stop();
                mediaCodec.release();
            } catch (IllegalStateException e) {
                Log.d(TAG, "stopEncoder. Status:error. Message: Couldn't stop the codec.");
                emitEncodeError(e);
            }
            mediaCodec = null;
            isMediaCodecStarted = false;
        }
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
        inputBuffersQueue.clear();
    }

    //TODO: Check if the observer could be notified by default on the subscribing thread, if is not specified another observing thread.
    //Currently, the observer will be notified on the encoding thread which is different than subscribing thread.
    @Override
    public Completable encode(byte[] frameData) {
        return Completable.create(emitter -> {
            Log.d(TAG, "encode. Status: started. Message: Frame encoding started.");
            if (frameData == null) {
                return;
            }
            encodeCompleteEmitter = emitter;
            queueInputBuffer(frameData);
        });
    }

    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int inputBufferIndex) {
        Log.d(TAG, "onInputBufferAvailable: " + inputBufferIndex);
        while (!inputBuffersQueue.offer(inputBufferIndex)) {
            inputBuffersQueue.poll();
        }
    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int outputBufferIndex, @NonNull MediaCodec.BufferInfo bufferInfo) {
        dequeueInputBuffer(mediaCodec, outputBufferIndex, bufferInfo);
    }

    @Override
    public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
        Log.d(TAG, String.format("onError. Message: Error thrown from MediaCodec. Error: %s", e.getMessage()));
        emitEncodeError(e);
    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
        Log.d(TAG, "onOutputFormatChanged");
    }

    /**
     * Send the frame data to codec in order to be encoded to H.264 format.
     */
    private void queueInputBuffer(byte[] frameData) {
        int index;
        try {
            index = inputBuffersQueue.take();
        } catch (InterruptedException e) {
            Log.d(TAG, String.format("queueInputBuffer. Status: error. Exception message: %s", e.getMessage()));
            return;
        }
        ByteBuffer inputBuffer;
        try {
            inputBuffer = mediaCodec.getInputBuffer(index);
        } catch (IllegalStateException e) {
            Log.d(TAG, "queueInputBuffer. Status: error. Message: Couldn't retrieve input buffer.");
            emitEncodeError(e);
            return;
        }
        if (inputBuffer == null) {
            Log.d(TAG, "queueInputBuffer. Status: error. Message: Frame data available but input buffer is null.");
            emitEncodeError(new Throwable());
            return;
        }
        try {
            int frameDataOffset = width * height + (width * height) / TWO_OPERATIONAL_VALUE;
            int frameDataLength = width * height + (width * height) / TWO_OPERATIONAL_VALUE;
            inputBuffer.put(frameData, frameDataOffset, frameDataLength);
            mediaCodec.queueInputBuffer(index, 0, frameDataLength, framePresentationTime, 0);
            updatePresentationTime();
            Log.d(TAG, "queueInputBuffer. Status: success. Message: Frame data available and queued as input buffer.");
        } catch (BufferOverflowException | IllegalStateException e) {
            if (isMediaCodecStarted) {
                Log.d(TAG,
                        "queueInputBuffer. Status: error. Message: Frame data available but failed to queued as input buffer. Error: " + e.getClass().getSimpleName());
                emitEncodeError(e);
            }
        }
    }

    /**
     * Receives the frame data from codec which is encoded as H.264 and writes the frames into an mp4 file.
     * @param mediaCodec the codec which handles the encoding operation.
     * @param index the index of the output buffer.
     * @param bufferInfo the information related to the current frame.
     */
    private void dequeueInputBuffer(MediaCodec mediaCodec, int index, MediaCodec.BufferInfo bufferInfo) {
        ByteBuffer outputBuffer;
        try {
            outputBuffer = mediaCodec.getOutputBuffer(index);
        } catch (IllegalStateException e) {
            Log.d(TAG, "dequeueInputBuffer. Status: error. Message: Couldn't retrieve outputBuffer");
            return;
        }
        if (outputBuffer == null) {
            Log.d(TAG, "dequeueInputBuffer. Message: Output buffer is null.");
            return;
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            Log.d(TAG, "dequeueInputBuffer. Message: Release output buffer.");
            mediaCodec.releaseOutputBuffer(index, false);
            return;
        }
        if (bufferInfo.size != 0) {
            if (mediaMuxer == null && !initMediaMuxer(mediaCodec)) {
                return;
            }
            mediaMuxer.writeSampleData(outputVideoIndex, outputBuffer, bufferInfo);
            if (encodeCompleteEmitter != null && !encodeCompleteEmitter.isDisposed()) {
                encodeCompleteEmitter.onComplete();
            }
            Log.d(TAG, "dequeueInputBuffer. Message: Write input buffer(H264) to mp4 file with MediaMuxer.");
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mediaMuxer.stop();
            mediaMuxer.release();
        }
        try {
            mediaCodec.releaseOutputBuffer(index, false);
        } catch (IllegalStateException e) {
            Log.e(TAG, "dequeueInputBuffer. Status: error. Message: Failed to release output buffer. " + e.getMessage());
        }
    }

    /**
     * Initializes the media muxer which is responsible to write the frame buffer into an mp4 file.
     * @param mediaCodec the frame encoder
     * @return {@code true} if the initialization was successful, {@code false} otherwise.
     */
    private boolean initMediaMuxer(@NonNull MediaCodec mediaCodec) {
        try {
            mediaMuxer = new MediaMuxer(videoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            Log.d(TAG, "initMediaMuxer. Status: success. Message: MediaMuxer was initialized.");
        } catch (IOException e) {
            Log.d(TAG, String.format("initMediaMuxer. Status: error. Message: MediaMuxer initialization error. Error msg: %s", e.getMessage()));
            emitEncodeError(e);
            return false;
        }
        Log.d(TAG, String.format("initMediaMuxer. Message: Create video index with MediaFormat. Output video index: %s", outputVideoIndex));
        outputVideoIndex = mediaMuxer.addTrack(mediaCodec.getOutputFormat());
        mediaMuxer.start();
        return true;
    }

    /**
     * Creates the output format for the {@link #mediaCodec}.
     * @param videoSize the size of the video frames.
     * @param colorFormat the color format of the video frames to encode.
     * @return the format of the encoded video.
     */
    private MediaFormat createOutputFormat(Size videoSize, int colorFormat) {
        MediaFormat result = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoSize.getWidth(), videoSize.getHeight());
        Log.d(TAG, String.format("createOutputFormat. Video width x height: %s x %s ", videoSize.getWidth(), videoSize.getHeight()));
        Log.d(TAG, String.format("createOutputFormat. Color format: %s ", colorFormat));
        result.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        //this value for video bit rate was chosen after testing multiple values, in order to maintain a smaller video size.
        int videoBitRate;
        if (videoSize.getRoundedMegaPixels() >= FRAME_MP) {
            videoBitRate = videoSize.getWidth() * videoSize.getHeight() * BITRATE_MULTIPLIER_HIGH_RESOLUTION;
        } else {
            videoBitRate = videoSize.getWidth() * videoSize.getHeight() * BITRATE_MULTIPLIER_LOW_RESOLUTION;
        }
        result.setInteger(MediaFormat.KEY_BIT_RATE, videoBitRate);
        Log.d(TAG, String.format("createOutputFormat. Video bit rate: %s ", videoBitRate));
        Log.d(TAG, String.format("createOutputFormat. Video frame rate: %s ", VIDEO_FRAME_RATE));
        Log.d(TAG, String.format("createOutputFormat. Video I frame interval: %s ", VIDEO_KEY_FRAME_INTERVAL));
        result.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE);
        result.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_KEY_FRAME_INTERVAL);
        Log.d(TAG, "createOutputFormat. Status: success. Message: Output format available for encoder.");
        return result;
    }

    /**
     * Emits an error when the encoder is not started or used properly.
     * @param e the error to pass forward to the observer.
     */
    private void emitEncodeError(Throwable e) {
        if (encodeCompleteEmitter != null && !encodeCompleteEmitter.isDisposed()) {
            Log.d(TAG, String.format("emitEncodeError. Message: %s", e.getMessage()));
            encodeCompleteEmitter.onError(e);
        }
    }

    /**
     * Update the presentation time for the next encoded frame.
     */
    private void updatePresentationTime() {
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        framePresentationTime = framePresentationTime + FRAME_PRESENTATION_INTERVAL;
    }
}
