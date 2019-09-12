package com.telenav.osv.common.service;

public interface OscBaseService {
    void stopSelf();

    void stopForeground(boolean notification);
}
