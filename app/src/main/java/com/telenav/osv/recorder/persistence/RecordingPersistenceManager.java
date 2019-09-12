package com.telenav.osv.recorder.persistence;

import java.util.ArrayList;
import java.util.UUID;
import android.location.Location;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.frame.model.Frame;
import com.telenav.osv.data.location.datasource.LocationLocalDataSource;
import com.telenav.osv.data.location.model.OSVLocation;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionVideo;
import com.telenav.osv.item.metadata.VideoData;
import com.telenav.osv.recorder.sensor.SensorManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import androidx.annotation.Nullable;
import io.fabric.sdk.android.Fabric;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

/**
 * Implementation of the {@link RecordingPersistence} interface that holds all the available functionality of the local recording persistence.
 * Created by cameliao on 2/6/18.
 */
public abstract class RecordingPersistenceManager implements RecordingPersistence {

    private static final String TAG = RecordingPersistenceManager.class.getSimpleName();

    /**
     * Synchronization object used for incrementing the frame index.
     */
    private final Object syncObject = new Object();

    /**
     * The current sequence that will hold all the saved frames.
     * This local cache will be continuously updated when a new frame is saved.
     */
    protected LocalSequence sequence;

    /**
     * The current frame index for the {@link #sequence}.
     */
    protected int frameIndex = 0;

    /**
     * The path to the folder where the frame or video should be saved.
     */
    protected String folderPath;

    /**
     * The frame size which is used also for the encoder output size.
     */
    protected Size formatSize;

    /**
     * The frame format before converting them into the encoding input format.
     */
    protected int imageFormat;

    /**
     * Scheduler for persisting the frames. This should be used for all the calls of {@link RecordingPersistence}.
     */
    protected Scheduler recordingPersistenceScheduler = Schedulers.single();

    /**
     * Local data source for the location storage.
     * This is used to store the location of a new frame.
     */
    private LocationLocalDataSource locationLocalDataSource;

    /**
     * Local data source for the sequence storage.
     * This is used to update the sequence when a new frame is received.
     */
    private SequenceLocalDataSource sequenceLocalDataSource;

    /**
     * Protected constructor for the current class to hide the initialisation from external sources.
     */
    protected RecordingPersistenceManager(SequenceLocalDataSource sequenceLocalDataSource, LocationLocalDataSource locationLocalDataSource) {
        this.sequenceLocalDataSource = sequenceLocalDataSource;
        this.locationLocalDataSource = locationLocalDataSource;
    }

    @Override
    public Completable start(LocalSequence sequence, Size formatSize, int imageFormat) {
        return Completable.create(emitter -> {
            Log.d(TAG, "start()");
            RecordingPersistenceManager.this.formatSize = formatSize;
            RecordingPersistenceManager.this.sequence = sequence;
            RecordingPersistenceManager.this.imageFormat = imageFormat;
            RecordingPersistenceManager.this.folderPath = sequence.getLocalDetails().getFolder().getPath();
            emitter.onComplete();
        }).subscribeOn(recordingPersistenceScheduler);

    }

    @Override
    public Completable stop() {
        return Completable.create(emitter -> {
            Log.d(TAG, "stop()");
            frameIndex = 0;
            emitter.onComplete();
        }).subscribeOn(recordingPersistenceScheduler);
    }

    /**
     * Removes the frame or video from the local data source.
     * @param id the id of the item which should be removed.
     */
    protected abstract void remove(String id);

    /**
     * Persist the frame in the {@code FrameLocalDataSource}. Uses internally {@link FrameLocalDataSource#saveFrame(Frame, String)} method.
     * @return {@code true} if the persist was successful, {@code false} otherwise.
     */
    protected boolean persistLocation(Location location, @Nullable String frameID, @Nullable String videoID, long diskSize, int index, long timestamp) {
        String locationID = UUID.randomUUID().toString();
        String sequenceID = sequence.getID();
        boolean locationPersist = locationLocalDataSource.saveLocation(
                new OSVLocation(locationID,
                        location,
                        sequenceID),
                videoID,
                frameID
        );
        if (locationPersist) {
            //update the disk size for the current sequence, if any error occurs trigger a location persistence failed
            if (!updateSequenceDiskSize(diskSize)) {
                //removing current location since the details of the sequence cannot be updated.
                boolean locationRemoval = locationLocalDataSource.delete(locationID);
                Log.d(TAG, String.format("persistLocation. Sequence details failed. Status: %s. Location id: %s. Sequence id: %s", locationRemoval, locationID, sequence.getID()));
                handleLocationPersistFailed();
                return false;
            }
            SensorManager.logVideoData(new VideoData(frameIndex, index, timestamp));
            //increment the index for the frame sequence identifier
            synchronized (syncObject) {
                frameIndex++;
            }
        } else {
            handleLocationPersistFailed();
        }
        return locationPersist;
    }

    /**
     * Updates the disk size in persistence for the sequence.
     * @param diskSize the new disk size to be persisted.
     * @return {@code true} if the persistence was successful, {@code false} otherwise.
     */
    protected boolean updateSequenceDiskSize(long diskSize) {
        if (sequence == null) {
            Log.d(TAG, "updateSequenceDiskSize. Status: error. Message: Sequence is null.");
            return false;
        }
        SequenceDetailsLocal sequenceDetailsLocal = sequence.getLocalDetails();
        long sequenceDiskSize = sequenceDetailsLocal.getDiskSize();
        long newSequenceDiskSize = sequenceDiskSize + diskSize;
        sequenceDetailsLocal.setDiskSize(newSequenceDiskSize);
        Log.w(TAG,
                String.format("updateSequenceDiskSize. Status: persist location and update sequence disk size. Old size: %s. New size: %s.",
                        sequenceDiskSize,
                        newSequenceDiskSize));
        return sequenceLocalDataSource.updateDiskSize(sequence.getID(), newSequenceDiskSize);
    }

    /**
     * Updates the sequence cache and the local data source information.
     * @param sequenceDetails the details of the sequence.
     * @param location the frame location.
     * @param distance the distance between the previous and the current frame.
     * @param videoCompression the compression mode for the current sequence.
     */
    protected void updateSequenceCache(SequenceDetails sequenceDetails, Location location, double distance, boolean videoCompression) {
        //update the initial location for the sequence based on the frame index
        if (frameIndex == 1) {
            Location sequenceLocation = sequenceDetails.getInitialLocation();
            sequenceLocation.setLatitude(location.getLatitude());
            sequenceLocation.setLongitude(location.getLongitude());
            Log.d(TAG, "updateSequenceCache. Message: Update initial location for first frame. ");
        }
        // add the coordinates to the polyline in the compression base
        SequenceDetailsCompressionBase compressionBase = sequence.getCompressionDetails();
        if (compressionBase.getCoordinates() == null) {
            compressionBase.setCoordinates(new ArrayList<>());
        }
        compressionBase.getCoordinates().add(location);
        Log.d(TAG, "updateSequenceCache. Message: Add location to compression coordinates.");
        //update distance for the current cache
        sequenceDetails.setDistance(sequenceDetails.getDistance() + distance);
        if (videoCompression) {
            SequenceDetailsCompressionVideo compressionVideo = (SequenceDetailsCompressionVideo) compressionBase;
            compressionBase.setLength(compressionVideo.getVideos().size());
        }
        compressionBase.setLocationsCount(frameIndex);
        Log.d(TAG, String.format("updateSequenceCache. Message: Set location count to %s.", frameIndex));
        boolean updateInPersistence = sequence != null && sequenceLocalDataSource.updateSequence(sequence);
        Log.d(TAG, String.format("updateSequenceCache. Status: %s. Message: Update the sequence in persistence. Sequence id: %s", updateInPersistence, sequence.getID()));
    }

    /**
     * Handle location persistence failed by logging the event on Fabric.
     */
    private void handleLocationPersistFailed() {
        Log.d(TAG, "handleLocationPersistFailed. Status: error. Message: Location persist failed.");
        if (Fabric.isInitialized()) {
            Answers.getInstance().logCustom(new CustomEvent("Frame persist failed."));
        }
    }
}
