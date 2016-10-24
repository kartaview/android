package com.telenav.osv.activity;

import java.util.ArrayList;
import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import com.google.android.gms.common.api.Status;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 26/09/16.
 */

public abstract class OSVActivity extends AppCompatActivity {

    private static final String TAG = "OSVActivity";

    public CameraHandlerService mCameraHandlerService;

    public abstract void openScreen(int screenRecording);

    public abstract OSVApplication getApp();

    public abstract int getCurrentScreen();

    public abstract void resolveLocationProblem(boolean b);

    public abstract void showSnackBar(CharSequence s, int lengthShort);

    public abstract String getCurrentFragment();

    public abstract void showSnackBar(int tip_map_screen, int lengthLong, int got_it_label, Runnable runnable);

    public abstract void showSnackBar(int tip_map_screen, int lengthLong);

    public abstract void continueAfterCrash();

    public abstract void switchPreviews();

    public abstract void enableProgressBar(boolean b);

    public abstract void openScreen(int screenNearby, Object extra);

    public abstract void setLocationResolution(Status status);

    public abstract void resizeHolderStatic(float v, boolean b);


    public boolean checkPermissionsForRecording() {
        Log.d(TAG, "checkPermissionsForRecording: ");
        ArrayList<String> needed = new ArrayList<>();
        int cameraPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int locationPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int storagePermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (cameraPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.CAMERA);
        }
        if (locationPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (storagePermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (needed.size() > 0){
            String[] array = new String[needed.size()];
            needed.toArray(array);
            ActivityCompat.requestPermissions(this, array, OSVApplication.START_RECORDING_PERMISSION);
            return false;
        }
        return true;
    }

    public boolean checkPermissionsForCamera() {
        Log.d(TAG, "checkPermissionsForCamera: ");
        ArrayList<String> needed = new ArrayList<>();
        int cameraPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (cameraPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.CAMERA);
        }

        if (needed.size() > 0){
            String[] array = new String[needed.size()];
            needed.toArray(array);
            ActivityCompat.requestPermissions(this, array, OSVApplication.CAMERA_PERMISSION);
            return false;
        }
        return true;
    }

    public boolean checkPermissionsForGPS() {
        Log.d(TAG, "checkPermissionsForGPS: ");
        ArrayList<String> needed = new ArrayList<>();
        int locationPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (locationPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (needed.size() > 0){
            String[] array = new String[needed.size()];
            needed.toArray(array);
            ActivityCompat.requestPermissions(this, array, OSVApplication.LOCATION_PERMISSION);
            return false;
        }
        return true;
    }

    public boolean checkPermissionsForGPSWithRationale(@StringRes int message) {
        Log.d(TAG, "checkPermissionsForGPSWithRationale: ");
        final ArrayList<String> needed = new ArrayList<>();
        int locationPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (locationPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (needed.size() > 0){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,needed.get(0))){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                AlertDialog dialog = builder.setMessage(message).setTitle(R.string.permission_request).setNeutralButton(R.string.ok_label,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String[] array = new String[needed.size()];
                                needed.toArray(array);
                                ActivityCompat.requestPermissions(OSVActivity.this, array, OSVApplication.LOCATION_PERMISSION_BT);
                            }
                        }).create();
                dialog.show();
                return false;
            } else {
                String[] array = new String[needed.size()];
                needed.toArray(array);
                ActivityCompat.requestPermissions(this, array, OSVApplication.LOCATION_PERMISSION_BT);
                return false;
            }
        }
        return true;
    }

    public abstract boolean needsCameraPermission();

    public abstract void setNeedsCameraPermission(boolean needs);
}
