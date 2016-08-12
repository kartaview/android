package com.telenav.osv.listener;

/**
 * Created by Kalman on 1/21/16.
 */
public interface SpeedChangedListener {
    void onSpeedChanged(float mSpeed, SpeedCategory category);

    enum SpeedCategory {
        SPEED_STATIONARY(10000), SPEED_5(5), SPEED_10(10), SPEED_15(15), SPEED_20(20), SPEED_25(25), SPEED_35(35);

        private final double distance;

        SpeedCategory(final double newValue) {
            distance = newValue;
        }

        public double getDistance() {
            return distance;
        }

    }
}
