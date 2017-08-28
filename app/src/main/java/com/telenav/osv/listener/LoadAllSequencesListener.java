package com.telenav.osv.listener;

import com.telenav.osv.item.Polyline;
import java.util.List;

/**
 * Created by Kalman on 11/18/15.
 */
public interface LoadAllSequencesListener {

  void onRequestFinished(List<Polyline> segments);

  void onRequestFailed();

  void onRequestSuccess();
}
