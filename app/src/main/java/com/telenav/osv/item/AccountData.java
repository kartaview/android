package com.telenav.osv.item;

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

  private static final String KEY_USER_TYPE_DEDICATED = "DEDICATED";

  private static final String KEY_USER_TYPE_BYOD = "BYOD";

  private static final String KEY_USER_TYPE_BAU = "BAU";

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
      case KEY_USER_TYPE_DEDICATED:
        return PreferenceTypes.USER_TYPE_DEDICATED;
      case KEY_USER_TYPE_BYOD:
        return PreferenceTypes.USER_TYPE_BYOD;
      case KEY_USER_TYPE_BAU:
        return PreferenceTypes.USER_TYPE_BAU;
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

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public String getProfilePictureUrl() {
    return profilePictureUrl;
  }

  public void setProfilePictureUrl(String profilePictureUrl) {
    this.profilePictureUrl = profilePictureUrl;
  }

  public int getUserType() {
    return userType;
  }

  public void setUserType(int userType) {
    this.userType = userType;
  }

  public int getAccountType() {
    return accountType;
  }

  public void setAccountType(int accountType) {
    this.accountType = accountType;
  }

  public boolean isDriver() {
    return userType == PreferenceTypes.USER_TYPE_BYOD || userType == PreferenceTypes.USER_TYPE_DEDICATED ||
        userType == PreferenceTypes.USER_TYPE_BAU;
  }

  @Override
  public String toString() {
    return "UserInfo{" + "id='" + id + '\'' + ", userName='" + userName + '\'' + ", displayName='" + displayName + '\'' +
        ", profilePictureUrl='" + profilePictureUrl + '\'' + ", userType=" + userType + ", accountType=" + accountType + '}';
  }
}
