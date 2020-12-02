package com.telenav.osv.activity;

import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.common.Injection;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.upload.UploadManager;
import com.telenav.osv.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract activity used in the project
 * Created by Kalman on 26/09/16.
 * This should not be used for new implementation, over time is intended to be replaced and deleted.
 */
@Deprecated
public abstract class OSVActivity extends KVActivityTempBase {

    private static final String TAG = "OSVActivity";

    public UploadManager uploadManager;

    ApplicationPreferences appPrefs;

    UserDataManager mUserDataManager;

    /**
     * Holder of location permission listeners which will be notified when the location permissions are granted.
     */
    List<LocationPermissionsListener> locationPermissionsListeners = new ArrayList<>();

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

    public abstract KVApplication getApp();

    public abstract int getCurrentScreen();

    public abstract void resolveLocationProblem();
    public abstract void resolveRecordingProblem();

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

    public UserDataManager getUserDataManager() {
        if (mUserDataManager == null) {
            mUserDataManager = new UserDataManager(this, Injection.provideUserRepository(this.getApplicationContext()));
        }
        return mUserDataManager;
    }

    public boolean hasPosition() {
        return false;
    }

    /**
     * Adds a new listener in {@link #locationPermissionsListeners}.
     * @param listener the listener to be added.
     */
    public void addLocationPermissionListener(LocationPermissionsListener listener) {
        locationPermissionsListeners.add(listener);
    }

    /**
     * Notifies the location permission listeners when the location permission is granted.
     */
    public void notifyLocationPermissionListenersGranted() {
        for (LocationPermissionsListener listener : locationPermissionsListeners) {
            listener.onLocationPermissionGranted();
        }
    }

    /**
     * Notifies the location permission listeners when the location permission is granted.
     */
    public void notifyLocationPermissionListenersDenied() {
        for (LocationPermissionsListener listener : locationPermissionsListeners) {
            listener.onLocationPermissionDenied();
        }
    }

    protected abstract void showSnackBar(final CharSequence text, final int duration, final CharSequence button, final Runnable onClick);
}
