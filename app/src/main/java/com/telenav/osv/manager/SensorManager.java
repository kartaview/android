package com.telenav.osv.manager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPOutputStream;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.SensorData;
import com.telenav.osv.utils.Log;

public class SensorManager implements SensorEventListener, LocationListener, ObdManager.ConnectionListener {

    public static final String LINE_SEPARATOR = "\n";

    public static final String SENSOR_FORMAT_VERSION = "1.1.1";

    private static final String TAG = "SensorManager";

    public static ConcurrentLinkedQueue<SensorData> sensorDataQueue = new ConcurrentLinkedQueue<>();

    public static float[] mOrientation = new float[3];

    private static OSVFile sSequence;

    private static GZIPOutputStream mGzipOutputStream;

    private static Runnable sFlushRunnable = new Runnable() {
        @Override
        public void run() {

            StringBuilder result = new StringBuilder();
            while (!sensorDataQueue.isEmpty() && result.length() < 2000) {
                result.append(sensorDataQueue.poll());
            }
//        Log.d(TAG, "flushToDisk: flushing " + size);
            appendLog(result.toString());
        }
    };

    private final OSVApplication mApplication;

    private static HandlerThread mBackgroundThread;

    private static Handler mBackgroundHandler;

    private Context context;

    private android.hardware.SensorManager mSensorManager;

    private android.location.LocationManager mLocationService;

    private float[] mRotationMatrix = new float[16];

    private float[] mGravity = new float[3];

    private float[] mGeomagnetic = new float[3];

    private float[] mRotationMatrixS = new float[16];

    private float[] mOrientationS = new float[3];

    private OSVFile mLogFile;

    public SensorManager(Context context) {
        // get sensorManager and initialise sensor listeners
        this.context = context;
        mSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLocationService = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mApplication = (OSVApplication) context.getApplicationContext();
        mBackgroundThread = new HandlerThread("SensorCollector", Process.THREAD_PRIORITY_FOREGROUND);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    static void logSensorData(SensorData sensorData) {
        sensorDataQueue.add(sensorData);
    }

    static void flushToDisk() {
        if (mBackgroundHandler == null || mBackgroundThread == null || !mBackgroundThread.isAlive()) {
            mBackgroundThread = new HandlerThread("SensorCollector", Process.THREAD_PRIORITY_FOREGROUND);
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
        mBackgroundHandler.post(sFlushRunnable);
    }

    private static void appendLog(String string) {
        try {
            mGzipOutputStream.write(string.getBytes());
        } catch (Exception e) {
            Log.d(TAG, "compress: " + Log.getStackTraceString(e));
        }
    }

    void onPauseOrStop() {
        if (mApplication.getOBDManager() != null) {
            mApplication.getOBDManager().stopRunnable();
            mApplication.getOBDManager().removeConnectionListener(this);
        }
        mSensorManager.unregisterListener(this);
        mLocationService.removeUpdates(this);
        flushToDisk();
        finishLog();
        mLogFile = null;
    }

    private void finishLog() {
        if (mBackgroundHandler == null || mBackgroundThread == null || !mBackgroundThread.isAlive()) {
            mBackgroundThread = new HandlerThread("SensorCollector", Process.THREAD_PRIORITY_FOREGROUND);
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mGzipOutputStream != null) {
                    try {
                        mGzipOutputStream.flush();
                        mGzipOutputStream.finish();
                        mGzipOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    void onResume(final OSVFile sequence) {
        if (mBackgroundHandler == null || mBackgroundThread == null || !mBackgroundThread.isAlive()) {
            mBackgroundThread = new HandlerThread("SensorCollector", Process.THREAD_PRIORITY_FOREGROUND);
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        } else {
            mBackgroundHandler.removeCallbacksAndMessages(null);
        }
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                sSequence = sequence;
                if (mApplication.getOBDManager() != null) {
                    mApplication.getOBDManager().addConnectionListener(SensorManager.this);
//            mOBDIIManager.connect();
                }
                if (mApplication.getOBDManager().isConnected()) {
                    onConnected();
                }
                if (!sSequence.exists()) {
                    sSequence.mkdir();
                }
                mLogFile = new OSVFile(sSequence, "track.txt.gz");
                if (mLogFile.exists()) {
                    mLogFile.delete();
                }
                try {
                    FileOutputStream os = new FileOutputStream(mLogFile, true);
                    mGzipOutputStream = new GZIPOutputStream(os);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                appendLog(Build.MANUFACTURER + " " + android.os.Build.MODEL + ";" + Build.VERSION.RELEASE + ";" + SENSOR_FORMAT_VERSION + ";" + OSVApplication.VERSION_NAME +
                        LINE_SEPARATOR);
                // restore the sensor listeners when user resumes the application.
                initListeners();
            }
        });
    }

    // This function registers sensor listeners for the accelerometer, magnetometer and gyroscope.
    private void initListeners() {
        int READING_RATE = 100000;
        boolean accelerometer = mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), READING_RATE);
        boolean gyroscope = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            gyroscope = mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR), READING_RATE);
        }
        boolean magnetometer = mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), READING_RATE);
        boolean pressure = mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), READING_RATE);
        boolean gravity = mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), READING_RATE);
        Log.d(TAG, "initSensors: accelerometer=" + accelerometer + ", rotation=" + gyroscope + ", magnetometer=" + magnetometer + ", barometer=" + pressure + ", gravity=" +
                gravity);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mLocationService.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, SensorManager.this);
            }
        });
    }

    @Override
    public synchronized void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
//                Log.d(TAG, "onSensorChanged: accelerometer");
                logSensorData(new SensorData(SensorData.ACCELEROMETER, event.values, System.currentTimeMillis()));
                mGravity = event.values;
                break;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
//                Log.d(TAG, "onSensorChanged: gyroscope");
                android.hardware.SensorManager.getRotationMatrixFromVector(mRotationMatrixS, event.values);
                android.hardware.SensorManager.getOrientation(mRotationMatrixS, mOrientationS);
                logSensorData(new SensorData(SensorData.ROTATION, mOrientationS, System.currentTimeMillis()));
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
//                Log.d(TAG, "onSensorChanged: magnetometer");
                mGeomagnetic = event.values;
                if (mGravity != null && mGeomagnetic != null) {
                    boolean success = android.hardware.SensorManager.getRotationMatrix(mRotationMatrix, null, mGravity, mGeomagnetic);
                    if (success) {
                        android.hardware.SensorManager.getOrientation(mRotationMatrix, mOrientation);
                        logSensorData(new SensorData(SensorData.COMPASS, mOrientation, System.currentTimeMillis()));
                    }
                }
                break;
            case Sensor.TYPE_PRESSURE:
//                Log.d(TAG, "onSensorChanged: pressure");
                logSensorData(new SensorData(event.values[0], System.currentTimeMillis()));
                break;
            case Sensor.TYPE_GRAVITY:
//                Log.d(TAG, "onSensorChanged: pressure");
                logSensorData(new SensorData(event.values[0], System.currentTimeMillis()));
                break;
        }
        if (sensorDataQueue.size()>12){
            flushToDisk();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        logSensorData(new SensorData(location));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected() {
        if (mApplication.getOBDManager() != null){
            mApplication.getOBDManager().startRunnable();
        }
    }

    @Override
    public void onDisconnected() {
        if (mApplication.getOBDManager() != null){
            mApplication.getOBDManager().stopRunnable();
        }
    }

    @Override
    public void onSpeedObtained(ObdManager.SpeedData speedData) {
        if (speedData.getSpeed() != -1) {
            logSensorData(new SensorData(speedData.getSpeed(), System.currentTimeMillis()));
        }
    }
}
