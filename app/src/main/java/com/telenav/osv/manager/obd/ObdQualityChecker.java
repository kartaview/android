package com.telenav.osv.manager.obd;

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

    private ObdManager.ConnectionListener mListener;

    private Handler mTimerHandler = new Handler(Looper.getMainLooper());

    private Runnable mTimeoutRunnable;

    ObdQualityChecker() {
        mTimeoutRunnable = () -> {
            if (mListener != null) {
                mListener.onObdDataTimedOut();
            }
            Log.d(TAG, "mTimeoutRunnable: no speed received for 5 seconds");
        };
    }

    void setListener(ObdManager.ConnectionListener listener) {
        mListener = listener;
    }

    void onSpeedObtained(SpeedData speedData) {
        if (speedData.getSpeed() != -1) {
            mTimerHandler.removeCallbacks(mTimeoutRunnable);
            mTimerHandler.postDelayed(mTimeoutRunnable, 5000);
        }
    }
}


