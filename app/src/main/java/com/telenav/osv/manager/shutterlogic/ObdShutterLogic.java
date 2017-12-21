package com.telenav.osv.manager.shutterlogic;

import android.location.Location;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.item.metadata.Obd2Data;
import com.telenav.osv.manager.location.SensorManager;
import com.telenav.osv.utils.Log;

/**
 * Shutter logic based on speed from OBDII devices
 * Created by Kalman on 17/05/2017.
 */
public class ObdShutterLogic extends ShutterLogic {

    private static final String TAG = "ObdShutterLogic";

    private static final float TIME_FRAME_FOR_PROCESSING_DISTANCE_IN_SECONDS = 1;

    private float averageSpeed = -1;

    private long referenceTime = 0;

    @Override
    public void onLocationChanged(Location reference, Location location) {
        Log.d(TAG, "#onLocationChanged");
    }

    @Override
    public void onSpeedChanged(SpeedData speedData) {
        if (speedData.getSpeed() != -1) {
            mSpeed = speedData.getSpeed();
            recalculateSpeedCategory(mSpeed);
            checkDistance(speedData);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    int getPriority() {
        return PRIORITY_OBD;
    }

    private void checkDistance(SpeedData speedData) {
        float distanceCovered;
        if (averageSpeed == -1) {
            averageSpeed = speedData.getSpeed();
            referenceTime = speedData.getTimestamp();
            return;
        } else {
            averageSpeed = (averageSpeed + speedData.getSpeed()) / 2f;
        }
        float timeFrame = (speedData.getTimestamp() - referenceTime) / 1000f;
        SensorManager.logOBD2Data(new Obd2Data(speedData.getSpeed(), speedData.getTimestamp()));
        if (timeFrame >= TIME_FRAME_FOR_PROCESSING_DISTANCE_IN_SECONDS) {
            distanceCovered = timeFrame * averageSpeed * 1000f / 3600f;
            if (distanceCovered >= mSpeedCategory.getDistance()) {
                Log.d(TAG, "checkDistance:timeFrame  " + timeFrame + " Distance Covered between photos " + distanceCovered);
                mShutterListener.requestTakeSnapshot(distanceCovered);
                averageSpeed = -1;
                referenceTime = 0;
            }
        }
    }
}
