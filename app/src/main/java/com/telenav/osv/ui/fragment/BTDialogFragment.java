package com.telenav.osv.ui.fragment;

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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.di.Injectable;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.obd.ObdManager;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.ui.list.DeviceAdapter;
import com.telenav.osv.utils.Log;
import java.util.Set;
import javax.inject.Inject;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class BTDialogFragment extends DialogFragment implements Injectable {

  /**
   * Tag for Log
   */
  public static final String TAG = "BTDialogFragment";

  @Inject
  Recorder mRecorder;

  /**
   * Member fields
   */
  private BluetoothAdapter mBtAdapter;

  /**
   * Newly discovered devices
   */
  private DeviceAdapter mDevicesAdapter;

  private MainActivity activity;

  private OnDeviceSelectedListener mDeviceSelectedListener;

  private ImageView mScanButton;

  private View.OnClickListener mScanOnClickListener;

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
          mDevicesAdapter.addDevice(device);
        }
        // When discovery is finished, change the Activity title
      } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
        setProgressBarIndeterminateVisibility(false);
        setTitle(R.string.bt_select_device_label);
        mDevicesAdapter.onDeviceSearchFinished();
      }
    }
  };

  /**
   * shared preferences
   */
  private SharedPreferences preferences;

  private ObdManager mObdManager;

  /**
   * The on-click listener for all devices in the ListViews
   */
  private OnDeviceSelectedListener mDeviceClickListener = device -> {
    if (device != null) {
      // Cancel discovery because it's costly and we're about to connect
      mBtAdapter.cancelDiscovery();

      preferences.edit().putString(
          mObdManager.getType() == ObdManager.TYPE_BLE ? Constants.EXTRAS_BLE_DEVICE_ADDRESS : Constants.EXTRAS_BT_DEVICE_ADDRESS,
          device.getAddress()).apply();
      if (mDeviceSelectedListener != null) {
        mDeviceSelectedListener.onDeviceSelected(device);
      }
      dismiss();
    }
  };

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_devices_list, container, false);
    activity = (MainActivity) getActivity();
    preferences = activity.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
    mObdManager = mRecorder.getOBDManager();

    // Initialize the button to perform device discovery
    mScanButton = view.findViewById(R.id.refresh_button);
    RecyclerView mDevicesList = view.findViewById(R.id.devices_list);
    mDevicesTitle = view.findViewById(R.id.devices_fragment_title);
    mScanOnClickListener = v -> doDiscovery();
    mScanButton.setOnClickListener(mScanOnClickListener);

    // Initialize array adapters. One for already paired devices and
    // one for newly discovered devices
    mDevicesAdapter = new DeviceAdapter(activity, mDeviceClickListener);
    mDevicesList.setLayoutManager(new LinearLayoutManager(activity));
    mDevicesList.setAdapter(mDevicesAdapter);
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
    mDevicesAdapter
        .highlight(mObdManager.getType() == ObdManager.TYPE_BLE ? BluetoothDevice.DEVICE_TYPE_LE : BluetoothDevice.DEVICE_TYPE_CLASSIC);
    // If there are paired devices, add each one to the ArrayAdapter
    if (!pairedDevices.isEmpty()) {
      ((TextView) view.findViewById(R.id.devices_fragment_title)).setText(R.string.bt_paired_devices_label);
      for (BluetoothDevice device : pairedDevices) {
        mDevicesAdapter.addDevice(device);
      }
    } else {
      mDevicesAdapter.onDeviceListFinished();
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
    Log.d(TAG, "doDiscovery: ");
    mDevicesAdapter.clearNew();
    // Indicate scanning in the title
    setProgressBarIndeterminateVisibility(true);
    setTitle(R.string.bt_scanning_label);

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
    if (visible) {
      Animation animation = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
      animation.setRepeatCount(-1);
      animation.setDuration(2000);
      mScanButton.startAnimation(animation);
      mScanButton.setOnClickListener(null);
    } else {
      mScanButton.clearAnimation();
      mScanButton.setOnClickListener(mScanOnClickListener);
    }
  }

  public void setDeviceSelectedListener(OnDeviceSelectedListener listener) {
    this.mDeviceSelectedListener = listener;
  }

  public interface OnDeviceSelectedListener {

    void onDeviceSelected(BluetoothDevice device);
  }
}
