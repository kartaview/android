package com.telenav.osv.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.telenav.osv.data.ApplicationPreferences;
import com.telenav.osv.data.DynamicPreferences;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * receives bootup event
 * Created by Kalman on 23/05/2017.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "BootBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            DynamicPreferences appPrefs = new Preferences(new ApplicationPreferences(context));
            if (Utils.folderSize(Utils.generateOSVFolder(context, appPrefs)) > 5 * 1024 * 1024) {
                final boolean autoSet = appPrefs.isAutoUploadEnabled();
                Log.d(TAG, "Network status has changed.");
                if (autoSet) {
                    UploadJobService.scheduleAutoUpload(context, appPrefs);
                }
            }
        }
    }
}
