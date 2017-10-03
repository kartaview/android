package com.telenav.osv.manager.obd;

import android.app.Activity;
import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.obd.OBDHelper;
import com.telenav.osv.ui.fragment.BTDialogFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class responsible for connecting to BT OBD devices
 * Created by Kalman on 3/17/16.
 */
class ObdBtManager extends ObdManager implements BTDialogFragment.OnDeviceSelectedListener {

  private static final String TAG = "ObdBtManager";

  private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  private final SharedPreferences preferences;

  private BluetoothSocket mSocket;

  private boolean mConnecting = false;

  private boolean mRunning = false;

  private ScheduledFuture<?> mConnectTask;

  private ScheduledFuture<?> mCollectTask;

  private Runnable mRunnable = () -> {
    SpeedData speed = new SpeedData("NULL");
    try {
      speed = getSpeed();
    } catch (SocketException se) {
      Log.d(TAG, Log.getStackTraceString(se));
      try {
        Thread.sleep(800);
      } catch (InterruptedException ignored) {
        Log.d(TAG, Log.getStackTraceString(ignored));
      }
      reconnect();
    } catch (InterruptedException ie) {
      reset();
      Log.d(TAG, Log.getStackTraceString(ie));
      try {
        Thread.sleep(800);
      } catch (InterruptedException ignored) {
        Log.d(TAG, Log.getStackTraceString(ignored));
      }
      reconnect();
    } catch (Exception xe) {
      Log.d(TAG, Log.getStackTraceString(xe));
      try {
        Thread.sleep(800);
      } catch (InterruptedException ignored) {
        Log.d(TAG, Log.getStackTraceString(ignored));
      }
      reconnect();
    }
    onSpeedObtained(speed);
  };

  private Runnable mConnectRunnable = new Runnable() {

    @Override
    public void run() {
      try {
        BluetoothAdapter bluetoothAdapter;
        final android.bluetooth.BluetoothManager bluetoothManager =
            (android.bluetooth.BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (!bluetoothAdapter.isEnabled()) {
          if (mConnectTask != null) {
            mConnectTask.cancel(true);
          }
          mThreadPoolExecutor.remove(mConnectRunnable);
          return;
        }
        final String deviceAddress =
            mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE).getString(Constants.EXTRAS_BT_DEVICE_ADDRESS, null);

        if (deviceAddress == null) {
          Log.w(TAG, "stopping service as no saved device");
          mConnecting = false;
          return;
        }

        if (deviceAddress.length() == 0) {
          Log.w(TAG, "stopping service as no saved device");
          mConnecting = false;
          preferences.edit().remove(Constants.EXTRAS_BT_DEVICE_ADDRESS).apply();
          return;
        }
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        try {
          int state = device.getBondState();
          if (state == BluetoothDevice.BOND_NONE) {
            device.createBond();
          }
        } catch (Exception e) {
          Log.w(TAG, "connect: " + Log.getStackTraceString(e));
        }

        // Get a BluetoothSocket for a connection with the
        // given BluetoothDevice
        mSocket = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);

        mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE).edit().putBoolean(Constants.BT_SERVICE_STARTED, true).apply();
        mSocket.connect();

        describe();
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Log.d(TAG, Log.getStackTraceString(e));
        }
        fastInit();
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Log.d(TAG, Log.getStackTraceString(e));
        }
        reset();
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Log.d(TAG, Log.getStackTraceString(e));
        }
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Log.d(TAG, Log.getStackTraceString(e));
        }
        describe();
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Log.d(TAG, Log.getStackTraceString(e));
        }
        mConnecting = false;
        sConnected = true;
        if (mConnectionListener != null) {
          mConnectionListener.onObdConnected();
        }
        if (mConnectTask != null) {
          mConnectTask.cancel(true);
        }
        mThreadPoolExecutor.remove(mConnectRunnable);
        startRunnable();
        return;
      } catch (IOException e) {
        Log.d(TAG, Log.getStackTraceString(e));
        sConnected = false;
        if (mConnectionListener != null) {
          mConnectionListener.onObdDisconnected();
        }
      }
      mConnecting = false;
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

  ObdBtManager(Context context, MutableLiveData<Integer> obdStatusLive, ConnectionListener listener) {
    super(context, obdStatusLive, listener);
    preferences = mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
  }

  public void connect() {
    if (mConnecting || (mSocket != null && mSocket.isConnected())) {
      return;
    }

    final String deviceAddress =
        mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE).getString(Constants.EXTRAS_BT_DEVICE_ADDRESS, null);

    if (deviceAddress == null) {
      Log.w(TAG, "stopping service as no saved device");
      mConnecting = false;
      return;
    }

    if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
      Log.w(TAG, " stopping service as Bluetooth not supported");
      mConnecting = false;
      return;
    }

    BluetoothAdapter bluetoothAdapter;
    final android.bluetooth.BluetoothManager bluetoothManager =
        (android.bluetooth.BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
    bluetoothAdapter = bluetoothManager.getAdapter();

    // Checks if Bluetooth is supported on the device.
    if (bluetoothAdapter == null) {
      Log.w(TAG, " stopping service as Bluetooth not supported");
      mConnecting = false;
      return;
    }

    // Ensures Bluetooth is available on the device and it is enabled. If not,
    // displays a dialog requesting user permission to enable Bluetooth.
    if (!bluetoothAdapter.isEnabled()) {
      Log.w(TAG, " stopping as bluetooth is off ");
      mConnecting = false;
      return;
    }

    mConnecting = true;
    if (mConnectionListener != null) {
      mConnectionListener.onObdConnecting();
    }
    mRunning = false;
    mConnectTask = mThreadPoolExecutor.scheduleAtFixedRate(mConnectRunnable, 0, 10000, TimeUnit.MILLISECONDS);
  }

  public void disconnect() {
    if (mCollectTask != null) {
      mCollectTask.cancel(true);
    }
    if ((mSocket == null || !mSocket.isConnected())) {
      Log.d(TAG, "disconnect: socket already disconnected");
      if (mConnectionListener != null) {
        mConnectionListener.onObdDisconnected();
      }
      return;
    }
    mThreadPoolExecutor.remove(mRunnable);
    if (mConnectTask != null) {
      mConnectTask.cancel(true);
    }
    mThreadPoolExecutor.remove(mConnectRunnable);
    mConnecting = false;
    mRunning = false;
    mThreadPoolExecutor.execute(() -> {
      if (mSocket != null && mSocket.isConnected()) {
        try {
          reset();
          mSocket.close();
          mSocket = null;
          Log.d(TAG, "disconnect: OBD disconnected.");
        } catch (Exception e) {
          Log.w(TAG, "disconnect: " + Log.getStackTraceString(e));
        }
      }
      sConnected = false;
      if (mConnectionListener != null) {
        mConnectionListener.onObdDisconnected();
      }
    });
  }

  public void startRunnable() {
    try {
      if (!mRunning && !mThreadPoolExecutor.getQueue().contains(mRunnable)) {
        mRunning = true;
        mCollectTask = mThreadPoolExecutor.scheduleAtFixedRate(mRunnable, 0, 100, TimeUnit.MILLISECONDS);
      }
    } catch (Exception e) {
      Log.w(TAG, "startRunnable: " + Log.getStackTraceString(e));
    }
  }

  public void stopRunnable() {
    mRunning = false;
    if (mCollectTask != null) {
      mCollectTask.cancel(true);
    }
    if (mThreadPoolExecutor != null) {
      mThreadPoolExecutor.remove(mRunnable);
    }
  }

  @Override
  public int getType() {
    return TYPE_BT;
  }

  void setAuto() {
    mThreadPoolExecutor.execute(() -> {
      try {
        if (mSocket != null && mSocket.isConnected()) {
          Log.d(TAG, "setAuto: ");
          runCommand(OBDHelper.CMD_SET_AUTO);
          runCommand("AT SS");
        }
      } catch (Exception e) {
        Log.w(TAG, Log.getStackTraceString(e));
      }
    });
  }

  void reset() {
    mThreadPoolExecutor.execute(() -> {
      try {
        if (mSocket != null && mSocket.isConnected()) {
          Log.d(TAG, "reset: ");
          runCommand("AT D");//set Default
          runCommand("AT Z");//reset
          runCommand("AT E0");//echo off/on *1
          runCommand("AT L0");//linefeeds off
          runCommand("AT S0");//spaces off/on *1
          try {
            Thread.sleep(500);
          } catch (Exception ignored) {
            Log.d(TAG, Log.getStackTraceString(ignored));
          }
          setAuto();
        }
      } catch (Exception e) {
        Log.w(TAG, Log.getStackTraceString(e));
      }
    });
  }

  public void onDeviceSelected(BluetoothDevice device) {
    if (device != null && device.getAddress().equals(preferences.getString(Constants.EXTRAS_BT_DEVICE_ADDRESS, null))) {
      preferences.edit().putInt(Constants.LAST_BT_CONNECTION_STATUS, Constants.STATUS_CONNECTING).apply();
    }
    connect();
  }

  @Override
  public boolean isFunctional(OSVActivity activity) {

    // Use this check to determine whether BT is supported on the device. Then
    // you can selectively disable BT-related features.
    if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
      activity.showSnackBar(R.string.bl_not_supported, Snackbar.LENGTH_SHORT);
      return false;
    }

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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
    BTDialogFragment btFragment = new BTDialogFragment();
    btFragment.setDeviceSelectedListener(this);
    btFragment.show(activity.getSupportFragmentManager(), BTDialogFragment.TAG);
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

  private SpeedData getSpeed() throws IOException, InterruptedException /*NonNumericResponseException, SocketException*/ {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      throw new IllegalStateException(TAG + " getSpeed: runnning on ui thread");
    }
    if (mSocket != null && mSocket.isConnected()) {
      return runCommand(OBDHelper.CMD_SPEED);
    }
    stopRunnable();
    return new SpeedData("");
  }

  private SpeedData runCommand(String command) throws IOException, InterruptedException, NumberFormatException {
    String rawData;
    byte b;
    InputStream in = mSocket.getInputStream();
    OutputStream out = mSocket.getOutputStream();
    out.write((command + '\r').getBytes());
    out.flush();
    StringBuilder res = new StringBuilder();

    long start = System.currentTimeMillis();
    while (in.available() > 0 && (char) (b = (byte) in.read()) != '>' && res.length() < 60 && System.currentTimeMillis() - start < 500) {

      res.append((char) b);
    }

    rawData = res.toString().trim();
    if (rawData.contains("CAN ERROR")) {
      reconnect();
      return new SpeedData("CAN ERROR");
    } else if (rawData.contains(OBDHelper.CMD_SPEED) || rawData.contains("410D") || rawData.contains("41 0D") || rawData.contains("10D1")) {

      rawData = rawData.replaceAll("\r", " ");
      rawData = rawData.replaceAll(OBDHelper.CMD_SPEED, "");
      rawData = rawData.replaceAll("41 0D", " ").trim();
      rawData = rawData.replaceAll("410D", " ").trim();
      rawData = rawData.replaceAll("10D1", " ").trim();
      String[] data = rawData.split(" ");
      int speed;
      try {
        speed = Integer.decode("0x" + data[0]);
      } catch (NumberFormatException exc) {
        return new SpeedData(data[0]);
      }
      return new SpeedData(speed);
    }
    return new SpeedData(rawData);
  }

  private void fastInit() {
    mThreadPoolExecutor.execute(() -> {
      try {
        if (mSocket != null && mSocket.isConnected()) {
          Log.d(TAG, "fastInit: ");
          runCommand(OBDHelper.CMD_FAST_INIT);
        }
      } catch (Exception e) {
        Log.w(TAG, Log.getStackTraceString(e));
      }
    });
  }

  private void describe() {
    mThreadPoolExecutor.execute(() -> {
      try {
        if (mSocket != null && mSocket.isConnected()) {
          Log.d(TAG, "describe: ");
          runCommand(OBDHelper.CMD_DEVICE_DESCRIPTION);
          runCommand(OBDHelper.CMD_DESCRIBE_PROTOCOL);
        }
      } catch (Exception e) {
        Log.w(TAG, Log.getStackTraceString(e));
      }
    });
  }

  private void reconnect() {
    if (mCollectTask != null) {
      mCollectTask.cancel(true);
    }
    if ((mSocket == null || !mSocket.isConnected())) {
      Log.d(TAG, "reconnect: socket already disconnected");
      if (mConnectionListener != null) {
        mConnectionListener.onObdDisconnected();
      }
      return;
    }
    mThreadPoolExecutor.remove(mRunnable);
    if (mConnectTask != null) {
      mConnectTask.cancel(true);
    }
    mThreadPoolExecutor.remove(mConnectRunnable);
    mConnecting = false;
    mRunning = false;
    mThreadPoolExecutor.execute(() -> {
      if (mSocket != null && mSocket.isConnected()) {
        try {
          reset();
          mSocket.close();
          mSocket = null;
          Log.d(TAG, "reconnect: OBD disconnected.");
        } catch (Exception e) {
          Log.w(TAG, "reconnect: " + Log.getStackTraceString(e));
        }
      }
      sConnected = false;
      if (mConnectionListener != null) {
        mConnectionListener.onObdDisconnected();
      }
      connect();
    });
  }
}
