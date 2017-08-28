package com.telenav.osv.event.network.upload;

/**
 * Created by Bencze Kalman on 2/12/2017.
 */
public class UploadCancelledEvent extends UploadEvent {

  private long total;

  private long remaining;

  public UploadCancelledEvent(long total, long remaining) {
    this.total = total;
    this.remaining = remaining;
  }
}
