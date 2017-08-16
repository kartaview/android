package com.telenav.osv.item;

import android.location.Location;
import android.os.Build;
import android.os.SystemClock;
import com.telenav.osv.utils.Log;

/**
 * regex for filtering bad lines
 * ^(?!([0-9]*\.?[0-9]*;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*
 * ([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)
 * ?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\
 * .?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*
 * ([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;[-+]?[0-9]*\.?[0-9]*([eE][-+]?[0-9]+)?;))
 * <p>
 * <p>
 * Created by Kalman on 2/11/16.
 */


public class SensorData {
    public static final int ACCELEROMETER = 0;

    public static final int ROTATION = 1;

    public static final int COMPASS = 2;

    public static final int GRAVITY = 3;

    private static final float ACCELERATION_TO_MPSS = 9.80665f;

    private static final String LINE_SEPARATOR = "\n";

    private static final String TAG = "SensorData";

    private static final long Y2015 = 1_450_000_000L;

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
            mTimeStamp = System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1_000_000L;
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
        boolean unixTs = (timeStamp / 1_000_000L) > Y2015;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !unixTs) {
            mTimeStamp = System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() - timeStamp) / 1_000_000L;
        } else if (unixTs) {
            mTimeStamp = timeStamp / 1_000_000L;
        } else {
            mTimeStamp = System.currentTimeMillis();
        }
    }

    public SensorData(float pressure, long timeStamp) {
        mPressure = new float[1];
        mPressure[0] = pressure;
        boolean unixTs = (timeStamp / 1_000_000L) > Y2015;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !unixTs) {
            mTimeStamp = System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() - timeStamp) / 1_000_000L;
        } else if (unixTs) {
            mTimeStamp = timeStamp / 1_000_000L;
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
        long seconds = mTimeStamp / 1_000L;
        long partial = mTimeStamp - (seconds * 1_000L);
//        Log.d(TAG, "toString: full   =" + mTimeStampNano);
//        Log.d(TAG, "toString: seconds=" + seconds + "." + partial);
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
            builder.append(-mRotation[1]);//pitch needs to be minus pitch
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
            builder.append(mPressure[0] / 10);
        }
        builder.append(";");
        if (mCompass != null) {
            builder.append(mCompass[0]);
        }
        builder.append(";");

        if (mVideoIndex != null) {
            builder.append(mVideoIndex[0]);
        }
        builder.append(";");
        if (mIndex != null) {
            builder.append(mIndex[0]);
        }
        builder.append(";");
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
        builder.append(";");
        //todo builder.append(vertical_accuracy);
        builder.append(LINE_SEPARATOR);
        String str = builder.toString();
        if (mIndex != null && mVideoIndex != null) {
            Log.d(TAG, "toString: created for video file = " + mVideoIndex[0] + " and frame = " + mIndex[0]);
        }
        return str;
    }
}