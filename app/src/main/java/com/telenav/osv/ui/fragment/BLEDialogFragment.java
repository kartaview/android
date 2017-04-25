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
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.manager.obd.ObdBleManager;
import com.telenav.osv.obd.BLEConnection;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.ui.list.BleDeviceAdapter;

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
     * the lable of the ble devices list
     */
    private TextView bleDevicesLabel;

    /**
     * adapter for the devices list
     */
    private BleDeviceAdapter mLeDeviceListAdapter;

    private BluetoothAdapter bluetoothAdapter;

    /**
     * shared preferences
     */
    private SharedPreferences preferences;

    private ObdBleManager mObdBleManager;

    private MainActivity activity;

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLeDeviceListAdapter.addDevice(result.getDevice());
                    }
                });
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            if (results.isEmpty()){
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(null);
                        }
                    });
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "error code is:" + errorCode);
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLeDeviceListAdapter.addDevice(null);
                    }
                });
            }
        }
    };

    private ImageView refreshButton;

    private View.OnClickListener mScanOnClickListener;

    private OnDeviceSelectedListener deviceSelectedListener;

    /**
     * device item list click listener
     */
    private ListView.OnItemClickListener deviceItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
            if (device == null) return;
            // stop the scanning
            if (BLEConnection.getInstance().isScanning()) {
                startScanning(false);
            }
            preferences.edit()
                    .putString(Constants.EXTRAS_BLE_DEVICE_ADDRESS, device.getAddress())
                    .apply();

            if (deviceSelectedListener != null) {
                deviceSelectedListener.onDeviceSelected(device);
            }

            mObdBleManager.onDeviceSelected(device);
            mObdBleManager.connect();
            dismiss();
        }
    };

    public void setDeviceSelectedListener(OnDeviceSelectedListener deviceSelectedListener) {
        this.deviceSelectedListener = deviceSelectedListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_devices_list, container, false);
        activity = (MainActivity) getActivity();
        try {
            mObdBleManager = (ObdBleManager) activity.getApp().getRecorder().getOBDManager();
        } catch (ClassCastException e){
            activity.getApp().getRecorder().createObdManager(PreferenceTypes.V_OBD_BLE);
            mObdBleManager = (ObdBleManager) activity.getApp().getRecorder().getOBDManager();
        }
        preferences = activity.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(activity, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            activity.finish();
        }

        bluetoothAdapter = BLEConnection.getInstance().initConnection(activity);

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(activity, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
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
        bleDevicesLabel = (TextView) root.findViewById(R.id.devices_fragment_title);
        refreshButton = (ImageView) root.findViewById(R.id.refresh_button);
        bleDevicesLabel.setText(R.string.ble_devices);
        mScanOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanning(true);
            }
        };
        mLeDeviceListAdapter = new BleDeviceAdapter(activity);
        devicesList.setAdapter(mLeDeviceListAdapter);
        startScanning(true);
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
                startScanning(true);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startScanning(boolean start){
        if (start){
            Animation animation = new RotateAnimation(0.0f, 360.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                    0.5f);
            animation.setRepeatCount(-1);
            animation.setDuration(2000);
            refreshButton.startAnimation(animation);
            refreshButton.setOnClickListener(null);
            BLEConnection.getInstance().startScanning(scanCallback);
        } else {
            refreshButton.clearAnimation();
            refreshButton.setOnClickListener(mScanOnClickListener);
            BLEConnection.getInstance().stopScanning(scanCallback);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bluetoothAdapter.isEnabled()) {
            startScanning(false);
        }
        final BleDeviceAdapter bleDeviceAdapter = mLeDeviceListAdapter;
        if (bleDeviceAdapter != null) {
            bleDeviceAdapter.clear();
            bleDeviceAdapter.notifyDataSetChanged();
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }
}
