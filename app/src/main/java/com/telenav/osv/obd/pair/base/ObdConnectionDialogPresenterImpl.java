package com.telenav.osv.obd.pair.base;

import com.telenav.osv.item.SpeedData;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.obd.manager.ObdManager.ObdTypes;
import com.telenav.osv.utils.Log;
import androidx.annotation.Nullable;

/**
 * The implementation class of the {@link ObdConnectionDialogContract.ObdConnectionDialogPresenter}.
 * @author cameliao
 */
public class ObdConnectionDialogPresenterImpl implements ObdConnectionDialogContract.ObdConnectionDialogPresenter {

    private static final String TAG = ObdConnectionDialogPresenterImpl.class.getSimpleName();

    /**
     * The View instance to update the UI.
     */
    protected ObdConnectionDialogContract.ObdConnectionDialogView view;

    /**
     * The instance of the recorder, containing the logic to communicate with the OBD.
     */
    protected ObdManager obdManager;

    /**
     * Default constructor for the current class.
     * @param view the view instance.
     * @param obdManager the instance of the {@code ObdManager} component.
     */
    public ObdConnectionDialogPresenterImpl(ObdConnectionDialogContract.ObdConnectionDialogView view, ObdManager obdManager) {
        this.view = view;
        view.setPresenter(this);
        this.obdManager = obdManager;
    }

    @Override
    public void start() {
        obdManager.addObdConnectionListener(this);
    }

    @Override
    public void setupObdStateDialog() {
        switch (obdManager.getObdState()) {
            case ObdManager.ObdState.OBD_DISCONNECTED:
                view.displayRetryDialog();
                break;
            case ObdManager.ObdState.OBD_CONNECTED:
                view.updateUiForObdConnected();
                break;
            default:
                view.displayConnectingDialog();
        }
    }

    @Override
    public void release() {
        obdManager.removeObdConnectionListener(this);
    }

    @Override
    public void disconnect() {
        obdManager.stopCollecting();
        view.dismissDialogs();
    }

    @Override
    public void connect(@ObdManager.ObdTypes int obdType, @Nullable String address) {
        boolean setupObd;
        if (obdType == ObdTypes.BLE && address != null) {
            setupObd = obdManager.setupObd(address);
        } else {
            setupObd = obdManager.setupObd(obdType);
        }
        Log.d(TAG, String.format("OBD setup. Type: %s. Address: %s.Success status: %s", obdType, address, setupObd));
        if (setupObd) {
            startCollecting();
        }
    }

    @Override
    public void retryConnecting() {
        stopCollecting();
        startCollecting();
    }

    @Override
    public void onSpeedObtained(SpeedData speed) {
        Log.d(TAG, String.format("Obd speed received: %s", speed.getSpeed()));
        if (speed.getSpeed() != -1) {
            view.updateUiForObdConnected();
        }
    }

    @Override
    public void onObdConnected() {
        Log.d(TAG, "onObdConnected");
    }

    @Override
    public void onObdDisconnected() {
        Log.d(TAG, "onObdDisconnected");
        view.displayRetryDialog();
    }

    @Override
    public void onObdConnecting() {
        Log.d(TAG, "onObdConnecting");
    }

    @Override
    public void onObdInitialised() {
        Log.d(TAG, "onObdInitialised");
        view.displayConnectingDialog();
    }

    /**
     * Starts the collecting start for the obd speed data.
     */
    private void startCollecting() {
        obdManager.startCollecting();
    }

    /**
     * Stops the collecting start for the obd speed data.
     */
    private void stopCollecting() {
        obdManager.stopCollecting();
    }
}
