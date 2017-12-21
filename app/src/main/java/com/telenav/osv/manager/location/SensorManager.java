package com.telenav.osv.manager.location;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import com.telenav.datacollectormodule.config.Config;
import com.telenav.datacollectormodule.datacollectorstarter.DataCollectorManager;
import com.telenav.datacollectormodule.datatype.EventDataListener;
import com.telenav.datacollectormodule.datatype.datatypes.BaseObject;
import com.telenav.datacollectormodule.datatype.datatypes.GPSData;
import com.telenav.datacollectormodule.datatype.datatypes.PositionObject;
import com.telenav.datacollectormodule.datatype.datatypes.PressureObject;
import com.telenav.datacollectormodule.datatype.datatypes.ThreeAxesObject;
import com.telenav.datacollectormodule.datatype.util.LibraryUtil;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.metadata.DataCollectorItemWrapper;
import com.telenav.osv.item.metadata.Obd2Data;
import com.telenav.osv.item.metadata.VideoData;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

public class SensorManager implements EventDataListener {

    private static final String LINE_SEPARATOR = "\n";

    private static final String SENSOR_FORMAT_VERSION = "1.1.6";

    private static final String TAG = "SensorManager";

    private static final int MAX_BUFFERED_LINES = 1000;

    private static final AtomicBoolean gpsAvailableInMetadata = new AtomicBoolean(false);

    private static final AtomicBoolean activeState = new AtomicBoolean(false);

    //TODO move everything from the static data queue, in a non-static field...all this logic pertaining to logging and to the sensor data queue
    //TODO doesn't need to be static, nor should it be in the sensor manager...a separate logger, or metadata-only responsible class is necessary
    private static ConcurrentLinkedQueue<Object> sensorDataQueue = new ConcurrentLinkedQueue<>();

    private static OSVFile sSequence;

    private static FileOutputStream mOutputStream;

    private static HandlerThread mBackgroundThread;

    private static Handler mBackgroundHandler;

    private static AtomicInteger atomicInteger = new AtomicInteger();

    private float[] mHeadingValues = new float[3];

    private OSVFile mLogFile;

    private Context context;

    /**
     * the manager used for collecting sensor data
     */
    private DataCollectorManager dataCollectorManager;

    public SensorManager(Context context) {
        this.context = context;
    }

    public static void logSensorData(DataCollectorItemWrapper sensorData) {
        sensorDataQueue.add(sensorData);
        if (isGpsData(sensorData)) {
            if (!gpsAvailableInMetadata.get()) {
                //to whomever will work on this in the future, this is just a quick workaround, and I apologise in advance
                //for the technical debt introduced by this 'quick fix'.
                //
                //this is done to know when at least one location entry is written in the metadata file. If a picture is
                //taken before at least one location entry exists in the metadata file, that picture will have its location
                //set to 0,0, when processed on the backend.
                //
                //however, ideally this app shouldn't even use two distinct location providers...(one for the map, one for
                //the metadata - datacollector). it should only use one location provider, which will provide location data
                //to both the map and the metadata writer class...then this issue wouldn't be an issue any longer.
                Log.d(TAG, "got gps position, setting atomic bool to true");
                gpsAvailableInMetadata.set(true);
            }
        }
        flushIfNeeded();
    }

    public static void logOBD2Data(Obd2Data obd2Data) {
        sensorDataQueue.add(obd2Data);
        flushIfNeeded();
    }

    public static void logVideoData(VideoData videoData) {
        sensorDataQueue.add(videoData);
        flushToDisk();
    }

    /**
     * Resets the flag (i.e. sets it to false) which stores whether at least one location entry exists in the metadata file, or not.
     */
    public static void resetGpsDataAvailableInMetadata() {
        Log.d(TAG, "#resetGpsDataAvailableInMetadata");
        gpsAvailableInMetadata.set(false);
    }

    /**
     * @return {@code true} if the metadata file contains at least one location entry, {@code false} otherwise.
     */
    public static boolean isGpsDataAvailableInMetadata() {
        return gpsAvailableInMetadata.get();
    }

    public static void flushToDisk() {
        ensureBackgroundHandlerExists();
        final ConcurrentLinkedQueue<Object> tempQueue = sensorDataQueue;
        mBackgroundHandler.post(() -> {
            atomicInteger.set(0);
            StringBuilder result = new StringBuilder();
            while (!tempQueue.isEmpty()) {
                result.append(tempQueue.poll());
                if (result.length() > 2000) {
                    appendLog(result.toString());
                    result = new StringBuilder();
                }
            }
            if (result.length() > 0) {
                appendLog(result.toString());
            }
            try {
                mOutputStream.flush();
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void onNewEvent(BaseObject baseObject) {
        if (!activeState.get()) {
            Log.d(TAG, "#onNewEvent. not recording, onNewEvent ignored");
            return;
        }
        switch (baseObject.getSensorType()) {
            case LibraryUtil.ACCELEROMETER:
                if (baseObject.getStatusCode() == LibraryUtil.PHONE_SENSOR_READ_SUCCESS) {
                    ThreeAxesObject accelerometerObject = (ThreeAxesObject) baseObject;
                    //osc implementation
                    logSensorData(new DataCollectorItemWrapper(accelerometerObject));
                }
                break;
            case LibraryUtil.HEADING:
                if (baseObject.getStatusCode() == LibraryUtil.PHONE_SENSOR_READ_SUCCESS) {
                    ThreeAxesObject compassObject = (ThreeAxesObject) baseObject;
                    logSensorData(new DataCollectorItemWrapper(compassObject));
                    //-z
                    mHeadingValues[0] = compassObject.getzValue();
                    //x
                    mHeadingValues[1] = compassObject.getxValue();
                    //y
                    mHeadingValues[2] = compassObject.getyValue();
                }
                break;
            case LibraryUtil.PRESSURE:
                if (baseObject.getStatusCode() == LibraryUtil.PHONE_SENSOR_READ_SUCCESS) {
                    PressureObject pressureObject = (PressureObject) baseObject;
                    logSensorData(new DataCollectorItemWrapper(pressureObject));
                }
                break;
            case LibraryUtil.GRAVITY:
                if (baseObject.getStatusCode() == LibraryUtil.PHONE_SENSOR_READ_SUCCESS) {
                    ThreeAxesObject gravityObject = ((ThreeAxesObject) baseObject);
                    logSensorData(new DataCollectorItemWrapper(gravityObject));
                }
                break;
            case LibraryUtil.LINEAR_ACCELERATION:
                if (baseObject.getStatusCode() == LibraryUtil.PHONE_SENSOR_READ_SUCCESS) {
                    ThreeAxesObject linearAccelerationObject = ((ThreeAxesObject) baseObject);
                    logSensorData(new DataCollectorItemWrapper(linearAccelerationObject));
                }
                break;
            case LibraryUtil.ROTATION_VECTOR_RAW:
                if (baseObject.getStatusCode() == LibraryUtil.PHONE_SENSOR_READ_SUCCESS) {
                    ThreeAxesObject rotVectRawObject = ((ThreeAxesObject) baseObject);
                    logSensorData(new DataCollectorItemWrapper(rotVectRawObject));
                }
                break;
            case LibraryUtil.PHONE_GPS:
                if (baseObject.getStatusCode() == LibraryUtil.PHONE_SENSOR_READ_SUCCESS) {
                    PositionObject positionObject = (PositionObject) baseObject;
                    Log.d(TAG, "SensorManager#logSensorData-> PHONE GPS. obj: " + ((baseObject != null) ? " Not null." : " Null."));
                    logSensorData(new DataCollectorItemWrapper(positionObject));
                } else {
                    Log.d(TAG, "SensorManager#logSensorData-> error when reading PHONE GPS. obj: " + ((baseObject != null) ? baseObject.getTimestamp() : "Null."));
                }
                break;
            case LibraryUtil.GPS_DATA:
                if (baseObject.getStatusCode() == LibraryUtil.PHONE_SENSOR_READ_SUCCESS) {
                    GPSData gpsData = (GPSData) baseObject;
                    Log.d(TAG, "SensorManager#logSensorData-> GPS DATA. obj: " + ((baseObject != null) ? " Not null." : " Null."));
                    logSensorData(new DataCollectorItemWrapper(gpsData));
                } else {
                    Log.d(TAG, "SensorManager#logSensorData-> error when reading GPS DATA. obj: " + ((baseObject != null) ? (baseObject.getTimestamp()) : "Null."));
                }
        }
    }

    private static boolean isGpsData(DataCollectorItemWrapper sensorData) {
        return sensorData.getType().equals(LibraryUtil.GPS_DATA) || sensorData.getType().equals(LibraryUtil.PHONE_GPS);
    }

    private static void flushIfNeeded() {
        int count = atomicInteger.incrementAndGet();
        if (count >= MAX_BUFFERED_LINES) {
            flushToDisk();
        }
    }

    private static void prettyPrintFlushedLogs(ConcurrentLinkedQueue<VideoData> tempQueue) {
        StringBuilder stringBuilder = new StringBuilder();
        for (VideoData entry : tempQueue) {
            stringBuilder.append(entry.toString()).append("\n");
        }

        Log.d(TAG, "Logs flushed: " + stringBuilder.toString());
    }

    private static void appendLog(String string) {
        try {
            if (mOutputStream != null) {
                mOutputStream.write(string.getBytes());
            }
        } catch (IOException ignored) {
        } catch (Exception e) {
            Log.w(TAG, "compress: " + Log.getStackTraceString(e));
        }
    }

    private static void finishLog() {
        ensureBackgroundHandlerExists();
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
                FileInputStream is = null;
                FileOutputStream os = null;
                GZIPOutputStream zip = null;
                try {
                    is = new FileInputStream(txtFile);
                    os = new FileOutputStream(zipFile);
                    zip = new GZIPOutputStream(os);

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        zip.write(buffer, 0, len);
                    }
                    if (zipFile.exists() && Utils.fileSize(zipFile) > 0) {
                        Log.d(TAG, "zipLog: successfully zipped");
                        txtFile.delete();
                    }
                } catch (IOException e) {
                    Log.w(TAG, "zipLog: " + Log.getStackTraceString(e));
                } finally {
                    try {
                        if (zip != null) {
                            zip.finish();

                            zip.close();
                        }
                        if (os != null) {
                            os.close();
                        }
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void ensureBackgroundHandlerExists() {
        if (mBackgroundHandler == null || mBackgroundThread == null || !mBackgroundThread.isAlive()) {
            mBackgroundThread = new HandlerThread("SensorCollector", Process.THREAD_PRIORITY_FOREGROUND);
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }

    public float[] getHeadingValues() {
        return mHeadingValues;
    }

    public void onPauseOrStop() {
        ensureBackgroundHandlerExists();
        mBackgroundHandler.post(() -> {
            Log.d(TAG, "Set active state to false");
            activeState.set(false);
            if (dataCollectorManager != null) {
                Log.d(TAG, "DataCollector stop collecting");
                dataCollectorManager.stopCollecting();
                Log.d(TAG, "DataCollector stop collecting--> Done");
            }
            resetGpsDataAvailableInMetadata();
        });

        flushToDisk();
        finishLog();
        mLogFile = null;
    }

    public void onResume(final OSVFile sequence, final boolean safe) {
        if (sequence == null) {
            return;
        }
        ensureBackgroundHandlerExists();
        mBackgroundHandler.post(() -> {
            Log.d(TAG, "Set active state to true");
            resetGpsDataAvailableInMetadata();
            activeState.set(true);
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
            appendLog(Build.MANUFACTURER + " " + android.os.Build.MODEL + ";" + Build.VERSION.RELEASE + ";" + SENSOR_FORMAT_VERSION + ";" + OSVApplication.VERSION_NAME + ";"
                    + (safe ? "photo" : "video") + LINE_SEPARATOR);

            //register for the desired phone sensors, and start the data collector library
            Config config = new Config();
            config.addSource(LibraryUtil.PHONE_SOURCE)
                    //heading
                    .addDataListener(SensorManager.this, LibraryUtil.HEADING)
                    .addSensorFrequency(LibraryUtil.HEADING, LibraryUtil.F_10HZ)
                    //pressure
                    .addDataListener(SensorManager.this, LibraryUtil.PRESSURE)
                    .addSensorFrequency(LibraryUtil.PRESSURE, LibraryUtil.F_10HZ)
                    //gravity
                    .addDataListener(SensorManager.this, LibraryUtil.GRAVITY)
                    .addSensorFrequency(LibraryUtil.GRAVITY, LibraryUtil.F_10HZ)
                    //linear accel
                    .addDataListener(SensorManager.this, LibraryUtil.LINEAR_ACCELERATION)
                    .addSensorFrequency(LibraryUtil.LINEAR_ACCELERATION, LibraryUtil.F_10HZ)
                    //rotation vector
                    .addDataListener(SensorManager.this, LibraryUtil.ROTATION_VECTOR_RAW)
                    .addSensorFrequency(LibraryUtil.ROTATION_VECTOR_RAW, LibraryUtil.F_10HZ)
                    //gps data
                    .addDataListener(SensorManager.this, LibraryUtil.GPS_DATA);

            dataCollectorManager = new DataCollectorManager(config);
            Log.d(TAG, "DataCollector start collecting");
            dataCollectorManager.startCollecting(context);
        });
    }
}