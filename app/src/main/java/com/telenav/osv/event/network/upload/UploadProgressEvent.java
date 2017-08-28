package com.telenav.osv.event.network.upload;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Bencze Kalman on 2/12/2017.
 */
public class UploadProgressEvent extends OSVStickyEvent {

  public long total;

  public long remaining;

  public UploadProgressEvent(long total, long remaining) {
    this.total = total;
    this.remaining = remaining;
  }

  @Override
  public Class getStickyClass() {
    return UploadProgressEvent.class;
  }
}
