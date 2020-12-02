package com.telenav.osv.obd.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.utils.Log;

/**
 * Component handling quality changes in the obd data
 * Created by Kalman on 17/05/2017.
 */
class ObdQualityChecker {

    private static final String TAG = "ObdQualityChecker";

    /**
     * The time waiting to receive a valid speed at first connection.
     */
    private static final int CONNECTION_TIME_OUT = 25000;

    private List<ObdManager.ObdConnectionListener> mListener = Collections.synchronizedList(new ArrayList<>());

    private Handler mTimerHandler = new Handler(Looper.getMainLooper());

    private Runnable mTimeoutRunnable;

    ObdQualityChecker() {
        mTimeoutRunnable = () -> {
            if (!mListener.isEmpty()) {
                synchronized (mListener) {
                    for (ObdManager.ObdConnectionListener listener : mListener) {
                    }
                }
            }
            Log.d(TAG, "mTimeoutRunnable: no speed received for 5 seconds");
        };
    }

    void addListener(ObdManager.ObdConnectionListener listener) {
        mListener.add(listener);
    }

    void removeListener(ObdManager.ObdConnectionListener listener) {
        mListener.remove(listener);
    }

    void onSpeedObtained(SpeedData speedData) {
        if (speedData.getSpeed() != -1) {
            mTimerHandler.removeCallbacks(mTimeoutRunnable);
            mTimerHandler.postDelayed(mTimeoutRunnable, 5000);
        }
    }

    /**
     * Starts a post delay handler with a timeout of 15 sec. If a valid speed is received in this time
     * the handler will be stopped.
     */
    void startQualityChecker() {
        mTimerHandler.removeCallbacks(mTimeoutRunnable);
        mTimerHandler.postDelayed(mTimeoutRunnable, CONNECTION_TIME_OUT);
    }

    /**
     * Stops the post delay handler for checking if the speed is valid.
     */
    void stopObdQualityChecker() {
        mTimerHandler.removeCallbacks(mTimeoutRunnable);
    }
}


