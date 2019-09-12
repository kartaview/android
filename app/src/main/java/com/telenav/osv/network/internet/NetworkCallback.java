package com.telenav.osv.network.internet;

import android.net.ConnectivityManager;
import android.net.Network;
import com.telenav.osv.common.event.SimpleEventBus;

/**
 * Network callback class
 */
public class NetworkCallback extends ConnectivityManager.NetworkCallback {

    SimpleEventBus simpleEventBus;

    public NetworkCallback(SimpleEventBus simpleEventBus) {
        this.simpleEventBus = simpleEventBus;
    }

    @Override
    public void onAvailable(Network network) {
        super.onAvailable(network);
        simpleEventBus.post(true);
    }

    @Override
    public void onUnavailable() {
        super.onUnavailable();
        simpleEventBus.post(false);
    }

    @Override
    public void onLost(Network network) {
        super.onLost(network);
        simpleEventBus.post(false);
    }
}
