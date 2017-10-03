package com.telenav.osv.manager.obd;

import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.command.ObdCommand;
import com.telenav.osv.command.ObdResetCommand;
import com.telenav.osv.item.SpeedData;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * abstract obd connection manager
 * Created by Kalman on 21/06/16.
 */
public abstract class ObdManager {

  public static final int TYPE_WIFI = 0;

  public static final int TYPE_BT = 1;

  public static final int TYPE_BLE = 2;

  public static final int STATE_DISCONNECTED = 0;

  public static final int STATE_CONNECTING = 1;

  public static final int STATE_CONNECTED = 2;

  /**
   * is the connection open
   */
  static boolean sConnected;

  final Context mContext;

  private final MutableLiveData<Integer> mObdStatus;

  ScheduledThreadPoolExecutor mThreadPoolExecutor;

  private ConnectionListener mListener;

  final ConnectionListener mConnectionListener = new ConnectionListener() {

    @Override
    public void onSpeedObtained(SpeedData speed) {
      if (mListener != null) {
        mListener.onSpeedObtained(speed);
      }
    }

    @Override
    public void onObdConnected() {
      mObdStatus.postValue(STATE_CONNECTED);
      if (mListener != null) {
        mListener.onObdConnected();
      }
    }

    @Override
    public void onObdDisconnected() {
      mObdStatus.postValue(STATE_DISCONNECTED);
      if (mListener != null) {
        mListener.onObdDisconnected();
      }
    }

    @Override
    public void onObdConnecting() {
      mObdStatus.postValue(STATE_CONNECTING);
      if (mListener != null) {
        mListener.onObdConnecting();
      }
    }

    @Override
    public void onObdDataTimedOut() {
      if (mListener != null) {
        mListener.onObdDataTimedOut();
      }
    }
  };

  private ObdQualityChecker mObdQualityChecker;

  public ObdManager(Context context, MutableLiveData<Integer> obdStatus, ConnectionListener listener) {
    mContext = context;
    mListener = listener;
    mThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().setDaemon(false).setNameFormat("OBDThreadPool")
        .setPriority(Thread.MAX_PRIORITY).build());
    mThreadPoolExecutor.setRemoveOnCancelPolicy(true);
    mObdStatus = obdStatus;
  }

  public static boolean isConnected() {
    return sConnected;
  }

  public static ObdManager get(Context context, MutableLiveData<Integer> obdStatusLive,
                               int type, ConnectionListener listener) {
    switch (type) {
      case TYPE_BT:
        return new ObdBtManager(context, obdStatusLive, listener);
      case TYPE_BLE:
        return new ObdBleManager(context, obdStatusLive, listener);
      default:
      case TYPE_WIFI:
        return new ObdWifiManager(context, obdStatusLive, listener);
    }
  }

  abstract void connect();

  abstract void disconnect();

  public abstract void startRunnable();

  public abstract void stopRunnable();

  public void removeConnectionListener() {
    mListener = null;
    if (mObdQualityChecker != null) {
      mObdQualityChecker.setListener(null);
    }
    mObdQualityChecker = null;
  }

  public void setConnectionListener(ConnectionListener listener) {
    mListener = listener;
    if (mObdQualityChecker == null) {
      mObdQualityChecker = new ObdQualityChecker();
    }
    mObdQualityChecker.setListener(listener);
  }

  public abstract int getType();

  abstract void setAuto();

  abstract void reset();

  public abstract void onDeviceSelected(BluetoothDevice device);

  @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
  public void onObdCommand(ObdCommand command) {
    if (command.start) {
      connect();
    } else {
      disconnect();
    }
  }

  @Subscribe(threadMode = ThreadMode.BACKGROUND)
  public void onResetCommand(ObdResetCommand command) {
    setAuto();
    reset();
  }

  void onSpeedObtained(SpeedData speedData) {
    if (mObdQualityChecker != null) {
      mObdQualityChecker.onSpeedObtained(speedData);
    }
    if (mListener != null) {
      mListener.onSpeedObtained(speedData);
    }
  }

  public abstract boolean isFunctional(OSVActivity activity);

  public abstract void registerReceiver();

  public abstract void unregisterReceiver();

  public void destroy() {
    stopRunnable();
    disconnect();
    if (mThreadPoolExecutor != null) {
      mThreadPoolExecutor.shutdown();
    }
  }

  public interface ConnectionListener {

    void onSpeedObtained(SpeedData speed);

    void onObdConnected();

    void onObdDisconnected();

    void onObdConnecting();

    void onObdDataTimedOut();
  }
}
