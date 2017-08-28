package com.telenav.osv.manager.location;

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
import android.os.Process;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.SensorData;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings({"MissingPermission", "ResultOfMethodCallIgnored"})
public class SensorManager implements SensorEventListener, LocationListener {

  private static final String LINE_SEPARATOR = "\n";

  private static final String SENSOR_FORMAT_VERSION = "1.1.6";

  private static final String TAG = "SensorManager";

  public static float[] mHeadingValues = new float[3];

  private static ConcurrentLinkedQueue<SensorData> sensorDataQueue = new ConcurrentLinkedQueue<>();

  private static OSVFile sSequence;

  private static FileOutputStream mOutputStream;

  private static HandlerThread mBackgroundThread;

  private static Handler mBackgroundHandler;

  private android.hardware.SensorManager mSensorManager;

  private android.location.LocationManager mLocationService;

  private float[] mHeadingMatrix = new float[16];

  private float[] mGravity = new float[3];

  private float[] mRotationMatrixS = new float[16];

  private float[] mOrientationS = new float[3];

  private OSVFile mLogFile;

  public SensorManager(Context context) {
    // get sensorManager and initialise sensor listeners
    mSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    mLocationService = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
  }

  public static void logSensorData(SensorData sensorData) {
    sensorDataQueue.add(sensorData);
  }

  public static void flushToDisk() {
    if (mBackgroundHandler == null || mBackgroundThread == null || !mBackgroundThread.isAlive()) {
      mBackgroundThread = new HandlerThread("SensorCollector", Process.THREAD_PRIORITY_FOREGROUND);
      mBackgroundThread.start();
      mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    final ConcurrentLinkedQueue<SensorData> tempQueue = sensorDataQueue;
    mBackgroundHandler.post(() -> {
      StringBuilder result = new StringBuilder();
      while (!tempQueue.isEmpty()) {
        result.append(tempQueue.poll());
        if (result.length() > 2000) {
          appendLog(result.toString());
          result = new StringBuilder();
        }
      }
      //        Log.d(TAG, "flushToDisk: flushing " + size);
      if (result.length() > 0) {
        appendLog(result.toString());
      }
      try {
        mOutputStream.flush();
      } catch (Exception ignored) {
      }
    });
  }

  private static void appendLog(String string) {
    try {
      mOutputStream.write(string.getBytes());
    } catch (IOException ignored) {
    } catch (Exception e) {
      Log.w(TAG, "compress: " + Log.getStackTraceString(e));
    }
  }

  private static void finishLog() {
    if (mBackgroundHandler == null || mBackgroundThread == null || !mBackgroundThread.isAlive()) {
      mBackgroundThread = new HandlerThread("SensorCollector", Process.THREAD_PRIORITY_FOREGROUND);
      mBackgroundThread.start();
      mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    mBackgroundHandler.post(() -> {
      if (mOutputStream != null) {
        try {
          try {
            String done = "DONE";
            mOutputStream.write(done.getBytes());
          } catch (Exception e) {
            Log.w(TAG, "compress: " + Log.getStackTraceString(e));
          }
          try {
            mOutputStream.flush();
          } catch (IllegalStateException ignored) {
          }
          mOutputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      zipLog();
    });
  }

  private static void zipLog() {
    if (sSequence != null) {
      OSVFile zipFile = new OSVFile(sSequence, "track.txt.gz");
      OSVFile txtFile = new OSVFile(sSequence, "track.txt");
      if (txtFile.exists()) {
        try {
          FileInputStream is = new FileInputStream(txtFile);
          FileOutputStream os = new FileOutputStream(zipFile);
          GZIPOutputStream zip = new GZIPOutputStream(os);

          byte[] buffer = new byte[1024];
          int len;
          while ((len = is.read(buffer)) != -1) {
            zip.write(buffer, 0, len);
          }
          zip.finish();
          zip.close();
          is.close();
          if (zipFile.exists() && Utils.fileSize(zipFile) > 0) {
            Log.d(TAG, "zipLog: successfully zipped");
            txtFile.delete();
          }
        } catch (IOException e) {
          Log.w(TAG, "zipLog: " + Log.getStackTraceString(e));
        }
      }
    }
  }

  public void onPauseOrStop() {
    mSensorManager.unregisterListener(this);
    try {
      mLocationService.removeUpdates(this);
    } catch (SecurityException ignored) {
    }
    flushToDisk();
    finishLog();
    mLogFile = null;
  }

  public void onResume(final OSVFile sequence, final boolean safe) {
    if (sequence == null) {
      return;
    }
    if (mBackgroundHandler == null || mBackgroundThread == null || !mBackgroundThread.isAlive()) {
      mBackgroundThread = new HandlerThread("SensorCollector", Process.THREAD_PRIORITY_FOREGROUND);
      mBackgroundThread.start();
      mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    mBackgroundHandler.post(() -> {
      sensorDataQueue = new ConcurrentLinkedQueue<>();
      sSequence = sequence;
      if (!sSequence.exists()) {
        sSequence.mkdir();
      }
      mLogFile = new OSVFile(sSequence, "track.txt");
      if (mLogFile.exists()) {
        mLogFile.delete();
      }
      try {
        mOutputStream = new FileOutputStream(mLogFile, true);
      } catch (Exception e) {
        e.printStackTrace();
      }
      appendLog(Build.MANUFACTURER + " " + Build.MODEL + ";" + Build.VERSION.RELEASE + ";" + SENSOR_FORMAT_VERSION + ";" +
                    OSVApplication.VERSION_NAME + ";" + (safe ? "photo" : "video") + LINE_SEPARATOR);
      // restore the sensor listeners when user resumes the application.
      initListeners();
    });
  }

  // This function registers sensor listeners for the accelerometer, magnetometer and gyroscope.
  private void initListeners() {
    int READING_RATE = 100000; // 10/sec
    //        int READING_RATE_HIGH = 50000; // defaults to 25/sec
    //        int READING_RATE = android.hardware.SensorManager.SENSOR_DELAY_FASTEST;
    boolean accelerometer = mSensorManager
        .registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), READING_RATE, mBackgroundHandler);
    boolean gyroscope = false;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
      gyroscope = mSensorManager
          .registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR), READING_RATE, mBackgroundHandler);
    }
    boolean magnetometer = mSensorManager
        .registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), READING_RATE, mBackgroundHandler);
    boolean pressure =
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), READING_RATE, mBackgroundHandler);
    boolean gravity =
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), READING_RATE, mBackgroundHandler);
    Log.d(TAG,
          "initSensors: accelerometer=" + accelerometer + ", rotation=" + gyroscope + ", magnetometer=" + magnetometer + ", barometer=" +
              pressure + ", gravity=" + gravity);
    mLocationService.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, SensorManager.this, mBackgroundHandler.getLooper());
  }

  @Override
  public synchronized void onSensorChanged(SensorEvent event) {
    switch (event.sensor.getType()) {
      case Sensor.TYPE_LINEAR_ACCELERATION:
        //                Log.d(TAG, "onSensorChanged: accelerometer " + event.values[0] + "     " + event.values[1] + "     " + event
        // .values[2]);
        logSensorData(new SensorData(SensorData.ACCELEROMETER, event.values, event.timestamp));
        mGravity = event.values;
        break;
      case Sensor.TYPE_GAME_ROTATION_VECTOR:
        android.hardware.SensorManager.getRotationMatrixFromVector(mRotationMatrixS, event.values);
        android.hardware.SensorManager.getOrientation(mRotationMatrixS, mOrientationS);
        logSensorData(new SensorData(SensorData.ROTATION, mOrientationS, event.timestamp));
        //                Log.d(TAG, "onSensorChanged: gyroscope  yaw = " + (-mOrientationS[0]) + "  pitch = " + (-mOrientationS[1]) + "
        // roll = " + mOrientationS[2]);
        break;
      case Sensor.TYPE_MAGNETIC_FIELD:
        //                Log.d(TAG, "onSensorChanged: magnetometer");
        float[] mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
          boolean success = android.hardware.SensorManager.getRotationMatrix(mHeadingMatrix, null, mGravity, mGeomagnetic);
          if (success) {
            android.hardware.SensorManager
                .remapCoordinateSystem(mHeadingMatrix, android.hardware.SensorManager.AXIS_X, android.hardware.SensorManager.AXIS_Z,
                                       mHeadingMatrix);
            android.hardware.SensorManager.getOrientation(mHeadingMatrix, mHeadingValues);
            mHeadingValues[0] = (float) Math.toDegrees(mHeadingValues[0]);
            mHeadingValues[1] = (float) Math.toDegrees(mHeadingValues[1]);
            mHeadingValues[2] = (float) Math.toDegrees(mHeadingValues[2]);
            mHeadingValues[0] = mHeadingValues[0] >= 0 ? mHeadingValues[0] : mHeadingValues[0] + 360;
            //                        Log.d(TAG, "onSensorChanged: heading azimuth                                    " + (int)
            // mHeadingValues[0]);
            //                        Log.d(TAG, "onSensorChanged: heading pitch                      " + (int) mHeadingValues[1]);
            //                        Log.d(TAG, "onSensorChanged: heading roll    " + (int) mHeadingValues[2]);
            logSensorData(new SensorData(SensorData.COMPASS, mHeadingValues, event.timestamp));// System.currentTimeMillis()));
          }
        }
        break;
      case Sensor.TYPE_PRESSURE:
        //                Log.d(TAG, "onSensorChanged: pressure " + event.values[0]);
        logSensorData(new SensorData(event.values[0], event.timestamp));
        break;
      case Sensor.TYPE_GRAVITY:
        //                Log.d(TAG, "onSensorChanged: gravity " + event.values[0] + "     " + event.values[1] + "     " + event.values[2]);
        logSensorData(new SensorData(SensorData.GRAVITY, event.values, event.timestamp));
        break;
    }
    if (sensorDataQueue.size() > 12) {
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
}
