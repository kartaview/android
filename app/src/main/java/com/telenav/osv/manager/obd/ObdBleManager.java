package com.telenav.osv.manager.obd;

import android.annotation.TargetApi;
import android.app.Activity;
import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.obd.BLEConnection;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.obd.OBDCommunication;
import com.telenav.osv.obd.OBDHelper;
import com.telenav.osv.obd.VehicleDataListener;
import com.telenav.osv.ui.fragment.BTDialogFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Class responsible for connecting to BLE OBD devices
 * Created by Kalman on 3/17/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ObdBleManager extends ObdManager implements BTDialogFragment.OnDeviceSelectedListener {

  private static final String VEHICLE_DATA_ACTION = "com.telenav.osv.obd.VEHICLE_DATA";

  private static final String VEHICLE_DATA_TYPE_SPEED = "SPEED";

  private static final String TAG = ObdBleManager.class.getSimpleName();

  /**
   * bluetooth connection thread instance
   */
  private static BluetoothCommunicationThread bluetoothCommunicationThread;

  /**
   * result from the written characteristic
   */
  private String characteristicResult = "";

  private VehicleDataListener internalVehicleDataDelegate = speed -> {
    Intent intent = new Intent(VEHICLE_DATA_ACTION);
    // You can also include some extra data.
    intent.putExtra(VEHICLE_DATA_TYPE_SPEED, speed);
    LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    broadcastUpdate(Constants.ACTION_DATA_AVAILABLE, String.valueOf(speed));
  };

  /**
   * Implements callback methods for GATT events that the app cares about.  For example,connection change and services discovered.
   */
  private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        OBDCommunication.getInstance().discoverServices();
        Log.d(TAG, "onConnectionStateChange STATE_CONNECTED");
        if (mConnectionListener != null) {
          mConnectionListener.onObdConnected();
        }
      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        Log.d(TAG, "onConnectionStateChange STATE_DISCONNECTED");
        mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE).edit().putBoolean(Constants.BLE_SERVICE_STARTED, false)
            .apply();
        broadcastUpdate(Constants.ACTION_GATT_DISCONNECTED, "Disconnected");
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        sConnected = true;
        startRunnable();
        broadcastUpdate(Constants.ACTION_GATT_DISCOVERED, "Discovered");
        OBDCommunication.getInstance().suscribeToNotification();
      }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      //            Log.d(TAG, "onCharacteristicRead: ");
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      //            Log.d(TAG, "onCharacteristicWrite: ");
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
      characteristicResult += characteristic.getStringValue(0);
      if (characteristicResult.trim().endsWith(">")) {
        final Integer finalSpeed = OBDHelper.convertResult(characteristicResult, internalVehicleDataDelegate);
        onSpeedObtained(new SpeedData(finalSpeed));
        characteristicResult = "";
      }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        if (descriptor.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) {
          broadcastUpdate(Constants.ACTION_GATT_SUBSCRIBED, "Subscribed");
        }
      }
    }
  };

  private final BroadcastReceiver mBluetoothBroadcastReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent != null && intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
          disconnect();
        } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
          connect();
        }
      }
    }
  };

  ObdBleManager(Context context, MutableLiveData<Integer> obdStatusLive, ConnectionListener listener) {
    super(context, obdStatusLive, listener);
  }

  /**
   * sends the FUEl level command
   */
  private static void sendSpeedCommand() {
    if (sConnected) {
      OBDCommunication.getInstance().writeOBDCommand(OBDHelper.CMD_SPEED);
    }
  }

  public void connect() {
    if (sConnected &&
        mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE).getBoolean(Constants.BLE_SERVICE_STARTED, false)) {
      return;
    }
    final String deviceAddress =
        mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE).getString(Constants.EXTRAS_BLE_DEVICE_ADDRESS, null);

    if (deviceAddress == null) {
      Log.w(TAG, " stopping service as no saved device");
      return;
    }

    if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      Log.w(TAG, " stopping service as Bluetooth not supported");
      return;
    }

    BluetoothAdapter bluetoothAdapter = BLEConnection.getInstance().initConnection(mContext);

    // Checks if Bluetooth is supported on the device.
    if (bluetoothAdapter == null) {
      Log.w(TAG, " stopping service as Bluetooth not supported");
      return;
    }

    // Ensures Bluetooth is available on the device and it is enabled. If not,
    // displays a dialog requesting user permission to enable Bluetooth.
    if (!bluetoothAdapter.isEnabled()) {
      Log.w(TAG, " stopping as bluetooth is off ");
      return;
    }

    if (!OBDCommunication.getInstance().initialize(mContext)) {
      Log.e(TAG, "Unable to initialize Bluetooth");
    }
    // Automatically connects to the device upon successful start-up initialization.
    if (mConnectionListener != null) {
      mConnectionListener.onObdConnecting();
    }
    OBDCommunication.getInstance().connect(mContext, deviceAddress, gattCallback);

    mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE).edit().putBoolean(Constants.BLE_SERVICE_STARTED, true).apply();
  }

  public void disconnect() {
    Log.d(TAG, " onDestroy () " +
        (bluetoothCommunicationThread != null ? bluetoothCommunicationThread.getId() : "null " + " service [" + this + "]"));

    OBDCommunication.getInstance().disconnect();

    if (bluetoothCommunicationThread != null) {
      bluetoothCommunicationThread.cancel();
    }
    sConnected = false;
    OBDCommunication.getInstance().disconnectFromBLEDevice();
    mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE).edit().putBoolean(Constants.BLE_SERVICE_STARTED, false).apply();
    if (mConnectionListener != null) {
      mConnectionListener.onObdDisconnected();
    }
  }

  /**
   * initialize and start the communication thread
   */
  public void startRunnable() {
    final BluetoothCommunicationThread currentThread = bluetoothCommunicationThread;
    if (currentThread != null) {
      currentThread.cancel();
    }
    bluetoothCommunicationThread = new BluetoothCommunicationThread();
    bluetoothCommunicationThread.start();
  }

  /**
   * initialize and start the communication thread
   */
  public void stopRunnable() {
    if (bluetoothCommunicationThread != null) {
      bluetoothCommunicationThread.cancel();
    }
  }

  @Override
  public int getType() {
    return TYPE_BLE;
  }

  @Override
  public void setAuto() {

  }

  @Override
  public void reset() {

  }

  @Override
  public void onDeviceSelected(BluetoothDevice device) {
    final SharedPreferences preferences = mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
    if (device.getAddress().equals(preferences.getString(Constants.EXTRAS_BLE_DEVICE_ADDRESS, ""))) {
      preferences.edit().putInt(Constants.LAST_BLE_CONNECTION_STATUS, Constants.STATUS_CONNECTING).apply();
    }
    connect();
  }

  @Override
  public boolean isFunctional(OSVActivity activity) {

    // Use this check to determine whether BLE is supported on the device. Then
    // you can selectively disable BLE-related features.
    if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      activity.showSnackBar(R.string.ble_not_supported, Snackbar.LENGTH_SHORT);
      return false;
    }

    BluetoothAdapter bluetoothAdapter = BLEConnection.getInstance().initConnection(activity);

    // Checks if Bluetooth is supported on the device.
    if (bluetoothAdapter == null) {
      activity.showSnackBar(R.string.error_bluetooth_not_supported, Snackbar.LENGTH_SHORT);
      return false;
    }

    if (!activity.checkPermissionsForGPSWithRationale(R.string.permission_bluetooth_rationale)) {
      return false;
    }

    // Ensures Bluetooth is available on the device and it is enabled. If not,
    // displays a dialog requesting user permission to enable Bluetooth.
    if (!bluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      activity.startActivityForResult(enableBtIntent, Utils.REQUEST_ENABLE_BT);
      return false;
    }
    BTDialogFragment blefr = new BTDialogFragment();
    blefr.setDeviceSelectedListener(this);
    blefr.show(activity.getSupportFragmentManager(), BTDialogFragment.TAG);
    return true;
  }

  @Override
  public void registerReceiver() {
    mContext.registerReceiver(mBluetoothBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
  }

  @Override
  public void unregisterReceiver() {
    mContext.unregisterReceiver(mBluetoothBroadcastReceiver);
  }

  /**
   * broadcasts the results for different actions (connect, disconnect, receive response )
   *
   * @param action - the action that occurred
   * @param result - the result of the action
   */
  private void broadcastUpdate(final String action, String result) {
    final Intent intent = new Intent(action);
    intent.putExtra(Constants.EXTRA_DATA, result);
    mContext.sendBroadcast(intent);
  }

  /**
   * defines the thread that sends requests to OBD2 dongle
   */
  private static class BluetoothCommunicationThread extends Thread {

    private long requestInterval = 100;

    private volatile boolean requestData;

    BluetoothCommunicationThread() {
      requestData = true;
    }

    public void run() {
      Log.d(TAG, " Thread Started " + this.getId());

      while (requestData && sConnected) {
        if (sConnected) {
          sendSpeedCommand();
        }
        synchronized (this) {
          try {
            this.wait(requestInterval);
          } catch (InterruptedException e) {
            Log.e(TAG, "interrupted exception = " + e.getMessage());
          }
        }
      }
    }

    public void cancel() {
      requestData = false;
      interrupt();
      Log.d(TAG, " cancel called () id[" + this.getId() + "] requestData[" + requestData + "]");
    }
  }
}
