package com.telenav.osv.command;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 16/11/2016.
 */

public class LogoutCommand extends OSVStickyEvent {

  @Override
  public Class getStickyClass() {
    return LogoutCommand.class;
  }
}
