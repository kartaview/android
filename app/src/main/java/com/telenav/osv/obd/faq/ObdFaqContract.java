package com.telenav.osv.obd.faq;

import android.content.Context;

import com.telenav.osv.common.adapter.model.GeneralItemBase;
import com.telenav.osv.common.model.base.BasePresenter;
import com.telenav.osv.common.model.base.BaseView;

import java.util.List;

/**
 * Contract for profile details.
 *
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
         * Open the email for the user to send a message to KV support team.
         *
         * @param context the {@code Context} required in order to open the email chooser.
         */
        void contactKvSupport(Context context);
    }
}
