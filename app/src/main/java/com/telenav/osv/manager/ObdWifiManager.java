package com.telenav.osv.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;

import com.telenav.osv.utils.Log;
import com.telenav.osv.obd.OBDHelper;

/**
 * Created by Kalman on 3/17/16.
 */
public class ObdWifiManager extends ObdManager {

    private static final String TAG = "OBDIIManager";

    private final Context mContext;

    private final HandlerThread mOBDThread;

    private final Handler mOBDHandler;

    public int mResetCounter = 0;

    private Socket mSocket;

    private boolean mConnecting = false;

    private boolean mRunning = false;

    private WifiManager.WifiLock wifiLock;

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run: running speed runnable");
            int speed = 0;
            long time = System.currentTimeMillis();
            try {
                speed = getSpeed().getSpeed();
                Log.d(TAG, "getSpeed: done in " + (System.currentTimeMillis() - time) + " ms");
            } catch (SocketException se) {
                se.printStackTrace();
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                }
                disconnect();
                connect();
            } catch (InterruptedException ie) {
                reset();
                ie.printStackTrace();
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                }
                disconnect();
                connect();
            } catch (IOException e) {
                e.printStackTrace();
                disconnect();
                connect();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    Thread.sleep(800);
                } catch (InterruptedException ex) {
                }
                disconnect();
                connect();
            }
            final int finalSpeed = speed;
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (ConnectionListener listener : mConnectionListeners) {
                        Log.d(TAG, "onSpeedObtained: " + finalSpeed);
                        listener.onSpeedObtained(new SpeedData(finalSpeed));
                    }
                }
            });
            if (mRunning) {
                mOBDHandler.postDelayed(mRunnable, 100);
            }
        }
    };

    public ObdWifiManager(Context context) {
        this.mContext = context;
        mOBDThread = new HandlerThread("OBDII", Thread.NORM_PRIORITY);
        mOBDThread.start();
        mOBDHandler = new Handler(mOBDThread.getLooper());
    }

    public boolean connect() {
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifiLock == null) {
            this.wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HighPerf wifi lock");
        }
        wifiLock.acquire();
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        String name = wifiInfo.getSSID();
        if (wifi.isWifiEnabled() && (name.contains("OBD") || name.contains("obd") || name.contains("link") || name.contains("LINK"))) {
            mConnecting = true;
            mOBDHandler.removeCallbacksAndMessages(null);
            mOBDHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mSocket = new Socket("192.168.0.10", 35000);
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
                        setAuto();
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
                }
            });
            return true;
        }
        return false;
    }

    public SpeedData getSpeed() throws IOException, InterruptedException /*NonNumericResponseException, SocketException*/ {
        if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
            return runCommand(OBDHelper.CMD_SPEED);
        }
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
        while ((char) (b = (byte) in.read()) != '>' && res.length() < 60 && System.currentTimeMillis() - start < 1000) { // && System.currentTimeMillis()-start<500
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
            disconnect();
            connect();
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

            Log.i(TAG, "rawData: " + rawData);
            Log.i(TAG, "data: " + data[0]);
            Log.i(TAG, "datawew: " + Integer.decode("0x" + data[0]));
            Log.i(TAG, "datawew: " + String.valueOf(Integer.decode("0x" + data[0])));

            return new SpeedData(Integer.decode("0x" + data[0]));

        }
        return new SpeedData(rawData);
    }

    public void fastInit() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
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
                    if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
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
            }
        });
    }

    public void setAuto() {
        mOBDHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
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
                    if (mSocket != null && mSocket.isConnected() && !mSocket.isClosed()) {
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
        if (wifiLock != null && wifiLock.isHeld())
            wifiLock.release();
        mOBDHandler.removeCallbacksAndMessages(null);
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

    public boolean isConnected() {
        return (mSocket != null && mSocket.isConnected());
    }

    @Override
    public boolean isBluetooth() {
        return false;
    }

    @Override
    public boolean isBle() {
        return false;
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
}
