package com.telenav.osv.listener;

/**
 * Created by Kalman on 1/12/16.
 */
public interface RecordingStateChangeListener {
    void onRecordingStatusChanged(boolean started);
}
