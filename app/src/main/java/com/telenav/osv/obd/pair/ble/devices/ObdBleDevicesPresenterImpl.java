package com.telenav.osv.obd.pair.ble.devices;

import java.util.Set;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.obd.pair.base.ObdConnectionDialogPresenterImpl;
import com.telenav.osv.utils.Log;

/**
 * The implementation class for the {@link com.telenav.osv.obd.pair.ble.devices.ObdBleDevicesContract.ObdBleDevicesPresenter}.
 * @author cameliao
 */

class ObdBleDevicesPresenterImpl extends ObdConnectionDialogPresenterImpl implements ObdBleDevicesContract.ObdBleDevicesPresenter {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ObdBleDevicesPresenterImpl.class.getSimpleName();

    /**
     * The View interface to communicate with the UI.
     */
    private ObdBleDevicesContract.ObdBleDevicesView view;

    /**
     * The bluetooth adapter used to discover the around bluetooth devices.
     */
    private BluetoothAdapter mBtAdapter;

    /**
     * Instance to the {@code ApplicationPreferences}.
     */
    private ApplicationPreferences applicationPreferences;

    /**
     * Default constructor of the class.
     * @param view the interface to communicate with the View
     * @param obdManager the instance {@code ObdManager}.
     */
    ObdBleDevicesPresenterImpl(ApplicationPreferences applicationPreferences, ObdBleDevicesContract.ObdBleDevicesView view, ObdManager obdManager) {
        super(view, obdManager);
        this.view = view;
        this.applicationPreferences = applicationPreferences;
        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void start() {
        // Get a set of currently paired devices
        super.start();
        mBtAdapter.isEnabled();
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        view.addPairedDevices(pairedDevices);
    }

    @Override
    public void onDeviceSelected(String address) {
        Log.d(TAG, String.format("Device selected. Address: %s", address));
        if (address != null && !address.isEmpty()) {
            mBtAdapter.cancelDiscovery();
            applicationPreferences.saveBooleanPreference(PreferenceTypes.K_OBD_MANUAL_STOPPED, false);
            connect(ObdManager.ObdTypes.BLE, address);
        }
    }

    @Override
    public void cancelDevicesDiscovery() {
        if (mBtAdapter != null && mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
    }

    @Override
    public void startDiscoveringDevices() {
        mBtAdapter.startDiscovery();
    }
}
