package com.telenav.osv.manager;

import java.util.ArrayList;
import org.greenrobot.eventbus.NoSubscriberEvent;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.positioner.SKPosition;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.hardware.gps.AccuracyEvent;
import com.telenav.osv.event.hardware.gps.LocationEvent;
import com.telenav.osv.event.hardware.gps.SpeedCategoryEvent;
import com.telenav.osv.event.hardware.gps.TrackChangedEvent;
import com.telenav.osv.event.hardware.obd.ObdSpeedEvent;
import com.telenav.osv.event.hardware.obd.ObdStatusEvent;
import com.telenav.osv.item.SensorData;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.manager.capture.ShutterManager;
import com.telenav.osv.manager.location.LocationManager;
import com.telenav.osv.manager.location.PositionMatcher;
import com.telenav.osv.manager.location.ScoreManager;
import com.telenav.osv.manager.location.SensorManager;
import com.telenav.osv.manager.obd.ObdManager;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;

/**
 * Component which handles recording related functionalities
 * Created by Kalman on 04/07/16.
 */
public class Recorder extends ShutterManager implements ObdManager.ConnectionListener, LocationManager.LocationEventListener {

    public static final float ACCURACY_GOOD = 15;

    public static final float ACCURACY_MEDIUM = 40;

    private static final String TAG = "Recorder";

    private static final float TIME_FRAME_FOR_PROCESSING_DISTANCE_IN_SECONDS = 1;

    private final Context mContext;

    private final LocationManager mLocationManager;

    private final ScoreManager mScoreManager;

    private float mCurrentAccuracy = 1000;

    private State mCurrentState = State.GPS_ONLY;

    private float mSpeed = 0;

    private ObdManager mOBDManager;

    private SpeedCategoryEvent.SpeedCategory mSpeedCategory = SpeedCategoryEvent.SpeedCategory.SPEED_STATIONARY;


    private boolean mTimerNotSet = false;

    private Handler mTimerHandler = new Handler();

    private Runnable mRunnable;

    private Location mPreviousLocation;

    private long mPreviousLocationTime = 0;

    private float averageSpeed = -1;

    private long referenceTime = 0;

    private Location mActualLocation;

    private PositionMatcher mMatcher;

    private Handler mHandler;

    private Thread recordThread;

    private long mLastSnapshotTime = 0;

    private ArrayList<SKCoordinate> mCurrentTrack = new ArrayList<>();

    private boolean mMapEnabled;

    public Recorder(OSVApplication app) {
        super(app);
        mContext = app;
        mHandler = new Handler(Looper.getMainLooper());
        mMatcher = new PositionMatcher(app);
        mScoreManager = new ScoreManager(appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true));
        mLocationManager = LocationManager.get(app, this);
        mLocationManager.connect();
        int obdType = appPrefs.getIntPreference(PreferenceTypes.K_OBD_TYPE);
        createObdManager(obdType);
        EventBus.register(this);
        mMapEnabled = !appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED);
    }

    public boolean isGPSAccurate() {
        if (mCurrentAccuracy <= ACCURACY_GOOD) {
            //"Good"
            return true;
        } else if (mCurrentAccuracy <= ACCURACY_MEDIUM) {
            //"Medium"
            return true;
        }
        //"Bad"
        return false;
    }

    @Override
    public void onLocationChanged(Location location) {
        mActualLocation = location;
        if (mPreviousLocation == null) {
            mPreviousLocation = location;
            mPreviousLocationTime = System.currentTimeMillis();
        }

        if (mMapEnabled) {
            if (isRecording()) {
                SKPosition position = new SKPosition(location);
                mCurrentTrack.add(position.getCoordinate());
                Log.d(TAG, "onLocationChanged: track size is " + mCurrentTrack.size());
                EventBus.postSticky(new TrackChangedEvent(mCurrentTrack));
            }
        }
        EventBus.postSticky(new LocationEvent(location));
        if (mMatcher != null && appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true)) {
            mMatcher.onLocationChanged(location);
        }
        if (mActualLocation.getAccuracy() != mCurrentAccuracy) {
            mCurrentAccuracy = mActualLocation.getAccuracy();
            EventBus.postSticky(new AccuracyEvent(mCurrentAccuracy));
        }
        if (isRecording() && mCurrentState.equals(State.GPS_ONLY)) {
            if (!location.hasSpeed()) {
                Log.d(TAG, "onLocationChanged: mActualLocation has no speed");
                double dist = mPreviousLocation.distanceTo(location);
                if (Math.abs(dist) > 0) {
                    float time = (mPreviousLocation.getTime() - location.getTime()) / 1000f;
                    location.setSpeed((float) (dist / time));
                    Log.d(TAG, "onLocationChanged: mActualLocation has no speed, calculated speed: " + dist + "m /" + time + "s");
                    recalculateSpeedCategory((float) (dist / time));
                }
            } else {
                recalculateSpeedCategory((location.getSpeed() * 18) / 5);
            }
            if (location.hasAccuracy() && location.getAccuracy() < ACCURACY_MEDIUM) {
                if (location.getAccuracy() < ACCURACY_GOOD + 5) {
                    LocationManager.ACCURATE = true;
                }
                mTimerHandler.removeCallbacksAndMessages(null);
                mTimerNotSet = true;
                Log.d(TAG, "onLocationChanged: accuracy higher than 40 m");
                double dist = ComputingDistance.distanceBetween(mPreviousLocation.getLongitude(), mPreviousLocation.getLatitude(), location.getLongitude(), location
                        .getLatitude());
//                Log.d(TAG, "onLocationChanged: distance difference: " + dist + ", focusing: " + FocusManager.mIsFocusing);
                if (((OSVApplication) mContext).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
                    //do nothing when autoshutter is on
                } else if (dist >= mSpeedCategory.getDistance()) { //&& !FocusManager.mIsFocusing) {
                    takeSnapshot(location, location.getAccuracy(), dist);
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
                            if (isRecording()) {
                                if (((OSVApplication) mContext).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
                                    //do nothing when autoshutter is on
                                } else {
                                    takeSnapshot(mPreviousLocation, 1000, 0);
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
        SpeedCategoryEvent.SpeedCategory newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_STATIONARY;
        if (mSpeed <= 1) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_STATIONARY;
        } else if (mSpeed <= 10) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_5;
        } else if (mSpeed <= 30) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_10;
        } else if (mSpeed <= 50) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_15;
        } else if (mSpeed <= 90) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_20;
        } else if (mSpeed <= 120) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_25;
        } else if (mSpeed > 120) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_35;
        }
        if (newCategory != mSpeedCategory) {
            Log.d(TAG, "recalculateSpeedCategory: speed category changed " + newCategory);
            mSpeedCategory = newCategory;
            EventBus.postSticky(new SpeedCategoryEvent(mSpeed, mSpeedCategory));
        }
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onObdStatusEvent(ObdStatusEvent event) {
        switch (event.type) {
            case ObdStatusEvent.TYPE_CONNECTED:
                mCurrentState = State.OBD_ONLY;
                mOBDManager.startRunnable();
                break;
            case ObdStatusEvent.TYPE_DISCONNECTED:
                mCurrentState = State.GPS_ONLY;
                mOBDManager.stopRunnable();
                break;
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onRecordingStarted(RecordingEvent event) {
        mMapEnabled = !appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED);
        if (event.started) {
            if (mMapEnabled) {
                mCurrentTrack.clear();
                mCurrentTrack.add(new SKPosition(mActualLocation).getCoordinate());
            }
            if (mActualLocation != null && appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true)) {
                mMatcher.onLocationChanged(mActualLocation);
            }
        } else {
            EventBus.clear(TrackChangedEvent.class);
        }
    }

    @Override
    public void onSpeedObtained(SpeedData speedData) {
        try {
            if (speedData.getSpeed() != -1) {
                SensorManager.logSensorData(new SensorData(speedData.getSpeed(), speedData.getTimestamp()));
                mSpeed = speedData.getSpeed();
                recalculateSpeedCategory(mSpeed);
                checkDistance(speedData);
                EventBus.post(new ObdSpeedEvent(speedData));
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "onSpeedObtained: " + Log.getStackTraceString(e));
        }
    }

    private void checkDistance(SpeedData speedData) {
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
                if (isRecording()) {
                    Log.d(TAG, "timeFrame  " + timeFrame + " Distance Covered between photos " + distanceCovered);
                    double gpsDistance = ComputingDistance.distanceBetween(mActualLocation.getLongitude(), mActualLocation.getLatitude(),
                            mPreviousLocation.getLongitude(), mPreviousLocation.getLatitude());
                    takeSnapshot(mActualLocation, mActualLocation.getAccuracy(), gpsDistance);
                    Log.d(TAG, "Take snapshot:  " + mActualLocation + " acuracy " + mActualLocation.getAccuracy()
                            + " distance from obd " + distanceCovered + " distance between locations "
                            + gpsDistance);
                    mPreviousLocation = mActualLocation;
                    mPreviousLocationTime = System.currentTimeMillis();
                    averageSpeed = -1;
                    referenceTime = 0;
                }
            }
        }
    }

    public void stopRecording() {
        if (recordThread != null && recordThread.isAlive()) {
            recordThread.interrupt();
        }
        stopSequence();
    }

    public void startRecording() {
        startSequence();
    }

    void startAutoShutter() {
        if (((OSVApplication) mContext).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
            recordThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mPreviousLocation = mActualLocation;
                    while (!Thread.interrupted() && isRecording()) {
                        if (isCameraIdle()) {
                            if (mLocationManager.hasPosition()) {
                                double lat = mActualLocation.getLatitude();
                                double lon = mActualLocation.getLongitude();
                                float accuracy = mLocationManager.getAccuracy();
                                double dist = ComputingDistance.distanceBetween(mPreviousLocation.getLongitude(), mPreviousLocation.getLatitude(), lon, lat);
                                Log.d(TAG, "recordThread: debug save photo");
                                mLastSnapshotTime = System.currentTimeMillis();
                                takeSnapshot(mActualLocation, accuracy, dist);
                                mPreviousLocation = mActualLocation;
                            }
                            try {
                                Thread.sleep(Math.min(100, Math.max(0, 100 - System.currentTimeMillis() - mLastSnapshotTime)));
                            } catch (InterruptedException e) {
                                Log.d(TAG, "startSequence: interrupted debug thread");
                                break;
                            }
                        } else {
                            try {
                                Thread.sleep(11);
                            } catch (InterruptedException e) {
                                Log.d(TAG, "startSequence: interrupted debug thread");
                                break;
                            }
                        }
                    }
                }
            });
            recordThread.start();
        }
    }

    public Sequence getRecordingSequence() {
        return getSequence();
    }

    public double getRecordingDistance() {
        return getAproximateDistance();
    }

    public int getRecordingNumberOfPictures() {
        return getNumberOfPictures();
    }

    public void openCamera() {
        open();
    }

    public void closeCamera() {
        releaseCamera();
    }


    public ObdManager getOBDManager() {
        return mOBDManager;
    }

    public void createObdManager(int type) {
        if (mOBDManager != null) {
            EventBus.unregister(mOBDManager);
            mOBDManager.removeConnectionListener();
        }
        switch (type) {
            case PreferenceTypes.V_OBD_BLE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mOBDManager = ObdManager.get(mContext, ObdManager.TYPE_BLE);
                } else {
                    appPrefs.saveIntPreference(PreferenceTypes.K_OBD_TYPE, PreferenceTypes.V_OBD_BT);
                    mOBDManager = ObdManager.get(mContext, ObdManager.TYPE_BT);
                }
                break;
            case PreferenceTypes.V_OBD_BT:
                mOBDManager = ObdManager.get(mContext, ObdManager.TYPE_BT);
                break;
            case PreferenceTypes.V_OBD_WIFI:
            default:
                mOBDManager = ObdManager.get(mContext, ObdManager.TYPE_WIFI);
                break;
        }
        EventBus.register(mOBDManager);
        mOBDManager.setConnectionListener(this);
    }


    public void connectLocation() {
        mLocationManager.connect();
    }

    public void disconnectLocation() {
        mLocationManager.disconnect();
    }

    public void takePhoto() {
        if (mLocationManager != null && hasPosition())
            try {
                double dist = ComputingDistance.distanceBetween(mPreviousLocation.getLongitude(), mPreviousLocation.getLatitude(), mActualLocation.getLongitude(),
                        mActualLocation.getLatitude());
                mLastSnapshotTime = System.currentTimeMillis();
                takeSnapshot(mActualLocation, mActualLocation.getAccuracy(), dist);
            } catch (Exception e) {
                Log.d(TAG, "takePhoto: " + Log.getStackTraceString(e));
            }
    }

    public boolean hasPosition() {
        return mPreviousLocation != null && mActualLocation != null;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void noSubscriber(NoSubscriberEvent event) {

    }

    private enum State {
        GPS_ONLY, OBD_ONLY
    }
}
