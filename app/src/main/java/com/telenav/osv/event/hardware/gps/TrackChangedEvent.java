package com.telenav.osv.event.hardware.gps;

import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.event.OSVStickyEvent;
import java.util.ArrayList;

/**
 * Created by Kalman on 19/04/2017.
 */

public class TrackChangedEvent extends OSVStickyEvent {

  public final ArrayList<SKCoordinate> track;

  public TrackChangedEvent(ArrayList<SKCoordinate> track) {
    this.track = track;
  }

  @Override
  public Class getStickyClass() {
    return TrackChangedEvent.class;
  }
}
