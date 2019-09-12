package com.telenav.osv.recorder.sensor;

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

    /**
     * Value used to write multiple lines, which have the number of characters grater than the current value, into metadata file.
     * The value was randomly selected.
     */
    private static final int MAX_BUFFERED_CHARACTERS = 2000;

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

    private static MetadataLoggerListener listener;

    /**
     * The array contains the heading values along the {@code z axis}, {@code x axis} and {@code y axis},
     * provided in this order.
     */
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
        Log.d(TAG, "#logOBD2Data. Obd speed flushed.");
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
                //concatenate multiple lines until the number of characters is grater than the default maximum number.
                //this is used to write multiple lines at once.
                if (result.length() > MAX_BUFFERED_CHARACTERS) {
                    try {
                        appendLog(result.toString());
                    } catch (Exception e) {
                        Log.w(TAG, "flushToDisk. Status: error. Message: Couldn't write to metadata file- " + e.getMessage());
                        if (listener != null) {
                            listener.onMetadataLoggingError(e);
                        }
                        return;
                    }
                    result = new StringBuilder();
                }
            }
            if (result.length() > 0) {
                try {
                    appendLog(result.toString());
                } catch (Exception e) {
                    Log.w(TAG, "flushToDisk. Status: error. Message: Couldn't write to metadata - " + e.getMessage());
                    if (listener != null) {
                        listener.onMetadataLoggingError(e);
                    }
                    return;
                }

            }
            try {
                mOutputStream.flush();
            } catch (IOException e) {
                Log.w(TAG, "flushToDisk. Status: error. Message: Couldn't flush to disk - " + e.getMessage());
                if (listener != null) {
                    listener.onMetadataLoggingError(e);
                }
            }
        });
    }

    @Override
    public void onNewEvent(BaseObject baseObject) {
        if (!activeState.get()) {
            Log.d(TAG, "#onNewEvent. not recording, onNewEvent ignored");
            return;
        }
        Log.d(TAG, String.format("#onNewEvent. recording. type: %s", baseObject.getSensorType()));
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

    private static void appendLog(String string) throws IOException {
        if (mOutputStream != null) {
            mOutputStream.write(string.getBytes());
            Log.d(TAG, "appendLog. Status: success. Message: Write logs.");
        }
    }

    private static void finishLog() {
        ensureBackgroundHandlerExists();
        mBackgroundHandler.post(() -> {
            if (mOutputStream != null) {
                try {
                    String done = "DONE";
                    mOutputStream.write(done.getBytes());
                } catch (Exception e) {
                    Log.w(TAG, "finishLog. Status: error. Message: Couldn't write metadata end - " + e.getMessage());
                }
                try {
                    mOutputStream.flush();
                } catch (Exception e) {
                    Log.w(TAG, "finishLog. Status: error. Message: Couldn't flush output stream - " + e.getMessage());
                }
                try {
                    mOutputStream.close();
                } catch (Exception e) {
                    Log.w(TAG, "finishLog. Status: error. Message: Couldn't close output stream - " + e.getMessage());
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

                    //define a buffer of 1KB = 1024 bytes
                    //the buffer is used to read sequentially from metadata file
                    //and then to write the information to the zip file.
                    byte[] buffer = new byte[1024];
                    int len;
                    //the total number of bytes read into the buffer, or
                    //-1 if there is no more data because the end of
                    //the file has been reached.
                    while ((len = is.read(buffer)) != -1) {
                        zip.write(buffer, 0, len);
                    }
                    if (zipFile.exists() && Utils.fileSize(zipFile) > 0) {
                        Log.d(TAG, "zipLog. Status: success. Message: successfully zipped, delete text file.");
                        boolean success = txtFile.delete();
                        Log.d(TAG, String.format("zipLog. Status: %s. Message: deleting text file.", success ? "success" : "failed"));
                    }
                } catch (IOException e) {
                    Log.w(TAG, String.format("zipLog. Status: error. Message: %s", e.getMessage()));
                    if (txtFile.exists()) {
                        Log.w(TAG, "zipLog. Status: fallback. Message: delete zip.");
                        boolean success = zipFile.delete();
                        Log.d(TAG, String.format("zipLog. Status: %s. Message: deleting zip file.", success ? "success" : "failed"));
                    }
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
                        Log.w(TAG, "zipLog. Status: error. Message: Couldn't close streams - " + e.getMessage());
                    }
                }
            }
            long metadataSize = 0;
            if (zipFile.exists()) {
                metadataSize = Utils.fileSize(zipFile);
            } else {
                metadataSize = Utils.fileSize(txtFile);
            }
            if (listener != null) {
                listener.onMetadataLoggingFinished(metadataSize);
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

    public void onResume(final OSVFile sequence, final boolean safe, MetadataLoggerListener metadataLoggerListener) {
        if (sequence == null) {
            metadataLoggerListener.onMetadataLoggingError(new Exception("Sequence is null."));
            return;
        }
        listener = metadataLoggerListener;
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
                Log.w(TAG, "onResume. Status: error. Message: Couldn't create output stream - " + e.getMessage());
                if (listener != null) {
                    listener.onMetadataLoggingError(e);
                }
                return;
            }
            try {
                appendLog(Build.MANUFACTURER + " " + android.os.Build.MODEL + ";" + Build.VERSION.RELEASE + ";" + SENSOR_FORMAT_VERSION + ";" + OSVApplication.VERSION_NAME + ";"
                        + (safe ? "photo" : "video") + LINE_SEPARATOR);
            } catch (Exception e) {
                Log.w(TAG, "onResume. Status: error. Message: Couldn't write to metadata - " + e.getMessage());
                if (listener != null) {
                    listener.onMetadataLoggingError(e);
                }
                return;
            }

            //register for the desired phone sensors, and start the data collector library
            if (dataCollectorManager == null) {
                Config.Builder configBuilder = new Config.Builder();
                configBuilder.addSource(LibraryUtil.PHONE_SOURCE)
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
                        .addSensorFrequency(LibraryUtil.ROTATION_VECTOR_RAW, LibraryUtil.F_10HZ);

                dataCollectorManager = new DataCollectorManager(context, configBuilder.build());
            }
            Log.d(TAG, "DataCollector start collecting");
            dataCollectorManager.startCollecting();
        });
    }

    /**
     * Interface which is notified when an error occurred during metadata logging.
     */
    public interface MetadataLoggerListener {

        /**
         * Callback received when an error was thrown during metadata writing.
         */
        void onMetadataLoggingError(Exception e);

        /**
         * Callback received when metadata logging was finished and the zipped file was created.
         * @param metadataSize {@code long} which represents the size of the metadata, either the {@code .txt} file or {@code .zip} file.
         */
        void onMetadataLoggingFinished(long metadataSize);
    }
}