package com.telenav.osv.manager.capture;

import java.io.FileOutputStream;
import java.io.IOException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;
import android.hardware.Camera;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.telenav.ffmpeg.FFMPEG;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.CameraShutterEvent;
import com.telenav.osv.event.hardware.camera.ImageSavedEvent;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.hardware.obd.ObdSpeedEvent;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.SensorData;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.location.LocationManager;
import com.telenav.osv.manager.location.SensorManager;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.manager.obd.ObdManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import io.fabric.sdk.android.Fabric;

/**
 * This class is responsible for taking the pictures frmo the camera.
 * Created by Kalman on 10/7/2015.
 */
@SuppressWarnings({"deprecation", "ResultOfMethodCallIgnored"})
public abstract class ShutterManager extends CameraManager implements Camera.ShutterCallback, ObdManager.ConnectionListener {
    public final static String TAG = "ShutterManager";

    private static final int MIN_FREE_SPACE = 500;

    private final Object shutterSynObject = new Object();

    private Handler mBackgroundHandler;

    private HandlerThread mHandlerThread;

    private Sequence mSequence;

    private String mSequencePath;

    private int mIndex = 0;

    private int mImageCounter = 0;

    private OSVApplication mContext;

    private boolean recording;

    private double mAproximateDistance = 0;

    private UploadManager mUploadManager;

    private long mTimestamp;

    private SensorManager mSensorManager;

    private int mOrientation = 0;

    private float mAccuracy;

    private boolean mCameraIdle = true;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Runnable mIdleRunnable = new Runnable() {
        @Override
        public void run() {
            mCameraIdle = true;
        }
    };

    private FFMPEG ffmpeg;

    private Location mLocation;


    private boolean mSafe;

    private Camera.PictureCallback mJpegPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] jpegData, Camera camera) {
            final long mTimestampF = mTimestamp;
            restartPreviewIfNeeded();
            mHandler.removeCallbacks(mIdleRunnable);
            if (!recording) {
                mCameraIdle = true;
                return;
            }
            if (mSequence == null) {
                if (mSequencePath == null) {
                    mCameraIdle = true;
                    if (Fabric.isInitialized()) {
                        Answers.getInstance().logCustom(new CustomEvent("Recording interrupted because sequence pointer null"));
                    }
                    stopSequence();
                    startSequence();
                    return;
                } else {
                    mSequence = new Sequence(new OSVFile(mSequencePath));
                }
            }
            synchronized (shutterSynObject) {
                final int mSequenceIdF = mSequence.sequenceId;
                final boolean mSafeF = mSafe;
                final int mIndexF = mIndex;
                final Location mLocationF = mLocation;
                final float mAccuracyF = mAccuracy;
                final int mOrientationF = mOrientation;
                String path;
                if (mSequence != null && mSequence.folder != null) {
                    path = mSequence.folder.getPath();
                } else {
                    path = mSequencePath;
                }
                final String mFolderPath = path;
                mCameraIdle = true;

                saveFrame(mSafeF, jpegData, mSequenceIdF, mIndexF, mFolderPath, mLocationF, mAccuracyF, mOrientationF, mTimestampF);
                mIndex++;
            }
        }
    };

    protected ShutterManager(Application application) {
        super(application);
        mContext = (OSVApplication) application;
        mHandlerThread = new HandlerThread("ShutterManager", Process.THREAD_PRIORITY_FOREGROUND);
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());
        mUploadManager = ((OSVApplication) application).getUploadManager();
        mSensorManager = new SensorManager(application);
    }

    private void saveFrame(final boolean safe, final byte[] jpegData, final int mSequenceIdF, final int mIndexF, final String folderPathF, final Location mLocationF, final
    float mAccuracyF, final int mOrientationF, final long mTimestampF) {
        Log.d(TAG, "saveFrame: posting frame data to handler");
        if (mBackgroundHandler == null || !mHandlerThread.getLooper().getThread().isAlive() || mHandlerThread.getLooper().getThread().isInterrupted()) {
            mHandlerThread = new HandlerThread("ShutterManager", Process.THREAD_PRIORITY_FOREGROUND);
            mHandlerThread.start();
            Log.d(TAG, "saveFrame: starting handlerThread for background operation");
            mBackgroundHandler = new Handler(mHandlerThread.getLooper());
        }

        if (mUploadManager != null && mUploadManager.isUploading()) {
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    mUploadManager.cancelUploadTasks();
                }
            });
        }
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                int available = (int) Utils.getAvailableSpace(mContext);
                Log.d(TAG, "saveFrame: entered data handler");
                if (available <= MIN_FREE_SPACE) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            boolean needToRestart = false;
                            try {
                                if (Utils.checkSDCard(mContext)) {
                                    mContext.getAppPrefs().saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE
                                            , !mContext.getAppPrefs().getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE));
                                    needToRestart = true;
                                    Toast.makeText(mContext, R.string.reached_current_storage_limit_message, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(mContext, R.string.reached_storage_limit, Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "saveFrame: minimum space reached" + Log.getStackTraceString(e));
                                Toast.makeText(mContext, R.string.reached_storage_limit, Toast.LENGTH_LONG).show();
                            }
                            stopSequence();
                            if (needToRestart) {
                                startSequence();
                            }
                        }
                    });
                    return;
                }
                if (jpegData == null) {
                    Log.w(TAG, "saveFrame: jpegData is null");
                    return;
                }
                mRunDetection = false;
                final long time = System.currentTimeMillis();
                int[] ret = new int[]{0, 0};
                if (safe) {
                    String path = folderPathF + "/" + mIndexF + ".jpg";
                    String tmpPath = path + ".tmp";
                    FileOutputStream out;
                    try {
                        out = new FileOutputStream(tmpPath);
                        out.write(jpegData);
                        out.close();
                        OSVFile jpg = new OSVFile(path);
                        OSVFile tmpFile = new OSVFile(tmpPath);
                        tmpFile.renameTo(jpg);
                        Log.v(TAG, "Saved JPEG data : " + jpg.getName() + ", size: " + ((float) jpg.length()) / 1024f / 1024f + " mb");

                        try {
                            SequenceDB.instance.insertPhoto(mSequenceIdF, -1, mIndexF, jpg.getPath(), mLocationF.getLatitude(),
                                    mLocationF.getLongitude(), mAccuracyF, mOrientationF);

                            SensorManager.logSensorData(new SensorData(mIndexF, 0, mTimestampF));
                            SensorManager.flushToDisk();
                        } catch (SQLiteConstraintException e) {
                            jpg.delete();
                            Log.w(TAG, "saveFrame: " + Log.getStackTraceString(e));
                            stopSequence();
                            startSequence();

                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to write image", e);
                    }
                } else {
                    ret = ffmpeg.encode(jpegData);
                    Log.d(TAG, "saveFrame: encoding done in " + (System.currentTimeMillis() - time) + " ms ,  video file " + ret[0] + " and frame " + ret[1]);
                }
                mRunDetection = true;

                if (ret[0] < 0 || ret[1] < 0) {
                    synchronized (shutterSynObject) {
                        mIndex--;
                    }
                    EventBus.post(new ImageSavedEvent(false));
                    if (ret[0] < 0) {
                        Toast.makeText(mContext, R.string.encoding_error_message, Toast.LENGTH_SHORT).show();
                        stopSequence();
                        startSequence();
                    }
                    return;
                }
                if (!safe) {
                    int mVideoIndexF = ret[0];
                    SensorManager.logSensorData(new SensorData(mIndexF, mVideoIndexF, mTimestampF));
                    SensorManager.flushToDisk();
                    try {
                        SequenceDB.instance.insertVideoIfNotAdded(mSequenceIdF, mVideoIndexF, folderPathF + "/" + mVideoIndexF + ".mp4");
                    } catch (Exception ignored) {}

                    try {
                        SequenceDB.instance.insertPhoto(mSequenceIdF, mVideoIndexF, mIndexF, folderPathF + "/" + mVideoIndexF + ".mp4", mLocationF.getLatitude(),
                                mLocationF.getLongitude(), mAccuracyF, mOrientationF);
                    } catch (final SQLiteConstraintException e) {
                        Log.w(TAG, "saveFrame: " + Log.getStackTraceString(e));
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                if (Fabric.isInitialized()) {
                                    Answers.getInstance().logCustom(new CustomEvent("SQLiteConstraintException at insert photo"));
                                }
                                stopSequence();
                                startSequence();
                            }
                        });

                    }
                }
                mImageCounter++;
                EventBus.post(new ImageSavedEvent(true));
            }
        });
    }

    protected int getNumberOfPictures() {
        return mImageCounter;
    }

    /**
     * takes a snapshot
     * exposure value, otherwise set it to 0
     * @param location location
     * @param dist distance
     */
    public void takeSnapshot(Location location, float accuracy, double dist) {
        if (mCameraIdle) {
            mCameraIdle = false;
            mHandler.removeCallbacks(mIdleRunnable);
            mHandler.postDelayed(mIdleRunnable, 2000);
            mLocation = location;
            mAccuracy = accuracy;
            mAproximateDistance = mAproximateDistance + dist;
            if (mIndex == 0 && mSequence != null) {
                mSequence.location.setLatitude(mLocation.getLatitude());
                mSequence.location.setLongitude(mLocation.getLongitude());
                SequenceDB.instance.updateSequenceLocation(mSequence.sequenceId, mLocation.getLatitude(), mLocation.getLongitude());
            }
            mOrientation = (int) Math.toDegrees(SensorManager.mHeadingValues[0]);
            boolean taken = takeSnapshot(this, mJpegPictureCallback);
            if (taken) {
                mTimestamp = System.currentTimeMillis();
                EventBus.post(new CameraShutterEvent());
                restartPreviewIfNeeded();
            }
        }
    }

    @Override
    public void onShutter() {
//        mTimestamp = System.currentTimeMillis();
//        EventBus.post(new CameraShutterEvent());
    }

    protected void startSequence() {
        if (mUploadManager != null && mUploadManager.isUploading()) {
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    mUploadManager.cancelUploadTasks();
                }
            });
        }
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {

                mCameraIdle = true;
                recording = true;
                mSafe = appPrefs.getBooleanPreference(PreferenceTypes.K_SAFE_MODE_ENABLED, false);
                mSequence = SequenceDB.instance.createNewSequence(mContext
                        , -1
                        , -1
                        , false //no 360 cam yet
                        , mContext.getAppPrefs().getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE)
                        , OSVApplication.VERSION_NAME
                        , false, mSafe);
                mSequencePath = mSequence.folder.getPath();
                if (mSequence == null) {
                    Toast.makeText(mContext, R.string.error_creating_folder_message, Toast.LENGTH_SHORT).show();
                    recording = false;
                    return;
                }
                if (!mSafe) {
                    try {
                        ffmpeg = new FFMPEG(new FFMPEG.ErrorListener() {
                            @Override
                            public void onError() {
                                int value = appPrefs.getIntPreference(PreferenceTypes.K_FFMPEG_CRASH_COUNTER);
                                appPrefs.saveIntPreference(PreferenceTypes.K_FFMPEG_CRASH_COUNTER, value + 1);
                                Log.d(TAG, "onError: FFMPEG crashed, counter is at " + (value + 1));
                            }
                        });
                    } catch (ExceptionInInitializerError e) {
                        Log.w(TAG, "startSequence: " + Log.getStackTraceString(e));
                        if (Fabric.isInitialized()) {
                            Answers.getInstance().logCustom(new CustomEvent("FFMPEG object creation failed."));
                        }
                        Log.e(TAG, "startSequence: could not init ffmpeg");
                        Toast.makeText(mContext, R.string.ffmpeg_init_error_message, Toast.LENGTH_SHORT).show();
                        stopSequence();
                        return;
                    } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
                        Log.e(TAG, "startSequence: could not init ffmpeg");
                        Toast.makeText(mContext, R.string.ffmpeg_init_error_message, Toast.LENGTH_SHORT).show();
                        stopSequence();
                        return;
                    }
                    int ret = -1;
                    try {
                        ret = ffmpeg.initial(mSequence.folder.getPath() + "/");
                    } catch (Exception ignored) {}
                    if (ret != 0) {
                        Log.e(TAG, "startSequence: could not create video file");
                        Toast.makeText(mContext, R.string.error_creating_video_file_message, Toast.LENGTH_SHORT).show();
                        stopSequence();
                        return;
                    }
                }
                if (mSequence == null || mSequence.folder == null) {
                    Log.e(TAG, "startSequence: could not create video file");
                    Toast.makeText(mContext, R.string.error_creating_video_file_message, Toast.LENGTH_SHORT).show();
                    stopSequence();
                    return;
                }

                EventBus.postSticky(new RecordingEvent(mSequence, true));
                mRunDetection = true;
                LocationManager.ACCURATE = false;
                mIndex = 0;

                mImageCounter = 0;
                mAproximateDistance = 0;
                OSVFile folder = null;
                if (mSequence == null || mSequence.folder == null){
                    if (mSequencePath != null){
                        folder = new OSVFile(mSequencePath);
                    }
                } else {
                    folder = mSequence.folder;
                }

                mSensorManager.onResume(folder, mSafe);
                if (Fabric.isInitialized()) {
                    Crashlytics.setBool(Log.RECORD_STATUS, true);
                }
            }
        });
    }

    protected void stopSequence() {
        stopSequence(false);
    }

    public void stopSequence(boolean synchronously) {
        if (!recording) {
            return;
        }
        recording = false;
        if (mSequence != null && SequenceDB.instance.getNumberOfFrames(mSequence.sequenceId) <= 0) {
            SequenceDB.instance.deleteRecords(mSequence.sequenceId);
            if (mSequence.folder != null) {
                mSequence.folder.delete();
            }
        }
        final Sequence finalSequence = mSequence;
        mSequence = null;
        mSequencePath = null;
        mIndex = 0;
        mImageCounter = 0;
        mAproximateDistance = 0;
        EventBus.clear(RecordingEvent.class);
        if (finalSequence != null) {
            SequenceDB.instance.updateSequenceFrameCount(finalSequence.sequenceId);
            finalSequence.refreshStats();
            if (Fabric.isInitialized()) {
                String userType = appPrefs.getStringPreference(PreferenceTypes.K_USER_TYPE);
                if (userType.equals("")){
                    userType = "unknown";
                }
                if (Fabric.isInitialized()) {
                    try {
                        Answers.getInstance().logCustom(new CustomEvent("Recorded track")
                                .putCustomAttribute("images", finalSequence.imageCount)
                                .putCustomAttribute("obd", "" + finalSequence.obd)
                                .putCustomAttribute("userType", userType)
                                .putCustomAttribute("length", finalSequence.getDistance())
                        );
                    } catch (Exception ignored) {}
                }
            }
        }
        EventBus.post(new RecordingEvent(finalSequence, false));
        mRunDetection = false;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mSensorManager.onPauseOrStop();
                if (!mSafe) {
                    if (ffmpeg != null) {
                        int ret = ffmpeg.close();
                        Log.d(TAG, "run: ffmpeg close: " + ret);
                    }
                }

                if (finalSequence == null) {
                    return;
                }
                mAproximateDistance = 0;
                if (SequenceDB.instance.getNumberOfFrames(finalSequence.sequenceId) <= 0) {
                    Sequence.deleteSequence(finalSequence.sequenceId);
                    if (finalSequence.folder != null) {
                        finalSequence.folder.delete();
                    }
                    Log.d(TAG, "stopSequence: deleted sequence");
                }
                mSequence = null;
                mSequencePath = null;
                Log.d(TAG, "stopSequence: Stopped recording");
                if (Fabric.isInitialized()) {
                    Crashlytics.setBool(Log.RECORD_STATUS, false);
                }
            }
        };
        if (synchronously) {
            runnable.run();
        } else {
            mBackgroundHandler.post(runnable);
        }
    }

    protected boolean isCameraIdle() {
        return mCameraIdle;
    }

    public boolean isRecording() {
        return recording;
    }

    protected double getAproximateDistance() {
        return mAproximateDistance;
    }

    public Sequence getSequence() {
        return mSequence;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onObdSpeed(ObdSpeedEvent event) {
        if (mSequence != null && !mSequence.obd) {
            mSequence.obd = true;
            SequenceDB.instance.setOBDForSequence(mSequence.sequenceId, true);
        }
    }
}
