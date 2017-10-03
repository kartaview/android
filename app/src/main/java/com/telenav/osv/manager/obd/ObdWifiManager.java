package com.telenav.osv.manager.obd;

import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.obd.OBDHelper;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class responsible for connecting to WIFI OBD devices
 * Created by Kalman on 3/17/16.
 */
class ObdWifiManager extends ObdManager {

  private static final String TAG = "OBDWifiManager";

  private Socket mSocket;

  private boolean mConnecting = false;

  private boolean mRunning = false;

  private WifiManager.WifiLock wifiLock;

  private ScheduledFuture<?> mConnectTask;

  private ScheduledFuture<?> mCollectTask;

  private Runnable mConnectRunnable = new Runnable() {

    @Override
    public void run() {
      try {
        mSocket = new Socket();
        Log.d(TAG, "mConnectRunnable: connecting");
        mSocket.connect(new InetSocketAddress("192.168.0.10", 35000), 8000);
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
        setAuto();
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
        mConnectionListener.onObdConnected();
        if (mConnectTask != null) {
          Log.d(TAG, "run: cancelling connect task");
          mConnectTask.cancel(true);
        }
        mThreadPoolExecutor.remove(mConnectRunnable);
        startRunnable();
        return;
      } catch (IOException e) {
        Log.d(TAG, Log.getStackTraceString(e));
        sConnected = false;
        mConnectionListener.onObdDisconnected();
      }
      mConnecting = false;
      mThreadPoolExecutor.remove(mConnectRunnable);
    }
  };

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

  private BroadcastReceiver mWifiBroadcastReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
        if (NetworkUtils.isWifiInternetAvailable(mContext) && isWifiObd()) {
          connect();
        } else if (sConnected) {
          disconnect();
        }
      }
    }
  };

  ObdWifiManager(Context context, MutableLiveData<Integer> obdStatusLive, ConnectionListener listener) {
    super(context, obdStatusLive, listener);
  }

  void connect() {
    if (mConnecting || (mSocket != null && mSocket.isConnected())) {
      return;
    }
    if (isWifiObd()) {
      if (wifiLock == null) {
        WifiManager wifi = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HighPerf wifi lock");
      }
      wifiLock.acquire();
      mConnecting = true;
      if (mConnectionListener != null) {
        mConnectionListener.onObdConnecting();
      }
      mRunning = false;
      mConnectTask = mThreadPoolExecutor.scheduleAtFixedRate(mConnectRunnable, 0, 10000, TimeUnit.MILLISECONDS);
      return;
    }
    mConnecting = false;
  }

  public void disconnect() {
    if (wifiLock != null && wifiLock.isHeld()) {
      wifiLock.release();
    }
    if ((mSocket == null || !mSocket.isConnected())) {
      Log.d(TAG, "disconnect: socket already disconnected");
      if (mConnectionListener != null) {
        mConnectionListener.onObdDisconnected();
      }
      return;
    }
    if (mCollectTask != null) {
      mCollectTask.cancel(true);
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
    return TYPE_WIFI;
  }

  void setAuto() {
    mThreadPoolExecutor.execute(() -> {
      try {
        if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
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
        if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
          Log.d(TAG, "reset: ");
          runCommand("AT D");//set Default
          runCommand("AT Z");//reset
          runCommand("AT E0");//echo off/on *1
          runCommand("AT L0");//linefeeds off
          runCommand("AT S0");//spaces off/on *1
          setAuto();
        }
      } catch (Exception e) {
        Log.w(TAG, Log.getStackTraceString(e));
      }
    });
  }

  @Override
  public void onDeviceSelected(BluetoothDevice device) {
    //do nothing, this should not be called on wifi obd manager
  }

  @Override
  public boolean isFunctional(final OSVActivity activity) {
    WifiManager wifi = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    WifiInfo wifiInfo = wifi.getConnectionInfo();
    String name = wifiInfo.getSSID();
    if (!wifi.isWifiEnabled() || !(name.contains("OBD") || name.contains("obd") || name.contains("link") || name.contains("LINK"))) {
      activity.showSnackBar(R.string.no_obd_detected_message, Snackbar.LENGTH_LONG, R.string.wifi_label,
                            () -> activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
      return false;
    }
    connect();
    return true;
  }

  @Override
  public void registerReceiver() {
    mContext.registerReceiver(mWifiBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
  }

  @Override
  public void unregisterReceiver() {
    mContext.unregisterReceiver(mWifiBroadcastReceiver);
  }

  private SpeedData getSpeed() throws IOException, InterruptedException /*NonNumericResponseException, SocketException*/ {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      throw new IllegalStateException(TAG + " getSpeed: runnning on ui thread");
    }
    if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
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

    // read until '>' arrives
    long start = System.currentTimeMillis();
    while ((char) (b = (byte) in.read()) != '>' && res.length() < 60 && System.currentTimeMillis() - start < 500) {
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
        if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
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
        if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
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
    if (wifiLock != null && wifiLock.isHeld()) {
      wifiLock.release();
    }
    if (mCollectTask != null) {
      mCollectTask.cancel(true);
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

  private boolean isWifiObd() {
    WifiManager wifi = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    WifiInfo wifiInfo = wifi.getConnectionInfo();
    String name = wifiInfo.getSSID();
    return (wifi.isWifiEnabled() && (name.contains("OBD") || name.contains("obd") || name.contains("link") || name.contains("LINK")));
  }
}
