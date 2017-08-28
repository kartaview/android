package com.telenav.osv.event.ui;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 21/02/2017.
 */
public class SequencesChangedEvent extends OSVStickyEvent {

  public final static String TAG = "SequencesChangedEvent";

  public final boolean online;

  public boolean diskChange;

  public int deletedSequenceId = 0;

  public SequencesChangedEvent(boolean online) {
    this.online = online;
  }

  public SequencesChangedEvent(boolean online, boolean diskChange) {
    this.online = online;
    this.diskChange = diskChange;
  }

  public SequencesChangedEvent(boolean online, int id) {
    this.online = online;
    this.deletedSequenceId = id;
  }

  @Override
  public Class getStickyClass() {
    return SequencesChangedEvent.class;
  }
}
