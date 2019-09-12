package com.telenav.osv.obd.connect;

import java.util.List;
import com.telenav.osv.common.adapter.model.GeneralItemBase;
import com.telenav.osv.common.model.base.BasePresenter;
import com.telenav.osv.common.model.base.BaseView;
import com.telenav.osv.common.model.base.OSCBaseFragment;

/**
 * The contract between {@link ConnectToObdFragment} and {@link ConnectToObdPresenterImpl}.
 * @author cameliao
 */

interface ConnectToObdContract {

    /**
     * Interface defining all the available UI operations, used to update the UI.
     */
    interface ConnectToObdView extends BaseView<ConnectToObdPresenter> {
        /**
         * Displays the given fragment by replacing the current one.
         * @param fragment the fragment to be displayed.
         * @param tag the tag of the fragment that should be displayed.
         */
        void displayFragment(OSCBaseFragment fragment, String tag);

    }

    /**
     * Interface containing all the available operations for the OBD connection.
     */
    interface ConnectToObdPresenter extends BasePresenter {

        /**
         * @return a list with {@link GeneralItemBase} elements representing the OBD connection options.
         */
        List<GeneralItemBase> getObdConnectionOptions();

        /**
         * @return {@code true} if the logged in user is byod user, {@code false} otherwise.
         */
        boolean isByod();
    }
}
