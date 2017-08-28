package com.telenav.osv.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class NetworkUtils {

  /**
   * name of the class used for networking
   */
  private static final String TAG = "NetworkUtils";

  /**
   * checks if there is WI-FI or network connection available
   */
  public static boolean isInternetAvailable(Context currentActivity) {
    final ConnectivityManager conectivityManager = (ConnectivityManager) currentActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
    final NetworkInfo networkInfo = conectivityManager.getActiveNetworkInfo();
    if (networkInfo != null) {
      if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
        if (networkInfo.isConnected()) {
          return true;
        }
      } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
        if (networkInfo.isConnected()) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Checks if wifi is ON
   */
  public static boolean isWifiOn(Context context) {
    WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    return wifi.isWifiEnabled();
  }

  /**
   * checks if there is WI-FI connection available
   */
  public static boolean isWifiInternetAvailable(Context currentActivity) {
    final ConnectivityManager connectivityManager = (ConnectivityManager) currentActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
    final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    if (networkInfo != null) {
      if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
        if (networkInfo.isConnected()) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * checks if there is network connection available
   */
  public static boolean isMobileInternetAvailable(Activity currentActivity) {
    final ConnectivityManager connectivityManager = (ConnectivityManager) currentActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
    final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    if (networkInfo != null) {
      if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
        if (networkInfo.isConnected()) {
          return true;
        }
      }
    }

    return false;
  }
}