package com.telenav.osv.recorder.persistence.frame;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.joda.time.DateTime;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.frame.model.Frame;
import com.telenav.osv.data.location.datasource.LocationLocalDataSource;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.recorder.persistence.RecordingFrame;
import com.telenav.osv.recorder.persistence.RecordingPersistenceManager;
import com.telenav.osv.recorder.persistence.RecordingPersistenceStatus;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import io.reactivex.Completable;

/**
 * Manager which is responsible to persist each picture frame in database and on local storage.
 * The manager receives an array of byte representing the frame information which are converted to JPEG format.
 */
public class FramePersistenceManager extends RecordingPersistenceManager {

    private static final String TAG = FramePersistenceManager.class.getSimpleName();

    /**
     * {@code String} format for the frame path, where the first string is the folder path
     * followed by the current frame index.
     */
    private static final String FORMAT_FRAME_PATH = "%s/%s.jpg";

    /**
     * Local data source for frame database persistence.
     */
    private FrameLocalDataSource frameLocalDataSource;

    /**
     * Constructor for frame persistence manager.
     * @param sequenceLocalDataSource local data source for sequence.
     * @param locationLocalDataSource local data source for location.
     * @param frameLocalDataSource local data source for frame.
     */
    public FramePersistenceManager(SequenceLocalDataSource sequenceLocalDataSource, LocationLocalDataSource locationLocalDataSource,
                                   FrameLocalDataSource frameLocalDataSource) {
        super(sequenceLocalDataSource, locationLocalDataSource);
        this.frameLocalDataSource = frameLocalDataSource;
    }

    @Override
    public Completable save(RecordingFrame frame) {
        return Completable.create(emitter -> {
            Log.d(TAG, "save()");
            int currentFrameIndex = frameIndex;
            String framePath = String.format(FORMAT_FRAME_PATH, folderPath, currentFrameIndex);
            String frameID = UUID.randomUUID().toString();
            //persist frame to local storage
            boolean persistFrame = frameLocalDataSource.saveFrame(
                    new Frame(frameID, framePath, new DateTime(frame.getTimestamp()), currentFrameIndex),
                    sequence.getID());
            if (!persistFrame) {
                Log.d(TAG, String.format("save. Status: error. Message: Failed to persist frame index %s", currentFrameIndex));
                emitter.onError(new Throwable());
                return;
            }
            //write frame on disk
            if (!writeJpegFile(framePath, frame.getFrameData().getFrameData())) {
                remove(frameID);
                emitter.onError(new Throwable());
                return;
            }
            //persist the location of the frame
            boolean persistLocation = persistLocation(
                    frame.getLocation(),
                    frameID,
                    null,
                    Utils.fileSize(new OSVFile(framePath)),
                    currentFrameIndex,
                    frame.getTimestamp());
            //if the location could not be persisted removes the frame
            Log.d(TAG, String.format("save. Status: %s. Sequence id: %s. Frame id: %s. Frame index: %s. Video compression: %s.",
                    persistLocation, sequence.getID(), frameID, currentFrameIndex, false));
            if (!persistLocation) {
                remove(frameID);
                emitter.onError(new Throwable(RecordingPersistenceStatus.STATUS_ERROR_LOCATION_PERSISTENCE));
                return;
            } else {
                updateSequenceCache(sequence.getDetails(), frame.getLocation(), frame.getDistance(), false);
            }
            emitter.onComplete();
        }).subscribeOn(recordingPersistenceScheduler);
    }

    @Override
    public Completable stop() {
        return super.stop().doOnComplete(() -> {
            folderPath = null;
            sequence = null;
        });
    }

    @Override
    protected void remove(String id) {
        boolean removeFrame = frameLocalDataSource.deleteFrame(id);
        Log.d(TAG, String.format("remove. Status: %s. Sequence id: %s. Frame id: %s", removeFrame, sequence.getID(), id));
    }

    /**
     * Writes on disk the frame data as a JPEG file.
     * @param framePath the frame path where the jpeg file will be stored.
     * @param frameData the byte array containing the jpeg information.
     * @return {@code true} if the file was created, {@code false} otherwise.
     */
    private boolean writeJpegFile(String framePath, byte[] frameData) {
        FileOutputStream out;
        try {
            out = new FileOutputStream(framePath);
            out.write(frameData);
            out.close();
            Log.d(TAG, String.format("createJpegFile. Status: success. Message: Wrote jpeg with index %s on disk. Path: %s.",
                    frameIndex, framePath));
        } catch (IOException e) {
            Log.e(TAG, String.format("createJpegFile. Status: error. Message: Failed to write jpeg with error: %s", e.getMessage()));
            return false;
        }
        return true;
    }
}
