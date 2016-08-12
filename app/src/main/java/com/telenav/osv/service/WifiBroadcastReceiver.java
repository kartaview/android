package com.telenav.osv.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Kalman on 10/14/2015.
 */
public class WifiBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //here, check that the network connection is available. If yes, start your service. If not, stop your service.
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        Intent intent2 = new Intent(context, UploadHandlerService.class);
        intent2.putExtra(UploadHandlerService.FLAG_NETWORK, true);
        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(intent2);
    }
}