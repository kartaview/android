package com.telenav.osv.obd.pair.ble.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.telenav.osv.R;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.adapter.GeneralSettingsAdapter;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.model.HeaderSettingItem;
import com.telenav.osv.obd.model.IconTitleBtDeviceItem;
import com.telenav.osv.obd.pair.base.ObdConnectionDialogFragment;
import com.telenav.osv.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The OBD BLE Devices fragment displaying all the around bluetooth devices. The class implements
 * {@link com.telenav.osv.obd.pair.ble.devices.ObdBleDevicesContract.ObdBleDevicesView}.
 * @author cameliao
 */

public class ObdBleDevicesFragment extends ObdConnectionDialogFragment implements ObdBleDevicesContract.ObdBleDevicesView {

    public static final String TAG = ObdBleDevicesFragment.class.getSimpleName();

    /**
     * The instance of the view business logic.
     */
    private ObdBleDevicesContract.ObdBleDevicesPresenter devicesPresenter;

    /**
     * Adapter class displaying the paired and the available bluetooth devices.
     */
    private GeneralSettingsAdapter devicesAdapter;

    /**
     * A flag representing if the list of bluetooth devices is refreshing.
     */
    private boolean isRefreshing;

    /**
     * The number of paired devices used for clearing all the available devices from adapter while refreshing.
     */
    private int numberOfPairedDevices = 0;

    /**
     * The list of available bluetooth devices, in order to display a device only one time.
     */
    private List<BluetoothDevice> bluetoothDevices;

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                isRefreshing = false;
                devicesAdapter.updateItem(new HeaderSettingItem(R.string.obd_devices_available_devices, true));
            }
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED && device.getName() != null) {
                    if (!bluetoothDevices.contains(device)) {
                        devicesAdapter.addItem(new IconTitleBtDeviceItem(device.getName(), device.getAddress(), R.drawable.vector_bluetooth_settings, true, devicesPresenter));
                        bluetoothDevices.add(device);
                    }
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (!isRefreshing) {
                    devicesAdapter.updateItem(new HeaderSettingItem(R.string.obd_devices_available_devices, false));
                }
            }
        }
    };

    /**
     * Factory method to create a new instance for the current fragment.
     * @return a new instance of the {@code ObdBleDevicesFragment}.
     */
    public static ObdBleDevicesFragment newInstance() {
        return new ObdBleDevicesFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KVApplication kvApplication = (KVApplication) getActivity().getApplication();
        devicesPresenter = new ObdBleDevicesPresenterImpl(kvApplication.getAppPrefs(),
                this,
                Injection.provideObdManager(kvApplication.getApplicationContext(), kvApplication.getAppPrefs()));
        bluetoothDevices = new ArrayList<>();
        devicesAdapter = new GeneralSettingsAdapter();
        registerBluetoothReceivers();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View obdBleDevicesLayout = inflater.inflate(R.layout.fragment_obd_ble_devices, container, false);
        SwipeRefreshLayout swipeRefreshLayout = obdBleDevicesLayout.findViewById(R.id.swipe_refresh_obd_ble_devices);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            doDiscovery();
            swipeRefreshLayout.setRefreshing(false);
            isRefreshing = true;
        });
        initObdBleDevicesList(obdBleDevicesLayout);
        devicesPresenter.start();
        return obdBleDevicesLayout;
    }

    @Override
    public void addPairedDevices(Set<BluetoothDevice> pairedDevices) {
        // If there are paired devices, addChild each one to the DeviceAdapter
        if (pairedDevices.size() > 0) {
            devicesAdapter.addItem(new HeaderSettingItem(R.string.obd_devices_paired_devices, false));
            for (BluetoothDevice device : pairedDevices) {
                devicesAdapter.addItem(new IconTitleBtDeviceItem(device.getName(), device.getAddress(), R.drawable.vector_bluetooth_settings, pairedDevices.size() > 1,
                        devicesPresenter));
            }
            this.numberOfPairedDevices = pairedDevices.size() + 1;
        }
        devicesAdapter.addItem(new HeaderSettingItem(R.string.obd_devices_available_devices, true));
        doDiscovery();
    }

    @Override
    public ToolbarSettings.Builder getToolbarSettings() {
        return new ToolbarSettings.Builder()
                .setTitle(R.string.obd_ble_pair_guide_choose_devices);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        devicesPresenter.cancelDevicesDiscovery();
        getContext().unregisterReceiver(mReceiver);
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    /**
     * Registers the broadcast receiver for discovered bluetooth devices.
     */
    private void registerBluetoothReceivers() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getContext().registerReceiver(mReceiver, filter);
    }

    /**
     * Initializes the list of bluetooth devices.
     */
    private void initObdBleDevicesList(View view) {
        RecyclerView recyclerViewDevices = view.findViewById(R.id.list_obd_ble_available_devices);
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewDevices.setAdapter(devicesAdapter);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Log.d(TAG, "doDiscovery: ");
        // Indicate scanning in the title
        devicesPresenter.cancelDevicesDiscovery();
        bluetoothDevices.clear();
        devicesAdapter.clearFromIndex(numberOfPairedDevices + 1);
        devicesPresenter.startDiscoveringDevices();

    }
}

