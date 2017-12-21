package com.telenav.osv.manager.shutterlogic;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.utils.Log;

/**
 * Logic containing the automatic picture taking (every 5 sec)
 * Created by Kalman on 17/05/2017.
 */
public class AutoShutterLogic extends ShutterLogic {

    private static final String TAG = "AutoShutterLogic";

    private static final long SHUTTER_DELAY = 5000;

    private HandlerThread mAutoShutterThread;

    private Handler mAutoShutterHandler;

    private Runnable mAutoShutterRunnable = new Runnable() {

        @Override
        public void run() {
            if (mAutoShutterHandler != null) {
                try {
                    Log.d(TAG, "run: mAutoShutterRunnable");
                    mShutterListener.requestTakeSnapshot(0);
                    mAutoShutterHandler.postDelayed(mAutoShutterRunnable, SHUTTER_DELAY);
                } catch (Exception e) {
                    Log.d(TAG, "mAutoShutterRunnable: " + Log.getStackTraceString(e));
                }
            }
        }
    };

    @Override
    public void onLocationChanged(Location reference, Location location) {
        //no need, as auto shutter uses time periods
        Log.d(TAG, "#onLocationChanged");
    }

    @Override
    public void onSpeedChanged(SpeedData speedData) {
        //no need, as auto shutter uses time periods
    }

    @Override
    int getPriority() {
        return PRIORITY_AUTO;
    }

    @Override
    public void start() {
        startAutoShutter();
    }

    @Override
    public void stop() {
        stopAutoShutter();
    }

    private void startAutoShutter() {
        try {
            if (mAutoShutterThread == null) {
                mAutoShutterThread = new HandlerThread("AutoShutterThread", Process.THREAD_PRIORITY_BACKGROUND);
                mAutoShutterThread.start();
                mAutoShutterHandler = new Handler(mAutoShutterThread.getLooper());
                Log.d(TAG, "startAutoShutter: running runnable");
                mAutoShutterHandler.post(mAutoShutterRunnable);
            }
        } catch (Exception e) {
            Log.d(TAG, "startAutoShutter: " + Log.getStackTraceString(e));
            if (mAutoShutterThread != null) {
                try {
                    mAutoShutterThread.quit();
                } catch (Exception ignored) {
                }
            }
            mAutoShutterThread = null;
            mAutoShutterHandler = null;
        }
    }

    private void stopAutoShutter() {
        if (mAutoShutterHandler != null) {
            Log.d(TAG, "stopAutoShutter: deleting handler");
            mAutoShutterHandler.removeCallbacksAndMessages(null);
            mAutoShutterHandler = null;
        }
        if (mAutoShutterThread != null) {
            Log.d(TAG, "stopAutoShutter: quitting thread");
            try {
                mAutoShutterThread.quit();
            } catch (Exception ignored) {
            }
            mAutoShutterThread = null;
        }
    }
}
