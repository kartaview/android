package com.telenav.osv.di;

import android.app.Application;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.telenav.osv.data.MapPreferences;
import com.telenav.osv.manager.location.AndroidLocationManager;
import com.telenav.osv.manager.location.GoogleLocationManager;
import com.telenav.osv.manager.location.LocationManager;
import com.telenav.osv.manager.location.LocationQualityChecker;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * Created by kalmanb on 9/21/17.
 */
@Module
class LocationModule {

  private static final String TAG = "LocationModule";

  @SuppressWarnings("deprecation")
  private static boolean isGooglePlayServices(Application app) {
    final int googlePlayServicesCheck = GooglePlayServicesUtil.isGooglePlayServicesAvailable(app);
    switch (googlePlayServicesCheck) {
      case ConnectionResult.SUCCESS:
      case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
        return true;
    }
    return false;
  }

  @Singleton
  @Provides
  LocationQualityChecker provideLocationQualityChecker() {
    return new LocationQualityChecker();
  }

  @Singleton
  @Provides
  LocationManager provideLocationManager(Application application, MapPreferences preferences, LocationQualityChecker qualityChecker) {
    if (isGooglePlayServices(application)) {
      return new GoogleLocationManager(application, preferences, qualityChecker);
    }
    return new AndroidLocationManager(application, preferences, qualityChecker);
  }
}
