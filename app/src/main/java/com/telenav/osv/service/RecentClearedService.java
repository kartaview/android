package com.telenav.osv.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 13/12/2016.
 */

public class RecentClearedService extends Service {

    private static final String TAG = "RecentClearedService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service Started");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy:");
    }

    public void onTaskRemoved(Intent rootIntent) {
        Log.e(TAG, "onTaskRemoved:");
        //Code here
        Recorder recorder = ((OSVApplication) getApplication()).getRecorder();
        if (recorder != null && recorder.isRecording()) {
            recorder.stopSequence(true);
            ((OSVApplication) getApplication()).getAppPrefs().saveBooleanPreference(PreferenceTypes.K_CRASHED, false);
            ((OSVApplication) getApplication()).getAppPrefs().saveBooleanPreference(PreferenceTypes.K_SHOW_CLEAR_RECENTS_WARNING, true);
        }
        stopSelf();
    }
}