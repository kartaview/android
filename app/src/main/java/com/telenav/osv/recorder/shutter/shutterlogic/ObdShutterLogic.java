package com.telenav.osv.recorder.shutter.shutterlogic;

import android.location.Location;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.item.metadata.Obd2Data;
import com.telenav.osv.listener.ShutterListener;
import com.telenav.osv.recorder.sensor.SensorManager;
import com.telenav.osv.utils.Log;

/**
 * Shutter logic based on speed from OBDII devices.
 * <p>This will transform the speed which is received in m/s into km/h in {@link #onSpeedChanged(SpeedData)} callback.
 * Also will call internally {@link #recalculateSpeedCategory(float)} in order to assign an category speed priority which it will be used for snapshot frequency issue.
 * <p>If certain criteria is passed an photo request will be issued, this will be checked by using {@link #requestSnapshotIfRequired(int, long)} method internally.
 * @author horatiuf
 */
public class ObdShutterLogic extends ShutterLogic {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ObdShutterLogic.class.getSimpleName();

    private static final double KM_PER_H_MULTIPLIER = 3.6;

    private static final int UNKNOWN_SPEED_VALUE = -1;

    private static final double SECOND_IN_MS = 1000d;

    private long referenceTime = 0;

    private double distanceBetweenUpdates = 0;

    @Override
    public void setShutterListener(ShutterListener shutterListener) {
        super.setShutterListener(shutterListener);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "#onLocationChanged");
    }

    @Override
    public void onSpeedChanged(SpeedData speedData) {
        Log.d(TAG, String.format("Current speed: %s m/s", speedData.getSpeed()));
        if (speedData.getSpeed() != UNKNOWN_SPEED_VALUE) {
            if (referenceTime == 0) {
                referenceTime = speedData.getTimestamp();
            }
            int speedInMetersPerSec = speedData.getSpeed();
            //transformation in km/h due to metadata requirements to log km/h.
            float speedInKmH = (float) (speedInMetersPerSec * KM_PER_H_MULTIPLIER);
            Log.d(TAG, String.format("Current speed: %s km/h. %s m/s",
                    speedInKmH,
                    speedData.getSpeed()));
            //calculates the speed category used for photo frequency
            recalculateSpeedCategory(speedInKmH);
            //request snapshot to be taken based on specific criteria
            requestSnapshotIfRequired(speedData.getSpeed(), speedData.getTimestamp());
            //log obd sensor
            SensorManager.logOBD2Data(new Obd2Data((int) mSpeed, speedData.getTimestamp()));
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

    /**
     * Calculates the distance for which a photo snapshot is required based on:
     * <ul>
     * <li>average speed - average between current speed and previous speed</li>
     * <li>timeframe - 1 sec window gaps between photos</li>
     * </ul>
     * This will result in an distance covered in that timeframe. In case the {@link #mSpeedCategory} distance associated with the speed is surpassed this will issue an photo
     * request by calling
     * {@link #mShutterListener#requestSnapshotIfRequired(long)}.
     * @param speed {@code float} represeting the speed in <b>m/s</b>.
     * @param timestamp {@code long} value representing the current timestamp.
     */
    private void requestSnapshotIfRequired(int speed, long timestamp) {
        Log.d(TAG, String.format("CheckDistance. Speed: %s. Last update time: %s. Current update time: %s."
                , speed
                , referenceTime
                , timestamp));
        double lastUpdateInSeconds = (timestamp - referenceTime) / SECOND_IN_MS;
        referenceTime = timestamp;

        distanceBetweenUpdates += lastUpdateInSeconds * speed;
        Log.d(TAG, String.format("CheckDistance. Distance covered between photos: %s.", distanceBetweenUpdates));
        if (distanceBetweenUpdates >= mSpeedCategory && mShutterListener != null) {
            mShutterListener.requestTakeSnapshot((float) distanceBetweenUpdates);
            distanceBetweenUpdates = 0;
            Log.d(TAG, "CheckDistance: image taken");
        }
    }
}
