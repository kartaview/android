package com.telenav.osv.common.service;

public interface KVBaseService {
    void stopSelf();

    void stopForeground(boolean notification);
}
