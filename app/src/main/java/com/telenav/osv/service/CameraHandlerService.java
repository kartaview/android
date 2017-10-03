package com.telenav.osv.service;

import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.CameraPermissionEvent;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.utils.Log;
import dagger.android.AndroidInjection;

/**
 * Service handling camera connection
 * Created by Kalman on 10/20/2015.
 */
public class CameraHandlerService extends Service {

    private static final String TAG = "CameraHandlerService";

    private static final int NOTIFICATION_ID = 114;

    private static final String NOTIFICATION_CHANNEL_RECORD = "notificationChannelRecord";

    private static final String NOTIFICATION_CHANNEL_RECORD_NAME = "Record Channel";

    private final CameraHandlerBinder mBinder = new CameraHandlerBinder();

    @Inject
    Recorder mRecorder;

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
        OSVApplication app = ((OSVApplication) getApplication());
        while (!app.isReady()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Log.d(TAG, "onStartCommand: " + Log.getStackTraceString(e));
            }
        }
        mRecorder.connectLocation();
        EventBus.register(this);
    }

    @Override
    public void onDestroy() {
        EventBus.unregister(this);
        mRecorder.closeCamera();
        mRecorder.disconnectLocation();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        setupCamera();
        return mBinder;
    }

    public void initChannels() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));

        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_RECORD,
                NOTIFICATION_CHANNEL_RECORD_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Channel used for the recording progress notification.");
        mNotificationManager.createNotificationChannel(channel);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onRecordingStatusChanged(RecordingEvent event) {
        if (event.started) {
            recordingStarted();
        } else {
            recordingStopped();
        }
    }

    private void setupCamera() {
        // Setup the Camera hardware and preview
        mRecorder.closeCamera();
        //        mWifiCamManager = new WifiCamManager(application);
        int cameraPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (cameraPermitted == PackageManager.PERMISSION_DENIED) {
            EventBus.postSticky(new CameraPermissionEvent());
        } else {
            mRecorder.openCamera();
        }
    }

    private void recordingStarted() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.K_OPEN_CAMERA, true);
        PendingIntent pnextIntent = PendingIntent.getActivity(this, 0, intent, 0);
        initChannels();
        Notification notification =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_RECORD).setContentTitle(getString(R.string.app_short_name))
                        .setContentText(getString(R.string.notification_sequence_recording_label)).setSmallIcon(R.drawable.ic_recording_pin)
                        .setOngoing(true).setWhen(0).setContentIntent(pnextIntent).build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void recordingStopped() {
        stopForeground(true);
    }

    //    public void setSignDetectedListener(SignDetectedListener signDetectedListener) {
    //        if (mCamManager != null) {
    //            mCamManager.setSignDetectedListener(signDetectedListener);
    //        }
    //    }

    public class CameraHandlerBinder extends Binder {

        public CameraHandlerService getService() {
            return CameraHandlerService.this;
        }
    }
}
