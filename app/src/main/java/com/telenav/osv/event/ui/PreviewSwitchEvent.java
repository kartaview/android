package com.telenav.osv.event.ui; /**
 * Created by Kalman on 15/02/2017.
 */

import com.telenav.osv.event.OSVEvent;

public class PreviewSwitchEvent extends OSVEvent {

  public final static String TAG = "PreviewSwitchEvent";

  public final boolean cameraTapped;

  public PreviewSwitchEvent(boolean cameraTapped) {
    this.cameraTapped = cameraTapped;
  }
}
