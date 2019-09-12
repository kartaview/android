package com.telenav.osv.item.network;

/**
 * Created by kalmanb on 8/3/17.
 */
public class AuthData extends ApiResponse {

    private String mAccessToken = "";

    private String mId = "";

    private String mUsername = "";

    private String mDisplayName = "";

    private int mUserType = 0;

    private String mLoginType;

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String accessToken) {
        this.mAccessToken = accessToken;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        this.mDisplayName = displayName;
    }

    public int getUserType() {
        return mUserType;
    }

    public void setUserType(int userType) {
        this.mUserType = userType;
    }

    public String getLoginType() {
        return mLoginType;
    }

    public void setLoginType(String loginType) {
        this.mLoginType = loginType;
    }
}
