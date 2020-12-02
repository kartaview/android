package com.telenav.osv.event.ui;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by kalmanb on 7/17/17.
 */
public class UserTypeChangedEvent extends OSVStickyEvent {

    public final int type;

    public UserTypeChangedEvent(int type) {
        this.type = type;
    }

    @Override
    public Class getStickyClass() {
        return UserTypeChangedEvent.class;
    }
}
