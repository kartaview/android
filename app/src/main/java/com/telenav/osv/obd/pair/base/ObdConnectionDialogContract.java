package com.telenav.osv.obd.pair.base;

import com.telenav.osv.common.model.base.BasePresenter;
import com.telenav.osv.common.model.base.BaseView;
import com.telenav.osv.obd.manager.ObdManager;
import androidx.annotation.Nullable;

/**
 * The contract between the {@link ObdConnectionDialogFragment} and {@link ObdConnectionDialogPresenterImpl}.
 * @author cameliao
 */
public interface ObdConnectionDialogContract {

    /**
     * Interface defining all the available UI operations regarding the obd dialog functionality.
     */
    interface ObdConnectionDialogView extends BaseView<ObdConnectionDialogPresenter> {
        /**
         * Updates the UI when the OBD is connected.
         */
        void updateUiForObdConnected();

        /**
         * Displays the connecting dialog.
         */
        void displayConnectingDialog();

        /**
         * Displays the retry dialog.
         */
        void displayRetryDialog();

        /**
         * Dismisses all the dialogs.
         */
        void dismissDialogs();
    }

    /**
     * Interface defining all the available operation for listening to the OBD connection state.
     */
    interface ObdConnectionDialogPresenter extends BasePresenter, ObdManager.ObdConnectionListener {

        /**
         * Connects the device to the OBD.
         */
        void connect(@ObdManager.ObdTypes int obdTypes, @Nullable String address);

        /**
         * Retries to connect to the OBD.
         */
        void retryConnecting();

        /**
         * Disconnects the device from OBD.
         */
        void disconnect();

        /**
         * Unregister the listeners and releases all the used objects.
         */
        void release();

        /**
         * Method which based on {@link ObdManager#getObdState()} will init a view dialog display.
         */
        void setupObdStateDialog();
    }
}
