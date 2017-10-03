package com.telenav.osv.di;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import com.telenav.osv.data.AccountPreferences;
import com.telenav.osv.data.ProfilePreferences;
import com.telenav.osv.item.view.profile.DriverProfileData;
import com.telenav.osv.item.view.profile.StatisticsData;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * todo only used in new profile fragment impl.
 * Created by kalmanb on 9/28/17.
 */
@Module
public class ProfileDataModule {

  @Singleton
  @Provides
  LiveData<DriverProfileData> provideDriverProfileData(MutableLiveData<DriverProfileData> data) {
    return data;
  }

  @Singleton
  @Provides
  LiveData<StatisticsData> provideStatisticsData(MutableLiveData<StatisticsData> data) {
    return data;
  }

  @Singleton
  @Provides
  MutableLiveData<DriverProfileData> provideMutableDriverProfileData(AccountPreferences prefs) {
    MutableLiveData<DriverProfileData> dataHolder = new MutableLiveData<>();
    DriverProfileData data = new DriverProfileData();
    data.setUsername(prefs.getUserName());
    data.setPhotoUrl(prefs.getUserPhotoUrl());
    data.setName(prefs.getUserDisplayName());
    dataHolder.setValue(data);
    return dataHolder;
  }

  @Singleton
  @Provides
  MutableLiveData<StatisticsData> provideMutableStatsData(ProfilePreferences prefs) {
    MutableLiveData<StatisticsData> dataHolder = new MutableLiveData<>();
    StatisticsData data = new StatisticsData();
    data.setDistance(prefs.getDistance());
    data.setAcceptedDistance(prefs.getAcceptedDistance());
    data.setRejectedDistance(prefs.getRejectedDistance());
    data.setObdDistance(prefs.getObdDistance());
    data.setTotalPhotos(prefs.getTracksCount());
    data.setTotalTracks(prefs.getPhotosCount());
    dataHolder.setValue(data);
    return dataHolder;
  }
}
