package com.telenav.osv.item.network;

import com.telenav.osv.item.AccountData;

/**
 * Api response containing login related info like OSC access token
 * Created by kalmanb on 8/3/17.
 */
public class AuthData extends ApiResponse {

  private String mAccessToken = "";

  private String mId = "";

  private String mUsername = "";

  private String mDisplayName = "";

  /**
   * see @{{@link UserData}}
   */
  private int mUserType = 0;

  /**
   * see @{{@link AccountData}}
   */
  private int mLoginType;

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

  public int getLoginType() {
    return mLoginType;
  }

  public void setLoginType(int loginType) {
    this.mLoginType = loginType;
  }
}
