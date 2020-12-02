package com.telenav.osv.obd.pair.ble.guide;

import android.content.Context;
import com.telenav.osv.common.model.base.BasePresenter;
import com.telenav.osv.common.model.base.BaseView;
import androidx.annotation.StringRes;

/**
 * The contract between {@link ObdBlePairGuideFragment} and {@link com.telenav.osv.obd.pair.ble.devices.ObdBleDevicesPresenterImpl}.
 * @author cameliao
 */

interface ObdBlePairGuideContract {

    /**
     * The interface of the OBD pair guide View containing all the available UI operations.
     */
    interface ObdBlePairGuideView extends BaseView<ObdBlePairGuidePresenter> {
        /**
         * Displays the OBD devices screen.
         */
        void displayChooseDevicesScreen();

        /**
         * Displays a snack bar.
         * @param messageId the message that will be displayed.
         * @param duration the duration for the snack display.
         */
        void showSnackBar(@StringRes int messageId, int duration);

        /**
         * Requests the bluetooth permissions. If the {@code resultCode} received in {@code onActivityResult} method is OK the OBD devices screen is displayed.
         */
        void requestBluetoothPermissions();
    }

    /**
     * The interface of the OBD pair guide Presenter containing all the business logic operations.
     */
    interface ObdBlePairGuidePresenter extends BasePresenter {
        /**
         * Checks if the OBD BLE is connected.
         * If the OBD is functional the OBD devices screen will be displayed.
         * If the OBD is not supported than a snack bar containing a message will be displayed.
         * If the OBD is needs permissions for bluetooth, than the permissions will be requested.
         * @param context the context of the app to check if the OBD is functional.
         */
        void checkObdBleState(Context context);
    }
}
