/*
 * OpenStreetView track viewer, using ffmpeg.
 * Plays back the video files associated with the track.
 * Combines multiple files for a seamless playback with seek and backwards playback functionality
 * Supports only the OSV specific files, h264 encoding, with ultra high definition.
 *
 * Based on :
 *
 * https://github.com/wseemann/FFmpegMediaPlayer
 *
 * FFmpegMediaPlayer: A unified interface for playing audio files and streams.
 *
 * Copyright 2016 William Seemann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.telenav.ffmpeg;

import java.io.IOException;
import java.lang.ref.WeakReference;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

@SuppressWarnings("JniMissingFunction")
public class FFMPEGTrackPlayer {

    private final static String TAG = "FFMPEGTrackPlayer";

    private static final String[] JNI_LIBRARIES = {
            "SDL2",
            "avutil",
            "swscale",
            "avfilter",
            "avcodec",
            "avformat",
            "swresample",
            "SDL2",
            "encode"
    };

    /* Do not change these values without updating their counterparts
     * in jni/mediaplayer.h!
     */
    private static final int MEDIA_NOP = 0; // interface test message

    private static final int MEDIA_PREPARED = 1;

    private static final int MEDIA_PLAYBACK_COMPLETE = 2;

    private static final int MEDIA_BUFFERING_UPDATE = 3;

    private static final int MEDIA_SEEK_COMPLETE = 4;

    private static final int MEDIA_SET_VIDEO_SIZE = 5;

    private static final int MEDIA_ON_FRAME = 6;

    private static final int MEDIA_PLAYBACK_PLAYING = 7;

    private static final int MEDIA_PLAYBACK_PAUSED = 8;

    private static final int MEDIA_ON_NEXT_FILE = 9;

    private static final int MEDIA_ON_PREVIOUS_FILE = 10;

    private static final int MEDIA_ERROR = 100;

    // Note no convenience method to create a MediaPlayer with SurfaceTexture sink.

    private static final int MEDIA_INFO = 200;

    static {
        for (String JNI_LIBRARY : JNI_LIBRARIES) {
            System.loadLibrary(JNI_LIBRARY);
        }

        native_init();
    }

    private long mNativeContext; // accessed by native methods

    private int mNativeSurfaceTexture;  // accessed by native methods

    private int mListenerContext; // accessed by native methods

    private SurfaceHolder mSurfaceHolder;

    private EventHandler mEventHandler;

    private PowerManager.WakeLock mWakeLock = null;

    private boolean mScreenOnWhilePlaying;

    private boolean mStayAwake;

    private OnPreparedListener mOnPreparedListener;

    private OnPlaybackListener mOnPlaybackListener;

    private OnCompletionListener mOnCompletionListener;

    private OnBufferingUpdateListener mOnBufferingUpdateListener;

    private OnSeekCompleteListener mOnSeekCompleteListener;

    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;

    private OnErrorListener mOnErrorListener;

    private OnInfoListener mOnInfoListener;

    /**
     * Default constructor. Consider using one of the create() methods for
     * synchronously instantiating a MediaPlayer from a Uri or resource.
     * <p>When done with the MediaPlayer, you should call  {@link #release()},
     * to free the resources. If not released, too many MediaPlayer instances may
     * result in an exception.</p>
     */
    public FFMPEGTrackPlayer() {

        mEventHandler = new EventHandler(this, Looper.getMainLooper());

        /* Native setup requires a weak reference to our object.
         * It's easier to create it here than in C++.
         */
        native_setup(new WeakReference<>(this));
    }

    private static native final void native_init();

    /**
     * Called from native code when an interesting event happens.  This method
     * just uses the EventHandler system to post the event back to the main app thread.
     * We use a weak reference to the original MediaPlayer object so that the native
     * code is safe from the object disappearing from underneath it.  (This is
     * the cookie passed to native_setup().)
     */
    private static void postEventFromNative(Object mediaplayer_ref,
                                            int what, int arg1, int arg2, Object obj) {
        FFMPEGTrackPlayer mp = (FFMPEGTrackPlayer) ((WeakReference) mediaplayer_ref).get();
        if (mp == null) {
            return;
        }

        if (mp.mEventHandler != null) {
            Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            mp.mEventHandler.sendMessage(m);
        }
    }

    /*
     * Update the MediaPlayer SurfaceTexture.
     * Call after setting a new display surface.
     */
    private native void _setVideoSurface(Surface surface);

    /**
     * Sets the {@link SurfaceHolder} to use for displaying the video
     * portion of the wseemann.media.
     * <p>
     * Either a surface holder or surface must be set if a display or video sink
     * is needed.  Not calling this method or {@link #setSurface(Surface)}
     * when playing back a video will result in only the audio track being played.
     * A null surface holder or surface will result in only the audio track being
     * played.
     * @param sh the SurfaceHolder to use for video display
     */
    public void setDisplay(SurfaceHolder sh) {
        mSurfaceHolder = sh;
        Surface surface;
        if (sh != null) {
            surface = sh.getSurface();
        } else {
            surface = null;
        }
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    /**
     * Sets the {@link Surface} to be used as the sink for the video portion of
     * the wseemann.media. This is similar to {@link #setDisplay(SurfaceHolder)}, but
     * does not support {@link #setScreenOnWhilePlaying(boolean)}.  Setting a
     * Surface will un-set any Surface or SurfaceHolder that was previously set.
     * A null surface will result in only the audio track being played.
     * <p>
     * If the Surface sends frames to a {@link SurfaceTexture}, the timestamps
     * returned from {@link SurfaceTexture#getTimestamp()} will have an
     * unspecified zero point.  These timestamps cannot be directly compared
     * between different wseemann.media sources, different instances of the same wseemann.media
     * source, or multiple runs of the same program.  The timestamp is normally
     * monotonically increasing and is unaffected by time-of-day adjustments,
     * but it is reset when the position is set.
     * @param surface The {@link Surface} to be used for the video portion of
     * the wseemann.media.
     */
    public void setSurface(Surface surface) {
        if (mScreenOnWhilePlaying && surface != null) {
            Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective for Surface");
        }
        mSurfaceHolder = null;
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    public void setDataSource(String[] path)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        _setDataSource(path);
    }

    private native void _setDataSource(
            String[] paths)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    /**
     * Starts or resumes playback. If playback had previously been paused,
     * playback will continue from where it was paused. If playback had
     * been stopped, or never started before, playback will start at the
     * beginning.
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void start() throws IllegalStateException {
        stayAwake(true);
        _start();
    }

    private native void _start() throws IllegalStateException;

    /**
     * Stops playback after playback has been stopped or paused.
     * @throws IllegalStateException if the internal player engine has not been
     *                               initialized.
     */
    public void stop() throws IllegalStateException {
        stayAwake(false);
        _stop();
    }

    private native void _stop() throws IllegalStateException;

    private native void _initSignalHandler() throws IllegalStateException;

    /**
     * Pauses playback. Call start() to resume.
     * @throws IllegalStateException if the internal player engine has not been
     *                               initialized.
     */
    public void pause() throws IllegalStateException {
        stayAwake(false);
        _pause();
    }

    private native void _pause() throws IllegalStateException;

    /**
     * Set the low-level power management behavior for this MediaPlayer.  This
     * can be used when the MediaPlayer is not playing through a SurfaceHolder
     * set with {@link #setDisplay(SurfaceHolder)} and thus can use the
     * high-level {@link #setScreenOnWhilePlaying(boolean)} feature.
     * <p>
     * <p>This function has the MediaPlayer access the low-level power manager
     * service to control the device's power usage while playing is occurring.
     * The parameter is a combination of {@link PowerManager} wake flags.
     * Use of this method requires {@link android.Manifest.permission#WAKE_LOCK}
     * permission.
     * By default, no attempt is made to keep the device awake during playback.
     * @param context the Context to use
     * @param mode the power/wake mode to set
     * @see PowerManager
     */
    public void setWakeMode(Context context, int mode) {
        boolean washeld = false;
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                washeld = true;
                mWakeLock.release();
            }
            mWakeLock = null;
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(mode | PowerManager.ON_AFTER_RELEASE, FFMPEGTrackPlayer.class.getName());
        mWakeLock.setReferenceCounted(false);
        if (washeld) {
            mWakeLock.acquire();
        }
    }

    /**
     * Control whether we should use the attached SurfaceHolder to keep the
     * screen on while video playback is occurring.  This is the preferred
     * method over {@link #setWakeMode} where possible, since it doesn't
     * require that the application have permission for low-level wake lock
     * access.
     * @param screenOn Supply true to keep the screen on, false to allow it
     * to turn off.
     */
    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (mScreenOnWhilePlaying != screenOn) {
            if (screenOn && mSurfaceHolder == null) {
                Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
            }
            mScreenOnWhilePlaying = screenOn;
            updateSurfaceScreenOn();
        }
    }

    private void stayAwake(boolean awake) {
        if (mWakeLock != null) {
            if (awake && !mWakeLock.isHeld()) {
                mWakeLock.acquire();
            } else if (!awake && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
        mStayAwake = awake;
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
        }
    }

    /**
     * Returns the width of the video.
     * @return the width of the video, or 0 if there is no video,
     * no display surface was set, or the width has not been determined
     * yet. The OnVideoSizeChangedListener can be registered via
     * {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
     * to provide a notification when the width is available.
     */
    public native int getVideoWidth();

    /**
     * Returns the height of the video.
     * @return the height of the video, or 0 if there is no video,
     * no display surface was set, or the height has not been determined
     * yet. The OnVideoSizeChangedListener can be registered via
     * {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
     * to provide a notification when the height is available.
     */
    public native int getVideoHeight();

    /**
     * Checks whether the MediaPlayer is playing.
     * @return true if currently playing, false otherwise
     */
    public native boolean isPlaying();

    /**
     * Seeks to specified time position.
     * @param frame the index of the frame
     * @throws IllegalStateException if the internal player engine has not been
     *                               initialized
     */
    public native void seekTo(int frame) throws IllegalStateException;

    /**
     * Gets the current playback position.
     * @return the current position in milliseconds
     */
    public native int getCurrentPosition();

    /**
     * Gets the duration of the file.
     * @return the duration in milliseconds
     */
    public native int getDuration();

    /**
     * set the fps delay
     * @return the duration in milliseconds
     */
    public native void setFPSDelay(boolean fast);

    /**
     * Sets the player to be looping or non-looping.
     *
     * @param looping whether to loop or not
     */
    public native void setLooping(boolean looping);

    /**
     * Checks whether the MediaPlayer is looping or non-looping.
     *
     * @return true if the MediaPlayer is currently looping, false otherwise
     */
    public native boolean isLooping();

    /**
     * Releases resources associated with this MediaPlayer object.
     * It is considered good practice to call this method when you're
     * done using the MediaPlayer. For instance, whenever the Activity
     * of an application is paused, this method should be invoked to
     * release the MediaPlayer object. In addition to unnecessary resources
     * (such as memory and instances of codecs) being hold, failure to
     * call this method immediately if a MediaPlayer object is no longer
     * needed may also lead to continuous battery consumption for mobile
     * devices, and playback failure if no multiple instances of the
     * same codec is supported on a device.
     */
    public void release() {
        stayAwake(false);
        updateSurfaceScreenOn();
        mOnPreparedListener = null;
        mOnBufferingUpdateListener = null;
        mOnCompletionListener = null;
        mOnSeekCompleteListener = null;
        mOnErrorListener = null;
        mOnInfoListener = null;
        mOnVideoSizeChangedListener = null;
        _release();
    }

    private native void _release();

    /**
     * Resets the MediaPlayer to its uninitialized state. After calling
     * this method, you will have to initialize it again by setting the
     * data source and calling prepare().
     */
    public void reset() {
        stayAwake(false);
        _reset();

        // make sure none of the listeners get called anymore
        mEventHandler.removeCallbacksAndMessages(null);
    }

    private native void _reset();

    private native final void native_setup(Object mediaplayer_this);

    private native final void native_finalize();

    @Override
    protected void finalize() throws Throwable {
        native_finalize();
        super.finalize();
    }

    /**
     * Register a callback to be invoked when the wseemann.media source is ready
     * for playback.
     * @param listener the callback that will be run
     */
    public void setOnPlaybackListener(OnPlaybackListener listener) {
        mOnPlaybackListener = listener;
    }

    /* Do not change these values without updating their counterparts
     * in include/wseemann.media/mediaplayer.h!
     */

    /**
     * Register a callback to be invoked when the wseemann.media source is ready
     * for playback.
     * @param listener the callback that will be run
     */
    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    /**
     * Register a callback to be invoked when the end of a wseemann.media source
     * has been reached during playback.
     * @param listener the callback that will be run
     */
    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    /**
     * Register a callback to be invoked when the status of a network
     * stream's buffer has changed.
     * @param listener the callback that will be run.
     */
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    /**
     * Register a callback to be invoked when a seek operation has been
     * completed.
     * @param listener the callback that will be run
     */
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    /**
     * Register a callback to be invoked when the video size is
     * known or updated.
     * @param listener the callback that will be run
     */
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    /**
     * Register a callback to be invoked when an error has happened
     * during an asynchronous operation.
     * @param listener the callback that will be run
     */
    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }


    /* Do not change these values without updating their counterparts
     * in include/wseemann.media/mediaplayer.h!
     */

    /**
     * Register a callback to be invoked when an info/warning is available.
     * @param listener the callback that will be run
     */
    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    public native void stepFrame(boolean forward);

    public native void setBackwards(boolean backwards);

    public void initSignalHandler() {
        _initSignalHandler();
    }

    public native void seeking(boolean started);

    /**
     * Interface definition for a callback to be invoked when the wseemann.media
     * source is ready for playback.
     */
    public interface OnPreparedListener {
        /**
         * Called when the wseemann.media file is ready for playback.
         * @param mp the MediaPlayer that is ready for playback
         */
        void onPrepared(FFMPEGTrackPlayer mp);
    }

    public interface OnPlaybackListener {
        void onPlay(FFMPEGTrackPlayer player);

        void onPause(FFMPEGTrackPlayer player);

        void onFrameChanged(FFMPEGTrackPlayer mp, int fileIndex, int frameIndex);

        void onNextFile(FFMPEGTrackPlayer mp, int fileIndex);

        void onPreviousFile(FFMPEGTrackPlayer mp, int fileIndex);
    }

    /**
     * Interface definition for a callback to be invoked when playback of
     * a wseemann.media source has completed.
     */
    public interface OnCompletionListener {
        /**
         * Called when the end of a wseemann.media source is reached during playback.
         * @param mp the MediaPlayer that reached the end of the file
         */
        void onCompletion(FFMPEGTrackPlayer mp);
    }

    /**
     * Interface definition of a callback to be invoked indicating buffering
     * status of a wseemann.media resource being streamed over the network.
     */
    public interface OnBufferingUpdateListener {
        /**
         * Called to update status in buffering a wseemann.media stream received through
         * progressive HTTP download. The received buffering percentage
         * indicates how much of the content has been buffered or played.
         * For example a buffering update of 80 percent when half the content
         * has already been played indicates that the next 30 percent of the
         * content to play has been buffered.
         * @param mp the MediaPlayer the update pertains to
         * @param percent the percentage (0-100) of the content
         * that has been buffered or played thus far
         */
        void onBufferingUpdate(FFMPEGTrackPlayer mp, int percent);
    }

    /**
     * Interface definition of a callback to be invoked indicating
     * the completion of a seek operation.
     */
    public interface OnSeekCompleteListener {
        /**
         * Called to indicate the completion of a seek operation.
         * @param mp the MediaPlayer that issued the seek operation
         */
        void onSeekComplete(FFMPEGTrackPlayer mp);
    }

    /**
     * Interface definition of a callback to be invoked when the
     * video size is first known or updated
     */
    public interface OnVideoSizeChangedListener {
        /**
         * Called to indicate the video size
         * @param mp the MediaPlayer associated with this callback
         * @param width the width of the video
         * @param height the height of the video
         */
        void onVideoSizeChanged(FFMPEGTrackPlayer mp, int width, int height);
    }

    /**
     * Interface definition of a callback to be invoked when there
     * has been an error during an asynchronous operation (other errors
     * will throw exceptions at method call time).
     */
    public interface OnErrorListener {
        /**
         * Called to indicate an error.
         * @param mp the MediaPlayer the error pertains to
         * @param what the type of error that has occurred:
         * @param extra an extra code, specific to the error. Typically
         * implementation dependant.
         * @return True if the method handled the error, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the OnCompletionListener to be called.
         */
        boolean onError(FFMPEGTrackPlayer mp, int what, int extra);
    }

    /**
     * Interface definition of a callback to be invoked to communicate some
     * info and/or warning about the wseemann.media or its playback.
     */
    public interface OnInfoListener {
        /**
         * Called to indicate an info or a warning.
         * @param mp the MediaPlayer the info pertains to.
         * @param what the type of info or warning.
         * @param extra an extra code, specific to the info. Typically
         * implementation dependant.
         * @return True if the method handled the info, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the info to be discarded.
         */
        boolean onInfo(FFMPEGTrackPlayer mp, int what, int extra);
    }

    private class EventHandler extends Handler {
        private FFMPEGTrackPlayer mMediaPlayer;

        EventHandler(FFMPEGTrackPlayer mp, Looper looper) {
            super(looper);
            mMediaPlayer = mp;
        }

        @Override
        public void handleMessage(Message msg) {
            if (mMediaPlayer.mNativeContext == 0) {
                Log.w(TAG, "mediaplayer went away with unhandled events");
                return;
            }
            switch (msg.what) {
                case MEDIA_PREPARED:
                    if (mOnPreparedListener != null)
                        mOnPreparedListener.onPrepared(mMediaPlayer);
                    return;

                case MEDIA_PLAYBACK_COMPLETE:
                    if (mOnCompletionListener != null)
                        mOnCompletionListener.onCompletion(mMediaPlayer);
                    stayAwake(false);
                    return;

                case MEDIA_PLAYBACK_PAUSED:
                    if (mOnPlaybackListener != null)
                        mOnPlaybackListener.onPause(mMediaPlayer);
                    stayAwake(false);
                    return;

                case MEDIA_PLAYBACK_PLAYING:
                    if (mOnPlaybackListener != null)
                        mOnPlaybackListener.onPlay(mMediaPlayer);
                    stayAwake(false);
                    return;

                case MEDIA_BUFFERING_UPDATE:
                    if (mOnBufferingUpdateListener != null)
                        mOnBufferingUpdateListener.onBufferingUpdate(mMediaPlayer, msg.arg1);
                    return;

                case MEDIA_SEEK_COMPLETE:
                    if (mOnSeekCompleteListener != null)
                        mOnSeekCompleteListener.onSeekComplete(mMediaPlayer);
                    return;

                case MEDIA_SET_VIDEO_SIZE:
                    if (mOnVideoSizeChangedListener != null)
                        mOnVideoSizeChangedListener.onVideoSizeChanged(mMediaPlayer, msg.arg1, msg.arg2);
                    return;

                case MEDIA_ON_FRAME:
                    if (mOnPlaybackListener != null)
                        mOnPlaybackListener.onFrameChanged(mMediaPlayer, msg.arg1, msg.arg2);
                    return;
                case MEDIA_ON_NEXT_FILE:
                    if (mOnPlaybackListener != null)
                        mOnPlaybackListener.onNextFile(mMediaPlayer, msg.arg1);
                    return;

                case MEDIA_ON_PREVIOUS_FILE:
                    if (mOnPlaybackListener != null)
                        mOnPlaybackListener.onPreviousFile(mMediaPlayer, msg.arg1);
                    return;

                case MEDIA_ERROR:
                    // For PV specific error values (msg.arg2) look in
                    // opencore/pvmi/pvmf/include/pvmf_return_codes.h
                    Log.e(TAG, "Error (" + msg.arg1 + "," + msg.arg2 + ")");
                    boolean error_was_handled = false;
                    if (mOnErrorListener != null) {
                        error_was_handled = mOnErrorListener.onError(mMediaPlayer, msg.arg1, msg.arg2);
                    }
                    if (mOnCompletionListener != null && !error_was_handled) {
                        mOnCompletionListener.onCompletion(mMediaPlayer);
                    }
                    stayAwake(false);
                    return;

                case MEDIA_INFO:
                    Log.i(TAG, "Info (" + msg.arg1 + "," + msg.arg2 + ")");
                    if (mOnInfoListener != null) {
                        mOnInfoListener.onInfo(mMediaPlayer, msg.arg1, msg.arg2);
                    }
                    // No real default action so far.
                    return;
                case MEDIA_NOP: // interface test message - ignore
                    break;

                default:
                    Log.e(TAG, "Unknown message type " + msg.what);
            }
        }
    }
}
