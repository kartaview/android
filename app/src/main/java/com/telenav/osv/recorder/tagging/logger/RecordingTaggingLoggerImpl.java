package com.telenav.osv.recorder.tagging.logger;

import android.location.Location;

import androidx.annotation.Nullable;

import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.event.hardware.camera.ImageSavedEvent;
import com.telenav.osv.item.KVFile;
import com.telenav.osv.recorder.tagging.converter.GeoJsonConverter;
import com.telenav.osv.utils.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.Subject;

/**
 * Implementation for {@code RecordingTaggingLogger} which will contain the low level logic of the interface.
 * This class should be available only during a recording session and should be released when the recording session ends.
 */
public class RecordingTaggingLoggerImpl implements RecordingTaggingLogger {

    /**
     * Name of the csv file which will be used as an input for the geoJson converter.
     */
    private static final String RECORDING_TAGGING_FILE_NAME_TXT = "recording_tagging.txt";

    /**
     * Value used to write multiple lines, which have the number of characters grater than the current value, into metadata file.
     * The value was randomly selected.
     */
    private static final int MAX_BUFFERED_CHARACTERS = 2000;

    /**
     * The class identifier for logging purposes.
     */
    private final static String TAG = RecordingTaggingLoggerImpl.class.getSimpleName();

    /**
     * Object used for synchronization across all signaling events.
     */
    private static final Object synchObject = new Object();

    /**
     * Concurrent queue used in order to store line write.
     */
    private ConcurrentLinkedQueue<Object> sensorDataQueue;

    /**
     * Synchronization boolean under the form of {@link AtomicBoolean} used for switching between one way and two way road types.
     */
    private AtomicBoolean oneWay = new AtomicBoolean();

    /**
     * Reference to the output stream used for IO operations.
     */
    private FileOutputStream mOutputStream;

    /**
     * The last location used for event signaling to be present into the csv file.
     */
    private Location lastLocation;

    /**
     * Reference to the geo json converter used at the end of the tagging process. This ensures a csv file will be converted to a geoJson file.
     */
    private GeoJsonConverter geoJsonConverter;

    /**
     * Reference to the text file in which lines of data will be appended by using {@link #appendLog(String)}.
     */
    private KVFile recordingTxtFile;

    /**
     * The sequence folder which will be required for tagging purposes both for the {@code .txt} csv file and for the {@code .geoJson} file.
     */
    private KVFile sequenceFolder;

    /**
     * The local source for the sequence used in order to update disk sizes for it.
     */
    private SequenceLocalDataSource sequenceLocalDataSource;

    /**
     * The {@code Disposable} which will be used to be cleared after the process is over.
     */
    private Disposable imageSavedSubjectDisposable;

    /**
     * Default constructor for the current class.
     */
    public RecordingTaggingLoggerImpl(Subject<ImageSavedEvent> imageSavedSubject, SequenceLocalDataSource sequenceLocalDataSource) {
        this.sequenceLocalDataSource = sequenceLocalDataSource;
        observeOnImageSavedSubject(imageSavedSubject);
        oneWay.set(false);
        geoJsonConverter = new GeoJsonConverter();
    }

    @Override
    public void onRoadClosed(@Nullable String note) {
        synchronized (synchObject) {
            logSensorData(true, false, note);
        }
    }

    @Override
    public void onNarrowRoad(@Nullable String note) {
        synchronized (synchObject) {
            logSensorData(false, true, note);
        }
    }

    @Override
    public void onRoadWay(boolean isOneWay) {
        synchronized (synchObject) {
            oneWay.set(isOneWay);
        }
    }

    @Override
    public void initTagging(KVFile sequenceFolder) {
        sensorDataQueue = new ConcurrentLinkedQueue<>();
        this.sequenceFolder = sequenceFolder;
        try {
            recordingTxtFile = new KVFile(sequenceFolder, RECORDING_TAGGING_FILE_NAME_TXT);
            this.mOutputStream = new FileOutputStream(recordingTxtFile, true);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "onResume. Status: error. Message: Couldn't create output stream - " + e.getMessage());
        }
    }

    @Override
    public void dropNote(String note) {
        synchronized (synchObject) {
            logSensorData(false, false, note);
        }
    }

    @Override
    public boolean isOneWay() {
        return oneWay.get();
    }

    @Override
    public void finish(String sequenceId, SequenceDetailsLocal sequenceDetailsLocal) {
        if (recordingTxtFile == null) {
            Log.d(TAG, "finish. Status: error. Message: txt file not found.");
            return;
        }
        if (imageSavedSubjectDisposable != null && !imageSavedSubjectDisposable.isDisposed()) {
            imageSavedSubjectDisposable.dispose();
            imageSavedSubjectDisposable = null;
        }
        flushToDisk();
        finishLog();
        long sequenceDiskSize = sequenceDetailsLocal.getDiskSize();
        long taggingFileSize = geoJsonConverter.convert(recordingTxtFile, sequenceFolder);
        long newSequenceSize = sequenceDiskSize + taggingFileSize;
        Log.d(TAG,
                String.format("finish. Status: update sequence disk size. New size: %s. Sequence size: %s. Tagging size: %s.",
                        newSequenceSize,
                        sequenceDiskSize,
                        taggingFileSize));
        sequenceLocalDataSource.updateDiskSize(sequenceId, newSequenceSize);
        sequenceDetailsLocal.setDiskSize(newSequenceSize);
    }

    /**
     * Flushes to disk all the data in the queue which contains line data. This is based of a logic on queue sizes.
     */
    private void flushToDisk() {
        final ConcurrentLinkedQueue<Object> tempQueue = sensorDataQueue;
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
                return;
            }
        }
        try {
            mOutputStream.flush();
        } catch (IOException e) {
            Log.w(TAG, "flushToDisk. Status: error. Message: Couldn't flush to disk - " + e.getMessage());
        }
    }

    /**
     * This will finish the logging process and clean the resources available.
     */
    private void finishLog() {
        if (mOutputStream != null) {
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
    }

    /**
     * Writes a line of {@code String} data into an output stream.
     * @param string the {@code String} which contains data to be appended to the {@link #mOutputStream}.
     * @throws IOException throws any exception related to IO operations.
     */
    private void appendLog(String string) throws IOException {
        if (mOutputStream != null) {
            mOutputStream.write(string.getBytes());
            Log.d(TAG, "appendLog. Status: success. Message: Write logs.");
        }
    }

    /**
     * Observes on image saved subject in order to persist the location with the current tagging information.
     * @param imageSavedSubject the subject which will deliver the {@code imageSavedEvent}.
     */
    private void observeOnImageSavedSubject(Subject<ImageSavedEvent> imageSavedSubject) {
        imageSavedSubjectDisposable = imageSavedSubject
                .observeOn(Schedulers.io())
                .filter(item -> mOutputStream != null)
                .subscribe(
                        imageSavedEvent -> {
                            synchronized (synchObject) {
                                lastLocation = imageSavedEvent.getImageLocation();
                                logSensorData(false, false, null);
                            }

                            Log.d(TAG, String.format("observeOnImageCaptureEvents. Status: imageSavedEvent. Message: New picture at location %s",
                                    imageSavedEvent.getImageLocation()));
                        },
                        throwable -> Log.d(TAG, String.format("observeOnImageCaptureEvents. Status: error. Message: %s", throwable.getLocalizedMessage()))
                );
    }

    /**
     * Logs the sensor data based on the set {@link GeoJsonConverter#convertToCsvLine(boolean, Location, boolean, boolean, String)}.
     */
    private void logSensorData(boolean closedRoad, boolean narrowRoad, @Nullable String escapeNote) {
        if (lastLocation == null) {
            Log.d(TAG, "logSensorData. Status: error. Message: Location is null.");
            return;
        }

        sensorDataQueue.add(geoJsonConverter.convertToCsvLine(oneWay.get(), lastLocation, closedRoad, narrowRoad, escapeNote));
        flushToDisk();
    }
}
