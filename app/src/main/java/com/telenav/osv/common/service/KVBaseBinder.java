package com.telenav.osv.common.service;

import android.os.Binder;

public abstract class KVBaseBinder extends Binder {
    public abstract KVBaseService getService();
}
