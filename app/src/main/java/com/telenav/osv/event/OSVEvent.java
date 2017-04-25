package com.telenav.osv.event;

/**
 * Created by Kalman on 07/11/2016.
 */

public abstract class OSVEvent {
    @Override
    public boolean equals(Object obj) {
        return this.getClass() == obj.getClass();
    }
}
