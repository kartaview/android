package com.telenav.osv.manager.obd;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.telenav.osv.command.ObdCommand;
import com.telenav.osv.command.ObdResetCommand;
import com.telenav.osv.item.SpeedData;

/**
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

    ConnectionListener mConnectionListener;


    Handler mUIHandler = new Handler(Looper.getMainLooper());

    public static boolean isConnected() {
        return sConnected;
    }

    public static ObdManager get(Context context, int type) {
        switch (type) {
            case TYPE_BT:
                return new ObdBtManager(context);
            case TYPE_BLE:
                return new ObdBleManager(context);
            default:
            case TYPE_WIFI:
                return new ObdWifiManager(context);
        }
    }

    abstract boolean connect();

    abstract void disconnect();

    public abstract void startRunnable();

    public abstract void stopRunnable();

    public void removeConnectionListener() {
        mConnectionListener = null;
    }

    ConnectionListener getConnectionListener() {
        return mConnectionListener;
    }

    public void setConnectionListener(ConnectionListener listener) {
        mConnectionListener = listener;
    }

    public abstract boolean isBluetooth();

    public abstract boolean isBle();

    public abstract boolean isWifi();

    abstract void setAuto();

    abstract void reset();

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

    public interface ConnectionListener {
        void onSpeedObtained(SpeedData speed);
    }
}
