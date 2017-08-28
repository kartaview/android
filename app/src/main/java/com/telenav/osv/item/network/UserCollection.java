package com.telenav.osv.item.network;

import com.telenav.osv.item.LeaderboardData;
import java.util.ArrayList;

/**
 * Created by kalmanb on 7/5/17.
 */
public class UserCollection extends ApiResponse {

  private ArrayList<LeaderboardData> userList = new ArrayList<>();

  public ArrayList<LeaderboardData> getUserList() {
    return userList;
  }
}
