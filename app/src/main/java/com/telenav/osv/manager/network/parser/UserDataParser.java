package com.telenav.osv.manager.network.parser;

import com.telenav.osv.item.network.UserData;
import com.telenav.osv.utils.Log;
import org.json.JSONObject;

/**
 * JSON parser for user profile data
 * Created by kalmanb on 8/1/17.
 */
public class UserDataParser extends ApiResponseParser<UserData> {

  private static final String TAG = "UserProfileParser";

  @Override
  public UserData getHolder() {
    return new UserData();
  }

  public UserData parse(String json) {
    UserData userData = super.parse(json);
    if (json != null && !json.isEmpty()) {
      final String userName;
      final String name;
      final String obdDistance;
      final String totalDistance;
      final double totalTracks;
      final String levelName;
      int rank = 0;
      int weeklyRank = 0;
      int score = 0;
      int level = 0;
      int xpProgress = 0;
      int xpTarget = 0;
      double totalPhotos;
      try {
        JSONObject obj = new JSONObject(json);
        JSONObject osv = obj.getJSONObject("osv");
        String id = osv.getString("id");
        userData.setUserId(id);
        userName = osv.getString("username");
        userData.setUserName(userName);
        name = osv.getString("full_name");
        userData.setDisplayName(name);
        String userType = osv.getString("type");
        if ("driver".equals(userType)) {
          userType = osv.getString("driver_type");
        }
        userData.setUserType(AuthDataParser.getUserTypeForString(userType));
        obdDistance = osv.getString("obdDistance");
        totalDistance = osv.getString("totalDistance");
        totalPhotos = osv.getDouble("totalPhotos");
        userData.setTotalPhotos(totalPhotos);
        totalTracks = osv.getDouble("totalTracks");
        userData.setTotalTracks(totalTracks);
        weeklyRank = osv.getInt("weeklyRank");
        userData.setWeeklyRank(weeklyRank);
        try {
          JSONObject gamification = osv.getJSONObject("gamification");

          rank = gamification.getInt("rank");
          userData.setOverallRank(rank);
          score = gamification.getInt("total_user_points");
          userData.setTotalPoints(score);
          level = gamification.getInt("level");
          userData.setLevel(level);
          levelName = gamification.getString("level_name");
          userData.setLevelName(levelName);
          xpProgress = gamification.getInt("level_progress");
          userData.setLevelProgress(xpProgress);
          try {
            xpTarget = gamification.getInt("level_target");
            userData.setLevelTarget(xpTarget);
          } catch (Exception e) {
            Log.w(TAG, "requestFinished: " + Log.getStackTraceString(e));
          }
        } catch (Exception e) {
          Log.w(TAG, "requestFinished: " + Log.getStackTraceString(e));
        }
        double totalDistanceNum = 0;
        double obdDistanceNum = 0;
        try {
          if (totalDistance != null) {
            totalDistanceNum = Double.parseDouble(totalDistance);
          }
        } catch (NumberFormatException e) {
          Log.w(TAG, "getUserProfileDetails: " + Log.getStackTraceString(e));
        }
        userData.setTotalDistance(totalDistanceNum);
        try {
          if (obdDistance != null) {
            obdDistanceNum = Double.parseDouble(obdDistance);
          }
        } catch (NumberFormatException e) {
          Log.w(TAG, "getUserProfileDetails: " + Log.getStackTraceString(e));
        }
        userData.setObdDistance(obdDistanceNum);
      } catch (Exception e) {
        Log.d(TAG, Log.getStackTraceString(e));
      }
    }
    return userData;
  }
}
