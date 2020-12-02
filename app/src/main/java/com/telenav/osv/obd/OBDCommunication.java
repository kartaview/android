package com.telenav.osv.obd;

import java.util.UUID;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Created by dianat on 3/25/2016.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class OBDCommunication {

    /**
     * UUID service
     */
    private static final UUID serviceUUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

    /**
     * UUID characteristic for write and notify
     */
    private static final UUID characteristicUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private final static String TAG = OBDCommunication.class.getSimpleName();

    /**
     * client characteristic
     */
    private static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * singleton instance
     */
    private static OBDCommunication sInstance;

    /**
     * bluetooth gatt - Generic Attribute Profile
     */
    private BluetoothGatt bluetoothGatt;

    /**
     * bluetooth adapter
     */
    private BluetoothAdapter bluetoothAdapter;

    /**
     * bluetooth manager
     */
    private BluetoothManager bluetoothManager;

    /**
     * address of the bluetoth device
     */
    private String bluetoothDeviceAddress;

    private OBDCommunication() {
    }

    public static OBDCommunication getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     * @param context context
     * @return Return true if the initialization is successful.
     */
    public boolean initialize(Context context) {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     * @param context context
     * @param address The device address of the destination device.
     * @param gattCallback the callback of the connection
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(Context context, final String address, final BluetoothGattCallback gattCallback) {
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (bluetoothDeviceAddress != null && address.equals(bluetoothDeviceAddress) && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing bluetoothGatt for connection.");
            return bluetoothGatt.connect();
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect parameter to true.
        bluetoothGatt = device.connectGatt(context, true, gattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bluetoothDeviceAddress = address;
        return true;
    }

    /**
     * subscribe the characteristic for the notification
     */
    public void suscribeToNotification() {
        //subscribe to notification
        BluetoothGatt gatt = bluetoothGatt;
        if (gatt != null) {
            BluetoothGattCharacteristic characteristic = gatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Discover the services
     */
    public void discoverServices() {
        bluetoothGatt.discoverServices();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void disconnectFromBLEDevice() {
        BluetoothGatt gatt = bluetoothGatt;
        if (gatt == null) {
            return;
        }
        gatt.close();
        bluetoothGatt = null;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGatt gatt = bluetoothGatt;
        if (gatt != null) {
            gatt.disconnect();
        }
    }

    /**
     * writes a command to the characteristic
     * @param p_command - command to be sent
     */
    public void writeOBDCommand(String p_command) {
        BluetoothGatt gatt = bluetoothGatt;
        if (gatt != null && bluetoothGatt.getService(serviceUUID) != null) {
            BluetoothGattCharacteristic writeChar = bluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);

            writeChar.setValue(p_command + '\r');
            gatt.writeCharacteristic(writeChar);
        }
    }

    private static class SingletonHolder {

        private static final OBDCommunication INSTANCE = new OBDCommunication();
    }
}
