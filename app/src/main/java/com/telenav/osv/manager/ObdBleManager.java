package com.telenav.osv.manager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.obd.VehicleDataListener;
import com.telenav.osv.obd.OBDCommunication;
import com.telenav.osv.obd.OBDConnection;
import com.telenav.osv.obd.OBDHelper;

/**
 * Created by dianat on 3/11/2016.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ObdBleManager extends ObdManager {
    public static final String VEHICLE_DATA_ACTION = "com.telenav.osv.obd.VEHICLE_DATA";
    public static final String VEHICLE_DATA_TYPE_SPEED = "SPEED";

    private static int DEFAULT_POLLING_INTERVAL = 1000 * 2;

    private final static String TAG = ObdBleManager.class.getSimpleName();

    /**
     * is the connection open
     */
    private volatile static boolean isConnected;

    private final Application mContext;

    private final SharedPreferences preferences;

    /**
     * result from the written characteristic
     */
    private String characteristicResult = "";

    /**
     * bluetooth connection thread instance
     */
    private static BluetoothCommunicationThread bluetoothCommunicationThread;

    public ObdBleManager(Application context){
        mContext = context;

        preferences = mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
    }

    /**
     * Implements callback methods for GATT events that the app cares about.  For example,connection change and services discovered.
     */
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            Log.d(TAG, "onCharacteristicRead: " + characteristic.getStringValue(0));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.d(TAG, "Characteristic " + characteristic.getUuid().toString() + " write : " + characteristic.getStringValue(0));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String str = characteristic.getStringValue(0);
            characteristicResult += characteristic.getStringValue(0);
//            Log.d(TAG, "onCharacteristicChanged " + str);
            if (characteristicResult.trim().endsWith(">")) {
                int speed = OBDHelper.convertResult(characteristicResult, internalVehicleDataDelegate);
                Log.d(TAG, "onCharacteristicChanged: result " + speed);
                final Integer finalSpeed = speed;
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (ConnectionListener listener : mConnectionListeners) {
                            listener.onSpeedObtained(new SpeedData(finalSpeed));
                        }
                    }
                });
                characteristicResult = "";
            }

        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                OBDCommunication.getInstance().discoverServices();
                Log.d(TAG, "onConnectionStateChange STATE_CONNECTED" );
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (ConnectionListener listener : mConnectionListeners) {
                            listener.onConnected();
                        }
                    }
                });
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "onConnectionStateChange STATE_DISCONNECTED" );
                broadcastUpdate(Constants.ACTION_GATT_DISCONNECTED, "Disconnected");
                isConnected = false;
                //Close gatt to release resources
                OBDCommunication.getInstance().disconnectFromBLEDevice();
                mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE)
                        .edit()
                        .putBoolean(Constants.BLE_SERVICE_STARTED, false)
                        .apply();
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (ConnectionListener listener : mConnectionListeners) {
                            listener.onDisconnected();
                        }
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isConnected = true;
                startRunnable();
                broadcastUpdate(Constants.ACTION_GATT_DISCOVERED, "Discovered");
                OBDCommunication.getInstance().suscribeToNotification();
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

    /**
     * sends the FUEl level command
     */
    public static void sendSpeedCommand() {
        if (isConnected) {
            OBDCommunication.getInstance().writeOBDCommand(OBDHelper.CMD_SPEED);
        }
    }

    /**
     * initialize and start the communication thread
     */
    public void startRunnable() {
//        long interval = getApplicationContext().getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE)
//                .getLong(Constants.TASK_INTERVAL_PERIOD, DEFAULT_POLLING_INTERVAL);
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
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public boolean isBluetooth() {
        return true;
    }
    @Override
    public boolean isBle() {
        return true;
    }

    @Override
    public void setAuto() {

    }

    @Override
    public void reset() {

    }

    public boolean connect() {
        final String deviceAddress = mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE).getString(Constants.EXTRAS_DEVICE_ADDRESS, null);

        if (deviceAddress == null) {
            Log.d(TAG, " stopping service as no saved device");
            return false;
        }

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d(TAG, " stopping service as Bluetooth not supported");
            return false;
        }

        BluetoothAdapter bluetoothAdapter = OBDConnection.getInstance().initConnection(mContext);

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Log.d(TAG, " stopping service as Bluetooth not supported");
            return false;
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.d(TAG, " stopping as bluetooth is off ");
            return false;
        }

//        if (batteryLevelOk()) {
            if (!OBDCommunication.getInstance().initialize(mContext)) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            OBDCommunication.getInstance().connect(mContext, deviceAddress, gattCallback);

            mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE)
                    .edit()
                    .putBoolean(Constants.BLE_SERVICE_STARTED, true)
                    .apply();
//        }
        return true;
    }

    public void disconnect() {
        Log.d(TAG, " onDestroy () " + (bluetoothCommunicationThread != null ? bluetoothCommunicationThread.getId() : "null " + " service [" + this + ""));

        OBDCommunication.getInstance().disconnect();

        if (bluetoothCommunicationThread != null) {
            bluetoothCommunicationThread.cancel();
        }
        mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE)
                .edit()
                .putBoolean(Constants.BLE_SERVICE_STARTED, false)
                .apply();
    }

    /**
     * return if the battery level is ok (higher than 15%)
     *
     * @return - true, if battery level ok (higher than 15%), false otherwise
     */
    private boolean batteryLevelOk() {
        SharedPreferences preferences = mContext.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
        // check the battery level so that if the battery is  low the service will not start
        IntentFilter batteryStatusIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatusIntent = mContext.registerReceiver(null, batteryStatusIntentFilter);

        if (batteryStatusIntent != null) {
            int level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPercentage = level / (float) scale;
            float lowBatteryPercentageLevel = 0.14f;

            try {
                int lowBatteryLevel = Resources.getSystem().getInteger(Resources.getSystem().getIdentifier("config_lowBatteryWarningLevel", "integer", "android"));
                lowBatteryPercentageLevel = lowBatteryLevel / (float) scale;
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Missing low battery threshold resource");
            }

            preferences.edit().putBoolean(Constants.BACKGROUND_SERVICE_BATTERY_CONTROL, batteryPercentage >= lowBatteryPercentageLevel).apply();
            return batteryPercentage >= lowBatteryPercentageLevel;
        } else {
            preferences.edit().putBoolean(Constants.BACKGROUND_SERVICE_BATTERY_CONTROL, true).apply();
            return true;
        }
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
//        Log.d(TAG, "broadcastUpdate: " + action);
        mContext.sendBroadcast(intent);
    }

    public void onDeviceSelected(BluetoothDevice device) {
        if (device.getAddress().equals(preferences.getString(Constants.EXTRAS_DEVICE_ADDRESS, ""))) {
            preferences.edit().putInt(Constants.LAST_CONNECTION_STATUS, Constants.STATUS_CONNECTING).apply();
        }
    }

    /**
     * List of commands supported
     */
//    static LinkedList<String> list = new LinkedList<>();

    /**
     * defines the thread that sends requests to OBD2 dongle
     */
    private static class BluetoothCommunicationThread extends Thread {

        private long requestInterval = 200;

        private volatile boolean requestData;

        public BluetoothCommunicationThread() {
            requestData = true;
        }

        public void run() {
//            list.clear();
            //Add Supported Commands
//            list.add(OBDHelper.CMD_SPEED);

            Log.d(TAG, " Thread Started " + this.getId());

            while (requestData && isConnected) {
//                String command = list.removeFirst();
                if (isConnected) {
//                    OBDCommunication.getInstance().writeOBDCommand(command);
//                    list.addLast(command);
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

    private VehicleDataListener internalVehicleDataDelegate = new VehicleDataListener() {

        @Override
        public void onSpeed(int speed) {
            Intent intent = new Intent(VEHICLE_DATA_ACTION);
            // You can also include some extra data.
            intent.putExtra(VEHICLE_DATA_TYPE_SPEED, speed);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
            broadcastUpdate(Constants.ACTION_DATA_AVAILABLE, String.valueOf(speed));
        }
    };
}
