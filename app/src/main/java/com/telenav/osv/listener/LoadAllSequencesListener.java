package com.telenav.osv.listener;


import java.util.List;
import com.telenav.osv.item.Polyline;

/**
 * Created by Kalman on 11/18/15.
 */
public interface LoadAllSequencesListener {
    void onRequestFinished(List<Polyline> segments);

    void onRequestFailed();

    void onRequestSuccess();
}
