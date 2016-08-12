package com.telenav.osv.listener;

import android.location.Location;
import com.google.android.gms.common.api.Status;

/**
 * Created by Kalman on 09/08/16.
 */

public interface LocationEventListener {
    void onLocationChanged(Location location);

    void onResolutionNeeded(Status status);
}
