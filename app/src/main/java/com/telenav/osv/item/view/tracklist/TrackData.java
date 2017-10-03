package com.telenav.osv.item.view.tracklist;

import java.lang.ref.WeakReference;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.text.SpannableString;
import com.telenav.osv.item.Sequence;

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

    public SpannableString getValueSpannable() {
        return valueSpannable;
    }

    public void setValueSpannable(SpannableString valueSpannable) {
        this.valueSpannable = valueSpannable;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(String frameCount) {
        this.frameCount = frameCount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public @ColorRes
    int getStatusColorResId() {
        return statusColorResId;
    }

    public void setStatusColorResId(@ColorRes int statusColorResId) {
        this.statusColorResId = statusColorResId;
    }

    public @StringRes
    int getStatusResId() {
        return statusResId;
    }

    public void setStatusResId(@StringRes int statusResId) {
        this.statusResId = statusResId;
    }

    public WeakReference<Sequence> getSequenceRef() {
        return sequenceRef;
    }

    public void setSequenceRef(WeakReference<Sequence> ref) {
        this.sequenceRef = ref;
    }
}
