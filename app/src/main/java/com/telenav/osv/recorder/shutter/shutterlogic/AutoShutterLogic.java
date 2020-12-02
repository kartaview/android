package com.telenav.osv.recorder.shutter.shutterlogic;

import java.util.concurrent.TimeUnit;
import android.location.Location;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.utils.Log;
import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Logic containing the automatic picture taking (every 5 sec)
 * Created by Kalman on 17/05/2017.
 */
public class AutoShutterLogic extends ShutterLogic {

    private static final String TAG = "AutoShutterLogic";

    private static final long SHUTTER_DELAY = 5;

    private Disposable disposable;

    @Override
    public void onSpeedChanged(SpeedData speedData) {
        //no need, as auto shutter uses time periods
    }

    @Override
    int getPriority() {
        return PRIORITY_AUTO;
    }

    @Override
    public void start(@Nullable Location location) {
        onLocationChanged(location);
        startAutoShutter();
    }

    @Override
    public void stop() {
        stopAutoShutter();
    }

    private void startAutoShutter() {
        Log.d(TAG, "startAutoShutter.");
        Observable.interval(0, SHUTTER_DELAY, TimeUnit.SECONDS).subscribe(new Observer<Long>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposable = d;
            }

            @Override
            public void onNext(Long aLong) {
                if (mShutterListener != null && isCurrentLocationValid()) {
                    mShutterListener.requestTakeSnapshot(0, currentCachedLocation);
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "startAutoShutter. Status: error. Message: " + e.getMessage());
            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void stopAutoShutter() {
        Log.d(TAG, "stopAutoShutter.");
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            Log.d(TAG, "stopAutoShutter. Status: success. Message: Stopped the time based request for picture ");
        }
    }
}
