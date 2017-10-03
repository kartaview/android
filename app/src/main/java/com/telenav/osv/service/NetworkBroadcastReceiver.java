package com.telenav.osv.service;

import javax.inject.Inject;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import com.telenav.osv.data.DynamicPreferences;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.manager.Recorder;
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

    private final Recorder mRecorder;

    private DynamicPreferences appPrefs;

    private UploadManager mUploadManager;

    @Inject
    public NetworkBroadcastReceiver(Context context, Recorder recorder, DynamicPreferences appPrefs, UploadManager uploadManager) {
        this.mRecorder = recorder;
        this.appPrefs = appPrefs;
        this.mUploadManager = uploadManager;
        this.mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            final boolean autoSet = appPrefs.isAutoUploadEnabled();
            final boolean dataSet = appPrefs.isDataUploadEnabled();
            final boolean localFilesExist = LocalSequence.getStaticSequences().size() != 0;
            boolean isWifi = NetworkUtils.isWifiInternetAvailable(mContext);
            boolean isNet = NetworkUtils.isInternetAvailable(mContext);
            boolean charging = Utils.isCharging(mContext);
            boolean needsCharging = appPrefs.isChargingUploadEnabled();
            Log.d(TAG, "Network status has changed." + " action = " + intent.getAction() + " isNet = " + isNet + ", isWifi = " + isWifi +
                    ", dataSet = " + dataSet + ", autoSet =" + " " + autoSet + ", localFilesExist = " + localFilesExist + ", charging = " + charging +
                    ", needsCharging = " + needsCharging);
            if (UploadManager.isUploading()) {
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
                    if (localFilesExist && !mRecorder.isRecording()) {
                        mUploadManager.uploadCache(LocalSequence.getStaticSequences().values());
                    }
                }
            }
        }
    }
}
