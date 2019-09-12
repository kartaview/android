package com.telenav.osv.event.network.upload;

import com.facebook.network.connectionclass.ConnectionQuality;
import com.telenav.osv.event.OSVEvent;

/**
 * Created by Bencze Kalman on 2/12/2017.
 */
public class UploadBandwidthEvent extends OSVEvent {

    public final ConnectionQuality bandwidthState;

    public final double bandwidth;

    public UploadBandwidthEvent(ConnectionQuality bandwidthState, final double bandwidth) {
        this.bandwidthState = bandwidthState;
        this.bandwidth = bandwidth;
    }
}
