package com.telenav.osv.item;

import com.skobbler.ngx.SKCoordinate;

/**
 * Created by Kalman on 11/17/15.
 */
public class ImageCoordinate extends SKCoordinate {

  public int index = 0;

  public ImageCoordinate(double v, double v1, int index) {
    super(v, v1);
    this.index = index;
  }
}
