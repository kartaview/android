package com.telenav.osv.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Kalman on 10/14/2015.
 */
public class BluetoothBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //here, check that the network connection is available. If yes, start your service. If not, stop your service.
        Intent intent2 = new Intent(context, CameraHandlerService.class);
        intent2.putExtra(CameraHandlerService.FLAG_BLUETOOTH, true);
        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(intent2);
    }
}