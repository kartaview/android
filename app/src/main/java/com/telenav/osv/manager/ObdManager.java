package com.telenav.osv.manager;

import java.util.concurrent.ConcurrentLinkedQueue;
import android.os.Handler;
import android.os.Looper;

/**
 * Created by Kalman on 21/06/16.
 */

public abstract class ObdManager {

    protected ConcurrentLinkedQueue<ConnectionListener> mConnectionListeners = new ConcurrentLinkedQueue<>();

    protected Handler mUIHandler = new Handler(Looper.getMainLooper());

    public static boolean sDisconnected = false;

    public abstract boolean connect();

    public abstract void disconnect();

    public abstract void startRunnable();

    public abstract void stopRunnable();

    public void addConnectionListener(ConnectionListener listener) {
        if (!mConnectionListeners.contains(listener)) {
            this.mConnectionListeners.add(listener);
            if (isConnected()) {
                listener.onConnected();
            } else if (isConnecting()){
                listener.onConnecting();
            } else {
                listener.onDisconnected();
            }
        }
    }

    protected abstract boolean isConnecting();

    public void removeConnectionListener(ConnectionListener listener) {
        this.mConnectionListeners.remove(listener);
    }

    public ConcurrentLinkedQueue<ConnectionListener> getConnectionListeners() {
        return mConnectionListeners;
    }

    public void setConnectionListeners(ConcurrentLinkedQueue<ConnectionListener> connectionListeners) {
        this.mConnectionListeners = connectionListeners;
        if (isConnected()) {
            for (ConnectionListener listener : mConnectionListeners) {
                listener.onConnected();

            }
        } else if (isConnecting()){
            for (ConnectionListener listener : mConnectionListeners) {
                listener.onConnecting();
            }
        } else {
            for (ConnectionListener listener : mConnectionListeners) {
                listener.onDisconnected();
            }
        }
    }

    public abstract boolean isConnected();

    public abstract boolean isBluetooth();

    public abstract boolean isBle();

    public abstract boolean isWifi();

    public abstract void setAuto();

    public abstract void reset();

    public interface ConnectionListener {
        void onConnected();

        void onDisconnected();

        void onSpeedObtained(SpeedData speed);

        void onConnecting();
    }

    public class SpeedData {

        private String errorCode = "";

        private int speed = -1;

        private long timestamp = System.currentTimeMillis();

        SpeedData(int speed) {
            this.speed = speed;
        }

        SpeedData(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public int getSpeed() {
            return speed;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
