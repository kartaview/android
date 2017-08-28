package com.telenav.osv.event.network.matcher;

import com.telenav.osv.event.OSVStickyEvent;
import com.telenav.osv.item.Segment;

/**
 * Created by Kalman on 14/11/2016.
 */

public class MatchedSegmentEvent extends OSVStickyEvent {

  public final Segment segment;

  public MatchedSegmentEvent(Segment matched) {
    this.segment = matched;
  }

  @Override
  public Class getStickyClass() {
    return MatchedSegmentEvent.class;
  }
}
