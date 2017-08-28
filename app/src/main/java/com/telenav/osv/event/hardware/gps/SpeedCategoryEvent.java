package com.telenav.osv.event.hardware.gps;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Event for the speed category change
 * Created by Kalman on 14/11/2016.
 */
public class SpeedCategoryEvent extends OSVStickyEvent {

  public final SpeedCategory category;

  public final float speed;

  public SpeedCategoryEvent(float speed, SpeedCategory category) {
    this.speed = speed;
    this.category = category;
  }

  @Override
  public Class getStickyClass() {
    return SpeedCategoryEvent.class;
  }

  public enum SpeedCategory {
    SPEED_STATIONARY(10000),
    SPEED_5(5),
    SPEED_10(10),
    SPEED_15(15),
    SPEED_20(20),
    SPEED_25(25),
    SPEED_35(35);

    private final double distance;

    SpeedCategory(final double newValue) {
      distance = newValue;
    }

    public double getDistance() {
      return distance;
    }

  }
}
