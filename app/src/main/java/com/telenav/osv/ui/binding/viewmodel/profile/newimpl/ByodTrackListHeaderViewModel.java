package com.telenav.osv.ui.binding.viewmodel.profile.newimpl;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.databinding.ObservableField;
import android.text.SpannableString;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.item.view.profile.StatisticsData;
import com.telenav.osv.item.view.tracklist.StatsData;

/**
 * todo only used in new profile fragment impl.
 * Created by kalmanb on 9/29/17.
 */
public class ByodTrackListHeaderViewModel extends TrackListHeaderViewModel {

  public ObservableField<SpannableString> acceptedDistance = new ObservableField<>();

  public ObservableField<SpannableString> rejectedDistance = new ObservableField<>();

  public ObservableField<SpannableString> obdDistance = new ObservableField<>();

  public ObservableField<SpannableString> images = new ObservableField<>();

  public ObservableField<SpannableString> tracks = new ObservableField<>();

  private LiveData<StatisticsData> statsData;

  public ByodTrackListHeaderViewModel(Application application, LifecycleOwner owner, ValueFormatter valueFormatter) {
    super(application, valueFormatter);
    this.owner = owner;
  }

  public void setStats(LiveData<StatisticsData> statsData) {
    this.statsData = statsData;

    this.statsData.observe(owner, stats -> {
      if (stats == null) {
        return;
      }
      StatsData formatted = create(stats);
      acceptedDistance.set(formatted.getAcceptedDistance());
      rejectedDistance.set(formatted.getRejectedDistance());
      obdDistance.set(formatted.getObdDistance());
      images.set(formatted.getTotalPhotos());
      tracks.set(formatted.getTotalTracks());
    });
  }
}
