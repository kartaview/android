package com.telenav.osv.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.telenav.osv.R;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.obd.ObdActivity;
import com.telenav.osv.recorder.RecorderManager;
import com.telenav.osv.ui.fragment.camera.preview.CameraPreviewFragment;
import com.telenav.osv.utils.Log;

import io.reactivex.disposables.Disposable;

/**
 * Service handling camera connection
 * Created by Kalman on 10/20/2015.
 */
public class CameraHandlerService extends Service implements CameraPreviewFragment.BackgroundLifecycleListener {

    /**
     * Intent extra used when opening app from recording notification
     * to go directly to camera view
     */
    public static final String K_OPEN_CAMERA = "open_camera";

    /**
     * The value for notification id.
     */
    private static final int NOTIFICATION_ID = 114;

    /**
     * The value for an error notification id.
     */
    private static final int ERROR_NOTIFICATION_ID = 115;

    /**
     * The value for channel id. This is required for API level 26 and higher
     * and is ignored by older versions.
     */
    private static final String CHANNEL_ID = "CameraService";

    /**
     * The value for channel id for an error notification.
     * This is required for API level 26 and higher
     * and is ignored by older versions.
     */
    private static final String ERROR_CHANNEL_ID = "ErrorRecording";

    private static String TAG = CameraHandlerService.class.getSimpleName();

    /**
     * Instance of the {@code Recorder} component.
     */
    private RecorderManager mRecorder;

    /**
     * Disposable object used to listen for recording events.
     */
    private Disposable recorderDisposable;

    /**
     * Disposable object used to listen for recording errors events.
     */
    private Disposable recorderErrorDisposable;

    /**
     * Instance to the service {@code Binder}.
     */
    private CameraServiceBinder binder;

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        mRecorder.stopRecording();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mRecorder = ((KVApplication) getApplication()).getRecorder();
        if (mRecorder.isRecording()) {
            startForeground(NOTIFICATION_ID, getRecordingNotification());
        }
        recorderDisposable = mRecorder.getRecordingStateObservable().subscribe(isRecordingStarted -> {
            if (isRecordingStarted) {
                startForeground(NOTIFICATION_ID, getRecordingNotification());
            } else {
                stopForeground(true);
            }
        });
        binder = new CameraServiceBinder();
    }

    @Override
    public void onResume() {
        disposeRecorderError();
    }

    @Override
    public void onPause() {
        recorderErrorDisposable = mRecorder.getRecordingErrorObservable().subscribe(e -> showRecordingErrorNotification());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mRecorder.isRecording()) {
            mRecorder.stopRecording();
        }
        stopForeground(true);
        if (recorderDisposable != null && !recorderDisposable.isDisposed()) {
            recorderDisposable.dispose();
        }
        disposeRecorderError();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void disposeRecorderError() {
        if (recorderErrorDisposable != null && !recorderErrorDisposable.isDisposed()) {
            recorderErrorDisposable.dispose();
        }
    }

    /**
     * Displays a notification with an error message.
     */
    private void showRecordingErrorNotification() {
        Intent intent = ObdActivity.newIntent(this, ObdActivity.SESSION_RECORDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ERROR_CHANNEL_ID)
                .setContentTitle(getString(R.string.recording_something_went_wrong))
                .setContentText(getString(R.string.recording_start_new_recording))
                .setSmallIcon(R.drawable.ic_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(ERROR_CHANNEL_ID, getString(R.string.recording_something_went_wrong), NotificationManager
                    .IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setLightColor(Color.YELLOW);
            channel.enableVibration(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                notificationManager.notify(ERROR_NOTIFICATION_ID, notificationBuilder.build());
            }
        } else {
            notificationBuilder.setLights(Color.YELLOW, 500, 500);
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND);
            NotificationManagerCompat.from(this).notify(ERROR_NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    /**
     * Creates a notification for the recording session.
     * @return an instance of {@code Notification}.
     */
    private Notification getRecordingNotification() {
        Intent intent = ObdActivity.newIntent(this, ObdActivity.SESSION_RECORDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_short_name))
                .setContentText(getString(R.string.notification_sequence_recording_label))
                .setSmallIcon(R.drawable.ic_recording_pin)
                .setOngoing(true)
                .setWhen(0)
                .setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.notification_sequence_recording_label), NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        return notificationBuilder.build();
    }

    /**
     * Binder class used to retrieve the instance of the service.
     */
    public class CameraServiceBinder extends Binder {
        public CameraHandlerService getService() {
            return CameraHandlerService.this;
        }
    }
}
