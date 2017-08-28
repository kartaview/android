package com.telenav.osv.event.hardware.camera;

import com.telenav.osv.event.OSVStickyEvent;
import com.telenav.osv.item.LocalSequence;
import javax.annotation.Nullable;

/**
 * Created by Kalman on 07/11/2016.
 */

public class RecordingEvent extends OSVStickyEvent {

  public final boolean started;

  public LocalSequence sequence;

  public RecordingEvent(@Nullable LocalSequence sequence, boolean started) {
    this.started = started;
    this.sequence = sequence;
  }

  @Override
  public Class getStickyClass() {
    return RecordingEvent.class;
  }
}
