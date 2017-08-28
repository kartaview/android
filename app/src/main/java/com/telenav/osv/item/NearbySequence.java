package com.telenav.osv.item;

import java.util.Date;

/**
 * Created by kalmanb on 7/11/17.
 */
public class NearbySequence extends Sequence {

  private static final String TAG = "NearbySequence";

  public NearbySequence(int sequenceId, Date date, int originalImageCount, String address, String thumbLink, String platform,
                        String platformVersion, String appVersion, int distance) {
    this.mId = sequenceId;
    this.mDate = date;
    this.mOriginalFrameCount = originalImageCount;
    this.mFrameCount = originalImageCount;
    this.mAddress = address;
    this.mThumbLink = thumbLink;
    this.mPlatform = platform;
    this.mPlatformVersion = platformVersion;
    this.mAppVersion = appVersion;
    this.mTotalLength = distance;
  }

  @Override
  public boolean isOnline() {
    return true;
  }

  @Override
  public boolean isSafe() {
    return true;
  }
}
