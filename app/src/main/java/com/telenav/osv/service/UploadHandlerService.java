package com.telenav.osv.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import com.crashlytics.android.Crashlytics;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.upload.UploadBandwidthEvent;
import com.telenav.osv.event.network.upload.UploadCancelledEvent;
import com.telenav.osv.event.network.upload.UploadFinishedEvent;
import com.telenav.osv.event.network.upload.UploadPausedEvent;
import com.telenav.osv.event.network.upload.UploadProgressEvent;
import com.telenav.osv.event.network.upload.UploadStartedEvent;
import com.telenav.osv.event.network.upload.UploadingSequenceEvent;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.listener.UploadProgressListener;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import io.fabric.sdk.android.Fabric;

public class UploadHandlerService extends Service implements UploadProgressListener {

    public static final String FLAG_NETWORK = "eventNetwork";

    private static final String TAG = UploadHandlerService.class.getSimpleName();

    private static final String FLAG_PAUSE = "pause";

    private static final String FLAG_CANCEL = "cancel";

    private static final int NOTIFICATION_ID = 113;

    private final UploadHandlerBinder mBinder = new UploadHandlerBinder();

    private final Object notificationSyncObject = new Object();

    private UploadManager mUploadManager;

    private NotificationCompat.Builder mNotification;

    private Handler mHandler;

    private boolean needsToSetNotification = true;

    private NotificationManager mNotificationManager;

    private ApplicationPreferences appPrefs;

    private int mLastProgress = -1;

    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        OSVApplication app = ((OSVApplication) getApplication());
        while (!app.isReady()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Log.d(TAG, "onCreate: " + Log.getStackTraceString(e));
            }
        }
        mHandler = new Handler(Looper.getMainLooper());
        mUploadManager = ((OSVApplication) getApplication()).getUploadManager();
        mUploadManager.setUploadProgressListener(this);
        appPrefs = ((OSVApplication) getApplication()).getAppPrefs();
        if (!mUploadManager.isUploading()) {
            if (mNotificationManager == null) {
                mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
            }
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
        mLastProgress = -1;
        mBroadcastReceiver = new NetworkBroadcastReceiver(UploadHandlerService.this, appPrefs, mUploadManager);
        registerReceiver(mBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        int flag = START_NOT_STICKY;
        appPrefs = ((OSVApplication) getApplication()).getAppPrefs();
        OSVApplication app = ((OSVApplication) getApplication());
        while (!app.isReady()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Log.d(TAG, "onStartCommand: " + Log.getStackTraceString(e));
            }
        }
        mUploadManager = ((OSVApplication) getApplication()).getUploadManager();
        if (mUploadManager == null) {
            return flag;
        }
        if (intent != null) {
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
            }
            if (intent.getBooleanExtra(FLAG_NETWORK, false)) {
                final boolean autoSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO, false);
                final boolean dataSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED, false);
                final boolean localFilesExist = LocalSequence.getStaticSequences().size() != 0;
                boolean isWifi = NetworkUtils.isWifiInternetAvailable(UploadHandlerService.this);
                boolean isNet = NetworkUtils.isInternetAvailable(UploadHandlerService.this);
                if (isNet && autoSet && (dataSet || isWifi)) {
                    if (localFilesExist && !((OSVApplication) getApplication()).getRecorder().isRecording()) {
                        mUploadManager.uploadCache(LocalSequence.getStaticSequences().values());
                    }
                }
            }
        }
        return flag;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onUploadStarted(final long mTotalSize, final int remainingSequences) {
        if (Fabric.isInitialized()) {
            Crashlytics.setBool(Log.UPLOAD_STATUS, true);
        }
        uploadStarted();
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                EventBus.postSticky(new UploadStartedEvent(mTotalSize, remainingSequences));
            }
        });
    }

    @Override
    public void onUploadingMetadata() {
        Log.d(TAG, "onUploadingMetadata: ");
    }

    @Override
    public void onPreparing(final int nrOfFrames) {
        Log.d(TAG, "onPreparing: " + nrOfFrames);
    }

    @Override
    public void onIndexingFinished() {
        Log.d(TAG, "onIndexingFinished: ");
    }

    @Override
    public void onUploadPaused() {
        initNotificationIfNeeded();
        synchronized (notificationSyncObject) {
            mNotification.setContentText(getString(R.string.track_upload_paused));
            mNotification.setOngoing(true);
            mNotificationManager.notify(NOTIFICATION_ID, mNotification.build());
        }
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                EventBus.postSticky(new UploadPausedEvent(true));
            }
        });
    }

    @Override
    public void onUploadResumed() {
        initNotificationIfNeeded();
        synchronized (notificationSyncObject) {
            mNotification.setContentText(getString(R.string.track_upload_resuming));
            mNotification.setOngoing(true);
            mNotificationManager.notify(NOTIFICATION_ID, mNotification.build());
        }
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                EventBus.postSticky(new UploadPausedEvent(false));
            }
        });
    }

    @Override
    public void onUploadCancelled(final long total, final long remaining) {
        if (Fabric.isInitialized()) {
            Crashlytics.setBool(Log.UPLOAD_STATUS, false);
        }
        Log.d(TAG, "onUploadCancelled: upload cancelled");
        initNotificationIfNeeded();
        mLastProgress = -1;
        synchronized (notificationSyncObject) {
            stopForeground(true);
            mNotification.setContentText(getString(R.string.track_upload_cancelled));
            mNotification.setProgress(0, 0, false);
            mNotification.setOngoing(false);
        }
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                EventBus.postSticky(new UploadCancelledEvent(total, remaining));
            }
        });
    }

    //    @Override
    //    public void onImageUploaded(int sequenceID, int numberOfImages, int fileIndex, boolean fromError) {
    //
    //    }

    @Override
    public void onUploadFinished() {
        if (Fabric.isInitialized()) {
            Crashlytics.setBool(Log.UPLOAD_STATUS, false);
        }
        initNotificationIfNeeded();
        uploadFinished();
        mLastProgress = -1;
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                EventBus.postSticky(new UploadFinishedEvent());
            }
        });
        mUploadManager.resetUploadStats();
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
                EventBus.postSticky(new UploadProgressEvent(finalTotalSize, remainingSize));
            }
        });
    }

    @Override
    public void onImageUploaded(final Sequence sequence) {
        Log.d(TAG, "onImageUploaded: " + sequence);
    }

    @Override
    public void onSequenceUploaded(final Sequence sequence) {
        Log.d(TAG, "onSequenceUploaded: " + sequence);
    }

    @Override
    public void onIndexingSequence(final Sequence sequence, final int remainingUploads) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                EventBus.post(new UploadingSequenceEvent(sequence, remainingUploads));
            }
        });
    }

    @Override
    public void onBandwidthStateChange(final ConnectionQuality bandwidthState, final double bandwidth) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (UploadManager.sUploadStatus == UploadManager.STATUS_IDLE || UploadManager.sUploadStatus == UploadManager.STATUS_PAUSED) {
                    EventBus.post(new UploadBandwidthEvent(ConnectionQuality.UNKNOWN, 0));
                } else {
                    EventBus.post(new UploadBandwidthEvent(bandwidthState, bandwidth));
                }
            }
        });
    }

    private void uploadStarted() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pnextIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        synchronized (notificationSyncObject) {
            if (mNotification != null && mNotification.mActions != null) {
                mNotification.mActions.clear();
            }
            mNotification = new NotificationCompat.Builder(this).setContentTitle("OSC").setContentText("Uploading images...")
                    .setSmallIcon(R.drawable.ic_upload_white).setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false)).setOngoing(false)
                    .setAutoCancel(false).setProgress(100, 0, false).setColor(getResources().getColor(R.color.accent_material_dark_1)).setWhen(0)
                    .setContentIntent(pnextIntent);

            startForeground(NOTIFICATION_ID, mNotification.build());
        }
    }

    private void uploadFinished() {
        synchronized (notificationSyncObject) {
            stopForeground(true);
            if (mNotification.mActions != null) {
                mNotification.mActions.clear();
            }
            mNotification.setContentText(getString(R.string.upload_finished_message));
            mNotification.setProgress(0, 0, false);
            mNotification.setOngoing(false);
            mNotificationManager.notify(NOTIFICATION_ID, mNotification.build());
        }
    }

    private void initNotificationIfNeeded() {
        synchronized (notificationSyncObject) {
            if (mHandler == null) {
                mHandler = new Handler(Looper.getMainLooper());
            }
            if (mNotification == null) {
                Intent intent = new Intent(this, MainActivity.class);
                PendingIntent pnextIntent = PendingIntent.getActivity(this, 0, intent, 0);
                mNotification = new NotificationCompat.Builder(this).setContentTitle(getString(R.string.app_short_name))
                        .setContentText(getString(R.string.uploading_images_message)).setSmallIcon(R.drawable.ic_upload_white).setOngoing(false)
                        .setAutoCancel(false).setProgress(100, 0, false).setColor(getResources().getColor(R.color.accent_material_dark_1)).setWhen(0)
                        .setContentIntent(pnextIntent);
            }

            if (mNotification.mActions != null) {
                mNotification.mActions.clear();
            }
            if (isUploading()) {
                Intent serviceIntent = new Intent(getApplicationContext(), UploadHandlerService.class);
                serviceIntent.putExtra(FLAG_PAUSE, true);
                PendingIntent pauseIntent = PendingIntent.getService(getApplicationContext(), 0, serviceIntent, 0);

                if (!mUploadManager.isIndexing()) {
                    mNotification.addAction(mUploadManager.isPaused() ? R.drawable.ic_play_arrow_black_36dp : R.drawable.ic_pause_black_36dp,
                            mUploadManager.isPaused() ? getString(R

                                    .string.resume_label) : getString(R.string.pause_label),
                            pauseIntent);
                }
                //                Intent intent = new Intent(getApplicationContext(), UploadHandlerService.class);
                //                intent.putExtra(FLAG_CANCEL, true);
                //                PendingIntent cancelIntent = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
                //                mNotification.addAction(R.drawable.ic_clear_black_36dp, "Cancel", cancelIntent);
            }
            if (mNotificationManager == null) {
                mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
            }
        }
    }

    private void updateNotification(long total, long remaining) {
        initNotificationIfNeeded();
        int progress = total > 0 ? (int) (((total - remaining) * 100L) / total) : 0;
        if (total > 0 && mLastProgress <= progress) {
            if (needsToSetNotification || remaining == 0) {
                needsToSetNotification = false;
                Log.d(TAG, "updateNotification: progress: " + progress);
                synchronized (notificationSyncObject) {
                    mLastProgress = progress;
                    mNotification.setProgress(100, mLastProgress, false);
                    mNotification.setContentText(getString(R.string.uploading_images) + ((total - remaining) * 100) / total + "%");
                }
                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        try {

                            synchronized (notificationSyncObject) {
                                mNotificationManager.notify(NOTIFICATION_ID, mNotification.build());
                            }
                        } catch (Exception ignored) {
                            Log.w(TAG, "updateNotification: " + ignored.getLocalizedMessage());
                        }
                        needsToSetNotification = true;
                    }
                }, 1000);
            }
        }
    }

    private boolean isUploading() {
        return mUploadManager != null && mUploadManager.isUploading();
    }

    public class UploadHandlerBinder extends Binder {

        public UploadHandlerService getService() {
            return UploadHandlerService.this;
        }
    }
}
