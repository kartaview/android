package com.telenav.osv.application;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ProcessLifecycleOwner;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Class responsible for formatting to local units
 * Created by kalmanb on 8/29/17.
 */
public class ValueFormatter {

  public static final String TAG = "ValueFormatter";

  public static final float KILOMETER_TO_MILE = 0.621371f;

  private static final float METER_TO_FEET = 3.28084f;

  private boolean metric;

  public ValueFormatter(LiveData<Boolean> metricLive) {
    metricLive.observe(ProcessLifecycleOwner.get(), this::setMetric);
  }

  private void setMetric(boolean metric) {
    this.metric = metric;
  }

  /**
   * Formats a given distance value (given in meters)
   *
   * @param distInMeters dist
   *
   * @return strings, value/unit
   */
  public String[] formatDistanceFromMeters(Integer distInMeters) {
    if (distInMeters == null) {
      return formatDistanceFromKiloMeters(0);
    }
    return formatDistanceFromKiloMeters((int) distInMeters);
  }

  public String[] formatDistanceFromMeters(int distInMeters) {
    if (!metric) {
      distInMeters = (int) (distInMeters * METER_TO_FEET);
    }
    DecimalFormat df2 = new DecimalFormat("#.#");
    if (distInMeters < 500) {
      return new String[] {distInMeters + "", (!metric ? " ft" : " m")};
    } else {
      return new String[] {df2.format((double) distInMeters / (!metric ? 5280 : 1000)) + "", (!metric ? " mi" : " km")};
    }
  }

  /**
   * Formats a given distance value (given in meters)
   *
   * @param dist dist
   *
   * @return value/unit
   */
  public String[] formatDistanceFromKiloMeters(Double dist) {
    if (dist == null) {
      return formatDistanceFromKiloMeters(0);
    }
    return formatDistanceFromKiloMeters((double) dist);
  }

  public String[] formatDistanceFromKiloMeters(double dist) {
    if (!metric) {
      dist = (int) (dist * KILOMETER_TO_MILE);
    }
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setGroupingSeparator(' ');
    DecimalFormat df2 = new DecimalFormat("#,###,###,###.#", symbols);
    return new String[] {df2.format(dist) + "", (!metric ? " mi" : " km")};
  }

  public String formatNumber(Double value) {
    if (value == null) {
      return formatNumber(0);
    }
    return formatNumber((double) value);
  }

  public String formatNumber(double value) {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setGroupingSeparator(' ');
    DecimalFormat formatter = new DecimalFormat("#,###,###,###", symbols);
    return formatter.format(value);
  }

  public String formatMoney(Double value) {
    if (value == null) {
      return formatMoney(0);
    }
    return formatMoney((double) value);
  }

  public String formatMoney(double value) {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setGroupingSeparator(',');
    DecimalFormat df2 = new DecimalFormat("#,###,###,###.##", symbols);
    return df2.format(value);
  }

  public String formatMoneyConstrained(Double value) {
    if (value == null) {
      return formatMoneyConstrained(0);
    }
    return formatMoneyConstrained((double) value);
  }

  public String formatMoneyConstrained(double value) {
    if (value < 100) {
      DecimalFormatSymbols symbols = new DecimalFormatSymbols();
      symbols.setGroupingSeparator(',');
      DecimalFormat df2 = new DecimalFormat("#,###,###,###.##", symbols);
      return df2.format(value);
    } else {
      DecimalFormatSymbols symbols = new DecimalFormatSymbols();
      symbols.setGroupingSeparator(',');
      DecimalFormat df2 = new DecimalFormat("#,###,###,###", symbols);
      return df2.format(value);
    }
  }

  public String formatDistanceFromKiloMetersFlat(Double dist) {
    if (dist == null) {
      return formatDistanceFromKiloMetersFlat(0);
    }
    return formatDistanceFromKiloMetersFlat((double) dist);
  }

  public String formatDistanceFromKiloMetersFlat(double dist) {
    String[] temp = formatDistanceFromKiloMeters(dist);
    return temp[0] + temp[1];
  }
}
