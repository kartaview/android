package com.telenav.osv.listener;


import com.telenav.osv.item.Polyline;

/**
 * Created by Kalman on 11/18/15.
 */
public interface LoadAllSequencesListener {
    void onRequestFinished(Polyline arrayList, int id);

    void onFinished(Polyline matched);

    void onRequestFailed();

    void onRequestSuccess();
}
