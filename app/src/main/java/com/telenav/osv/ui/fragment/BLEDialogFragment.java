package com.telenav.osv.ui.fragment;

import java.util.List;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.manager.ObdBleManager;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.obd.OBDConnection;
import com.telenav.osv.ui.list.LeDeviceAdapter;

/**
 * Created by Kalman on 21/06/16.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BLEDialogFragment extends DialogFragment {

    public final static String TAG = BLEDialogFragment.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 1;

    /**
     * the view of the fragment
     */
    private View root;

    /**
     * listview of ble devices
     */
    private ListView devicesList;

    /**
     * text view showed in case the sdk is not initialized
     */
    private TextView sdkNotInitializedTv;

    /**
     * the lable of the ble devices list
     */
    private TextView bleDevicesLabel;

    /**
     * adapter for the devices list
     */
    private LeDeviceAdapter mLeDeviceListAdapter;

    private BluetoothAdapter bluetoothAdapter;

    /**
     * shared preferences
     */
    private SharedPreferences preferences;

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLeDeviceListAdapter.addDevice(result.getDevice());
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "error code is:" + errorCode);
        }
    };


    private ObdBleManager mObdBleManager;

    /**
     * device item list click listener
     */
    private ListView.OnItemClickListener deviceItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
            if (device == null) return;
            // stop the scanning
            if (OBDConnection.getInstance().isScanning()) {
                OBDConnection.getInstance().stopScanning(scanCallback);
            }
            preferences.edit()
                    .putString(Constants.EXTRAS_DEVICE_ADDRESS, device.getAddress())
                    .apply();

            if (deviceSelectedListener != null) {
                deviceSelectedListener.onDeviceSelected(device);
            }

            mObdBleManager.onDeviceSelected(device);
            mObdBleManager.connect();
            dismiss();
        }
    };

    private MainActivity mActivity;

    public void setDeviceSelectedListener(OnDeviceSelectedListener deviceSelectedListener) {
        this.deviceSelectedListener = deviceSelectedListener;
    }

    private OnDeviceSelectedListener deviceSelectedListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_devices_list, container, false);
        mActivity = (MainActivity) getActivity();
        try {
            mObdBleManager = (ObdBleManager) mActivity.getApp().getOBDManager();
        } catch (ClassCastException e){
            mObdBleManager = new ObdBleManager(mActivity.getApp());
        }
        preferences = getActivity().getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

        bluetoothAdapter = OBDConnection.getInstance().initConnection(getActivity());

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(getActivity(), R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            //context.finish();
            //return;
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        initViews();

        return root;
    }

    /**
     * Initialize the view from the fragment
     */
    private void initViews() {
        devicesList = (ListView) root.findViewById(R.id.devices_list);
        devicesList.setOnItemClickListener(deviceItemClickListener);
        sdkNotInitializedTv = (TextView) root.findViewById(R.id.sdk_not_initialized);
        bleDevicesLabel = (TextView) root.findViewById(R.id.ble_devices_lable);

        mLeDeviceListAdapter = new LeDeviceAdapter(getActivity());
        devicesList.setAdapter(mLeDeviceListAdapter);
        OBDConnection.getInstance().startScanning(scanCallback);
        refreshScanMenu();
        showBleList(true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                dismiss();
                return;
            } else if (resultCode == Activity.RESULT_OK) {
                OBDConnection.getInstance().startScanning(scanCallback);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bluetoothAdapter.isEnabled()) {
            OBDConnection.getInstance().stopScanning(scanCallback);
        }
        final LeDeviceAdapter leDeviceAdapter = mLeDeviceListAdapter;
        if (leDeviceAdapter != null) {
            leDeviceAdapter.clear();
            leDeviceAdapter.notifyDataSetChanged();
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Check if scanning is running for refresing the options menu
     */
    private void refreshScanMenu() {
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    getActivity().invalidateOptionsMenu();
                    handler.postDelayed(this, 10000);
                }
            }
        });
    }

    /**
     * show the ble devices or show the "sdk not initialized" message
     *
     * @param show - true if show the ble devices list, false otherwise
     */
    private void showBleList(boolean show) {
        bleDevicesLabel.setVisibility((show) ? View.VISIBLE : View.GONE);
        devicesList.setVisibility((show) ? View.VISIBLE : View.GONE);
        sdkNotInitializedTv.setVisibility((show) ? View.GONE : View.VISIBLE);
    }

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }
}
