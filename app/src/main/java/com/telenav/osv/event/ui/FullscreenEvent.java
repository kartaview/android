package com.telenav.osv.event.ui; /**
 * Created by Kalman on 13/02/2017.
 */

import com.telenav.osv.event.OSVEvent;

public class FullscreenEvent extends OSVEvent {

  public final static String TAG = "FullscreenEvent";

  public final boolean fullscreen;

  public FullscreenEvent(boolean fullscreen) {
    this.fullscreen = fullscreen;
  }
}
