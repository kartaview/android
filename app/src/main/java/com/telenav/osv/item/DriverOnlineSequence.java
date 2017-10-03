package com.telenav.osv.item;

import java.util.Date;

/**
 * Created by kalmanb on 7/11/17.
 */
public class DriverOnlineSequence extends OnlineSequence {

  private static final String TAG = "DriverOnlineSequence";

  public DriverOnlineSequence(int sequenceId, Date date, int originalImageCount, String address, String thumbLink, boolean obd,
                              String platform, String platformVersion, String appVersion, int distance, double value, String currency) {
    super(sequenceId, date, originalImageCount, address, thumbLink, obd, platform, platformVersion, appVersion, distance, value);
    this.mCurrency = currency;
  }

  @Override
  public boolean isUserTrack() {
    return false;
  }

  @Override
  public boolean hasValue() {
    return getCurrency() != null && !"".equals(getCurrency()) &&
        !getServerStatus().equals(SERVER_STATUS_REJECTED);
  }
}
