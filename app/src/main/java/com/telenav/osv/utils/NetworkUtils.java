package com.telenav.osv.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.jarvis.login.utils.LoginUtils;
import com.telenav.osv.network.internet.NetworkCallback;
import com.telenav.osv.network.response.NetworkRequestHeaderIdentifiers;

public class NetworkUtils {

    /**
     * name of the class used for networking
     */
    private static final String TAG = "NetworkUtils";

    /**
     * Checks if there is WI-FI or network connection available.
     */
    public static boolean isInternetAvailable(Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (networkCapabilities == null) {
                    return false;
                }
                return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);

            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo == null) {
                    return false;
                }
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                        || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE
                        || networkInfo.getType() == ConnectivityManager.TYPE_VPN) {
                    return networkInfo.isConnected();
                }
            }
        }
        return false;
    }

    /**
     * Checks if there is WI-FI or mobile data connection is available.
     * <p>This will be checked based on app settings given by {@code isMobileDataEnabled} which represents a special case for when they are off meaning the user wants wi-fi only
     * internet availability.</p>
     */
    public static boolean isInternetConnectionAvailable(Context context, boolean isMobileDataEnabled) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (networkCapabilities == null) {
                    return false;
                }
                if (!isMobileDataEnabled) {
                    //if mobile data is disabled this means the user will want wifi only capabilities
                    return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
                }
                //check for all network capabilities
                return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo == null) {
                    return false;
                }
                if (!isMobileDataEnabled) {
                    //if mobile data is disabled this means the user will want wifi only capabilities
                    return networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                            || networkInfo.getType() == ConnectivityManager.TYPE_VPN;
                }
                //check for all network capabilities
                return networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                        || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE
                        || networkInfo.getType() == ConnectivityManager.TYPE_VPN;
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

    public static SimpleEventBus isInternetAvailableStream(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        SimpleEventBus internetEventBus = new SimpleEventBus();
        NetworkCallback networkCallback = new NetworkCallback(internetEventBus);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                connectivityManager.registerNetworkCallback(
                        new NetworkRequest.Builder().build(),
                        networkCallback);
            }
        }
        return internetEventBus;
    }

    /**
     * @param applicationPreferences the application preferences to check against for user data.
     * @param url the url to be encapsulated by the {@code GlideUrl}
     * @return the {@code GlideUrl} representing the url with authorization if required.
     */
    public static GlideUrl provideGlideUrlWithAuthorizationIfRequired(ApplicationPreferences applicationPreferences, String url){
        GlideUrl glideUrl;
        if (LoginUtils.isLoginTypePartner(applicationPreferences)) {
            glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                    .addHeader(NetworkRequestHeaderIdentifiers.HEADER_AUTH_TYPE, NetworkRequestHeaderIdentifiers.HEADER_AUTH_TYPE_VALUE_TOKEN)
                    .addHeader(NetworkRequestHeaderIdentifiers.HEADER_AUTHORIZATION, applicationPreferences.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN))
                    .build());
        } else {
            glideUrl = new GlideUrl(url);
        }

        return glideUrl;
    }
}