package com.telenav.osv.data;

import android.arch.lifecycle.MutableLiveData;

/**
 * Class for handling preferences easily
 * Created by kalmanb on 9/14/17.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public interface DynamicPreferences extends GeneralPreferences {

    String[] URL_ENV = {"openstreetcam.org/", "staging.openstreetcam.org/", "testing.openstreetcam.org/", "beta.openstreetcam.org/"};

    boolean isAutoUploadEnabled();

    MutableLiveData<Boolean> getAutoUploadLive();

    boolean isDataUploadEnabled();

    MutableLiveData<Boolean> getDataUploadLive();

    boolean isChargingUploadEnabled();

    MutableLiveData<Boolean> getChargingUploadLive();

    MutableLiveData<Boolean> getSaveAuthLive();

    boolean getSaveAuth();

    MutableLiveData<Boolean> getUsingMetricUnitsLive();

    boolean isUsingMetricUnits();

    void setUsingMetricUnits(boolean value);

    MutableLiveData<Boolean> getExtStorageLive();

    boolean isUsingExternalStorage();

    void setUsingExternalStorage(boolean value);

    MutableLiveData<Boolean> getGamificationEnabledLive();

    boolean isGamificationEnabled();

    void setGamificationEnabled(boolean value);

    MutableLiveData<Boolean> getMapEnabledLive();

    boolean isMapEnabled();

    void setMapEnabled(boolean value);

    MutableLiveData<Boolean> getMiniMapEnabledLive();

    boolean isMiniMapEnabled();

    void setMiniMapEnabled(boolean value);

    MutableLiveData<Integer> getObdTypeLive();

    int getObdType();

    void setObdType(int value);

    MutableLiveData<Integer> getObdStatusLive();

    int getObdStatus();
}
