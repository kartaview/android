package com.telenav.osv.item.view.tracklist;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import com.telenav.osv.R;
import com.telenav.osv.application.ValueFormatter;

/**
 * Created by kalmanb on 9/1/17.
 */
public class StatsDataFactory {

    public static StatsData create(Context context, ValueFormatter valueFormatter, double accepted, double rejected, double obd, int tracks,
                                   int photos) {
        StatsData statsData = new StatsData();

        String imagesText = context.getString(R.string.account_images_label);
        String distanceText = context.getString(R.string.account_distance_label);
        String acceptedText = context.getString(R.string.account_distance_accepted_label);
        String rejectedText = context.getString(R.string.account_distance_rejected_label);
        String obdText = context.getString(R.string.account_obd_label);
        String tracksText = context.getString(R.string.account_tracks_label);

        statsData.setDistance(getSpannableForStats(context, distanceText, valueFormatter.formatDistanceFromKiloMetersFlat(accepted)));
        statsData.setAcceptedDistance(getSpannableForStats(context, acceptedText, valueFormatter.formatDistanceFromKiloMetersFlat(accepted)));
        statsData.setRejectedDistance(getSpannableForStats(context, rejectedText, valueFormatter.formatDistanceFromKiloMetersFlat(rejected)));
        statsData.setObdDistance(getSpannableForStats(context, obdText, valueFormatter.formatDistanceFromKiloMetersFlat(obd)));
        statsData.setTotalPhotos(getSpannableForStats(context, imagesText, valueFormatter.formatNumber(photos)));

        String second = valueFormatter.formatNumber(tracks);
        SpannableString styledString = new SpannableString(tracksText + second);
        styledString.setSpan(new StyleSpan(Typeface.NORMAL), 0, tracksText.length(), 0);
        styledString.setSpan(new StyleSpan(Typeface.BOLD), tracksText.length(), second.length() + tracksText.length(), 0);
        statsData.setTotalTracks(styledString);
        return statsData;
    }

    private static SpannableString getSpannableForStats(Context context, String first, String second) {
        SpannableString styledString = new SpannableString(first + second);
        styledString.setSpan(new TextAppearanceSpan(context, R.style.profileHeaderInfoTextSmall), 0, first.length(), 0);
        styledString
                .setSpan(new TextAppearanceSpan(context, R.style.profileHeaderInfoTextLarge), first.length(), second.length() + first.length(), 0);
        return styledString;
    }
}
