package com.telenav.osv.obd.pair.ble.guide;

import android.content.Context;
import com.google.android.material.snackbar.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.obd.ObdFunctionalState;
import com.telenav.osv.obd.manager.ObdManager;

/**
 * The implementation class of the {@link com.telenav.osv.obd.pair.ble.guide.ObdBlePairGuideContract.ObdBlePairGuidePresenter}.
 * @author cameliao
 */

class ObdBlePairGuidePresenterImpl implements ObdBlePairGuideContract.ObdBlePairGuidePresenter {

    /**
     * Instance of the View used to update the UI.
     */
    private ObdBlePairGuideContract.ObdBlePairGuideView view;

    /**
     * Instance to the {@code ObdManager} component.
     */
    private ObdManager obdManager;

    /**
     * Default constructor of the class.
     * @param view the instance of the view.
     * @param obdManager the instance of the {@code ObdManager} component.
     */
    ObdBlePairGuidePresenterImpl(ObdBlePairGuideContract.ObdBlePairGuideView view, ObdManager obdManager) {
        this.view = view;
        this.obdManager = obdManager;
        view.setPresenter(this);
    }

    @Override
    public void start() {}

    @Override
    public void checkObdBleState(Context context) {
        if (!obdManager.isConnected()) {
            switch (obdManager.isObdBleFunctional(context)) {
                case ObdFunctionalState.OBD_FUNCTIONAL:
                    view.displayChooseDevicesScreen();
                    break;
                case ObdFunctionalState.OBD_NOT_SUPPORTED:
                    view.showSnackBar(R.string.error_bluetooth_not_supported, Snackbar.LENGTH_SHORT);
                    break;
                case ObdFunctionalState.OBD_NEED_PERMISSIONS:
                    view.requestBluetoothPermissions();
                    break;
            }
        }
    }
}
