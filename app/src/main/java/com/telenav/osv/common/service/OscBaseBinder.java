package com.telenav.osv.common.service;

import android.os.Binder;

public abstract class OscBaseBinder extends Binder {
    public abstract OscBaseService getService();
}
