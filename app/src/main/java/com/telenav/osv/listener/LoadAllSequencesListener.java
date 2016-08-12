package com.telenav.osv.listener;

import com.telenav.osv.ui.fragment.MapFragment;

/**
 * Created by Kalman on 11/18/15.
 */
public interface LoadAllSequencesListener {
    void onRequestFinished(MapFragment.Polyline arrayList, int id);

    void onFinished();
}
