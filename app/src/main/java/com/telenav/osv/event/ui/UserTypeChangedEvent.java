package com.telenav.osv.event.ui;

import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by kalmanb on 7/17/17.
 */
public class UserTypeChangedEvent extends OSVStickyEvent {

  public final int type;

  public UserTypeChangedEvent(int type) {
    this.type = type;
  }

  @Override
  public Class getStickyClass() {
    return UserTypeChangedEvent.class;
  }

  public boolean isDriver() {
    return type == PreferenceTypes.USER_TYPE_BAU || type == PreferenceTypes.USER_TYPE_BYOD || type == PreferenceTypes.USER_TYPE_DEDICATED;
  }
}
