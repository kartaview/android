package com.telenav.osv.manager.network.parser;

import com.telenav.osv.item.network.AuthData;
import com.telenav.osv.utils.Log;
import org.json.JSONException;
import org.json.JSONObject;

import static com.telenav.osv.item.network.UserData.TYPE_BAU;
import static com.telenav.osv.item.network.UserData.TYPE_BYOD;
import static com.telenav.osv.item.network.UserData.TYPE_CONTRIBUTOR;
import static com.telenav.osv.item.network.UserData.TYPE_DEDICATED;
import static com.telenav.osv.item.network.UserData.TYPE_QA;

/**
 * Created by kalmanb on 8/3/17.
 */
public class AuthDataParser extends ApiResponseParser<AuthData> {

  private static final String TAG = "AuthDataParser";

  private static final String KEY_USER_TYPE_DEDICATED = "DEDICATED";

  private static final String KEY_USER_TYPE_BYOD = "BYOD";

  private static final String KEY_USER_TYPE_BAU = "BAU";

  private static final String KEY_USER_TYPE_USER = "user";

  private static final String KEY_USER_TYPE_QA = "qa";

  public static int getUserTypeForString(String type) {
    switch (type) {
      case KEY_USER_TYPE_DEDICATED:
        return TYPE_DEDICATED;
      case KEY_USER_TYPE_BYOD:
        return TYPE_BYOD;
      case KEY_USER_TYPE_BAU:
        return TYPE_BAU;
      case KEY_USER_TYPE_QA:
        return TYPE_QA;
      case KEY_USER_TYPE_USER:
      default:
        return TYPE_CONTRIBUTOR;
    }
  }

  @Override
  public AuthData getHolder() {
    return new AuthData();
  }

  public AuthData parse(String json) {
    AuthData authData = super.parse(json);

    try {
      JSONObject obj;
      obj = new JSONObject(json);
      JSONObject osv = obj.getJSONObject("osv");
      authData.setAccessToken(osv.getString("access_token"));
      authData.setId(osv.getString("id"));
      authData.setUsername(osv.getString("username"));
      authData.setDisplayName(osv.getString("full_name"));
      String type = osv.getString("type");
      if ("driver".equals(type)) {
        type = osv.getString("driver_type");
      }
      int typeNum = getUserTypeForString(type);
      authData.setUserType(typeNum);
    } catch (JSONException e) {
      Log.d(TAG, Log.getStackTraceString(e));
    }

    return authData;
  }
}
