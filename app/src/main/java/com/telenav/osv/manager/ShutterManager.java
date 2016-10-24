package com.telenav.osv.manager;

import java.util.ArrayList;
import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;
import android.hardware.Camera;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.widget.Toast;
import com.telenav.ffmpeg.FFMPEG;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.http.RequestListener;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.RecordinStoppedException;
import com.telenav.osv.item.SensorData;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.listener.ImageSavedListener;
import com.telenav.osv.listener.RecordingStateChangeListener;
import com.telenav.osv.ui.fragment.SettingsFragment;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;

/**
 * This class is responsible for taking the pictures frmo the camera.
 * Created by Kalman on 10/7/2015.
 */
public class ShutterManager implements Camera.ShutterCallback, ObdManager.ConnectionListener {
    public final static String TAG = "ShutterManager";

    private final Object shutterSynObject = new Object();

    private RecordingStateChangeListener mRecordingStateListener;

    private Handler mBackgroundHandler;

    private HandlerThread mHandlerThread;

    private LocationManager mLocationManager;

    private Sequence mSequence;

    private String mSequencePath;

    private int mIndex = 0;

    private int mImageCounter = 0;

    private OSVApplication mContext;

    private boolean recording;

    private Camera.ShutterCallback mShutterCallback;

    private double mAproximateDistance = 0;

    private UploadManager mUploadManager;

    private Thread recordThread;

    private FocusManager mFocusManager;

    private ImageSavedListener imageSavedListener;

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

    private long mLastSnapshotTime = 0;

    private FFMPEG ffmpeg;

    private Location mLocation;

//    private HttpEventListener mWifiCamImageCallback = new HttpEventListener() {
//        private String latestCapturedFileId;
//
//        private boolean ImageAdd = false;
//
//        @Override
//        public void onCheckStatus(boolean newStatus) {
//            if (newStatus) {
//                Log.d(TAG, "takePicture: FINISHED");
//            } else {
//                Log.d(TAG, "takePicture: IN PROGRESS");
//            }
//        }
//
//        @Override
//        public void onObjectChanged(String latestCapturedFileId) {
//            this.ImageAdd = true;
//            this.latestCapturedFileId = latestCapturedFileId;
//            Log.d(TAG, "ImageAdd:FileId " + this.latestCapturedFileId);
//        }
//
//        @Override
//        public void onCompleted() {
//            Log.d(TAG, "CaptureComplete");
//            mWifiCameraManager.restartPreview();
//            if (ImageAdd) {
//                mWifiCameraManager.getImage(latestCapturedFileId, new ImageDataCallback() {
//
//                    @Override
//                    public void onImageDataReceived(ImageData imgData) {
//                        if (Looper.myLooper() == Looper.getMainLooper()) {
//                            Log.d(TAG, "onPictureTaken: ui thread");
//                        }
//                        if (!recording) {
//                            return;
//                        }
//                        int reserved = SettingsFragment.MIN_FREE_SPACE;
//                        long ret = Utils.getAvailableSpace(mContext);
//                        if (ret < 0) {
//                            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (Fabric.isInitialized()) {
//                                        Crashlytics.logException(new RecordinStoppedException("Recording stopped because storage state unavailable."));
//                                    }
//                                    Toast.makeText(mContext, "Storage unavailable, please check sdcard, or restart phone.", Toast.LENGTH_LONG).show();
//                                    stopSequence();
//                                }
//                            });
//                            return;
//                        }
//                        if (ret <= reserved) {
//                            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (Fabric.isInitialized()) {
//                                        Crashlytics.logException(new RecordinStoppedException("Recording stopped because Reached storage space limit."));
//                                    }
//                                    Toast.makeText(mContext, "Reached storage limit, stopping recording.", Toast.LENGTH_LONG).show();
//                                    stopSequence();
//                                }
//                            });
//                            return;
//                        }
//                        String path = Utils.generateOSVFolder(mContext) + "/SEQ_" + mSequence.sequenceId + "/" + mIndex + ".p.jpg";
//                        String tmpPath = path + ".tmp";
//                        OSVFile tmpfile = new OSVFile(tmpPath);
//                        if (!tmpfile.exists()) {
//                            try {
//                                tmpfile.createNewFile();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        FileOutputStream out = null;
//                        try {
//                            out = new FileOutputStream(tmpPath);
//                            out.write(imgData.getRawData());
//                            out.close();
//                            OSVFile jpg = new OSVFile(path);
//                            tmpfile.renameTo(jpg);
//
//                            Log.v(TAG, "Saved JPEG data : " + jpg.getName() + ", size: " + ((float) jpg.length()) / 1024f / 1024f + " mb");
//                            try {
//                                SequenceDB.instance.insertPhoto(mSequence.sequenceId, mIndex, jpg.getPath(), mLocation.getLatitude(),
//                                        mLocation.getLongitude(), mAccuracy, 0);
//                            } catch (final SQLiteConstraintException e) {
//                                Log.w(TAG, "onPositionerClicked: " + Log.getStackTraceString(e));
//                                new Handler(Looper.getMainLooper()).post(new Runnable() {
//                                    @Override
//                                    public void run() {
//
//                                        if (Fabric.isInitialized()) {
//                                            Crashlytics.logException(new RecordinStoppedException("Recording stopped because SQLiteException was raised."));
//                                            Crashlytics.logException(e);
//                                        }
//                                        stopSequence();
//                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                startSequence();
//                                            }
//                                        }, 1000);
//                                    }
//                                });
//
//                            }
//                            mWifiCameraManager.deleteImage(latestCapturedFileId, new HttpEventListener() {
//                                @Override
//                                public void onCheckStatus(boolean newStatus) {
//                                }
//
//                                @Override
//                                public void onObjectChanged(String latestCapturedFileId) {
//                                    Log.d(TAG, "delete " + latestCapturedFileId);
//                                }
//
//                                @Override
//                                public void onCompleted() {
//                                    mHandler.removeCallbacks(mIdleRunnable);
//                                    mCameraIdle = true;
//                                    Log.d(TAG, latestCapturedFileId + "image deleted from external camera.");
//                                }
//
//                                @Override
//                                public void onError(String errorMessage) {
//                                    Log.d(TAG, "delete error " + errorMessage);
//                                }
//                            });
//
//                        } catch (IOException e) {
//                            Log.e(TAG, "Failed to write image", e);
//                        } finally {
//                            SensorManager.logSensorData(new SensorData(mIndex, mTimestamp));
//                            SensorManager.flushToDisk();
//                            mIndex++;
//                            mImageCounter++;
//                        }
//                        if (mLocationManager.hasPosition()) {
//                            mAproximateDistance = (int) (mAproximateDistance + ComputingDistance.distanceBetween(mLocationManager.getActualLocation().getLongitude(),
//                                    mLocationManager
//                                            .getActualLocation().getLatitude(), mLocationManager.getPreviousLocation().getLongitude(), mLocationManager.getPreviousLocation()
//                                            .getLatitude
//                                                    ()));
//                        }
//                    }
//
//                    @Override
//                    public void onRequestFailed() {
//
//                    }
//                });
//            }
//        }
//
//        @Override
//        public void onError(String errorMessage) {
//            Log.d(TAG, "CaptureError " + errorMessage);
//            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                @Override
//                public void run() {
////                        btnShoot.setEnabled(true);
////                    changeCameraStatus(R.string.text_camera_standby);
//                }
//            });
//        }
//    };

    private Camera.PictureCallback mJpegPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] jpegData, Camera camera) {
            final long mTimestampF = mTimestamp;
            CameraManager.instance.restartPreviewIfNeeded();
            if (mFocusManager != null) {
                mFocusManager.checkFocusManual();
            }
            mHandler.removeCallbacks(mIdleRunnable);
            mCameraIdle = true;
            if (!recording) {
                return;
            }
            if (mSequence == null) {
                if (mSequencePath == null) {
//                    if (Fabric.isInitialized()) {
//                        Crashlytics.logException(new RecordinStoppedException("Recording stopped because Sequence pointer is null"));
//                    }
                    stopSequence();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startSequence();
                        }
                    }, 1000);
                    return;
                } else {
                    mSequence = new Sequence(new OSVFile(mSequencePath));
                }
            }
            synchronized (shutterSynObject) {
                mIndex++;
                final int mSequenceIdF = mSequence.sequenceId;
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
                saveFrame(jpegData, mSequenceIdF, mIndexF, mFolderPath, mLocationF, mAccuracyF, mOrientationF, mTimestampF);
            }
        }
    };

    public ShutterManager(Application application) {
        mContext = (OSVApplication) application;
        mHandlerThread = new HandlerThread("ShutterManager", Process.THREAD_PRIORITY_FOREGROUND);
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());
        mUploadManager = ((OSVApplication) application).getUploadManager();
        mLocationManager = ((OSVApplication) application).getLocationManager();
        mSensorManager = ((OSVApplication) application).getSensorManager();

    }

    public void setRecordingStateChangeListener(RecordingStateChangeListener listener) {
        mRecordingStateListener = listener;
//        if (mRecordingStateListener != null) {
//            mRecordingStateListener.onRecordingStatusChanged(recording);
//        }
    }

    private void saveFrame(final byte[] jpegData, final int mSequenceIdF, final int mIndexF, final String folderPathF, final Location mLocationF, final
    float mAccuracyF, final int mOrientationF, final long mTimestampF) {
        Log.d(TAG, "saveFrame: posting frame data to handler");
        if (mHandlerThread.getState() == Thread.State.TERMINATED) {
            mHandlerThread = new HandlerThread("ShutterManager", Process.THREAD_PRIORITY_FOREGROUND);
            mHandlerThread.start();
            mBackgroundHandler = new Handler(mHandlerThread.getLooper());
        }
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                int reserved = SettingsFragment.MIN_FREE_SPACE;
                int available = (int) Utils.getAvailableSpace(mContext);
                Log.d(TAG, "saveFrame: entered data handler");
                if (available <= reserved) {
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
//                                Crashlytics.logException(e);
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
                CameraManager.instance.stopDetection();
                final long time = System.currentTimeMillis();
                int[] ret = ffmpeg.encode(jpegData);
                Log.d(TAG, "saveFrame: encoding done in " + (System.currentTimeMillis() - time) + " ms ,  video file " + ret[0] + " and frame " + ret[1]);
                CameraManager.instance.startDetection();

                if (ret[0] < 0 || ret[1] < 0) {
                    synchronized (shutterSynObject) {
                        mIndex--;
                    }
                    if (imageSavedListener != null) {
                        imageSavedListener.onImageSaved(false);
                    }
                    if (ret[0] < 0) {
                        Toast.makeText(mContext, R.string.encoding_error_message, Toast.LENGTH_SHORT).show();
                        stopSequence();
                    }
                    return;
                }
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

//                            if (Fabric.isInitialized()) {
//                                Crashlytics.logException(new RecordinStoppedException("Recording stopped because SQL"));
//                                Crashlytics.logException(e);
//                            }
                            stopSequence();
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    startSequence();
                                }
                            }, 1000);
                        }
                    });

                }
                mImageCounter++;
                if (imageSavedListener != null) {
                    imageSavedListener.onImageSaved(true);
                }
            }
        });
    }

//    private void writeExifLocation(String path, double lat, double lon) throws IOException {
//        ExifInterface exif = new ExifInterface(path);
//        //String latitudeStr = "90/1,12/1,30/1";
//        double alat = Math.abs(lat);
//        String dms = Location.convert(alat, Location.FORMAT_SECONDS);
//        String[] splits = dms.split(":");
//        String[] secnds = (splits[2]).split("\\.");
//        String seconds;
//        if (secnds.length == 0) {
//            seconds = splits[2];
//        } else {
//            seconds = secnds[0];
//        }
//
//        String latitudeStr = splits[0] + "/1," + splits[1] + "/1," + seconds + "/1";
//        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latitudeStr);
//
//        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, lat > 0 ? "N" : "S");
//
//        double alon = Math.abs(lon);
//
//
//        dms = Location.convert(alon, Location.FORMAT_SECONDS);
//        splits = dms.split(":");
//        secnds = (splits[2]).split("\\.");
//
//        if (secnds.length == 0) {
//            seconds = splits[2];
//        } else {
//            seconds = secnds[0];
//        }
//        String longitudeStr = splits[0] + "/1," + splits[1] + "/1," + seconds + "/1";
//
//        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, longitudeStr);
//        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lon > 0 ? "E" : "W");
////        if (mBearing != 0.0f) {
////            exif.setAttribute("GPSImgDirection", String.valueOf(mBearing));
////            exif.setAttribute("GPSTrack", String.valueOf(mBearing));
////        }
//        exif.saveAttributes();
//    }

    public int getPictureIndex() {
        return mIndex;
    }

    public int getNumberOfPictures() {
        return mImageCounter;
    }

    public void setShutterCallback(Camera.ShutterCallback mShutterCallback) {
        this.mShutterCallback = mShutterCallback;
    }

    /**
     * takes a snapshot
     * exposure value, otherwise set it to 0
     * @param location location
     * @param dist
     * @return true if image was taken
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
            boolean taken = CameraManager.instance.takeSnapshot(this, null, mJpegPictureCallback);
            if (taken) {
                if (mShutterCallback != null) {
                    mShutterCallback.onShutter();
                }
            }
        }
    }

    @Override
    public void onShutter() {
        mTimestamp = System.currentTimeMillis();
    }

    public void startSequence() {
        mCameraIdle = true;
        recording = true;
        mSequence = SequenceDB.instance.createNewSequence(mContext
                , mLocationManager.getActualLocation().getLatitude()
                , mLocationManager.getActualLocation().getLongitude()
                , false //no 360 cam yet
                , mContext.getAppPrefs().getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE)
                , OSVApplication.VERSION_NAME
                , ((OSVApplication) mContext.getApplicationContext()).getOBDManager().isConnected());
        mSequencePath = mSequence.folder.getPath();
        if (mSequence == null) {
            Toast.makeText(mContext, R.string.error_creating_folder_message, Toast.LENGTH_SHORT).show();
            recording = false;
            return;
        }
        try {
            ffmpeg = new FFMPEG();
        } catch (ExceptionInInitializerError e) {
            Log.d(TAG, "startSequence: " + Log.getStackTraceString(e));
//            if (Fabric.isInitialized()) {
//                Crashlytics.logException(e);
//            }
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

        mRecordingStateListener.onRecordingStatusChanged(true);
        LocationManager.ACCURATE = false;
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSequence == null || mSequence.folder == null) {
                    Log.e(TAG, "startSequence: could not create video file");
                    Toast.makeText(mContext, R.string.error_creating_video_file_message, Toast.LENGTH_SHORT).show();
                    stopSequence();
                }
                if (mContext.getOBDManager() != null) {
                    mContext.getOBDManager().addConnectionListener(ShutterManager.this);
                }
                mIndex = 0;

                mImageCounter = 0;
                mAproximateDistance = 0;
                mSensorManager.onResume(((mSequence == null || mSequence.folder == null) && mSequencePath != null) ? new OSVFile(mSequencePath) : mSequence.folder);
//                AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//                mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
//                mgr.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
//                mgr.setStreamMute(AudioManager.STREAM_DTMF, true);
//                mgr.setStreamMute(0, true);
                if (((OSVApplication) mContext).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
                    recordThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mLocationManager.setPreviousLocation(mLocationManager.getActualLocation());
                            while (!Thread.interrupted() && recording) {
                                if (mCameraIdle) {
                                    if (mLocationManager.hasPosition()) {
                                        double lat = mLocationManager.getActualLocation().getLatitude();
                                        double lon = mLocationManager.getActualLocation().getLongitude();
                                        float accuracy = mLocationManager.getAccuracy();
                                        double dist = ComputingDistance.distanceBetween(mLocationManager.getPreviousLocation().getLongitude(), mLocationManager
                                                .getPreviousLocation()
                                                .getLatitude(), lon, lat);
                                        Log.d(TAG, "recordThread: debug save photo");
                                        mLastSnapshotTime = System.currentTimeMillis();
                                        takeSnapshot(mLocationManager.getActualLocation(), accuracy, dist);
                                        mLocationManager.setPreviousLocation(mLocationManager.getActualLocation());
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
        });

    }

    public void stopSequence() {
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
        mRecordingStateListener.onRecordingStatusChanged(false);
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if (recordThread != null && recordThread.isAlive()) {
                    recordThread.interrupt();
                }
                mSensorManager.onPauseOrStop();
                if (ffmpeg != null) {
                    int ret = ffmpeg.close();
                }

                if (finalSequence == null) {
                    return;
                }
//                AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//                mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
//                mgr.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
//                mgr.setStreamMute(0, false);
                mAproximateDistance = 0;
                if (SequenceDB.instance.getNumberOfFrames(finalSequence.sequenceId) > 0) {
                    SequenceDB.instance.updateSequenceFrameCount(finalSequence.sequenceId);
                    finalSequence.refreshStats();
                    ApplicationPreferences appPrefs = ((OSVApplication) mContext.getApplicationContext()).getAppPrefs();
                    if (!appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN).equals("") && appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO) && NetworkUtils
                            .isInternetAvailable(mContext)) {
                        if (NetworkUtils.isWifiInternetAvailable(mContext) || appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED)) {
                            ArrayList<Sequence> list = new ArrayList<>();
                            list.add(finalSequence);
                            mUploadManager.uploadCache(new RequestListener() {
                                @Override
                                public void requestFinished(final int status) {
                                    if (status == STATUS_FAILED) {
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(mContext, R.string.upload_failed_message, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else if (status == STATUS_SUCCESS_SEQUENCE) {
                                        Log.d(TAG, "requestFinished: sequence ID created");
                                    }
                                }
                            }, list);

                        }
                        Log.d(TAG, "stopSequence: data not enabled, or no internet connection");
                    } else {
                        Log.d(TAG, "stopSequence: auto upload off, or no connection");
                    }
                } else {
                    if (SequenceDB.instance.getNumberOfFrames(finalSequence.sequenceId) <= 0) {
                        Sequence.removeSequence(finalSequence.sequenceId);
                        if (finalSequence.folder != null) {
                            finalSequence.folder.delete();
                        }
                        Log.d(TAG, "stopSequence: deleted sequence");
                    }
                }
                mSequence = null;
                mSequencePath = null;
            }
        });
    }


    public boolean isRecording() {
        return recording;
    }

    public double getAproximateDistance() {
        return mAproximateDistance;
    }

    public Sequence getSequence() {
        return mSequence;
    }

    public void setFocusManager(FocusManager focusManager) {
        this.mFocusManager = focusManager;
    }

    public void setImageSavedListener(ImageSavedListener imageSavedListener) {
        this.imageSavedListener = imageSavedListener;
    }

    @Override
    public void onConnected() {
        if (mSequence != null && !mSequence.obd) {
            mSequence.obd = true;
            SequenceDB.instance.setOBDForSequence(mSequence.sequenceId, true);
        }
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onSpeedObtained(ObdManager.SpeedData speedData) {

    }

    @Override
    public void onConnecting() {

    }

}
