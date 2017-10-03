package com.telenav.osv.service;

import javax.inject.Inject;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.utils.Log;

/**
 * service waiting for the app to be removed from the recent apps panel
 * Created by Kalman on 13/12/2016.
 */
public class RecentClearedService extends Service {

    private static final String TAG = "RecentClearedService";

    @Inject
    Preferences prefs;

    @Inject
    Recorder mRecorder;

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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onTaskRemoved(Intent rootIntent) {
        Log.e(TAG, "onTaskRemoved:");
        if (mRecorder.isRecording()) {
            mRecorder.stopRecording(true);
            prefs.setCrashed(false);
            prefs.setShouldShowClearRecentsWarning(true);
        }
        stopSelf();
    }
}