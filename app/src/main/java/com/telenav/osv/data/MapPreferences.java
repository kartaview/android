package com.telenav.osv.data;

import android.location.Location;

/**
 * Class for handling preferences easily
 * Created by kalmanb on 9/14/17.
 */
public interface MapPreferences {

  public Location getLastLocation();

  public void saveLastLocation(Location loc);

  public float getRecordingMapZoom();

  public void setRecordingMapZoom(float value);

  public void postRecordingMapZoom(float value);

  public String getMapResourcesPath();

  public void setMapResourcesPath(String path);
}
