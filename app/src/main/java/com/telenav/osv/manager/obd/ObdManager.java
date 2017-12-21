package com.telenav.osv.manager.obd;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.command.ObdCommand;
import com.telenav.osv.command.ObdResetCommand;
import com.telenav.osv.item.SpeedData;

/**
 * abstract obd connection manager
 * Created by Kalman on 21/06/16.
 */
public abstract class ObdManager {

    public static final int TYPE_WIFI = 0;

    public static final int TYPE_BT = 1;

    public static final int TYPE_BLE = 2;

    /**
     * is the connection open
     */
    static boolean sConnected;

    final Context mContext;

    ScheduledThreadPoolExecutor mThreadPoolExecutor;

    ConnectionListener mConnectionListener;

    private ObdQualityChecker mObdQualityChecker;

    public ObdManager(Context context, ConnectionListener listener) {
        mContext = context;
        mConnectionListener = listener;
        mThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().setDaemon(false).setNameFormat("OBDThreadPool")
                .setPriority(Thread.MAX_PRIORITY).build());
    }

    public static boolean isConnected() {
        return sConnected;
    }

    public static ObdManager get(Context context, int type, ConnectionListener listener) {
        switch (type) {
            case TYPE_BT:
                return new ObdBtManager(context, listener);
            case TYPE_BLE:
                return new ObdBleManager(context, listener);
            default:
            case TYPE_WIFI:
                return new ObdWifiManager(context, listener);
        }
    }

    public abstract void startRunnable();

    public abstract void stopRunnable();

    public void removeConnectionListener() {
        mConnectionListener = null;
        if (mObdQualityChecker != null) {
            mObdQualityChecker.setListener(null);
        }
        mObdQualityChecker = null;
    }

    public void setConnectionListener(ConnectionListener listener) {
        mConnectionListener = listener;
        if (mObdQualityChecker == null) {
            mObdQualityChecker = new ObdQualityChecker();
        }
        mObdQualityChecker.setListener(listener);
    }

    public abstract int getType();

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

    public abstract boolean isFunctional(OSVActivity activity);

    public abstract void registerReceiver();

    public abstract void unregisterReceiver();

    public void destroy() {
        stopRunnable();
        disconnect();
    }

    public interface ConnectionListener {

        void onSpeedObtained(SpeedData speed);

        void onObdConnected();

        void onObdDisconnected();

        void onObdConnecting();

        void onObdDataTimedOut();
    }

    abstract void connect();

    abstract void disconnect();

    abstract void setAuto();

    abstract void reset();

    void onSpeedObtained(SpeedData speedData) {
        if (mObdQualityChecker != null) {
            mObdQualityChecker.onSpeedObtained(speedData);
        }
        if (mConnectionListener != null) {
            mConnectionListener.onSpeedObtained(speedData);
        }
    }
}
