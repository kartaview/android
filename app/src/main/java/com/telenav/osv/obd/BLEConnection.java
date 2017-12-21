package com.telenav.osv.obd;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by dianat on 3/25/2016.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BLEConnection {

    /**
     * Default stops scanning after 10 sec
     */
    private static final long DEFAULT_SCAN_PERIOD = 10000;

    private static final String TAG = "BLEConnection";

    /**
     * stop scanning after scanPeriod of time
     */
    private long scanPeriod = DEFAULT_SCAN_PERIOD;

    /**
     * bleutooth adapter
     */
    private BluetoothAdapter bluetoothAdapter;

    /**
     * handler for stopping the scan after SCAN_PERIOD
     */
    private Handler handler = new Handler(Looper.getMainLooper());

    /**
     * for Lollipop.
     */
    private BluetoothLeScanner bluetoothLeScanner;

    /**
     * is scanning in progress
     */
    private boolean scanning;

    private BLEConnection() {
    }

    public static BLEConnection getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * initialize the objects needed for the connection
     * @param context context
     * @return the bluetooth adapter instance
     */
    public BluetoothAdapter initConnection(Context context) {
        // Initializes Bluetooth adapter.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        return bluetoothAdapter;
    }

    /**
     * Starts the scanning
     * For Lollipop
     * @param scanCallback - the callback of the scanning
     */
    public void startScanning(final ScanCallback scanCallback) {
        final BluetoothLeScanner scanner = bluetoothLeScanner;
        if (scanner != null && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    scanning = false;
                    try {
                        scanner.stopScan(scanCallback);
                    } catch (IllegalStateException e) {
                        Log.d(TAG, "startScanning: " + Log.getStackTraceString(e));
                    }
                }
            }, scanPeriod);

            scanning = true;
            scanner.startScan(scanCallback);
        }
    }

    /**
     * Stop the scanning
     * For Lollipop
     * @param scanCallback - the callback of the scanning
     */
    public void stopScanning(final ScanCallback scanCallback) {
        scanning = false;
        final BluetoothLeScanner scanner = bluetoothLeScanner;
        if (scanner != null) {
            scanner.stopScan(scanCallback);
        }
    }

    /**
     * Starts the scanning
     * For Jelly Bean MR2 and Kitkat
     * @param leScanCallback - the callback of the scanning
     */
    @Deprecated
    public void startScanning(final BluetoothAdapter.LeScanCallback leScanCallback) {
        // Stops scanning after a pre-defined scan period.
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                scanning = false;
                bluetoothAdapter.stopLeScan(leScanCallback);
            }
        }, scanPeriod);

        scanning = true;
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    /**
     * Stop the scanning
     * For Jelly Bean MR2 and Kitkat
     * @param leScanCallback - the callback of the scanning
     */
    @Deprecated
    public void stopScanning(final BluetoothAdapter.LeScanCallback leScanCallback) {
        scanning = false;
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    /**
     * Returns if the scanning is in progress
     * @return - true is scanning in progress, false otherwise
     */
    public boolean isScanning() {
        return scanning;
    }

    /**
     * set the scanning period
     * @param period - period to scan
     */
    public void setScanPeriod(int period) {
        scanPeriod = period;
    }

    private static class SingletonHolder {

        private static final BLEConnection INSTANCE = new BLEConnection();
    }
}
