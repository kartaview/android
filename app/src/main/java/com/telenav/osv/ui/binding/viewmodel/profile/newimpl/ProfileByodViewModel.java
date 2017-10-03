package com.telenav.osv.ui.binding.viewmodel.profile.newimpl;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.databinding.ObservableField;
import com.telenav.osv.R;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.data.AccountPreferences;
import com.telenav.osv.item.view.profile.DriverProfileData;
import com.telenav.osv.item.view.profile.StatisticsData;
import com.telenav.osv.utils.Log;
import javax.inject.Inject;

/**
 * todo only used in new profile fragment impl.
 * Created by kalmanb on 9/21/17.
 */
public class ProfileByodViewModel extends ProfileViewModel {

  private static final String TAG = "ProfileByodViewModel";

  private final LiveData<DriverProfileData> profileData;

  private final LiveData<StatisticsData> statsData;

  public ObservableField<String> distance = new ObservableField<>();

  public ObservableField<String> name = new ObservableField<>();

  public ObservableField<String> rate = new ObservableField<>();

  public ObservableField<String> value = new ObservableField<>();

  public ObservableField<String> pictureUrl = new ObservableField<>();

  private ListItemViewModelFactory listItemFactory;

  private ByodTrackListHeaderViewModel header;

  @Inject
  public ProfileByodViewModel(Application application, AccountPreferences prefs, ValueFormatter formatter,
                              LiveData<DriverProfileData> profileData, LiveData<StatisticsData> statsData) {
    super(application, prefs, formatter);
    this.profileData = profileData;
    this.statsData = statsData;
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    Log.d(TAG, "onCleared: ");
  }

  @Override
  public void setOwner(LifecycleOwner lifecycleOwner) {
    owner = lifecycleOwner;
    profileData.observe(owner, data -> {
      if (data == null) {
        return;
      }
      distance.set(formatter.formatDistanceFromKiloMetersFlat(data.getCurrentAccepted()));
      name.set(data.getName());
      rate.set(data.getRate() + " " + data.getCurrency() + getApplication().getString(R.string.partial_rate_km_label));
      value.set(formatter.formatMoney(data.getValue()));
      pictureUrl.set(data.getPhotoUrl());
    });
    listItemFactory = new ListItemViewModelFactory(getApplication(), owner, formatter);
    header = listItemFactory.create(ByodTrackListHeaderViewModel.class);
    header.setStats(statsData);
    headerList.insertItem(header);
  }
}
