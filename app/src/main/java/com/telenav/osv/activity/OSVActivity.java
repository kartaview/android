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
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.utils.Log;

/**
 * Abstract activity used in the project
 * Created by Kalman on 26/09/16.
 */
public abstract class OSVActivity extends AppCompatActivity/*,SignDetectedListener*/ {

    private static final String TAG = "OSVActivity";

    public UploadManager mUploadManager;

    ApplicationPreferences appPrefs;

    UserDataManager mUserDataManager;

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "onLowMemory: ----------------------------");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        String levelRepr = "";
        switch (level) {
            case TRIM_MEMORY_BACKGROUND:
                levelRepr = "TRIM_MEMORY_BACKGROUND";
                break;
            case TRIM_MEMORY_COMPLETE:
                levelRepr = "TRIM_MEMORY_COMPLETE";
                break;
            case TRIM_MEMORY_MODERATE:
                levelRepr = "TRIM_MEMORY_MODERATE";
                break;
            case TRIM_MEMORY_RUNNING_CRITICAL:
                levelRepr = "TRIM_MEMORY_RUNNING_CRITICAL";
                break;
            case TRIM_MEMORY_RUNNING_LOW:
                levelRepr = "TRIM_MEMORY_RUNNING_LOW";
                break;
            case TRIM_MEMORY_RUNNING_MODERATE:
                levelRepr = "TRIM_MEMORY_RUNNING_MODERATE";
                break;
            case TRIM_MEMORY_UI_HIDDEN:
                levelRepr = "TRIM_MEMORY_UI_HIDDEN";
                break;
        }
        Log.d(TAG, "onTrimMemory: --------------------------- level " + levelRepr);
    }

    public abstract OSVApplication getApp();

    public abstract int getCurrentScreen();

    public abstract void resolveLocationProblem(boolean b);

    public abstract void hideSnackBar();

    public void showSnackBar(final int resId, final int duration) {
        showSnackBar(resId, duration, null, null);
    }

    public void showSnackBar(final CharSequence text, final int duration) {
        showSnackBar(text, duration, null, null);
    }

    public void showSnackBar(final int resId, final int duration, final String button, final Runnable onClick) {
        showSnackBar(getText(resId), duration, button, onClick);
    }

    public void showSnackBar(final int resId, final int duration, final int buttonResId, final Runnable onClick) {
        showSnackBar(getText(resId), duration, getText(buttonResId), onClick);
    }

    public abstract void enableProgressBar(boolean b);

    public abstract boolean isPortrait();

    public void openScreen(int screen) {
        openScreen(screen, null);
    }

    public abstract void openScreen(int screenNearby, Object extra);

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

        if (needed.size() > 0) {
            String[] array = new String[needed.size()];
            needed.toArray(array);
            ActivityCompat.requestPermissions(this, array, OSVApplication.START_RECORDING_PERMISSION);
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

        if (needed.size() > 0) {
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

        if (needed.size() > 0) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, needed.get(0))) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                AlertDialog dialog = builder.setMessage(message).setTitle(R.string.permission_request)
                        .setNeutralButton(R.string.ok_label, new DialogInterface.OnClickListener() {

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

    public UserDataManager getUserDataManager() {
        if (mUserDataManager == null) {
            mUserDataManager = new UserDataManager(this);
        }
        return mUserDataManager;
    }

    public abstract boolean hasPosition();

    protected abstract void showSnackBar(final CharSequence text, final int duration, final CharSequence button, final Runnable onClick);

    void checkPermissionsForCamera() {
        Log.d(TAG, "checkPermissionsForCamera: ");
        ArrayList<String> needed = new ArrayList<>();
        int cameraPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (cameraPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.CAMERA);
        }

        if (needed.size() > 0) {
            String[] array = new String[needed.size()];
            needed.toArray(array);
            ActivityCompat.requestPermissions(this, array, OSVApplication.CAMERA_PERMISSION);
        }
    }
}
