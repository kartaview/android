package com.telenav.osv.recorder.shutter.shutterlogic;

import java.util.concurrent.TimeUnit;
import android.location.Location;
import com.telenav.osv.item.SpeedData;
import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Takes 2 pictures per second with a constant frequency. Useful for monitoring & debugging encoding.
 * <p>
 * Enable or disable using the setting from debug menu.
 */
public class BenchmarkShutterLogic extends ShutterLogic {

    private static final int AUTO_PHOTOS_INTERVAL = 500;

    private static final TimeUnit AUTO_PHOTO_TIME_INTERVAL_UNIT = TimeUnit.MILLISECONDS;

    private static final String TAG = BenchmarkShutterLogic.class.getSimpleName();

    private Disposable autoPhotosDisposable;

    public BenchmarkShutterLogic(boolean isFunctional) {
        super();
        this.setFunctional(isFunctional);
    }

    @Override
    public void onSpeedChanged(SpeedData speedData) { /*whatever*/ }

    @Override
    int getPriority() {
        return PRIORITY_BENCHMARKING;
    }

    @Override
    public void start(@Nullable Location location) {
        startAutoPhotos();
    }

    @Override
    public void stop() {
        stopAutoPhotos();
    }

    private void stopAutoPhotos() {
        android.util.Log.w(TAG, "stopAutoPhotos");
        if (autoPhotosDisposable != null && !autoPhotosDisposable.isDisposed()) {
            android.util.Log.w(TAG, "stopAutoPhotos: stopping");
            autoPhotosDisposable.dispose();
            autoPhotosDisposable = null;
        } else {
            android.util.Log.w(TAG, "stopAutoPhotos: already stopped");
        }
    }

    private void startAutoPhotos() {
        android.util.Log.w(TAG, "startAutoPhotos");
        if (autoPhotosDisposable == null) {
            android.util.Log.w(TAG, "startAutoPhotos: starting");
            autoPhotosDisposable = Observable.interval(0, AUTO_PHOTOS_INTERVAL, AUTO_PHOTO_TIME_INTERVAL_UNIT)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(integer -> {
                        android.util.Log.w(TAG, "!!!!!!!!!!AUTO PHOTO REQUEST!!!!!!!!!!");
                        mShutterListener.requestTakeSnapshot(0, currentCachedLocation);
                    });
        } else {
            android.util.Log.w(TAG, "startAutoPhotos: already started");
        }
    }
}