package com.telenav.osv.event.network;

import com.telenav.osv.event.OSVStickyEvent;
import com.telenav.osv.item.AccountData;

/**
 * Created by Kalman on 16/11/2016.
 */
public class LoginChangedEvent extends OSVStickyEvent {

  public final boolean logged;

  public final AccountData accountData;

  public LoginChangedEvent(boolean logged, AccountData userInfo) {
    this.logged = logged;
    this.accountData = userInfo;
  }

  @Override
  public Class getStickyClass() {
    return LoginChangedEvent.class;
  }
}
