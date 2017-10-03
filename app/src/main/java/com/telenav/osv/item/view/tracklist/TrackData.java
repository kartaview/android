package com.telenav.osv.item.view.tracklist;

import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.text.SpannableString;
import com.telenav.osv.item.Sequence;
import java.lang.ref.WeakReference;

/**
 * Created by kalmanb on 8/31/17.
 */
public class TrackData {

  private SpannableString valueSpannable;

  private String distance;

  private String frameCount;

  private String date;

  private String address;

  private @ColorRes
  int statusColorResId;

  private @StringRes
  int statusResId;

  private WeakReference<Sequence> sequenceRef;

  public void setSequenceRef(WeakReference<Sequence> ref) {
    this.sequenceRef = ref;
  }

  public void setValueSpannable(SpannableString valueSpannable) {
    this.valueSpannable = valueSpannable;
  }

  public void setDistance(String distance) {
    this.distance = distance;
  }

  public void setFrameCount(String frameCount) {
    this.frameCount = frameCount;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public void setStatusColorResId(@ColorRes int statusColorResId) {
    this.statusColorResId = statusColorResId;
  }

  public void setStatusResId(@StringRes int statusResId) {
    this.statusResId = statusResId;
  }

  public SpannableString getValueSpannable() {
    return valueSpannable;
  }

  public String getDistance() {
    return distance;
  }

  public String getFrameCount() {
    return frameCount;
  }

  public String getDate() {
    return date;
  }

  public String getAddress() {
    return address;
  }

  public @ColorRes
  int getStatusColorResId() {
    return statusColorResId;
  }

  public @StringRes
  int getStatusResId() {
    return statusResId;
  }

  public WeakReference<Sequence> getSequenceRef() {
    return sequenceRef;
  }

  @Override
  public String toString() {
    return "TrackData{" +
        "valueSpannable=" + valueSpannable +
        ", distance='" + distance + '\'' +
        ", frameCount='" + frameCount + '\'' +
        ", date='" + date + '\'' +
        ", address='" + address + '\'' +
        ", statusColorResId=" + statusColorResId +
        ", statusResId=" + statusResId +
        ", sequenceRef=" + sequenceRef +
        '}';
  }
}
