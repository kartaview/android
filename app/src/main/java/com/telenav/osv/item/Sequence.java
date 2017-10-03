package com.telenav.osv.item;

import com.skobbler.ngx.SKCoordinate;
import java.util.Date;
import java.util.HashMap;

/**
 * abstract sequence base class
 * Created by Kalman on 11/18/15.
 */
//todo break down the sequence class into must have info and lazily instantiated extension data like:
// * scoreInfo(basic info about score) containing scoreHistory (the breakdown of the score value)
// * trackData (coordinates list) and frameData (individual frame related info like frame url)
public abstract class Sequence {

  private static final String TAG = "Sequence";

  double mTotalLength;

  int mOriginalFrameCount;

  int mFrameCount;

  String mThumbLink = "";

  String mAddress = "";

  Polyline mPolyline = new Polyline(0);

  int mId = -1;

  Date mDate;

  SKCoordinate mLocation = new SKCoordinate();

  boolean mHasObd = false;

  String mAppVersion = "";

  double value = -1;

  HashMap<Integer, ScoreHistory> mScoreHistory = new HashMap<>();

  String mPlatform = "";

  String mPlatformVersion = "";

  String mCurrency = "";

  private String mServerStatus = "";

  private boolean mIsPublic;

  private int mSeekToFrame = 0;

  public double getTotalLength() {
    return mTotalLength;
  }

  public void setTotalLength(double totalLength) {
    this.mTotalLength = totalLength;
  }

  public int getOriginalFrameCount() {
    return mOriginalFrameCount;
  }

  public int getFrameCount() {
    return mFrameCount;
  }

  public void setFrameCount(int count) {
    mFrameCount = count;
  }

  public String getThumbLink() {
    return mThumbLink;
  }

  public String getAddress() {
    return mAddress;
  }

  public void setAddress(String address) {
    this.mAddress = address;
  }

  public Polyline getPolyline() {
    return mPolyline;
  }

  public int getId() {
    return mId;
  }

  public Date getDate() {
    return mDate;
  }

  public SKCoordinate getLocation() {
    return mLocation;
  }

  public void setLocation(SKCoordinate mLocation) {
    this.mLocation = mLocation;
  }

  public boolean hasObd() {
    return mHasObd;
  }

  public String getAppVersion() {
    return mAppVersion;
  }

  public double getValue() {
    return value;
  }

  public HashMap<Integer, ScoreHistory> getScoreHistories() {
    return mScoreHistory;
  }

  public String getServerStatus() {
    return mServerStatus;
  }

  public void setServerStatus(String mServerStatus) {
    this.mServerStatus = mServerStatus;
  }

  public String getCurrency() {
    return mCurrency;
  }

  public boolean isPublic() {
    return mIsPublic;
  }

  public void setPublic(boolean isPublic) {
    this.mIsPublic = isPublic;
  }

  public int getRequestedFrameIndex() {
    return mSeekToFrame;
  }

  public void setRequestedFrameIndex(int seekToFrame) {
    this.mSeekToFrame = seekToFrame;
  }

  public int getDistance() {
    return (int) mTotalLength;
  }

  @Override
  public String toString() {
    return "Sequence (id " + mId + " images " + mFrameCount + " from " + mOriginalFrameCount + " and " + value + " Points" + ")";
  }

  public void setScoreHistory(HashMap<Integer, ScoreHistory> scoreHistory) {
    this.mScoreHistory = scoreHistory;
  }

  public void setHasObd(boolean hasObd) {
    this.mHasObd = hasObd;
  }

  public abstract boolean isOnline();

  public int getScore() {
    return (int) getValue();
  }

  public void setScore(double score) {
    this.value = score;
  }

  public abstract boolean isSafe();

  public boolean isUserTrack() {
    return true;
  }

  public boolean hasValue() {
    return value >= 0;
  }
}