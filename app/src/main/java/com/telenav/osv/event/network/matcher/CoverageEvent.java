package com.telenav.osv.event.network.matcher;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 07/12/2016.
 */
public class CoverageEvent extends OSVStickyEvent {

  public final boolean available;

  public CoverageEvent(boolean available) {
    this.available = available;
  }

  @Override
  public Class getStickyClass() {
    return CoverageEvent.class;
  }
}
