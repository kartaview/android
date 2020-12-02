package com.telenav.osv.application.initialisation;

import android.content.Context;
import android.location.Location;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.telenav.osv.common.listener.GenericListener;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.frame.model.Frame;
import com.telenav.osv.data.location.datasource.LocationLocalDataSource;
import com.telenav.osv.data.location.model.KVLocation;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionVideo;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.data.video.model.Video;
import com.telenav.osv.item.KVFile;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Class which handles all related functionality related to data consistency.
 * @author horatiuf
 */
public class DataConsistency {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = DataConsistency.class.getSimpleName();

    /**
     * The starting year for the default date.
     */
    private static final int EPOCH_START_YEAR = 1970;

    /**
     * Instance for the current class.
     */
    private static DataConsistency INSTANCE;

    /**
     * The generic listener which will be used to inform about the status of the consistency.
     */
    private CopyOnWriteArrayList<GenericListener> listeners;

    /**
     * The data source for the sequences. Used in order to update specific fields for data consistency.
     */
    private SequenceLocalDataSource sequenceLocalDataSource;

    /**
     * The location source in order to calculate the distance if required.
     */
    private LocationLocalDataSource locationLocalDataSource;

    /**
     * Instance representing the video data source. This will be used to check the path for each video from persistence to exist physically on device.
     */
    private VideoLocalDataSource videoLocalDataSource;

    /**
     * Instance representing the frame data source. This will be used to check the path for each frame from persistence to exist physically on device.
     */
    private FrameLocalDataSource frameLocalDataSource;

    /**
     * Reference to application context required for external storage path check.
     */
    private Context context;

    /**
     * The status for the data consistency mechanism. This is a value from {@link DataConsistencyStatus} interface. By default the {@link DataConsistencyStatus#IDLE} value is
     * initialised.
     */
    @DataConsistencyStatus
    private int status = DataConsistencyStatus.IDLE;

    /**
     * The rx stream representing the data consistency. This is kept in memory for termination/quick stop.
     */
    private Disposable disposable;

    /**
     * The default constructor for the current class.
     * @param sequenceLocalDataSource the data source for the sequences. Used in order to update specific fields for data consistency.
     * @param locationLocalDataSource the location source in order to calculate the distance if required.
     * @param videoLocalDataSource instance representing the video data source. This will be used to check the path for each video from persistence to exist physically on device.
     * @param frameLocalDataSource instance representing the frame data source. This will be used to check the path for each frame from persistence to exist physically on device.
     * @param context the context required for data consistency external folder check.
     */
    private DataConsistency(@NonNull SequenceLocalDataSource sequenceLocalDataSource,
                            @NonNull LocationLocalDataSource locationLocalDataSource,
                            @NonNull VideoLocalDataSource videoLocalDataSource,
                            @NonNull FrameLocalDataSource frameLocalDataSource,
                            @NonNull Context context) {
        this.sequenceLocalDataSource = sequenceLocalDataSource;
        this.locationLocalDataSource = locationLocalDataSource;
        this.frameLocalDataSource = frameLocalDataSource;
        this.videoLocalDataSource = videoLocalDataSource;
        this.context = context;
        listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * @param sequenceLocalDataSource the data source for the sequences. Used in order to update specific fields for data consistency.
     * @param locationLocalDataSource the location source in order to calculate the distance if required.
     * @param videoLocalDataSource instance representing the video data source. This will be used to check the path for each video from persistence to exist physically on device.
     * @param frameLocalDataSource instance representing the frame data source. This will be used to check the path for each frame from persistence to exist physically on device.
     * @param context the context required for data consistency external folder check.
     * @return {@code DataConsistency} representing {@link #INSTANCE}.
     */
    public static DataConsistency getInstance(@NonNull SequenceLocalDataSource sequenceLocalDataSource,
                                              @NonNull LocationLocalDataSource locationLocalDataSource,
                                              @NonNull VideoLocalDataSource videoLocalDataSource,
                                              @NonNull FrameLocalDataSource frameLocalDataSource,
                                              @NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new DataConsistency(
                    sequenceLocalDataSource,
                    locationLocalDataSource,
                    videoLocalDataSource,
                    frameLocalDataSource,
                    context);
        }
        return INSTANCE;
    }

    /**
     * Add the given listener to the collection of listeners. If the listener exists it will not be added again.
     * @param listener the {@code GenericListener} to be added.
     */
    public void addListener(GenericListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given listener from the collection of listeners if it was added before.
     * @param listener the {@code GenericListener} to be removed.
     */
    public void removeListener(GenericListener listener) {
        listeners.remove(listener);
    }

    /**
     * Process each sequence for both file and data consistency.
     * <p> In case there is any file inconsistencies with the sequence it will be removed, for data inconsistency they will be automatically corrected.
     */
    public void start() {
        disposable = sequenceLocalDataSource
                .getSequences()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        //OnSuccess
                        items -> {
                            status = DataConsistencyStatus.PROCESSING;
                            Log.d(TAG, "start. Status: initialised. Message: Starting to start all sequences for consistency.");
                            for (LocalSequence localSequence : items) {
                                String sequenceId = localSequence.getID();
                                Log.d(TAG, String.format("start. Status: start. Message: Starting to start sequence :%s for consistency.", sequenceId));
                                SequenceDetails sequenceDetails = localSequence.getDetails();
                                SequenceDetailsLocal sequenceDetailsLocal = localSequence.getLocalDetails();
                                SequenceDetailsCompressionBase sequenceDetailsCompressionBase = localSequence.getCompressionDetails();
                                boolean sequenceFileConsistency = isSequenceFileConsistent(sequenceDetailsLocal,
                                        sequenceDetails.getOnlineId(),
                                        sequenceDetailsCompressionBase instanceof SequenceDetailsCompressionVideo,
                                        sequenceId);
                                Log.d(TAG, String.format("start. Status: %s. Id: %s. Message: Process sequence file consistency.", sequenceFileConsistency, sequenceId));
                                if (!sequenceFileConsistency) {
                                    removeSequence(localSequence.getLocalDetails().getFolder(), sequenceId);
                                    continue;
                                }

                                sequenceDataConsistency(sequenceDetailsLocal, sequenceDetails, sequenceId);
                                Log.d(TAG, "start. Status: finished. Message: Finish to start sequence consistency.");
                            }
                            notifyListeners(false);
                            Log.d(TAG, "start. Status: end. Message: Processed all sequences for consistency.");
                        },
                        //onError
                        throwable -> {
                            Log.d(TAG, String.format("start. Status: error. Message: %s.", throwable.getLocalizedMessage()));
                            notifyListeners(true);
                        }
                );
    }

    /**
     * @return the current status of the data consistency mechanism. This is a value from {@link DataConsistencyStatus} interface.
     */
    @DataConsistencyStatus
    public int getStatus() {
        return status;
    }

    /**
     * Forces stopping on the data consistency mechanism.
     */
    public void dispose() {
        disposable.dispose();
        INSTANCE = null;
    }

    /**
     * Notifies the listeners on success or on error.
     * @param isError flag which if {@code true} notifies the error method, otherwise the success method.
     */
    private void notifyListeners(boolean isError) {
        status = DataConsistencyStatus.PROCESSED;
        if (!listeners.isEmpty()) {
            for (GenericListener genericListener : listeners) {
                if (isError) {
                    genericListener.onError();
                } else {
                    genericListener.onSuccess();
                }
            }
        }
    }

    /**
     * Checks and fix the data consistency for the sequence. This will represent:
     * <ul>
     * <li>disk size - this will be calculated if 0</li>
     * <li>creation time - if the disk size is not set this means that it is post-migration therefore a date instead of the default one is required</li>
     * <li>address name - if not set there will be given a default value</li>
     * <li>distance - if not set this will be calculated by taking all locations of a sequence and compute the distance between each one of them groups of to, e.g. 1st with 2nd,
     * 2nd with 3rd etc</li>
     * </ul>
     * @param sequenceDetailsLocal the local details of the sequence, used for disk size and creation time check and/or fix.
     * @param sequenceDetails the details of the sequences used for distance and address name check and/or fix.
     * @param sequenceId the sequence identifier
     */
    private void sequenceDataConsistency(@NonNull SequenceDetailsLocal sequenceDetailsLocal, @NonNull SequenceDetails sequenceDetails, @NonNull String sequenceId) {
        Log.d(TAG, "sequenceDataConsistency. Status: initialising. Message: Starting to start data specific sequence consistency");
        KVFile sequenceFolder = sequenceDetailsLocal.getFolder();
        if (sequenceDetailsLocal.getConsistencyStatus() == SequenceDetailsLocal.SequenceConsistencyStatus.EXTERNAL_DATA_MISSING) {
            boolean updateDiskSize = sequenceLocalDataSource.updateDiskSize(sequenceId, 0);
            Log.d(TAG,
                    String.format(
                            "sequenceDataConsistency update disk size. Status: %s. Size: 0. Message: Updating the disk size since the external data is missing.",
                            updateDiskSize));
        } else if (sequenceDetailsLocal.getDiskSize() == 0) {
            Long newDiskSize = Utils.folderSize(sequenceFolder);
            boolean updateDiskSize = sequenceLocalDataSource.updateDiskSize(sequenceId, newDiskSize);
            Log.d(TAG,
                    String.format(
                            "sequenceDataConsistency update disk size. Status: %s. Size: %s. Message: Updating the disk size since it is not set.",
                            updateDiskSize,
                            newDiskSize));
        }
        if (sequenceDetails.getDateTime().getYear() == EPOCH_START_YEAR) {
            long dateTimeLong = sequenceFolder.lastModified();
            boolean updateDateTime = sequenceLocalDataSource.updateDateTime(sequenceId, new DateTime(dateTimeLong));
            Log.d(TAG,
                    String.format(
                            "sequenceDataConsistency update distance. Status: %s. Distance: %s. Message: Updating the datetime since it is set as default.",
                            updateDateTime,
                            dateTimeLong));
        }
        if (sequenceDetails.getDistance() == 0) {
            updateDistanceForSequenceId(sequenceDetails, sequenceId);
        }

        int videoNo = videoLocalDataSource.getVideoCountBySequenceId(sequenceId);
        int frameNo = locationLocalDataSource.getLocationsCountBySequenceId(sequenceId);
        boolean updateCompressionInfo = sequenceLocalDataSource.updateCompressionSizeInfo(sequenceId, frameNo, videoNo);
        Log.d(TAG, String.format(
                "sequenceDataConsistency update compression info. Status: %s. Message: Updating compression size of the sequence. Video no: %s. Frame no: %s.",
                updateCompressionInfo,
                videoNo,
                frameNo));
        Log.d(TAG, "isSequenceFileConsistent. Status: finishing. Message: Finish to start file specific sequence consistency");
    }

    /**
     * Updates the distance of the sequence. This happens internally by getting all the locations for the sequence and computing the distance between them.
     * <p> This will only happen at a migration since there is no other way to get the old distance to the new sequence since it was only cached and not persisted.
     */
    private void updateDistanceForSequenceId(SequenceDetails sequenceDetails, String sequenceId) {
        List<KVLocation> sequenceLocations = locationLocalDataSource.getLocationsBySequenceId(sequenceId).blockingGet();
        if (sequenceLocations != null) {
            double distance = 0;
            for (int i = 0; i < sequenceLocations.size() - 1; i++) {
                Location currentLocation = sequenceLocations.get(i).getLocation();
                Location nextLocation = sequenceLocations.get(i + 1).getLocation();
                distance += ComputingDistance.distanceBetween(
                        currentLocation.getLongitude(),
                        currentLocation.getLatitude(),
                        nextLocation.getLongitude(),
                        nextLocation.getLatitude());
            }
            if (distance != 0) {
                boolean updateDistance = sequenceLocalDataSource.updateDistance(sequenceId, distance);
                Log.d(TAG,
                        String.format(
                                "sequenceDataConsistency update distance. Status: %s. Distance: %s. Message: Updating the distance since it is not set.",
                                updateDistance,
                                distance));
                sequenceDetails.setDistance(distance);
            } else {
                Log.d(TAG, "sequenceDataConsistency update distance: Status: abort. Message: Not enough locations in order to compute distance.");
            }
        }
    }

    /**
     * Checks if the sequences:
     * <ul>
     * <li>has a folder and children exist</li>
     * <li>the metadata status:
     * <ul>
     * <li>online id set then metadata file should not exist</li>
     * <li>online id not set then metadata file should exist</li>
     * </ul>
     * </li>
     * <li>compression sizes - the number of videos/folder from persistence should be equal with the file numbers with their respective extensions</li>
     * </ul>
     * @param sequenceDetailsLocal the local sequence details used in order to update the consistency status and do the file consistency for the sequence folder.
     * @param onlineId the online identifier which represent the sequence on the server. This will be used in the file check.
     * @param isVideoCompression flag to see what kind of compression check will be performed, either video or frame.
     * @param sequenceId the {@code Sequence} identifier.
     * @return {@code false} if the sequence is invalid and should be removed, {@code true} otherwise if it was performed successful.
     */
    private boolean isSequenceFileConsistent(@NonNull SequenceDetailsLocal sequenceDetailsLocal,
                                             long onlineId,
                                             boolean isVideoCompression,
                                             @NonNull String sequenceId) {
        KVFile sequenceFolder = sequenceDetailsLocal.getFolder();
        Log.d(TAG, "isSequenceFileConsistent. Status: initialising. Message: Starting to start file specific sequence consistency");
        boolean sequenceFileConsistency = false;
        if (sequenceFolder.exists()) {
            sequenceFileConsistency = processSequenceFileConsistency(onlineId, isVideoCompression, sequenceId, sequenceDetailsLocal);
        } else if (Utils.getExternalStoragePath(context) == null && !sequenceFolder.getPath().contains(Utils.getInternalStoragePath(context))) {
            Log.d(TAG, "isSequenceFileConsistent. Status: finishing. Message: Finish to start file specific sequence consistency");
            int missingExternal = SequenceDetailsLocal.SequenceConsistencyStatus.EXTERNAL_DATA_MISSING;
            boolean updateConsistencyStatus = sequenceLocalDataSource.updateConsistencyStatus(sequenceId, missingExternal);
            Log.d(TAG, String.format(
                    "isSequenceFileConsistent. Status: external data missing. Message: Sd card was removed, updating consistency status flag. Status: %s",
                    updateConsistencyStatus));
            sequenceDetailsLocal.setConsistencyStatus(missingExternal);
            sequenceFileConsistency = true;
        }

        Log.d(TAG, String.format("isSequenceFileConsistent. Status: %s. Message: Finish to start file specific sequence consistency.", sequenceFileConsistency));
        return sequenceFileConsistency;
    }

    private boolean processSequenceFileConsistency(long onlineId,
                                                   boolean isVideoCompression,
                                                   @NonNull String sequenceId,
                                                   @NonNull SequenceDetailsLocal sequenceDetailsLocal) {
        Log.d(TAG, "processSequenceFileConsistency. Status: file size check. Message: Starting to check the sequence folder file size and online id set");
        boolean isOnlineNotSetSet = onlineId == SequenceDetails.ONLINE_ID_NOT_SET;
        KVFile sequenceFolder = sequenceDetailsLocal.getFolder();
        if (isOnlineNotSetSet && Utils.folderSize(sequenceFolder) == 0) {
            Log.d(TAG, "processSequenceFileConsistency. Status: invalid. Message: Empty sequence folder.");
            return false;
        }
        Log.d(TAG, "processSequenceFileConsistency. Status: metadata check. Message: Starting to check the metadata existence");
        File[] metadata = Utils.findFilesByExtension(sequenceFolder, new ArrayList<String>() {
            {
                add(SequenceDetailsCompressionBase.SequenceFilesExtensions.METADATA_DEFAULT);
            }

            {
                add(SequenceDetailsCompressionBase.SequenceFilesExtensions.METADATA_TXT);
            }
        });
        boolean metadataExists = metadata.length != 0;
        if (isOnlineNotSetSet) {
            if (!metadataExists) {
                int metadataMissingStatus = SequenceDetailsLocal.SequenceConsistencyStatus.METADATA_MISSING;
                boolean updateConsistencyStatus = sequenceLocalDataSource.updateConsistencyStatus(sequenceId, metadataMissingStatus);
                Log.d(TAG, String.format(
                        "processSequenceFileConsistency. Status: %s. Message: Online id not set and metadata file not found. Updating consistency status flag.",
                        updateConsistencyStatus));
                sequenceDetailsLocal.setConsistencyStatus(metadataMissingStatus);
            }
        } else if (metadataExists) {
            for (File file : metadata) {
                boolean deletingMetadataFiles = file.delete();
                Log.d(TAG, String.format(
                        "processSequenceFileConsistency. Status: metadata remove. Status: %s. Name: %s. Message: Online id set and metadata file not found. Removing metadata.",
                        deletingMetadataFiles,
                        file.getName()));
            }
        }
        Log.d(TAG, "processSequenceFileConsistency. Status: compression check. Message: Starting to check the compression size");
        if (isVideoCompression) {
            videoSequenceFileCompressionCheck(sequenceId, sequenceDetailsLocal);
        } else {
            frameSequenceCompressionCheck(sequenceId, sequenceDetailsLocal);
        }
        int validStatus = SequenceDetailsLocal.SequenceConsistencyStatus.VALID;
        boolean updateConsistencyStatus = sequenceLocalDataSource.updateConsistencyStatus(sequenceId, validStatus);
        Log.d(TAG, String.format("processSequenceFileConsistency. Status: %s. Message: Updating consistency status flag to valid.", updateConsistencyStatus));
        sequenceDetailsLocal.setConsistencyStatus(validStatus);
        return true;
    }

    /**
     * File consistency regarding compression, this will check based on compression if every video/photo is present.
     */
    private void videoSequenceFileCompressionCheck(@NonNull String sequenceId, @NonNull SequenceDetailsLocal sequenceDetailsLocal) {
        Log.d(TAG, "isSequenceFileConsistent. Status: video compression check. Message: Starting to check the video compression checks.");
        List<Video> videos = videoLocalDataSource.getVideos(sequenceId).blockingGet();
        if (videos != null && !videos.isEmpty()) {
            for (Video video : videos) {
                if (isFileAbsent(video.getPath())) {
                    int dataMissing = SequenceDetailsLocal.SequenceConsistencyStatus.DATA_MISSING;
                    boolean updateConsistencyStatus = sequenceLocalDataSource.updateConsistencyStatus(sequenceId, dataMissing);
                    Log.d(TAG, String.format("videoSequenceFileCompressionCheck. Status: %s. Message: Missing video, updating consistency status flag.", updateConsistencyStatus));
                    sequenceDetailsLocal.setConsistencyStatus(dataMissing);
                    return;
                }
            }
        }
        Log.d(TAG, "isSequenceFileConsistent. Status: video check complete. Message: Finished video compression check successfully.");
    }

    /**
     * File consistency regarding compression, this will check based on compression if every video/photo is present.
     */
    private void frameSequenceCompressionCheck(@NonNull String sequenceId, @NonNull SequenceDetailsLocal sequenceDetailsLocal) {
        Log.d(TAG, "isSequenceFileConsistent. Status: frame compression check. Message: Starting to check the frame compression checks.");
        List<Frame> frames = frameLocalDataSource.getFrames(sequenceId).blockingGet();
        if (frames != null && !frames.isEmpty()) {
            for (Frame frame : frames) {
                if (isFileAbsent(frame.getFilePath())) {
                    int dataMissing = SequenceDetailsLocal.SequenceConsistencyStatus.DATA_MISSING;
                    boolean updateConsistencyStatus = sequenceLocalDataSource.updateConsistencyStatus(sequenceId, dataMissing);
                    Log.d(TAG, String.format(
                            "frameSequenceCompressionCheck. Status: %s. Message: Missing frame, updating consistency status flag.",
                            updateConsistencyStatus));
                    sequenceDetailsLocal.setConsistencyStatus(dataMissing);
                    return;
                }
            }
        }
        Log.d(TAG, "isSequenceFileConsistent. Status: frame check complete. Message: Finished frame compression check successfully.");
    }

    /**
     * @param path the path to the physical file.
     * @return {@code true} if the file exists, {@code false} otherwise.
     */
    private boolean isFileAbsent(String path) {
        return !new KVFile(path).exists();
    }

    /**
     * Removes the sequence folder and all its children by internally calling {@link KVFile#delete()} method.
     */
    private void removeSequence(KVFile file, String sequenceId) {
        boolean removeSequence = file.delete() && sequenceLocalDataSource.deleteSequence(sequenceId);
        Log.d(TAG, String.format("removeSequence. Status: %s. Message: Removal of both database and physical folder.", removeSequence));
    }

    /**
     * Interface that shows the data consistency status. This can be:
     * <ul>
     * <li>{@link #IDLE}</li>
     * <li>{@link #PROCESSED}</li>
     * <li>{@link #PROCESSING}</li>
     * </ul>
     */
    @IntDef
    public @interface DataConsistencyStatus {

        /**
         * The value for when the data consistency is idle. This will be used as a default value.
         */
        int IDLE = 0;

        /**
         * The value for when the data consistency is in processing stage meaning is performing a data consistency.
         */
        int PROCESSING = 1;

        /**
         * The value for when the data consistency had been already processed.
         */
        int PROCESSED = 2;
    }
}
