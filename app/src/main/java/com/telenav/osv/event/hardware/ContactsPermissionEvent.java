package com.telenav.osv.event.hardware;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 01/03/2017.
 */

public class ContactsPermissionEvent extends OSVStickyEvent {

    public final static String TAG = "ContactsPermissionEvent";

    public final String loginType;

    public ContactsPermissionEvent(String loginType) {
        this.loginType = loginType;
    }

    @Override
    public Class getStickyClass() {
        return ContactsPermissionEvent.class;
    }
}
