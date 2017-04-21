package com.telenav.osv.event.ui;

import com.telenav.osv.event.OSVEvent;

/**
 * Created by Kalman on 23/12/2016.
 */
public class GamificationSettingEvent extends OSVEvent{
    public boolean enabled;

    public GamificationSettingEvent(boolean enabled){
        this.enabled = enabled;
    }
}
