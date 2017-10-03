package com.telenav.osv.item.view.tracklist;

import android.graphics.Typeface;
import android.support.annotation.StringRes;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import com.telenav.osv.R;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.item.OnlineSequence;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.utils.Utils;
import java.lang.ref.WeakReference;

/**
 * converts Sequence to TrackData info holder
 * Created by kalmanb on 8/31/17.
 */
public class TrackDataFactory {

  public static TrackData create(Sequence sequence, ValueFormatter valueFormatter) {
    TrackData trackData = new TrackData();
    trackData.setSequenceRef(new WeakReference<>(sequence));
    trackData.setAddress(sequence.getAddress());

    trackData.setDate(Utils.numericCardDateFormat.format(sequence.getDate()));
    String[] distance = valueFormatter.formatDistanceFromMeters(sequence.getDistance());
    trackData.setDistance(distance[0] + distance[1]);
    trackData.setFrameCount(String.valueOf(sequence.getFrameCount()));
    trackData.setStatusResId(getFormattedStatus(sequence.getServerStatus()));
    int resId = getFormattedStatusColor(sequence.getServerStatus());
    trackData.setStatusColorResId(resId);
    SpannableString styledString;
    String first = "";
    String second = "";
    if (sequence.isUserTrack()) {
      if (sequence.getScore() > 10000) {
        first = sequence.getScore() / 1000 + "K\n";
      } else {
        first = sequence.getScore() + "\n";
      }
      second = "pts";
    } else {
      if (sequence.hasValue()) {
        first = valueFormatter.formatMoneyConstrained(sequence.getValue()) + "\n";
        second = sequence.getCurrency();
      }
    }
    styledString = new SpannableString(first + second);
    styledString.setSpan(new StyleSpan(Typeface.BOLD), 0, first.length(), 0);
    styledString.setSpan(new StyleSpan(Typeface.NORMAL), first.length(), second.length() + first.length(), 0);
    styledString.setSpan(new AbsoluteSizeSpan(16, true), 0, first.length(), 0);
    styledString.setSpan(new AbsoluteSizeSpan(12, true), first.length(), second.length() + first.length(), 0);
    trackData.setValueSpannable(styledString);
    return trackData;
  }

  private static int getFormattedStatusColor(String status) {
    switch (status) {
      default:
      case OnlineSequence.SERVER_STATUS_PROCESSED:
        return -1;
      case OnlineSequence.SERVER_STATUS_UPLOADING:
      case OnlineSequence.SERVER_STATUS_APPROVED:
        return R.color.sequence_card_status_text_color_green;
      case OnlineSequence.SERVER_STATUS_UPLOADED:
      case OnlineSequence.SERVER_STATUS_TBD:
        return R.color.sequence_card_status_text_color_blue;
      case OnlineSequence.SERVER_STATUS_REJECTED:
        return R.color.sequence_card_status_text_color_red;
    }
  }

  private static @StringRes
  int getFormattedStatus(String status) {
    switch (status) {
      default:
      case OnlineSequence.SERVER_STATUS_PROCESSED:
        return -1;
      case OnlineSequence.SERVER_STATUS_UPLOADING:
        return R.string.track_status_uploading;
      case OnlineSequence.SERVER_STATUS_UPLOADED:
        return R.string.track_status_processing;
      case OnlineSequence.SERVER_STATUS_APPROVED:
        return R.string.track_status_accepted;
      case OnlineSequence.SERVER_STATUS_REJECTED:
        return R.string.track_status_rejected;
      case OnlineSequence.SERVER_STATUS_TBD:
        return R.string.track_status_in_review;
    }
  }
}
