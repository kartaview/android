package com.telenav.osv.service;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.CameraPermissionEvent;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.utils.Log;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Service handling camera connection
 * Created by Kalman on 10/20/2015.
 */
public class CameraHandlerService extends Service {

  private static final String TAG = "CameraHandlerService";

  private static final int NOTIFICATION_ID = 114;

  private final CameraHandlerBinder mBinder = new CameraHandlerBinder();

  private Recorder mRecorder;

  @Override
  public void onCreate() {
    super.onCreate();
    OSVApplication app = ((OSVApplication) getApplication());
    while (!app.isReady()) {
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
        Log.d(TAG, "onStartCommand: " + Log.getStackTraceString(e));
      }
    }
    mRecorder = ((OSVApplication) getApplication()).getRecorder();
    mRecorder.connectLocation();
    EventBus.register(this);
  }

  @Override
  public void onDestroy() {
    EventBus.unregister(this);
    mRecorder.closeCamera();
    if (mRecorder != null) {
      mRecorder.disconnectLocation();
    }
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    setupCamera();
    return mBinder;
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
    Notification notification = new NotificationCompat.Builder(this).setContentTitle(getString(R.string.app_short_name))
        .setContentText(getString(R.string.notification_sequence_recording_label)).setSmallIcon(R.drawable.ic_recording_pin)
        .setOngoing(true).setWhen(0).setContentIntent(pnextIntent).build();
    startForeground(NOTIFICATION_ID, notification);
  }

  private void recordingStopped() {
    stopForeground(true);
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
  public void onRecordingStatusChanged(RecordingEvent event) {
    if (event.started) {
      recordingStarted();
    } else {
      recordingStopped();
    }
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
