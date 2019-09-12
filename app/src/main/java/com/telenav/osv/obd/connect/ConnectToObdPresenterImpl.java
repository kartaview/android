package com.telenav.osv.obd.connect;

import java.util.ArrayList;
import java.util.List;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.adapter.model.GeneralItemBase;
import com.telenav.osv.obd.model.LeftIconTitleSubtitleItem;
import com.telenav.osv.obd.pair.ble.guide.ObdBlePairGuideFragment;
import com.telenav.osv.obd.pair.wifi.guide.ObdWiFiPairGuideFragment;
import androidx.annotation.NonNull;

/**
 * The implementation class of the {@link com.telenav.osv.obd.connect.ConnectToObdContract.ConnectToObdPresenter}.
 * @author cameliao
 */

class ConnectToObdPresenterImpl implements ConnectToObdContract.ConnectToObdPresenter {

    /**
     * The instance of the {@link com.telenav.osv.obd.connect.ConnectToObdContract.ConnectToObdView}, used for UI updates.
     */
    private ConnectToObdContract.ConnectToObdView view;

    /**
     * Flag that shows if the logged in user is a byod user.
     */
    private boolean byod;

    /**
     * Default constructor for the current class.
     * @param view the instance of view, for UI updates.
     */
    ConnectToObdPresenterImpl(@NonNull ConnectToObdContract.ConnectToObdView view, ApplicationPreferences applicationPreferences) {
        this.view = view;
        this.byod = applicationPreferences.getIntPreference(PreferenceTypes.K_USER_TYPE) == PreferenceTypes.USER_TYPE_BYOD;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public List<GeneralItemBase> getObdConnectionOptions() {
        List<GeneralItemBase> optionsList = new ArrayList<>();
        optionsList.add(new LeftIconTitleSubtitleItem(R.string.connect_to_obd_over_bluetooth, R.string.obd_connect_get_started, R.drawable.vector_bluetooth,
                clickConsumer -> view.displayFragment(ObdBlePairGuideFragment.newInstance(), ObdBlePairGuideFragment.TAG)));
        optionsList.add(new LeftIconTitleSubtitleItem(R.string.connect_to_obd_over_wifi, R.string.obd_connect_get_started, R.drawable.vector_wi_fi,
                clickConsumer -> view.displayFragment(ObdWiFiPairGuideFragment.newInstance(), ObdWiFiPairGuideFragment.TAG)));
        return optionsList;
    }

    @Override
    public boolean isByod() {
        return byod;
    }
}
