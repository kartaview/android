package com.telenav.osv.item;

/**
 * Created by Kalman on 15/11/2016.
 */

public class SpeedData {

    private String errorCode = "";

    private int speed = -1;

    private long timestamp;

    public SpeedData(int speed, long timestamp) {
        this.speed = speed;
        this.timestamp = timestamp;
    }

    public SpeedData(String errorCode, long timestamp) {
        this.errorCode = errorCode;
        this.timestamp = timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * @return {@code int} representing the speed in m/s.
     */
    public int getSpeed() {
        return speed;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
