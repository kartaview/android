package com.telenav.osv.listener.network;

import com.telenav.osv.item.network.AuthData;
import com.telenav.osv.item.network.OsmProfileData;

/**
 * Created by kalmanb on 8/3/17.
 */
public interface OsmAuthDataListener extends NetworkResponseDataListener<AuthData> {

  void requestFinished(int status, OsmProfileData osmProfileData);
}
