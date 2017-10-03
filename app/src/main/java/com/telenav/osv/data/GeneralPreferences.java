package com.telenav.osv.data;

import android.arch.lifecycle.MutableLiveData;

/**
 * Class for handling preferences easily
 * Created by kalmanb on 9/14/17.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public interface GeneralPreferences {

    boolean isDebugEnabled();

    MutableLiveData<Boolean> getDebugEnabledLive();

    MutableLiveData<Integer> getServerTypeLive();

    int getServerType();

    void setServerType(int value);
}
