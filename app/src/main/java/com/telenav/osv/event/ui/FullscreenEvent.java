package com.telenav.osv.event.ui; /**
 * Created by Kalman on 13/02/2017.
 */

import com.telenav.osv.event.OSVEvent;

public class FullscreenEvent extends OSVEvent {

  public static final String TAG = "FullscreenEvent";

  public final boolean fullscreen;

  public FullscreenEvent(boolean fullscreen) {
    this.fullscreen = fullscreen;
  }
}
