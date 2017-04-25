package com.telenav.osv.event.hardware.obd;

import com.telenav.osv.event.OSVEvent;
import com.telenav.osv.item.SpeedData;

/**
 * Created by Kalman on 15/11/2016.
 */

public class ObdSpeedEvent extends OSVEvent {

    public final SpeedData data;

    public ObdSpeedEvent(SpeedData data){
        this.data = data;
    }
}
