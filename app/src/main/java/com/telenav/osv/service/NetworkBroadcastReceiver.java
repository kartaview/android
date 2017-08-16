package com.telenav.osv.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;

/**
 * Network broadcasts receiver
 * Created by kalmanb on 7/10/17.
 */
public class NetworkBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkBroadcastReceiver";

    private final Context mContext;

    private ApplicationPreferences appPrefs;

    private UploadManager mUploadManager;

    public NetworkBroadcastReceiver(Context context, ApplicationPreferences appPrefs, UploadManager uploadManager) {
        this.appPrefs = appPrefs;
        this.mUploadManager = uploadManager;
        this.mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            final boolean autoSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO, false);
            final boolean dataSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED, false);
            final boolean localFilesExist = LocalSequence.getStaticSequences().size() != 0;
            boolean isWifi = NetworkUtils.isWifiInternetAvailable(mContext);
            boolean isNet = NetworkUtils.isInternetAvailable(mContext);
            boolean charging = Utils.isCharging(mContext);
            boolean needsCharging = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_CHARGING);
            Log.d(TAG, "Network status has changed." + " action = " + intent.getAction() + " isNet = " + isNet + ", isWifi = " + isWifi + ", dataSet = " + dataSet + ", autoSet =" +
                    " " + autoSet
                    + ", localFilesExist = " + localFilesExist + ", charging = " + charging + ", needsCharging = " + needsCharging);
            if (mUploadManager.isUploading()) {
                if (mUploadManager.isPaused()) {
                    if (autoSet) {
                        if (isWifi || (dataSet && isNet)) {
                            mUploadManager.resumeUpload();
                        }
                    }
                } else {
                    if (!isNet || (!dataSet && !isWifi)) {
                        mUploadManager.pauseUpload();
                    }
                }
            } else {
                if (isNet && autoSet && (dataSet || isWifi) && (!needsCharging || charging)) {
                    if (localFilesExist && !((OSVApplication) mContext.getApplicationContext()).getRecorder().isRecording()) {
                        mUploadManager.uploadCache(LocalSequence.getStaticSequences().values());
                    }
                }
            }
        }
    }
}
