package com.telenav.osv.item;

import android.location.Location;
import android.os.Build;
import android.os.SystemClock;
import com.telenav.osv.utils.Log;
import com.telenav.osv.manager.SensorManager;

/**
 * Created by Kalman on 2/11/16.
 */


public class SensorData {

    public static final int ACCELEROMETER = 0;

    public static final int ROTATION = 1;

    public static final int COMPASS = 2;

    public static final int GRAVITY = 3;

    public static final float ACCELERATION_TO_MPSS = 9.80665f;

    private static final String TAG = "SensorData";

    private final long mTimeStamp;

    private int[] mSpeed = null;

    private int[] mIndex;

    private int[] mVideoIndex;

    private Location mLocation;

    private float[] mAccelerometer;

    private float[] mGravity;

    private float[] mRotation;

    private float[] mCompass;

    private float[] mPressure;

    public SensorData(Location location) {
        mLocation = location;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mTimeStamp = System.currentTimeMillis() - ((SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1000000);
        } else {
            mTimeStamp = System.currentTimeMillis();
        }
    }

    public SensorData(int type, float[] data, long timeStamp) {
        switch (type) {
            case ACCELEROMETER:
                mAccelerometer = data;
                break;
            case ROTATION:
                mRotation = data;
                break;
            case COMPASS:
                mCompass = data;
                break;
            case GRAVITY:
                mGravity = data;
                break;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mTimeStamp = System.currentTimeMillis() - ((SystemClock.elapsedRealtimeNanos() - timeStamp) / 1000000);
        } else {
            mTimeStamp = System.currentTimeMillis();
        }
    }

    public SensorData(float pressure, long timeStamp) {
        mPressure = new float[1];
        mPressure[0] = pressure;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mTimeStamp = System.currentTimeMillis() - ((SystemClock.elapsedRealtimeNanos() - timeStamp) / 1000000);
        } else {
            mTimeStamp = System.currentTimeMillis();
        }
    }

    public SensorData(int index, int videoIndex, long millis) {
        mIndex = new int[1];
        mVideoIndex = new int[1];
        mIndex[0] = index;
        mVideoIndex[0] = videoIndex;
        mTimeStamp = millis;
    }

    public SensorData(int speed, long millis) {
        mSpeed = new int[1];
        mSpeed[0] = speed;
        mTimeStamp = millis;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        long seconds = mTimeStamp / 1000L;
        long partial = mTimeStamp - (seconds * 1000L);

        builder.append(seconds);
        builder.append(".");
        builder.append((int) partial);
        builder.append(";");
        if (mLocation != null) {
            builder.append(mLocation.getLongitude());
            builder.append(";");
            builder.append(mLocation.getLatitude());
            builder.append(";");
            if (mLocation.hasAltitude()) {
                builder.append(mLocation.getAltitude());
            }
            builder.append(";");
            if (mLocation.hasAccuracy()) {
                builder.append(mLocation.getAccuracy());
            }
            builder.append(";");
            if (mLocation.hasSpeed()) {
                builder.append(mLocation.getSpeed());
            }
            builder.append(";");
        } else {
            builder.append(";;;;;");
        }
        if (mRotation != null) {
            builder.append(-mRotation[0]);//yaw // from metadata 1.1.1 yaw needs to be minus yaw
            builder.append(";");
            builder.append(mRotation[1]);//pitch
            builder.append(";");
            builder.append(mRotation[2]);//roll
            builder.append(";");
        } else {
            builder.append(";;;");
        }
        if (mAccelerometer != null) {
            builder.append(mAccelerometer[0] / ACCELERATION_TO_MPSS);
            builder.append(";");
            builder.append(mAccelerometer[1] / ACCELERATION_TO_MPSS);
            builder.append(";");
            builder.append(mAccelerometer[2] / ACCELERATION_TO_MPSS);
            builder.append(";");
        } else {
            builder.append(";;;");
        }
        if (mPressure != null) {
            builder.append(mPressure[0]);
            builder.append(";");
        } else {
            builder.append(";");
        }
        if (mCompass != null) {
            builder.append(mCompass[0]);
            builder.append(";");
        } else {
            builder.append(";");
        }

        if (mVideoIndex != null) {
            builder.append(mVideoIndex[0]);
            builder.append(";");
        } else {
            builder.append(";");
        }
        if (mIndex != null) {
            builder.append(mIndex[0]);
            builder.append(";");
        } else {
            builder.append(";");
        }
        if (mGravity != null) {
            builder.append(mGravity[0] / ACCELERATION_TO_MPSS);
            builder.append(";");
            builder.append(mGravity[1] / ACCELERATION_TO_MPSS);
            builder.append(";");
            builder.append(mGravity[2] / ACCELERATION_TO_MPSS);
            builder.append(";");
        } else {
            builder.append(";;;");
        }
        if (mSpeed != null) {
            builder.append(mSpeed[0]);
        }
        builder.append(SensorManager.LINE_SEPARATOR);
        String str = builder.toString();
        if (mIndex!= null && mVideoIndex != null){
            Log.d(TAG, "toString: created for fileIndex = " + mIndex[0] + " and video file = " + mVideoIndex[0]);
        }
        return str;
    }
}