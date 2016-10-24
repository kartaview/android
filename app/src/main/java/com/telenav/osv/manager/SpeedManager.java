package com.telenav.osv.manager;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.listener.AccuracyListener;
import com.telenav.osv.listener.SpeedChangedListener;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 04/07/16.
 */

public class SpeedManager implements ObdManager.ConnectionListener, AccuracyListener {

    private static final String TAG = "SpeedManager";

    private static final float TIME_FRAME_FOR_PROCESSING_DISTANCE_IN_SECONDS = 1;

    private final Context mContext;

    private ShutterManager mShutterManager;

    private float mCurrentAccuracy = 1000;

    private State mCurrentState = State.GPS_ONLY;

    private float mSpeed = 0;

    private SpeedChangedListener.SpeedCategory mSpeedCategory = SpeedChangedListener.SpeedCategory.SPEED_STATIONARY;

    private SpeedChangedListener mSpeedChangedListener;

    private boolean mTimerNotSet = false;

    private Handler mTimerHandler = new Handler();

    private Runnable mRunnable;

    private Location mPreviousLocation;

    private long mPreviousLocationTime = 0;

    private float averageSpeed = -1;

    private long referenceTime = 0;

    private Location location;


    public SpeedManager(Context context) {
        mContext = context;
    }

    public void setShutterManager(ShutterManager manager) {
        mShutterManager = manager;
    }

    public void setSpeedChangedListener(SpeedChangedListener speedChangedListener) {
        this.mSpeedChangedListener = speedChangedListener;
    }

    @Override
    public void onAccuracyChanged(float accuracy) {
        mCurrentAccuracy = accuracy;
    }

    public boolean isGPSAccurate() {
        if (mCurrentAccuracy <= LocationManager.ACCURACY_GOOD) {
            //"Good"
            return true;
        } else if (mCurrentAccuracy <= LocationManager.ACCURACY_MEDIUM) {
            //"Medium"
            return true;
        }
        //"Bad"
        return false;
    }

    public void onLocationChanged(Location location) {
        this.location = location;
        if (mCurrentState.equals(State.OBD_ONLY)) {
            return;
        }
        if (mShutterManager != null && mShutterManager.isRecording()) {
            if (!location.hasSpeed()) {
                Log.d(TAG, "onLocationChanged: location has no speed");
                double dist = ComputingDistance.distanceBetween(mPreviousLocation.getLongitude(), mPreviousLocation.getLatitude(),
                        location.getLongitude(), location.getLatitude());
                if (Math.abs(dist) > 0) {
                    float time = (System.currentTimeMillis() - mPreviousLocationTime) / 1000f;
                    location.setSpeed((float) (dist / time));
                    Log.d(TAG, "onLocationChanged: location has no speed, calculated speed: " + dist + "m /" + time + "s");
                    recalculateSpeedCategory((float) (dist / time));
                }
            } else {
                recalculateSpeedCategory((location.getSpeed() * 18) / 5);
            }
            if (location.hasAccuracy() && location.getAccuracy() < LocationManager.ACCURACY_MEDIUM) {
                if (location.getAccuracy() < LocationManager.ACCURACY_GOOD + 5) {
                    LocationManager.ACCURATE = true;
                }
                mTimerHandler.removeCallbacksAndMessages(null);
                mTimerNotSet = true;
                Log.d(TAG, "onLocationChanged: accuracy higher than 40 m");
                double dist = ComputingDistance.distanceBetween(mPreviousLocation.getLongitude(), mPreviousLocation.getLatitude(), location.getLongitude(), location
                        .getLatitude());
                Log.d(TAG, "onLocationChanged: distance difference: " + dist + ", focusing: " + FocusManager.mIsFocusing);
                if (((OSVApplication) mContext).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
                    //do nothing when autoshutter is on
                } else if (dist >= mSpeedCategory.getDistance()) { //&& !FocusManager.mIsFocusing) {
                    mShutterManager.takeSnapshot(location, location.getAccuracy(), dist);
                    mPreviousLocation = location;
                    mPreviousLocationTime = System.currentTimeMillis();
                    Log.d(TAG, "onLocationChanged: image taken");
                }
            } else {
                if (mTimerNotSet && LocationManager.ACCURATE) {
                    mTimerNotSet = false;
                    mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (mShutterManager != null && mShutterManager.isRecording()) {
                                if (((OSVApplication) mContext).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
                                    //do nothing when autoshutter is on
                                } else {
                                    mShutterManager.takeSnapshot(mPreviousLocation, 1000, 0);
                                    Log.d(TAG, "onLocationChanged: image taken while tunnel mode");
                                }
                            }
                            mTimerNotSet = true;
                            mTimerHandler.removeCallbacksAndMessages(null);
                            mTimerHandler.postDelayed(mRunnable, 2000);
                        }
                    };
                    mTimerHandler.removeCallbacksAndMessages(null);
                    mTimerHandler.postDelayed(mRunnable, 2000);
                }
                Log.d(TAG, "onLocationChanged: accuracy too low");
            }
        } else {
            mPreviousLocation.setLatitude(location.getLatitude());
            mPreviousLocation.setLongitude(location.getLongitude());
            mPreviousLocationTime = System.currentTimeMillis();
        }
    }


    private void recalculateSpeedCategory(float speed) {
        mSpeed = speed;
//        Log.d(TAG, "recalculateSpeedCategory: speed: " + (int) mSpeed);
        SpeedChangedListener.SpeedCategory newCategory = SpeedChangedListener.SpeedCategory.SPEED_STATIONARY;
        if (mSpeed <= 1) {
            newCategory = SpeedChangedListener.SpeedCategory.SPEED_STATIONARY;
        } else if (mSpeed <= 10) {
            newCategory = SpeedChangedListener.SpeedCategory.SPEED_5;
        } else if (mSpeed <= 30) {
            newCategory = SpeedChangedListener.SpeedCategory.SPEED_10;
        } else if (mSpeed <= 50) {
            newCategory = SpeedChangedListener.SpeedCategory.SPEED_15;
        } else if (mSpeed <= 90) {
            newCategory = SpeedChangedListener.SpeedCategory.SPEED_20;
        } else if (mSpeed <= 120) {
            newCategory = SpeedChangedListener.SpeedCategory.SPEED_25;
        } else if (mSpeed > 120) {
            newCategory = SpeedChangedListener.SpeedCategory.SPEED_35;
        }
        if (newCategory != mSpeedCategory) {
            Log.d(TAG, "recalculateSpeedCategory: speed category changed " + newCategory);
            mSpeedCategory = newCategory;
            if (mSpeedChangedListener != null) {
                mSpeedChangedListener.onSpeedChanged(mSpeed, mSpeedCategory);
            }
        }
    }


    @Override
    public void onConnected() {
        mCurrentState = State.OBD_ONLY;
    }

    @Override
    public void onDisconnected() {
        mCurrentState = State.GPS_ONLY;
    }

    @Override
    public void onSpeedObtained(ObdManager.SpeedData speedData) {
        try {
            if (speedData.getSpeed() != -1) {
                mSpeed = speedData.getSpeed();
                recalculateSpeedCategory(mSpeed);
                checkDistance(speedData);
            }
        } catch (NumberFormatException e) {
            Log.d(TAG, "onSpeedObtained: " + Log.getStackTraceString(e));
        }
    }

    @Override
    public void onConnecting() {

    }

    private void checkDistance(ObdManager.SpeedData speedData) {
        float distanceCovered;
//        Log.d(TAG, " reference time: " + referenceTime);
        if (averageSpeed == -1) {
            averageSpeed = speedData.getSpeed();
            referenceTime = speedData.getTimestamp();
            return;
        } else {
            averageSpeed = (averageSpeed + speedData.getSpeed()) / 2;
        }
        float timeFrame = (speedData.getTimestamp() - referenceTime) / 1000f;
        if (timeFrame >= TIME_FRAME_FOR_PROCESSING_DISTANCE_IN_SECONDS) {
            distanceCovered = timeFrame * averageSpeed * 1000f / 3600f;
            Log.d(TAG, "averageSpeed km/h: " + averageSpeed + " averageSpeed in m/s  " + averageSpeed * 1000f / 3600f);
            Log.d(TAG, "timeFrame  " + timeFrame + " Distance Covered " + distanceCovered);
            if (distanceCovered >= mSpeedCategory.getDistance()) {
                if (mShutterManager != null && mShutterManager.isRecording()) {
                    Log.d(TAG, "timeFrame  " + timeFrame + " Distance Covered between photos " + distanceCovered);
                    double gpsDistance = ComputingDistance.distanceBetween(location.getLongitude(), location.getLatitude(),
                            getPreviousLocation().getLongitude(), getPreviousLocation().getLatitude());
                    mShutterManager.takeSnapshot(location, location.getAccuracy(), gpsDistance);
                    Log.d(TAG, "Take snapshot:  " + location + " acuracy " + location.getAccuracy()
                            + " distance from obd " + distanceCovered + " distance between locations "
                            +gpsDistance);
                    setPreviousLocation(location);
                    averageSpeed = -1;
                    referenceTime = 0;
                }
            }
        }
    }

    public Location getPreviousLocation() {
        return mPreviousLocation;
    }

    public void setPreviousLocation(Location previousLocation) {
        this.mPreviousLocation = previousLocation;
        mPreviousLocationTime = System.currentTimeMillis();
    }

    private enum State {
        GPS_ONLY, OBD_ONLY
    }
}
