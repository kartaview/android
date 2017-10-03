package com.telenav.osv.ui.binding.viewmodel.profile.newimpl;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import com.telenav.osv.R;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.item.view.profile.StatisticsData;
import com.telenav.osv.item.view.tracklist.StatsData;

/**
 * todo only used in new profile fragment impl.
 * Created by kalmanb on 9/29/17.
 */
public class TrackListHeaderViewModel extends ListItemViewModel {

    private final ValueFormatter valueFormatter;

    public TrackListHeaderViewModel(Application application, ValueFormatter valueFormatter) {
        super(application);
        this.valueFormatter = valueFormatter;
    }

    private static SpannableString getSpannableForStats(Context context, String first, String second) {
        SpannableString styledString = new SpannableString(first + second);
        styledString.setSpan(new TextAppearanceSpan(context, R.style.profileHeaderInfoTextSmall), 0, first.length(), 0);
        styledString
                .setSpan(new TextAppearanceSpan(context, R.style.profileHeaderInfoTextLarge), first.length(), second.length() + first.length(), 0);
        return styledString;
    }

    public StatsData create(StatisticsData data) {
        StatsData statsData = new StatsData();
        Context context = getApplication();

        String imagesText = context.getString(R.string.account_images_label);
        String distanceText = context.getString(R.string.account_distance_label);
        String acceptedText = context.getString(R.string.account_distance_accepted_label);
        String rejectedText = context.getString(R.string.account_distance_rejected_label);
        String obdText = context.getString(R.string.account_obd_label);
        String tracksText = context.getString(R.string.account_tracks_label);

        statsData.setDistance(getSpannableForStats(context, distanceText, valueFormatter.formatDistanceFromKiloMetersFlat(data.getDistance())));
        statsData.setAcceptedDistance(
                getSpannableForStats(context, acceptedText, valueFormatter.formatDistanceFromKiloMetersFlat(data.getAcceptedDistance())));
        statsData.setRejectedDistance(
                getSpannableForStats(context, rejectedText, valueFormatter.formatDistanceFromKiloMetersFlat(data.getRejectedDistance())));
        statsData
                .setObdDistance(getSpannableForStats(context, obdText, valueFormatter.formatDistanceFromKiloMetersFlat(data.getObdDistance())));
        statsData.setTotalPhotos(getSpannableForStats(context, imagesText, valueFormatter.formatNumber(data.getTotalPhotos())));

        String second = valueFormatter.formatNumber(data.getTotalTracks());
        SpannableString styledString = new SpannableString(tracksText + second);
        styledString.setSpan(new StyleSpan(Typeface.NORMAL), 0, tracksText.length(), 0);
        styledString.setSpan(new StyleSpan(Typeface.BOLD), tracksText.length(), second.length() + tracksText.length(), 0);
        statsData.setTotalTracks(styledString);
        return statsData;
    }
}
