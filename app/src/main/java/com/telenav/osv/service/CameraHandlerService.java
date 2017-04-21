package com.telenav.osv.service;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.command.ObdCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.CameraPermissionEvent;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.obd.ObdManager;
import com.telenav.osv.obd.BLEConnection;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 10/20/2015.
 */
public class CameraHandlerService extends Service {

    public static final String FLAG_BLUETOOTH = "eventBluetooth";

    private static final String TAG = "CameraHandlerService";

    private static final int NOTIFICATION_ID = 114;

    private final CameraHandlerBinder mBinder = new CameraHandlerBinder();

    public boolean mCameraReady = false;

    public boolean mCameraFailed = false;

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        setupCamera();
        return mBinder;
    }

    protected void setupCamera() {
        // Setup the Camera hardware and preview
        mRecorder.closeCamera();
//        mWifiCamManager = new WifiCamManager(application);
        mCameraFailed = false;
        mCameraReady = false;
        int cameraPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (cameraPermitted == PackageManager.PERMISSION_DENIED) {
            EventBus.postSticky(new CameraPermissionEvent());
        } else {
            mRecorder.openCamera();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        OSVApplication app = ((OSVApplication) getApplication());
        int flag = START_NOT_STICKY;
        if (intent != null) {
            while (!app.isReady()) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Log.d(TAG, "onStartCommand: " + Log.getStackTraceString(e));
                }
            }
            if (!intent.getBooleanExtra(FLAG_BLUETOOTH, false)) {
                return flag;
            }
            ObdManager obdManager = app.getRecorder().getOBDManager();
            if (!obdManager.isBluetooth()) {
                return flag;
            }
            BluetoothAdapter adapter;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                BluetoothManager bm = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
                adapter = bm.getAdapter();
            } else {
                adapter = BluetoothAdapter.getDefaultAdapter();
            }
            if (adapter.isEnabled()) {
                if (obdManager.isBle()) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        return flag;
                    }

                    // Use this check to determine whether BLE is supported on the device. Then
                    // you can selectively disable BLE-related features.
                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                        return flag;
                    }

                    BLEConnection.getInstance().initConnection(this);

                    SharedPreferences preferences = getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
                    if (preferences.getBoolean(Constants.BLE_SERVICE_STARTED, false) && preferences.getString(Constants.EXTRAS_BLE_DEVICE_ADDRESS, null) != null) {
                        EventBus.postSticky(new ObdCommand(true));
                        return START_STICKY;
                    } else {
                        return flag;
                    }
                } else {
                    // Use this check to determine whether BT is supported on the device. Then
                    // you can selectively disable BT-related features.
                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                        return flag;
                    }

                    SharedPreferences preferences = getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
                    if (preferences.getBoolean(Constants.BT_SERVICE_STARTED, false) && preferences.getString(Constants.EXTRAS_BT_DEVICE_ADDRESS, null) != null) {
                        EventBus.postSticky(new ObdCommand(true));
                        return START_STICKY;
                    }
                }
            }
        }
        return flag;
    }


    public void recordingStarted() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.K_OPEN_CAMERA, true);
        PendingIntent pnextIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_short_name))
                .setContentText(getString(R.string.notification_sequence_recording_label))
                .setSmallIcon(R.drawable.ic_recording_pin)
                .setOngoing(true)
                .setWhen(0)
                .setContentIntent(pnextIntent).build();
        startForeground(NOTIFICATION_ID, notification);
    }


    public void recordingStopped() {
        stopForeground(true);
    }

    @Override
    public void onDestroy() {

        mRecorder.closeCamera();

        if (mRecorder != null) {
            mRecorder.disconnectLocation();
        }
        super.onDestroy();
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
