package com.telenav.osv.obd;

import android.util.Log;

/**
 * Created by dianat on 3/25/2016.
 */
public class OBDHelper {

  /**
   * command for speed
   */
  public static final String CMD_SPEED = "010D1";

  /**
   * command for speed
   */
  public static final String CMD_WARM_START = "AT WS";

  /**
   * command for speed
   */
  public static final String CMD_FAST_INIT = "AT FI";

  public static final String CMD_SET_AUTO = "AT SP 00";

  public static final String CMD_DEVICE_DESCRIPTION = "AT @1";

  public static final String CMD_DESCRIBE_PROTOCOL = "AT DP";

  private static final String TAG = "OBDHelper";

  /**
   * converts the characteristic result to integer value according to the command type formula
   * the formulas used for calculation where taken for: https://en.wikipedia.org/wiki/OBD-II_PIDs
   *
   * @param charResult - the characteristic result
   *
   * @return - the integer value
   */
  public static int convertResult(String charResult, VehicleDataListener vehicleDataListener) {
    String request = null;
    String response = null;
    String[] arrayRes = charResult.split("\r");
    if (arrayRes.length > 1) {
      request = arrayRes[0];
      response = arrayRes[1];
    }
    String[] splitResponse = response != null ? response.split(" ") : new String[0];
    if (request != null) {
      switch (request) {
        case CMD_SPEED:
          if (splitResponse.length >= 3) {
            // formula used: A, where A is the first byte
            Integer speed = Integer.parseInt(splitResponse[2], 16);
            if (vehicleDataListener != null) {
              vehicleDataListener.onSpeed(speed);
            }
            return speed;
          }
          break;
        default:
          Log.e(TAG, "convertResult: cannot parse");
      }
    }
    return -1;
  }
}
