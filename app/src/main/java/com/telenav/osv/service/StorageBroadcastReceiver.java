package com.telenav.osv.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by Kalman on 10/14/2015.
 */
public class StorageBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "StorageBcReceiver";//handle storage

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_MEDIA_REMOVED:
            case Intent.ACTION_MEDIA_UNMOUNTED:
            case Intent.ACTION_MEDIA_BAD_REMOVAL:
            case Intent.ACTION_MEDIA_EJECT:
                Log.w(TAG, "onReceive: " + intent.getAction());
                Utils.checkSDCard(context);
                break;
        }
    }
}