package com.telenav.osv.service;

import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.command.UploadCancelCommand;
import com.telenav.osv.command.UploadPauseCommand;
import com.telenav.osv.data.DynamicPreferences;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.db.SequenceDB;
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
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import dagger.android.AndroidInjection;
import io.fabric.sdk.android.Fabric;

/**
 * scheduled upload task
 * Created by Kalman on 23/05/2017.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class UploadJobService extends JobService implements UploadProgressListener {

    private static final String TAG = "UploadJobService";

    private static final int UPLOAD_JOB_ID = 116119;

    private static final String INVOCATION_TYPE = "invocationType";

    private static final int UPLOAD_AUTO = 0;

    private static final int UPLOAD_ON_DEMAND = 1;

    private static final String FLAG_PAUSE = "pause";

    //private static final String FLAG_CANCEL = "cancel";

    private static final int NOTIFICATION_ID = 113;

    private static final String NOTIFICATION_CHANNEL_UPLOAD = "uploadNotificationChannel";

    private static final String NOTIFICATION_CHANNEL_UPLOAD_NAME = "Upload Channel";

    private final Object notificationSyncObject = new Object();

    @Inject
    Recorder mRecorder;

    @Inject
    UploadManager mUploadManager;

    @Inject
    Preferences appPrefs;

    @Inject
    SequenceDB db;

    @Inject
    NetworkBroadcastReceiver mNetworkBroadcastReceiver;

    private NotificationCompat.Builder mNotification;

    private NotificationManager mNotificationManager;

    private int mLastProgress = -1;

    private JobParameters mParameters;

    private long mLastUpdate = System.currentTimeMillis();

    public static void cancelAutoUpload(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }
        jobScheduler.cancel(UploadJobService.UPLOAD_JOB_ID);
    }

    public static int scheduleAutoUpload(Context context, DynamicPreferences prefs) {
        int result = JobScheduler.RESULT_FAILURE;
        try {
            PersistableBundle bundle = new PersistableBundle();
            bundle.putInt(UploadJobService.INVOCATION_TYPE, UploadJobService.UPLOAD_AUTO);
            final boolean autoSet = prefs.isAutoUploadEnabled();
            final boolean dataEnabled = prefs.isDataUploadEnabled();
            if (autoSet) {
                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                if (jobScheduler == null) {
                    return result;
                }
                jobScheduler.cancel(UploadJobService.UPLOAD_JOB_ID);
                JobInfo.Builder builder = new JobInfo.Builder(UploadJobService.UPLOAD_JOB_ID, new ComponentName(context, UploadJobService.class))
                        .setRequiredNetworkType(dataEnabled ? JobInfo.NETWORK_TYPE_ANY : JobInfo.NETWORK_TYPE_UNMETERED)
                        .setPersisted(true)
                        .setExtras(bundle)
                        .setRequiresCharging(prefs.isChargingUploadEnabled());
                result = jobScheduler.schedule(builder.build());
                Log.d(TAG, "scheduleAutoUpload: scheduled upload task, result = " + result);
            }
        } catch (Exception e) {
            Log.d(TAG, "scheduleAutoUpload: " + Log.getStackTraceString(e));
        }
        return result;
    }

    public static int scheduleImmediateUpload(Context context) {
        int result = JobScheduler.RESULT_FAILURE;
        try {
            PersistableBundle bundle = new PersistableBundle();
            bundle.putInt(UploadJobService.INVOCATION_TYPE, UploadJobService.UPLOAD_ON_DEMAND);
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                return result;
            }
            jobScheduler.cancel(UploadJobService.UPLOAD_JOB_ID);
            JobInfo.Builder builder = new JobInfo.Builder(UploadJobService.UPLOAD_JOB_ID, new ComponentName(context, UploadJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(false)
                    .setExtras(bundle)
                    .setRequiresCharging(false);
            result = jobScheduler.schedule(builder.build());
            Log.d(TAG, "scheduleImmediateUpload: scheduled upload task, result = " + result);
        } catch (Exception e) {
            Log.d(TAG, "scheduleImmediateUpload: " + Log.getStackTraceString(e));
        }
        return result;
    }

    //@Override
    //public int onStartCommand(Intent intent, int flags, int startId) {
    //  super.onStartCommand(intent, flags, startId);
    //  Log.d(TAG, "onStartCommand: ");
    //  int flag = START_NOT_STICKY;
    //  appPrefs = ((OSVApplication) getApplication()).getAppPrefs();
    //  OSVApplication app = ((OSVApplication) getApplication());
    //  while (!app.isReady()) {
    //    try {
    //      Thread.sleep(5);
    //    } catch (InterruptedException e) {
    //      Log.d(TAG, "onStartCommand: " + Log.getStackTraceString(e));
    //    }
    //  }
    //  mUploadManager = ((OSVApplication) getApplication()).getUploadManager();
    //  if (mUploadManager == null) {
    //    return flag;
    //  }
    //  mUploadManager.setUploadProgressListener(UploadJobService.this);
    //  if (intent != null) {
    //    if (intent.getBooleanExtra(FLAG_CANCEL, false)) {
    //      Log.d(TAG, "Upload cancelled.");
    //      mUploadManager.cancelUploadTasks();
    //    } else if (intent.getBooleanExtra(FLAG_PAUSE, false)) {
    //      Log.d(TAG, "Upload paused.");
    //      if (mUploadManager.isPaused()) {
    //        mUploadManager.resumeUpload();
    //      } else {
    //        mUploadManager.pauseUpload();
    //      }
    //    }
    //    if (intent.getBooleanExtra(FLAG_NETWORK, false)) {
    //      final boolean autoSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO, false);
    //      final boolean dataSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED, false);
    //      final boolean localFilesExist = LocalSequence.getStaticSequences().size() != 0;
    //      boolean isWifi = NetworkUtils.isWifiInternetAvailable(UploadJobService.this);
    //      boolean isNet = NetworkUtils.isInternetAvailable(UploadJobService.this);
    //      if (isNet && autoSet && (dataSet || isWifi)) {
    //        if (localFilesExist && !((OSVApplication) getApplication()).getRecorder().isRecording()) {
    //          mUploadManager.uploadCache(LocalSequence.getStaticSequences().values());
    //        }
    //      }
    //    }
    //  }
    //  return flag;
    //}

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
        EventBus.register(this);
        mUploadManager.setUploadProgressListener(UploadJobService.this);
        Log.d(TAG, "onCreate: ");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        mUploadManager.setUploadProgressListener(null);
        mUploadManager.destroy();
        mUploadManager = null;
        super.onDestroy();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        mParameters = params;
        Log.d(TAG, "onStartJob: ");
        BackgroundThreadPool.post(() -> {
            final boolean autoSet = appPrefs.isAutoUploadEnabled();
            final boolean dataSet = appPrefs.isDataUploadEnabled();

            final boolean localFilesExist = LocalSequence.getStaticSequences().size() != 0;
            boolean isWifi = NetworkUtils.isWifiInternetAvailable(UploadJobService.this);
            boolean isNet = NetworkUtils.isInternetAvailable(UploadJobService.this);
            boolean isrecording = mRecorder.isRecording();
            Log.d(TAG, "onStartJob: isnet " + isNet +
                    " iswifi " + isWifi + " localfiles " + localFilesExist + " dataset " + dataSet + " autoset " + autoSet + " isrec " + isrecording);
            if (isNet
                    && (autoSet || params.getExtras().getInt(INVOCATION_TYPE) == UPLOAD_ON_DEMAND)
                    && (dataSet || isWifi)) {
                if (localFilesExist && !isrecording) {
                    mUploadManager.uploadCache(LocalSequence.getStaticSequences().values());
                }
            }
            registerReceiver(mNetworkBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "onStopJob: ");
        finishTasks();
        return false;
    }

    @Override
    public void onUploadStarted(final long mTotalSize, final int remainingSequences, final int numberOfSequences) {
        if (Fabric.isInitialized()) {
            Crashlytics.setBool(Log.UPLOAD_STATUS, true);
        }
        uploadStarted();
        EventBus.postSticky(new UploadStartedEvent(mTotalSize, remainingSequences, numberOfSequences));
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
        EventBus.postSticky(new UploadPausedEvent(true));
    }

    @Override
    public void onUploadResumed() {
        initNotificationIfNeeded();
        synchronized (notificationSyncObject) {
            mNotification.setContentText(getString(R.string.track_upload_resuming));
            mNotification.setOngoing(true);
            mNotificationManager.notify(NOTIFICATION_ID, mNotification.build());
        }
        EventBus.postSticky(new UploadPausedEvent(false));
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
        EventBus.postSticky(new UploadCancelledEvent(total, remaining));
        finishTasks();
        jobFinished(mParameters, true);
    }

    @Override
    public void onUploadFinished() {
        if (Fabric.isInitialized()) {
            Crashlytics.setBool(Log.UPLOAD_STATUS, false);
        }
        initNotificationIfNeeded();
        uploadFinished();
        mLastProgress = -1;
        EventBus.postSticky(new UploadFinishedEvent());
        mUploadManager.resetUploadStats();
        finishTasks();
        jobFinished(mParameters, false);
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
        EventBus.postSticky(new UploadProgressEvent(finalTotalSize, remainingSize));
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
    public void onIndexingSequence(final Sequence sequence, final int remainingUploads, final int numberOfSequences) {
        EventBus.post(new UploadingSequenceEvent(sequence, remainingUploads, numberOfSequences));
    }

    @Override
    public void onBandwidthStateChange(final ConnectionQuality bandwidthState, final double bandwidth) {
        if (UploadManager.sUploadStatus == UploadManager.STATUS_IDLE || UploadManager.sUploadStatus == UploadManager.STATUS_PAUSED) {
            EventBus.post(new UploadBandwidthEvent(ConnectionQuality.UNKNOWN, 0, mUploadManager.getRemainingSizeValue()));
        } else {
            EventBus.post(new UploadBandwidthEvent(bandwidthState, bandwidth, mUploadManager.getRemainingSizeValue()));
        }
    }

    @Subscribe
    public void onUploadCancelled(UploadCancelCommand command) {
        if (mUploadManager != null) {
            if (UploadManager.isUploading()) {
                mUploadManager.cancelUploadTasks();
            }
        }
    }

    @Subscribe
    public void onUploadPause(UploadPauseCommand command) {
        final boolean dataSet = appPrefs.isDataUploadEnabled();
        if (UploadManager.isUploading()) {
            if (mUploadManager.isPaused()) {
                if (NetworkUtils.isInternetAvailable(this)) {
                    if (dataSet || NetworkUtils.isWifiInternetAvailable(this)) {
                        mUploadManager.resumeUpload();
                    } else {
                        Toast.makeText(this, R.string.no_wifi_label, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, R.string.no_internet_connection_label, Toast.LENGTH_SHORT).show();
                }
            } else {
                mUploadManager.pauseUpload();
            }
        }
    }

    public void initChannels() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        if (mNotificationManager == null) {
            mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        }
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_UPLOAD,
                NOTIFICATION_CHANNEL_UPLOAD_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Channel used for the upload progress notification.");
        mNotificationManager.createNotificationChannel(channel);
    }

    private void finishTasks() {
        if (mNetworkBroadcastReceiver != null) {
            unregisterReceiver(mNetworkBroadcastReceiver);
        }
        EventBus.unregister(this);
    }

    private void uploadStarted() {
        synchronized (notificationSyncObject) {
            if (mNotification != null && mNotification.mActions != null) {
                mNotification.mActions.clear();
            }
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pnextIntent = PendingIntent.getActivity(this, 0, intent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            initChannels();
            mNotification =
                    new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_UPLOAD).setContentTitle(getString(R.string.app_short_name))
                            .setContentText(getString(R.string.uploading_images_message))
                            .setSmallIcon(R.drawable.ic_upload_white).setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false)).setOngoing(true)
                            .setAutoCancel(false).setProgress(100, 0, false).setColor(getResources().getColor(R.color.accent_material_dark_1))
                            .setWhen(0)
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
            if (mNotification == null) {
                Intent intent = new Intent(this, MainActivity.class);
                PendingIntent pnextIntent = PendingIntent.getActivity(this, 0, intent, 0);

                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
                initChannels();
                mNotification =
                        new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_UPLOAD).setContentTitle(getString(R.string.app_short_name))
                                .setContentText(getString(R.string.uploading_images_message))
                                .setSmallIcon(R.drawable.ic_upload_white).setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false)).setOngoing(true)
                                .setAutoCancel(false).setProgress(100, 0, false).setColor(getResources().getColor(R.color.accent_material_dark_1))
                                .setWhen(0)
                                .setContentIntent(pnextIntent);
            }

            if (mNotification.mActions != null) {
                mNotification.mActions.clear();
            }
            if (isUploading()) {
                Intent serviceIntent = new Intent(getApplicationContext(), UploadJobService.class);
                serviceIntent.putExtra(FLAG_PAUSE, true);
                PendingIntent pauseIntent = PendingIntent.getService(getApplicationContext(), 0, serviceIntent, 0);

                if (!mUploadManager.isIndexing()) {
                    mNotification.addAction(mUploadManager.isPaused() ? R.drawable.ic_play_arrow_black_36dp : R.drawable.ic_pause_black_36dp,
                            mUploadManager.isPaused() ? getString(R.string.resume_label) : getString(R.string.pause_label),
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
            boolean needsToSetNotification = System.currentTimeMillis() - mLastUpdate > 1000;
            if (needsToSetNotification || remaining == 0) {
                mLastUpdate = System.currentTimeMillis();
                Log.d(TAG, "updateNotification: progress: " + progress);
                synchronized (notificationSyncObject) {
                    mLastProgress = progress;
                    mNotification.setProgress(100, mLastProgress, false);
                    mNotification.setContentText(getString(R.string.uploading_images) + ((total - remaining) * 100) / total + "%");
                }
                synchronized (notificationSyncObject) {
                    mNotificationManager.notify(NOTIFICATION_ID, mNotification.build());
                }
            }
        }
    }

    private boolean isUploading() {
        return mUploadManager != null && UploadManager.isUploading();
    }
}
