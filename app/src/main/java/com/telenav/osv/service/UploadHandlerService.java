package com.telenav.osv.service;

import java.util.concurrent.ConcurrentLinkedQueue;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.listener.UploadProgressListener;
import com.telenav.osv.manager.UploadManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;

public class UploadHandlerService extends Service implements UploadProgressListener {

    public static final String FLAG_PAUSE = "pause";

    public static final String FLAG_CANCEL = "cancel";

    public static final String FLAG_NETWORK = "network";

    private static final String TAG = UploadHandlerService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 113;

    private final UploadHandlerBinder mBinder = new UploadHandlerBinder();

    private final ConcurrentLinkedQueue<UploadProgressListener> mUploadProgressListeners = new ConcurrentLinkedQueue<>();

    public UploadManager mUploadManager;

    private NotificationCompat.Builder mNotification;

    private Handler mHandler;

    private boolean needsToSetNotification = true;

    private NotificationManager mNotificationManager;

    private ApplicationPreferences appPrefs;

    private boolean mBound = false;

    private PendingIntent pauseIntent;

    private PendingIntent cancelIntent;

    private final Object mListenerSyncObject = new Object();


    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        mUploadManager = ((OSVApplication) getApplication()).getUploadManager();
        mUploadManager.setUploadProgressListener(this);
        appPrefs = ((OSVApplication) getApplication()).getAppPrefs();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        int flag = 0;
        appPrefs = ((OSVApplication) getApplication()).getAppPrefs();
        mUploadManager = ((OSVApplication) getApplication()).getUploadManager();
        if (mUploadManager == null) {
            return flag;
        }
        final boolean autoSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO, false);
        final boolean dataSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED, false);
        final boolean localFilesExist = Sequence.forceRefreshLocalSequences().size() != 0;
        boolean stayAlive = false;
        if (intent != null) {
            if (NetworkUtils.isWifiInternetAvailable(this)){
                if (((OSVApplication)getApplication()).getOBDManager() != null && appPrefs.getBooleanPreference(PreferenceTypes.K_OBD_CONNECTED)) {
                    ((OSVApplication)getApplication()).getOBDManager().connect();
                }
            }
            if (intent.getBooleanExtra(FLAG_CANCEL, false)) {
                Log.d(TAG, "Upload cancelled.");
                mUploadManager.cancelUploadTasks();
            } else if (intent.getBooleanExtra(FLAG_PAUSE, false)) {
                Log.d(TAG, "Upload paused.");
                if (mUploadManager.isPaused()) {
                    mUploadManager.resumeUpload();
                } else {
                    mUploadManager.pauseUpload();
                }
            } else if (intent.getBooleanExtra(FLAG_NETWORK, false)) {
                Log.d(TAG, "Network status has changed.");
                if (localFilesExist) {
                    if (NetworkUtils.isInternetAvailable(this)) {
                        //start if needed
                        if (mUploadManager.isUploading()) {
                            if (mUploadManager.isPaused()) {
                                if (NetworkUtils.isWifiInternetAvailable(this) || dataSet) {
                                    mUploadManager.resumeUpload();

                                }
                            } else {
                                if (!NetworkUtils.isWifiInternetAvailable(this) && !dataSet) {
                                    mUploadManager.pauseUpload();
                                }
                            }
                            stayAlive = true;
                        } else {
                            if (autoSet && (dataSet || NetworkUtils.isWifiInternetAvailable(this))) {
                                mUploadManager.uploadCacheIfAutoEnabled();
                                stayAlive = true;
                            }
                        }
                    } else {
                        if (mUploadManager.isUploading() && !mUploadManager.isPaused()) {
                            mUploadManager.pauseUpload();
                            stayAlive = true;
                        }
                    }
                }
                if (mBound) {
                    stayAlive = true;
                    flag = START_STICKY;
                }
                if (!stayAlive) {
                    stopSelf();
                }
            }
        }
        return flag;
    }


    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBound = false;
        return super.onUnbind(intent);
    }

    public void uploadStarted() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pnextIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_launcher);
        if (mNotification != null && mNotification.mActions != null) {
            mNotification.mActions.clear();
        }
        mNotification = new NotificationCompat.Builder(this)
                .setContentTitle("OSV")
                .setContentText("Uploading images...")
                .setSmallIcon(R.drawable.icon_upload_white)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setOngoing(false)
                .setProgress(100, 0, false)
                .setWhen(0)
                .setContentIntent(pnextIntent);

        startForeground(NOTIFICATION_ID, mNotification.build());
    }

    public void uploadFinished() {
        stopForeground(true);
        if (mNotification.mActions != null) {
            mNotification.mActions.clear();
        }
        mNotification.setContentText(getString(R.string.upload_finished_message));
        mNotification.setProgress(0, 0, false);
        mNotification.setOngoing(false);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification.build());
    }

    /**
     * adds an uploadlistener object
     * @param uploadProgressListener
     * @return true if there is an upload ongoing
     */
    public void addUploadProgressListener(UploadProgressListener uploadProgressListener) {
        synchronized (mListenerSyncObject) {
            if (!mUploadProgressListeners.contains(uploadProgressListener)) {
                Log.d(TAG, "addUploadProgressListener: " + uploadProgressListener.getClass().getSimpleName());
                this.mUploadProgressListeners.add(uploadProgressListener);
                if (UploadManager.sUploadStatus != UploadManager.STATUS_IDLE) {
                    uploadProgressListener.onUploadStarted(mUploadManager.getTotalSizeValue());
                    long totalSize = mUploadManager.getTotalSizeValue();
                    long remainingSize = mUploadManager.getRemainingSizeValue();
                    if (totalSize != 0) {
                        if (!mUploadManager.isIndexing()) {
                            uploadProgressListener.onIndexingFinished();
                        }
                        uploadProgressListener.onProgressChanged(totalSize, remainingSize);
                        if (UploadManager.sUploadStatus == UploadManager.STATUS_PAUSED) {
                            uploadProgressListener.onUploadPaused();
                        } else {
                            uploadProgressListener.onUploadResumed();
                        }
                    }
                } else {
//                    uploadProgressListener.onUploadFinished(mUploadManager.getSuccessfulUploadsValue(), mUploadManager.getFailedUploadsValue());
                }
            }
        }
    }

    public void removeUploadProgressListener(UploadProgressListener listener) {
        synchronized (mListenerSyncObject) {
            Log.d(TAG, "removeUploadProgressListener: " + listener.getClass().getSimpleName());
            this.mUploadProgressListeners.remove(listener);
        }
    }

    private void initNotificationIfNeeded() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        if (mNotification == null) {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pnextIntent = PendingIntent.getActivity(this, 0, intent, 0);
            mNotification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_short_name))
                    .setContentText(getString(R.string.uploading_images_message))
                    .setSmallIcon(R.drawable.icon_upload_white)
                    .setOngoing(false)
                    .setProgress(100, 0, false)
                    .setWhen(0)
                    .setContentIntent(pnextIntent);
        }

        if (mNotification.mActions != null) {
            mNotification.mActions.clear();
        }
        if (isUploading()) {
            Intent serviceIntent = new Intent(getApplicationContext(), UploadHandlerService.class);
            serviceIntent.putExtra(FLAG_PAUSE, true);
            pauseIntent = PendingIntent.getService(getApplicationContext(), 0, serviceIntent, 0);

            Intent intent = new Intent(getApplicationContext(), UploadHandlerService.class);
            intent.putExtra(FLAG_CANCEL, true);
            cancelIntent = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
            if (!mUploadManager.isIndexing()) {
                mNotification.addAction(mUploadManager.isPaused() ? R.drawable.ic_play_arrow_black_36dp : R.drawable.ic_pause_black_36dp, mUploadManager.isPaused() ? getString(R
                        .string.resume_label) :
                        getString(R.string.pause_label), pauseIntent);
            }
//            mNotification.addAction(R.drawable.ic_clear_black_36dp, "Cancel", cancelIntent);
        }
        if (mNotificationManager == null) {
            mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        }
    }

    @Override
    public void onProgressChanged(long totalSize, final long remainingSize) {
//        Log.d(TAG, "onProgressChanged: totalSize = " + totalSize + ", remainingSize = " + remainingSize);
        if (totalSize == 0) {
            totalSize = 1;
        }
        initNotificationIfNeeded();
        updateNotification(totalSize, remainingSize);
        final long finalTotalSize = totalSize;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onProgressChanged(finalTotalSize, remainingSize);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onImageUploaded(final Sequence sequence, final boolean success) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onImageUploaded(sequence, success);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onSequenceUploaded(final Sequence sequence) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onSequenceUploaded(sequence);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onIndexingSequence(final Sequence sequence, final int remainingUploads) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onIndexingSequence(sequence, remainingUploads);
                        }
                    }
                }
            }
        });
    }

//    @Override
//    public void onImageUploaded(int sequenceID, int numberOfImages, int startIndex, boolean fromError) {
//
//    }

    @Override
    public void onUploadPaused() {
        initNotificationIfNeeded();
        mNotification.setContentText(getString(R.string.track_upload_paused));
        mNotification.setOngoing(true);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification.build());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onUploadPaused();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onUploadResumed() {
        initNotificationIfNeeded();
        mNotification.setContentText(getString(R.string.track_upload_resuming));
        mNotification.setOngoing(true);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification.build());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onUploadResumed();
                        }
                    }
                }
            }
        });
    }


    @Override
    public void onUploadStarted(final long mTotalSize) {
        uploadStarted();
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onUploadStarted(mTotalSize);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onUploadingMetadata() {
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onUploadingMetadata();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onIndexingFinished() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onIndexingFinished();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onPreparing(final int nrOfFrames) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onPreparing(nrOfFrames);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onUploadCancelled(final long total, final long remaining) {
        android.util.Log.d(TAG, "onUploadCancelled: upload cancelled");
        initNotificationIfNeeded();
        stopForeground(true);
        mNotification.setContentText(getString(R.string.track_upload_cancelled));
        mNotification.setProgress(0, 0, false);
        mNotification.setOngoing(false);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onUploadCancelled(total, remaining);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onUploadFinished(final int successful, final int unsuccessful) {
        initNotificationIfNeeded();
        uploadFinished();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            listener.onUploadFinished(successful, unsuccessful);
                        }
                    }
                }
            }
        });
    }

    private void updateNotification(long total, long remaining) {
        initNotificationIfNeeded();
        if (total > 0) {
            if (needsToSetNotification || remaining == 0) {
                needsToSetNotification = false;
                Log.d(TAG, "updateNotification: progress: " + ((total - remaining) * 100) / total);
                mNotification.setProgress(100, (int) (((total - remaining) * 100L) / total), false);
                mNotification.setContentText(getString(R.string.uploading_images) + ((total - remaining) * 100) / total + "%");
                mNotificationManager.notify(NOTIFICATION_ID, mNotification.build());
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        needsToSetNotification = true;
                    }
                }, 1000);
            }
        }
    }

    public boolean isUploading() {
        return mUploadManager != null && mUploadManager.isUploading();
    }

    @Override
    public void onBandwidthStateChange(final ConnectionQuality bandwidthState, final double bandwidth) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSyncObject) {
                    for (UploadProgressListener listener : mUploadProgressListeners) {
                        if (listener != null) {
                            if (UploadManager.sUploadStatus == UploadManager.STATUS_IDLE || UploadManager.sUploadStatus == UploadManager.STATUS_PAUSED) {
                                listener.onBandwidthStateChange(ConnectionQuality.UNKNOWN, 0);
                            } else {
                                listener.onBandwidthStateChange(bandwidthState, bandwidth);
                            }
                        }
                    }
                }
            }
        });
    }

    public class UploadHandlerBinder extends Binder {
        public UploadHandlerService getService() {
            return UploadHandlerService.this;
        }
    }
}
