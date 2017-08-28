package com.telenav.osv.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by Kalman on 23/05/2017.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {

  private static final String TAG = "BootBroadcastReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
      if (Utils.folderSize(Utils.generateOSVFolder(context)) > 5 * 1024 * 1024) {
        ApplicationPreferences appPrefs = new ApplicationPreferences(context);
        final boolean autoSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO, false);
        Log.d(TAG, "Network status has changed.");
        if (autoSet) {
          UploadManager.scheduleAutoUpload(context);
        }
      }
    }
  }
}
