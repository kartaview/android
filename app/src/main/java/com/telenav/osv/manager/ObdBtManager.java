package com.telenav.osv.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.obd.OBDHelper;
import com.telenav.osv.utils.Log;

/**
 *
 * Created by Kalman on 11/10/16.
 */
public class ObdBtManager extends ObdManager {

    private static final String TAG = "ObdBtManager";

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final SharedPreferences preferences;

    private BluetoothSocket mSocket;

    private final Context mContext;

    private final HandlerThread mOBDThread;

    private final Handler mOBDHandler;

    private boolean mConnecting = false;

    private boolean mRunning = false;

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
//            Log.d(TAG, "run: running speed runnable");
            int speed = 0;
            long time = System.currentTimeMillis();
            try {
                speed = getSpeed().getSpeed();
                if (speed < 0){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
//                    reset();
                }
                Log.d(TAG, "getSpeed: done in " + (System.currentTimeMillis() - time) + " ms , speed obtained: " + speed);
            } catch (SocketException se) {
                se.printStackTrace();
                try {
                    Thread.sleep(800);
                } catch (InterruptedException ignored) {}
                disconnect(true);
            } catch (InterruptedException ie) {
                reset();
                ie.printStackTrace();
                try {
                    Thread.sleep(800);
                } catch (InterruptedException ignored) {}
                disconnect(true);
            } catch (Exception xe) {
                xe.printStackTrace();
                try {
                    Thread.sleep(800);
                } catch (InterruptedException ignored) {}
                disconnect(true);
            }
            final int finalSpeed = speed;
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (ConnectionListener listener : mConnectionListeners) {
                        listener.onSpeedObtained(new SpeedData(finalSpeed));
                    }
                }
            });
            if (mRunning) {
                mOBDHandler.postDelayed(mRunnable, 100);
            }
        }
    };

    private Runnable mConnectRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                BluetoothAdapter bluetoothAdapter;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    final android.bluetooth.BluetoothManager bluetoothManager = (android.bluetooth.BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
                    bluetoothAdapter = bluetoothManager.getAdapter();
                } else {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                }
                final String deviceAddress = mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE).getString(Constants.EXTRAS_BT_DEVICE_ADDRESS, null);

                if (deviceAddress == null) {
                    Log.d(TAG, "stopping service as no saved device");
                    mConnecting = false;
                    return;
                }

                if (deviceAddress.length() == 0) {
                    Log.d(TAG, "stopping service as no saved device");
                    mConnecting = false;
                    preferences.edit()
                            .remove(Constants.EXTRAS_BT_DEVICE_ADDRESS)
                            .apply();
                    return;
                }
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    try {
                        int state = device.getBondState();
                        if (state == BluetoothDevice.BOND_NONE) {
                            device.createBond();
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "connect: " + Log.getStackTraceString(e));
                    }
                }

                // Get a BluetoothSocket for a connection with the
                // given BluetoothDevice
                mSocket = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);

                mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE)
                        .edit()
                        .putBoolean(Constants.BT_SERVICE_STARTED, true)
                        .apply();
                mSocket.connect();

                describe();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                fastInit();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                reset();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                describe();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (ConnectionListener listener : mConnectionListeners) {
                            listener.onConnected();
                        }
                    }
                });
                mConnecting = false;
                startRunnable();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (ConnectionListener listener : mConnectionListeners) {
                            listener.onDisconnected();
                        }
                    }
                });
            }
            mConnecting = false;
            mOBDHandler.postDelayed(mConnectRunnable, 10000);
        }
    };

    public ObdBtManager(Context context) {
        this.mContext = context;
        preferences = mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
        mOBDThread = new HandlerThread("OBDII", Thread.NORM_PRIORITY);
        mOBDThread.start();
        mOBDHandler = new Handler(mOBDThread.getLooper());
    }

    public boolean connect() {
        if (mConnecting || isConnected() || sDisconnected){
            return false;
        }

        final String deviceAddress = mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE).getString(Constants.EXTRAS_BT_DEVICE_ADDRESS, null);

        if (deviceAddress == null) {
            Log.d(TAG, "stopping service as no saved device");
            mConnecting = false;
            return false;
        }

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Log.d(TAG, " stopping service as Bluetooth not supported");
            mConnecting = false;
            return false;
        }

        BluetoothAdapter bluetoothAdapter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final android.bluetooth.BluetoothManager bluetoothManager = (android.bluetooth.BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Log.d(TAG, " stopping service as Bluetooth not supported");
            mConnecting = false;
            return false;
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, " stopping as bluetooth is off ");
            mConnecting = false;
            return false;
        }

        mConnecting = true;

        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                for (ConnectionListener listener : mConnectionListeners) {
                    listener.onConnecting();
                }
            }
        });
        mOBDHandler.removeCallbacksAndMessages(null);
        mRunning = false;
        mOBDHandler.post(mConnectRunnable);
        return true;
    }

    public SpeedData getSpeed() throws IOException, InterruptedException /*NonNumericResponseException, SocketException*/ {
        if (Looper.myLooper() == Looper.getMainLooper()){
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

        // read until '>' arrives
        long start = System.currentTimeMillis();
        while (in.available() > 0 && (char) (b = (byte) in.read()) != '>' && res.length() < 60 && System.currentTimeMillis() - start < 1000) { // && System.currentTimeMillis()-start<500
            res.append((char) b);
        }

        rawData = res.toString().trim();
        Log.d(TAG, "runCommand: rawData = " + rawData);
        if (rawData.contains("STOPPED")) {
            Thread.sleep(1000);
        } else if (rawData.contains("SEARCHING")) {
            Thread.sleep(1000);
        } else if (rawData.contains("CAN ERROR")) {
            Thread.sleep(1000);
            disconnect(true);
            return new SpeedData("CAN ERROR");
        } else if (rawData.contains("NO DATA")) {
            Thread.sleep(1000);
        } else if (rawData.contains(OBDHelper.CMD_SPEED) || rawData.contains("410D")) {

            rawData = rawData.replaceAll("\r", " ");
            rawData = rawData.replaceAll(OBDHelper.CMD_SPEED, "");
            rawData = rawData.replaceAll("41 0D", " ").trim();
            rawData = rawData.replaceAll("410D", " ").trim();
            rawData = rawData.replaceAll("10D1", " ").trim();
            String[] data = rawData.split(" ");

            int speed = Integer.decode("0x" + data[0]);
            return new SpeedData(speed);

        }
        return new SpeedData(rawData);
    }

    public void fastInit() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null && mSocket.isConnected()) {
                        Log.d(TAG, "fastInit: ");
                        runCommand(OBDHelper.CMD_FAST_INIT);
                    }
                } catch (Exception e) {
                    Log.w(TAG, Log.getStackTraceString(e));
                }
            }
        });
    }

    public void warmStart() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null && mSocket.isConnected()) {
                        Log.d(TAG, "warmStart: ");
                        runCommand(OBDHelper.CMD_WARM_START);
                    }
                } catch (Exception e) {
                    Log.w(TAG, Log.getStackTraceString(e));
                }
            }
        });
    }

    public void reset() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
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
                        } catch (Exception ignored){}
                        setAuto();
                    }
                } catch (Exception e) {
                    Log.w(TAG, Log.getStackTraceString(e));
                }
            }
        });
    }

    public void setAuto() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null && mSocket.isConnected()) {
                        Log.d(TAG, "setAuto: ");
                        runCommand(OBDHelper.CMD_SET_AUTO);
                        runCommand("AT SS");
                    }
                } catch (Exception e) {
                    Log.w(TAG, Log.getStackTraceString(e));
                }
            }
        });
    }

    public void describe() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null && mSocket.isConnected()) {
                        Log.d(TAG, "describe: ");
                        runCommand(OBDHelper.CMD_DEVICE_DESCRIPTION);
                        runCommand(OBDHelper.CMD_DESCRIBE_PROTOCOL);
                    }
                } catch (Exception e) {
                    Log.w(TAG, Log.getStackTraceString(e));
                }
            }
        });
    }

    public void disconnect() {
        mOBDHandler.removeCallbacksAndMessages(null);
        mConnecting = false;
        mRunning = false;
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSocket != null && mSocket.isConnected()) {
                    try {
                        reset();
                        mSocket.close();
                        mSocket = null;
                        Log.d(TAG, "disconnect: OBD disconnected.");
                    } catch (Exception e) {
                        Log.d(TAG, "disconnect: " + Log.getStackTraceString(e));
                    }
                }
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (ConnectionListener listener : mConnectionListeners) {
                            listener.onDisconnected();
                        }
                    }
                });
            }
        });
    }

    public void disconnect(final boolean reconnect) {
        mOBDHandler.removeCallbacksAndMessages(null);
        mConnecting = false;
        mRunning = false;
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSocket != null && mSocket.isConnected()) {
                    try {
                        reset();
                        mSocket.close();
                        mSocket = null;
                        Log.d(TAG, "disconnect: OBD disconnected.");
                    } catch (Exception e) {
                        Log.d(TAG, "disconnect: " + Log.getStackTraceString(e));
                    }
                }
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (ConnectionListener listener : mConnectionListeners) {
                            listener.onDisconnected();
                        }
                    }
                });
                if (reconnect){
                    connect();
                }
            }
        });
    }

    public boolean isConnected() {
        return (mSocket != null && mSocket.isConnected());
    }

    public void startRunnable() {
        try {
            if (mOBDHandler != null && !mRunning) {
                mRunning = true;
                mOBDHandler.post(mRunnable);
            }
        } catch (Exception e) {
            Log.d(TAG, "onConnected: " + Log.getStackTraceString(e));
        }
    }

    public void stopRunnable() {
        mRunning = false;
        mOBDHandler.removeCallbacks(mRunnable);
    }

    @Override
    protected boolean isConnecting() {
        return mConnecting;
    }

    @Override
    public boolean isBluetooth() {
        return true;
    }

    @Override
    public boolean isBle() {
        return false;
    }

    @Override
    public boolean isWifi() {
        return false;
    }

    public void onDeviceSelected(String address) {
        if (address.equals(preferences.getString(Constants.EXTRAS_BT_DEVICE_ADDRESS, null))) {
            preferences.edit().putInt(Constants.LAST_BT_CONNECTION_STATUS, Constants.STATUS_CONNECTING).apply();
        }
    }
}
