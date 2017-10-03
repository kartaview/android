package com.telenav.osv.di;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.data.AccountPreferences;
import com.telenav.osv.data.ApplicationPreferences;
import com.telenav.osv.data.DynamicPreferences;
import com.telenav.osv.data.MapPreferences;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.data.ProfilePreferences;
import com.telenav.osv.data.RecordingPreferences;
import com.telenav.osv.data.RunPreferences;
import com.telenav.osv.data.UIPreferences;
import com.telenav.osv.data.VersionPreferences;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.di.viewmodel.ViewModelModule;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.capture.CameraManager;
import com.telenav.osv.manager.location.LocationManager;
import com.telenav.osv.manager.location.ScoreManager;
import com.telenav.osv.manager.location.SensorManager;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * Main app module
 * Created by kalmanb on 9/21/17.
 */
@Module(includes = {
    ViewModelModule.class,
})
class AppModule {

  private static final String TAG = "AppModule";

  @Singleton
  @Provides
  SequenceDB provideDB(Context context, Preferences preferences) {
    return new SequenceDB(context, preferences);
  }

  @Provides
  LifecycleOwner provideLifeCycleOwner() {
    return ProcessLifecycleOwner.get();
  }

  @Singleton
  @Provides
  Recorder provideRecorder(Application application, SequenceDB db, Preferences prefs, CameraManager cameraManager,
                           SensorManager sensorManager,
                           LocationManager locationManager, ScoreManager scoreManager) {
    return new Recorder(application, db, prefs, cameraManager, locationManager, sensorManager, scoreManager);
  }

  @Singleton
  @Provides
  ValueFormatter provideValueFormatter(DynamicPreferences prefs) {
    return new ValueFormatter(prefs.getUsingMetricUnitsLive());
  }

  @Singleton
  @Provides
  ApplicationPreferences provideApplicationPreferences(Application application) {
    return new ApplicationPreferences(application);
  }

  @Singleton
  @Provides
  Preferences providePreferences(ApplicationPreferences appPrefs) {
    return new Preferences(appPrefs);
  }

  @Singleton
  @Provides
  MapPreferences provideMapPreferences(Preferences prefs) {
    return prefs;
  }

  @Singleton
  @Provides
  AccountPreferences provideAccountPreferences(Preferences prefs) {
    return prefs;
  }

  @Singleton
  @Provides
  ProfilePreferences provideProfilePreferences(Preferences prefs) {
    return prefs;
  }

  @Singleton
  @Provides
  DynamicPreferences provideDynamicPreferences(Preferences prefs) {
    return prefs;
  }

  @Singleton
  @Provides
  RecordingPreferences provideRecordingPreferences(Preferences prefs) {
    return prefs;
  }

  @Singleton
  @Provides
  RunPreferences provideRunPreferences(Preferences prefs) {
    return prefs;
  }

  @Singleton
  @Provides
  UIPreferences provideUIPreferences(Preferences prefs) {
    return prefs;
  }

  @Singleton
  @Provides
  VersionPreferences provideVersionPreferences(Preferences prefs) {
    return prefs;
  }
}
