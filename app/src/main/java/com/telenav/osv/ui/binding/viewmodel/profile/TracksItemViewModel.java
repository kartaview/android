package com.telenav.osv.ui.binding.viewmodel.profile;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.text.SpannableString;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.view.tracklist.TrackData;
import com.telenav.osv.ui.Navigator;
import java.lang.ref.WeakReference;

/**
 * ViewModel used for the sequence cards list in my profile screen
 * Created by kalmanb on 8/29/17.
 */
public class TracksItemViewModel extends BaseObservable {

  private final WeakReference<Navigator> navigator;

  @Bindable
  public boolean showValue;

  private TrackData trackData;

  public TracksItemViewModel(Navigator navigator, boolean showValue) {
    this.navigator = new WeakReference<>(navigator);
    this.showValue = showValue;
  }

  public void setTrackData(TrackData trackData) {
    this.trackData = trackData;
    notifyChange();
  }

  public void onItemClicked() {
    navigator.get().openScreen(Navigator.SCREEN_PREVIEW, trackData.getSequenceRef().get());
  }

  @Bindable
  public @StringRes
  int getSequenceStatus() {
    return trackData.getStatusResId();
  }

  @Bindable
  public Sequence getSequence() {
    return trackData.getSequenceRef().get();
  }

  @Bindable
  public @ColorRes
  int getSequenceStatusColor() {
    return trackData.getStatusColorResId();
  }

  @Bindable
  public String getSequenceTitle() {
    return trackData.getAddress();
  }

  @Bindable
  public String getSequenceDate() {
    return trackData.getDate();
  }

  @Bindable
  public String getSequencePhotoCount() {
    return trackData.getFrameCount();
  }

  @Bindable
  public String getSequenceDistance() {
    return trackData.getDistance();
  }

  @Bindable
  public SpannableString getSequenceValue() {
    return trackData.getValueSpannable();
  }
}
