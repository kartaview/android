package com.telenav.osv.di;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import com.telenav.osv.data.AccountPreferences;
import com.telenav.osv.data.DynamicPreferences;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.data.ProfilePreferences;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.item.view.profile.DriverProfileData;
import com.telenav.osv.item.view.profile.StatisticsData;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.network.GeometryRetriever;
import com.telenav.osv.manager.network.IssueReporter;
import com.telenav.osv.manager.network.LoginManager;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.service.NetworkBroadcastReceiver;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * Module providing the network related dependencies
 * Created by kalmanb on 9/26/17.
 */
@Module
class NetworkModule {

  @Singleton
  @Provides
  GeometryRetriever provideGeometryRetriever(Context context, AccountPreferences prefs) {
    return new GeometryRetriever(context, prefs);
  }

  @Singleton
  @Provides
  UploadManager provideUploadManager(Context context, SequenceDB db, Preferences prefs, AccountPreferences accountPrefs) {
    return new UploadManager(context, db, prefs, accountPrefs);
  }

  @Singleton
  @Provides
  LoginManager provideLoginManager(Context context, Preferences prefs, AccountPreferences accountPrefs) {
    return new LoginManager(context, prefs, accountPrefs);
  }

  @Singleton
  @Provides
  IssueReporter provideIssueReporter(Context context, AccountPreferences accountPrefs) {
    return new IssueReporter(context, accountPrefs);
  }

  @Singleton
  @Provides
  UserDataManager provideUserDataManager(Context context, ProfilePreferences accountPrefs, MutableLiveData<DriverProfileData> data,
                                         MutableLiveData<StatisticsData> stats) {
    return new UserDataManager(context, accountPrefs, data, stats);
  }

  @Provides
  NetworkBroadcastReceiver provideNetworkBroadcastReceiver(Context context, Recorder recorder, DynamicPreferences appPrefs,
                                                           UploadManager uploadManager) {
    return new NetworkBroadcastReceiver(context, recorder, appPrefs, uploadManager);
  }
}
