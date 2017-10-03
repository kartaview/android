package com.telenav.osv.manager.playback.framesprovider;

import android.os.Handler;
import android.os.Looper;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.manager.playback.framesprovider.data.TrackInfo;

/**
 * Abstract provider of frames used by a {@link PlaybackManager} implementation.
 * <p>
 * Loads frames through {@link #fetchFrames(int)}, from the data source being implementation specific.
 * <p>
 * Once the data is available, subclasses should notify its data listener through either {@link #notifyListenerOfFailure()} or {@link
 * #notifyListenerOfLoadComplete(TrackInfo)}. The listener will always be notified on the main thread.
 * <p>
 * To set a listener, call {@link #setOnFramesLoadListener(OnFramesLoadListener)}.
 * <p>
 * Created by catalinj on 9/28/17.
 */
public abstract class AbstractFramesProvider {

    /**
     * Handler used to notify the listeners on the main thread
     */
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * listener object interested in being notified when frames either have finished loading, or when their loading has failed.
     */
    private OnFramesLoadListener listener;

    /**
     * Call this to load the sequence with the specified sequence id. the data source of the frames fetched here is implementation specific.
     * <p>
     * Following a call to this method, a call to either {@link #notifyListenerOfFailure()} or {@link
     * #notifyListenerOfLoadComplete(TrackInfo)}
     * will always occur.
     * <p>
     * The listener will always be notified on the main application thread, even if this is called from a background thread.
     * @param sequenceId the Id of the sequence that needs to be loaded.
     */
    public abstract void fetchFrames(int sequenceId);

    /**
     * Registers a {@link OnFramesLoadListener} to receive updates regarding the load status of the frames requested through {@link
     * #fetchFrames(int)}.
     * @param onFramesLoadListener the listener.
     */
    public final void setOnFramesLoadListener(OnFramesLoadListener onFramesLoadListener) {
        this.listener = onFramesLoadListener;
    }

    /**
     * Call this to notify the listener on the main thread that loading frames has failed.
     */
    protected final void notifyListenerOfFailure() {
        mainThreadHandler.post(() -> {
            if (listener != null) {
                listener.onFramesLoadFailed();
            }
        });
    }

    /**
     * Call this to notify the listener on the main thread that frames have been successfully loaded.
     * @param trackInfo a {@link TrackInfo} which contains the loaded data.
     */
    protected final void notifyListenerOfLoadComplete(final TrackInfo trackInfo) {
        mainThreadHandler.post(() -> {
            if (listener != null) {
                listener.onFramesLoaded(trackInfo);
            }
        });
    }

    /**
     * Listener to be notified when frames loading finishes successfully or with an error. These callbacks will always be called on the main
     * thread.
     */
    public interface OnFramesLoadListener {

        /**
         * Called when frames have been loaded successfully. Always called from the main thread.
         * @param trackInfo the data holder for the frames data.
         */
        void onFramesLoaded(TrackInfo trackInfo);

        /**
         * Called when frames loading has failed. Always called from the main thread.
         */
        void onFramesLoadFailed();
    }
}
