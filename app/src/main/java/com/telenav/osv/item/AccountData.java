package com.telenav.osv.item;

import org.jetbrains.annotations.NotNull;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.item.network.ApiResponse;

/**
 * Holder class for user account info
 * Created by kalmanb on 7/5/17.
 */
public class AccountData extends ApiResponse {

    public static final int ACCOUNT_TYPE_OSM = 0;

    public static final int ACCOUNT_TYPE_GOOGLE = 1;

    public static final int ACCOUNT_TYPE_FACEBOOK = 2;

    private static final String KEY_USER_TYPE_USER = "user";

    private static final String KEY_USER_TYPE_QA = "qa";

    private static final String KEY_LOGIN_TYPE_GOOGLE = "GOOGLE";

    private static final String KEY_LOGIN_TYPE_FACEBOOK = "FACEBOOK";

    private static final String KEY_LOGIN_TYPE_OSM = "OSM";

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

    public static int getUserTypeForString(String type) {
        switch (type) {
            default:
            case KEY_USER_TYPE_USER:
                return PreferenceTypes.USER_TYPE_CONTRIBUTOR;
            case KEY_USER_TYPE_QA:
                return PreferenceTypes.USER_TYPE_QA;
        }
    }

    public static int getAccountTypeForString(String type) {
        switch (type) {
            default:
            case KEY_LOGIN_TYPE_OSM:
                return ACCOUNT_TYPE_OSM;
            case KEY_LOGIN_TYPE_GOOGLE:
                return ACCOUNT_TYPE_GOOGLE;
            case KEY_LOGIN_TYPE_FACEBOOK:
                return ACCOUNT_TYPE_FACEBOOK;
        }
    }

    @NotNull
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

    public String getDisplayName() {
        return displayName;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }
}
