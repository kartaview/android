package com.telenav.osv.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.telenav.osv.utils.NetworkUtils;

/**
 * Network broadcasts receiver used for internet connection updates.
 */
public class NetworkBroadcastReceiver extends BroadcastReceiver {

    /**
     * Listener which will be notified when the network connection changes.
     */
    private NetworkConnectionListener networkChangeListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (networkChangeListener != null) {
            networkChangeListener.onNetworkConnectionChange(NetworkUtils.isInternetAvailable(context));
        }
    }

    public void addNetworkConnectionListener(NetworkConnectionListener listener) {
        networkChangeListener = listener;
    }

    public void removeNetworkConnectionListener() {
        networkChangeListener = null;
    }

    /**
     * Interface for network connection updates.
     */
    public interface NetworkConnectionListener {
        /**
         * Method is called when the network connection changes.
         * @param isConnected {@code true} if the device has a network connection, {@code false} otherwise.
         */
        void onNetworkConnectionChange(boolean isConnected);
    }
}
