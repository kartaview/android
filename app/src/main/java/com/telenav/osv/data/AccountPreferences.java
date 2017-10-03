package com.telenav.osv.data;

import android.arch.lifecycle.LiveData;

/**
 * Class for handling preferences easily
 * Created by kalmanb on 9/14/17.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public interface AccountPreferences extends GeneralPreferences {

    boolean isLoggedIn();

    LiveData<Boolean> observeLogin();

    int getLoginType();

    void setLoginType(int type);

    LiveData<String> getAuthTokenLive();

    String getAuthToken();

    void saveAuthToken(String accessToken);

    String getUserName();

    void setUserName(String userName);

    String getUserId();

    void setUserId(String id);

    String getUserDisplayName();

    void setUserDisplayName(String userDisplayName);

    String getUserPhotoUrl();

    void setUserPhotoUrl(String url);

    LiveData<String> getUserPhotoUrlLive();

    LiveData<Integer> getUserTypeLive();

    int getUserType();

    void setUserType(int type);

    boolean isDriver();

    LiveData<Boolean> isDriverLive();

    boolean loggedIn();
}
