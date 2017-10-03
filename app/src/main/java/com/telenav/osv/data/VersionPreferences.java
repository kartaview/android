package com.telenav.osv.data;

/**
 * Class for handling preferences easily
 * Created by kalmanb on 9/14/17.
 */
public interface VersionPreferences {

  String getVersionName();

  void setVersionName(String versionName);

  int getVersionCode();

  void setVersionCode(int versionCode);

  int getVersionCodeForSdk();

  void setVersionCodeForSdk(int version);
}
