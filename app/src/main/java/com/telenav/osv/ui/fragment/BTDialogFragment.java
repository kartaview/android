package com.telenav.osv.ui.fragment;

import java.util.Set;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.manager.obd.ObdBtManager;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.ui.list.BTDeviceAdapter;
import com.telenav.osv.utils.Log;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class BTDialogFragment extends DialogFragment {

    /**
     * Tag for Log
     */
    public static final String TAG = "BTDialogFragment";

    /**
     * Member fields
     */
    private BluetoothAdapter mBtAdapter;

    /**
     * Newly discovered devices
     */
    private BTDeviceAdapter mNewDevicesAdapter;

    private MainActivity activity;

    private View view;

    private OnDeviceSelectedListener mDeviceSelectedListener;

    private ImageView mScanButton;

    private View.OnClickListener mScanOnClickListener;

    private TextView mNewDevicesTitle;

    private ListView mNewDevicesListView;

    private ListView mPairedListView;

    private TextView mDevicesTitle;

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED && device.getName() != null) {
                    mNewDevicesAdapter.addDevice(device.getName(), device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.bt_select_device_label);
                if (mNewDevicesAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.bt_none_found).toString();
                    mNewDevicesAdapter.addDevice(noDevices, "");
                }
            }
        }
    };

    /**
     * shared preferences
     */
    private SharedPreferences preferences;

    private ObdBtManager mObdBtManager;

    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            TextView addressText = (TextView) v.findViewById(R.id.device_address);
            String address = addressText.getText().toString();
            if (address.length() > 0) {
                preferences.edit()
                        .putString(Constants.EXTRAS_BT_DEVICE_ADDRESS, address)
                        .apply();
                mDeviceSelectedListener.onDeviceSelected(address);

                mObdBtManager.onDeviceSelected(address);
                mObdBtManager.connect();
                dismiss();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view  = inflater.inflate(R.layout.fragment_devices_list,container, false);
        activity = (MainActivity) getActivity();
        preferences = activity.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
        try {
            mObdBtManager = (ObdBtManager) activity.getApp().getRecorder().getOBDManager();
        } catch (ClassCastException e){
            activity.getApp().getRecorder().createObdManager(PreferenceTypes.V_OBD_BLE);
            mObdBtManager = (ObdBtManager) activity.getApp().getRecorder().getOBDManager();
        }
        // Initialize the button to perform device discovery
        mScanButton = (ImageView) view.findViewById(R.id.refresh_button);
        mPairedListView = (ListView) view.findViewById(R.id.devices_list);
        mNewDevicesTitle = (TextView) view.findViewById(R.id.new_devices_fragment_title);
        mDevicesTitle = (TextView) view.findViewById(R.id.devices_fragment_title);
        mNewDevicesListView = (ListView) view.findViewById(R.id.new_devices_list);
        mScanOnClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
            }
        };
        mScanButton.setOnClickListener(mScanOnClickListener);

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        BTDeviceAdapter adapter = new BTDeviceAdapter(activity);
        mNewDevicesAdapter = new BTDeviceAdapter(activity);

        // Find and set up the ListView for paired devices
        mPairedListView.setAdapter(adapter);
        mPairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        mNewDevicesListView.setVisibility(View.VISIBLE);
        mNewDevicesListView.setAdapter(mNewDevicesAdapter);
        mNewDevicesListView.setOnItemClickListener(mDeviceClickListener);
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.getApplicationContext().registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.getApplicationContext().registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            ((TextView) view.findViewById(R.id.devices_fragment_title)).setText(R.string.bt_paired_devices_label);
            for (BluetoothDevice device : pairedDevices) {
                adapter.addDevice(device.getName(), device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.bt_none_paired).toString();
            adapter.addDevice(noDevices, "");
        }
        doDiscovery();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        activity.getApplicationContext().unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.bt_scanning_label);

        // Turn on sub-title for new devices
        mNewDevicesTitle.setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    private void setTitle(@StringRes int resource) {
        if (mDevicesTitle != null) {
            mDevicesTitle.setText(resource);
        }
    }

    private void setProgressBarIndeterminateVisibility(boolean visible) {
        if (visible){
            Animation animation = new RotateAnimation(0.0f, 360.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                    0.5f);
            animation.setRepeatCount(-1);
            animation.setDuration(2000);
            mScanButton.startAnimation(animation);
            mScanButton.setOnClickListener(null);
        } else {
            mScanButton.clearAnimation();
            mScanButton.setOnClickListener(mScanOnClickListener);

        }
    }

    public void setListener(OnDeviceSelectedListener listener) {
        this.mDeviceSelectedListener = listener;
    }


    public interface OnDeviceSelectedListener {
        void onDeviceSelected(String device);
    }
}
