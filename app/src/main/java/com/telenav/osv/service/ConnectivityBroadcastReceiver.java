package com.telenav.osv.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;

/**
 * broadcast receiver for network connection events
 * Created by Kalman on 10/14/2015.
 */
public class ConnectivityBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ConnectivityBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION) || intent.getAction().equals(Intent
                .ACTION_BATTERY_CHANGED))) {
            Log.d(TAG, "onReceive: " + intent.getAction());
            ApplicationPreferences appPrefs = new ApplicationPreferences(context);
            final boolean autoSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO, false);
            final boolean dataSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED, false);
            boolean isWifi = NetworkUtils.isWifiInternetAvailable(context);
            boolean isNet = NetworkUtils.isInternetAvailable(context);
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean charging = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
            boolean needsCharging = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_CHARGING);
            if (isNet && autoSet && (dataSet || isWifi) && (!needsCharging || charging)) {
                Intent intent2 = new Intent(context, UploadHandlerService.class);
                intent2.putExtra(UploadHandlerService.FLAG_NETWORK, true);
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startService(intent2);
            }
        }
    }
}