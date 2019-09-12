package com.telenav.osv.obd.faq;

import java.util.List;
import android.content.Context;
import com.telenav.osv.common.adapter.model.GeneralItemBase;
import com.telenav.osv.common.model.base.BasePresenter;
import com.telenav.osv.common.model.base.BaseView;

/**
 * Contract for profile details.
 * @author horatiuf
 */

public interface ObdFaqContract {

    /**
     * The interface representing all the view functionality available for the presenter.
     */
    interface ObdFaqView extends BaseView<ObdFaqPresenter> {

    }

    /**
     * The interface representing the functionality available to the view.
     */
    interface ObdFaqPresenter extends BasePresenter {

        /**
         * @return {@code List<SettingsItemBase>} representing the obd faq settings.
         */
        List<GeneralItemBase> getObdFaqSettings();

        /**
         * Open the email for the user to send a message to OSC support team.
         * @param context the {@code Context} required in order to open the email chooser.
         */
        void contactOscSupport(Context context);
    }
}
