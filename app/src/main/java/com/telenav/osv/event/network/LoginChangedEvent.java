package com.telenav.osv.event.network;

import com.telenav.osv.event.OSVStickyEvent;

/**
 * Created by Kalman on 16/11/2016.
 */
public class LoginChangedEvent extends OSVStickyEvent {

    public final boolean logged;

    public final boolean driver;

    public final String username;

    public final String displayName;

    public final String userPhoto;

    public LoginChangedEvent(boolean logged, String username, String displayName, String photoUrl, boolean driver) {
        this.logged = logged;
        this.username = username;
        this.driver = driver;
        this.userPhoto = photoUrl;
        this.displayName = displayName;
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof LoginChangedEvent;
    }

    @Override
    public Class getStickyClass() {
        return LoginChangedEvent.class;
    }
}
