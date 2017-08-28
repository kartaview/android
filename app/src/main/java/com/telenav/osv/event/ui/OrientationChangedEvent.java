package com.telenav.osv.event.ui;

import com.telenav.osv.event.OSVEvent;

/**
 * Created by Kalman on 31/01/2017.
 */
public class OrientationChangedEvent extends OSVEvent {

  public final static String TAG = "OrientationChangedEvent";

  public int orientation;

  public OrientationChangedEvent() {
  }
}
