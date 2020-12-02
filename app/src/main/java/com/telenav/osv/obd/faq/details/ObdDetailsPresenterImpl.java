package com.telenav.osv.obd.faq.details;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.utils.Log;
import static com.telenav.osv.obd.faq.details.ObdDetailsContract.ObdDetailsPresenter;
import static com.telenav.osv.obd.faq.details.ObdDetailsContract.ObdDetailsView;

/**
 * The concrete implementation for {@code ObdDetailsPresenter}.
 * @author horatiuf
 * @see ObdDetailsPresenter
 */

public class ObdDetailsPresenterImpl implements ObdDetailsPresenter {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ObdDetailsPresenterImpl.class.getSimpleName();

    /**
     * Flag that signal if the current settings are set to imperial.
     */
    private boolean imperial;

    /**
     * Default constructor for the current class.
     */
    public ObdDetailsPresenterImpl(ObdDetailsView obdDetailsView, ApplicationPreferences applicationPreferences) {
        imperial = !applicationPreferences.getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);

        obdDetailsView.setPresenter(this);
    }

    @Override
    public void start() {

    }

    @Override
    public void openRecommendation(@ObdDetailsRecommendations String recommendation, Context context) {
        if (recommendation == null) {
            Log.d(TAG, "The url cannot be null.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(recommendation));
        context.startActivity(intent);
    }

    @Override
    public boolean isImperial() {
        return imperial;
    }
}
