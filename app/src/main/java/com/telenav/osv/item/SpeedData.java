package com.telenav.osv.item;

/**
 * Model class for holding obd speed reading
 * Created by Kalman on 15/11/2016.
 */
public class SpeedData {

  private String errorCode = "";

  private int speed = -1;

  private long timestamp = System.currentTimeMillis();

  public SpeedData(int speed) {
    this.speed = speed;
  }

  public SpeedData(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public int getSpeed() {
    return speed;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
