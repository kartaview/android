package com.telenav.osv.item;

import com.telenav.osv.item.network.UserData;

/**
 * Holder class for user account info
 * Created by kalmanb on 7/5/17.
 */
@SuppressWarnings("unused")
public class AccountData extends UserData {

    public static final int ACCOUNT_TYPE_NONE = 0;

    public static final int ACCOUNT_TYPE_OSM = 1;

    public static final int ACCOUNT_TYPE_GOOGLE = 2;

    public static final int ACCOUNT_TYPE_FACEBOOK = 3;

    private String id;

    private String userName;

    private String displayName;

    private String profilePictureUrl;

    private int userType;

    private int accountType;

    public AccountData(String id, String userName, String displayName, String profilePictureUrl, int userType, int accountType) {
        this.id = id;
        this.userName = userName;
        this.displayName = displayName;
        this.profilePictureUrl = profilePictureUrl;
        this.userType = userType;
        this.accountType = accountType;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    @Override
    public String toString() {
        return "UserInfo{" + "id='" + id + '\'' + ", userName='" + userName + '\'' + ", displayName='" + displayName + '\'' +
                ", profilePictureUrl='" + profilePictureUrl + '\'' + ", userType=" + userType + ", accountType=" + accountType + '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public int getAccountType() {
        return accountType;
    }

    public void setAccountType(int accountType) {
        this.accountType = accountType;
    }

    public boolean isDriver() {
        return userType == TYPE_BYOD || userType == TYPE_DEDICATED ||
                userType == TYPE_BAU;
    }
}
