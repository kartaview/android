package com.telenav.osv.obd.connect;

import com.telenav.osv.common.adapter.model.GeneralItemBase;
import com.telenav.osv.common.model.base.BasePresenter;
import com.telenav.osv.common.model.base.BaseView;
import com.telenav.osv.common.model.base.KVBaseFragment;

import java.util.List;

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
         *
         * @param fragment the fragment to be displayed.
         * @param tag      the tag of the fragment that should be displayed.
         */
        void displayFragment(KVBaseFragment fragment, String tag);

    }

    /**
     * Interface containing all the available operations for the OBD connection.
     */
    interface ConnectToObdPresenter extends BasePresenter {

        /**
         * @return a list with {@link GeneralItemBase} elements representing the OBD connection options.
         */
        List<GeneralItemBase> getObdConnectionOptions();
    }
}
