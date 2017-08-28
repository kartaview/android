package com.telenav.osv.manager.network.parser;

import com.telenav.osv.item.AccountData;
import com.telenav.osv.item.network.AuthData;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kalmanb on 8/3/17.
 */
public class AuthDataParser extends ApiResponseParser<AuthData> {

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
      int typeNum = AccountData.getUserTypeForString(type);
      authData.setUserType(typeNum);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return authData;
  }
}
