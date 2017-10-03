package com.telenav.osv.ui.binding.viewmodel.profile;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.text.SpannableString;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.item.view.tracklist.StatsData;

/**
 * ViewModel used for the sequence cards list in my profile screen
 * Created by kalmanb on 8/29/17.
 */
public class TracksHeaderViewModel extends BaseObservable {

    private final ValueFormatter valueFormatter;

    private final Context context;

    @Bindable
    public boolean showHeader;

    private StatsData stats = new StatsData();

    public TracksHeaderViewModel(Context context, ValueFormatter valueFormatter, boolean showHeader) {
        this.valueFormatter = valueFormatter;
        this.showHeader = showHeader;
        this.context = context;
    }

    public void setStats(StatsData stats) {
        if (stats != null) {
            this.stats = stats;
            notifyChange();
        }
    }

    @Bindable
    public SpannableString getImagesText() {
        return stats.getTotalPhotos();
    }

    @Bindable
    public SpannableString getDistanceText() {
        return stats.getAcceptedDistance();
    }

    @Bindable
    public SpannableString getAcceptedDistanceText() {
        return stats.getAcceptedDistance();
    }

    @Bindable
    public SpannableString getRejectedDistanceText() {
        return stats.getRejectedDistance();
    }

    @Bindable
    public SpannableString getObdDistanceText() {
        return stats.getObdDistance();
    }

    @Bindable
    public SpannableString getTracksText() {
        return stats.getTotalTracks();
    }
}
