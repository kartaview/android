package com.telenav.osv.data.sequence.datasource.local;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import android.content.Context;
import com.telenav.osv.application.initialisation.DataConsistency;
import com.telenav.osv.common.Injection;
import com.telenav.osv.data.database.DataConverter;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.location.datasource.LocationLocalDataSource;
import com.telenav.osv.data.location.model.OSVLocation;
import com.telenav.osv.data.score.datasource.ScoreDataSource;
import com.telenav.osv.data.sequence.database.dao.SequenceDao;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.utils.Log;
import androidx.annotation.NonNull;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * @author horatiuf
 */
public class SequenceLocalDataSourceImpl implements SequenceLocalDataSource {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = SequenceLocalDataSourceImpl.class.getSimpleName();

    /**
     * The index representing the sequence id in the name of the folder of a sequence.
     */
    private static final int INDEX_SEQUENCE_ID = 1;

    /**
     * The pattern used for the physical path of the frames for the a sequence.
     */
    private static final String SEQUENCE_FRAME_FILE_PATH_PATTERN = "file:///%s";

    /**
     * The pattern used for sequence address. This is a placeholder mostly.
     */
    private static final String SEQUENCE_ADDRESS_PATTERN = "Track %s";

    /**
     * Instance for the current class.
     */
    private static SequenceLocalDataSourceImpl INSTANCE;

    /**
     * Instance to the local data source for frames. This is used in order to obtain the frames associated with the sequence.
     * @see FrameLocalDataSource
     */
    private FrameLocalDataSource frameLocalDataSource;

    /**
     * Instance to the local data source for score. Used in order to calculate the score for the sequence.
     * @see ScoreDataSource
     */
    private ScoreDataSource scoreLocalDataSource;

    /**
     * Instance to the local data source for video. Used in order to obtain the video associated with the sequence.
     * @see ScoreDataSource
     */
    private VideoLocalDataSource videoLocalDataSource;

    /**
     * Instance to the local data source for location. Used in order to obtain the location associated with the sequence.
     * @see LocationLocalDataSource
     */
    private LocationLocalDataSource locationLocalDataSource;

    /**
     * The database DAO for {@code Sequence} data.
     */
    private SequenceDao sequenceDao;

    /**
     * Default constructor for the current class. Made private to prevent intention outside the current class scope.
     */
    private SequenceLocalDataSourceImpl(@NonNull Context context,
                                        @NonNull FrameLocalDataSource frameLocalDataSource,
                                        @NonNull ScoreDataSource scoreLocalDataSource,
                                        @NonNull LocationLocalDataSource locationLocalDataSource,
                                        @NonNull VideoLocalDataSource videoLocalDataSource) {
        this.frameLocalDataSource = frameLocalDataSource;
        this.scoreLocalDataSource = scoreLocalDataSource;
        this.videoLocalDataSource = videoLocalDataSource;
        this.locationLocalDataSource = locationLocalDataSource;

        //init the sequence persistence.
        sequenceDao = Injection.provideOSCDatabase(context).sequenceDao();
    }


    /**
     * @return {@code VideoLocalDataSource} representing {@link #INSTANCE}.
     */
    public static SequenceLocalDataSource getInstance(@NonNull Context context,
                                                      @NonNull FrameLocalDataSource frameLocalDataSource,
                                                      @NonNull ScoreDataSource scoreLocalDataSource,
                                                      @NonNull LocationLocalDataSource locationLocalDataSource,
                                                      @NonNull VideoLocalDataSource videoLocalDataSource) {
        if (INSTANCE == null) {
            INSTANCE = new SequenceLocalDataSourceImpl(context,
                    frameLocalDataSource,
                    scoreLocalDataSource,
                    locationLocalDataSource,
                    videoLocalDataSource);
        }
        return INSTANCE;
    }

    @Override
    public Single<List<LocalSequence>> getSequences() {
        return sequenceDao
                .findAll()
                .doOnSuccess(items -> Log.d(TAG, "getSequences. Status: success. Message: Sequences found."))
                .doOnError(throwable -> Log.d(TAG, String.format("getSequences. Status: error. Message: %s.", throwable.getLocalizedMessage())))
                .flatMap(entities -> Observable
                        .fromIterable(entities)
                        .map(DataConverter::toLocalSequence)
                        .toList());
    }

    @Override
    public Single<List<LocalSequence>> getSequences(boolean include, @DataConsistency.DataConsistencyStatus Integer... sequenceConsistencyStatus) {
        return sequenceDao
                .findAll(getSequenceConsistencyStatuses(include, sequenceConsistencyStatus))
                .doOnSuccess(items -> Log.d(TAG, "getSequences. Status: success. Message: Sequences found."))
                .doOnError(throwable -> Log.d(TAG, String.format("getSequences. Status: error. Message: %s.", throwable.getLocalizedMessage())))
                .flatMap(entities -> Observable
                        .fromIterable(entities)
                        .map(DataConverter::toLocalSequence)
                        .toList());
    }

    @Override
    public Single<List<String>> getSequencesIds() {
        return sequenceDao
                .findAllIds()
                .doOnSuccess(items -> Log.d(TAG, "getSequencesIds. Status: success. Message: Sequences ids found."))
                .doOnError(throwable -> Log.d(TAG, String.format("getSequencesIds. Status: error. Message: %s.", throwable.getLocalizedMessage())));
    }

    @Override
    public Single<List<LocalSequence>> getSequencesWithReward(boolean include, @DataConsistency.DataConsistencyStatus Integer... sequenceConsistencyStatus) {
        return sequenceDao
                .findAllWithRewards(getSequenceConsistencyStatuses(include, sequenceConsistencyStatus))
                .doOnSuccess(items -> Log.d(TAG, "getSequencesWithReward. Status: success. Message: Sequences found."))
                .doOnError(throwable -> Log.d(TAG, String.format("getSequencesWithReward. Status: error. Message: %s.", throwable.getLocalizedMessage())))
                .flatMap(entities -> Observable
                        .fromIterable(entities)
                        .map(item -> DataConverter.toLocalSequence(
                                scoreLocalDataSource,
                                item))
                        .toList());
    }

    @Override
    public Single<List<LocalSequence>> getSequencesWithReward() {
        return sequenceDao
                .findAllWithRewards()
                .doOnSuccess(items -> Log.d(TAG, "getSequencesWithReward. Status: success. Message: Sequences found."))
                .doOnError(throwable -> Log.d(TAG, String.format("getSequencesWithReward. Status: error. Message: %s.", throwable.getLocalizedMessage())))
                .flatMap(entities -> Observable
                        .fromIterable(entities)
                        .map(item -> DataConverter.toLocalSequence(
                                scoreLocalDataSource,
                                item))
                        .toList());
    }

    @Override
    public boolean persistSequence(@NonNull LocalSequence sequence) {
        boolean sequencePersist = sequenceDao.insert(DataConverter.toSequenceEntity(sequence)) != 0;
        Log.d(TAG, String.format("persistSequence. Status: %s. Sequence id: %s.", sequencePersist, sequence.getID()));
        return sequencePersist;
    }

    @Override
    public boolean isPopulated() {
        boolean isPopulated = sequenceDao.countAll() != 0;
        Log.d(TAG, String.format("isPopulated. Status: %s. ", isPopulated));
        return isPopulated;
    }

    @Override
    public boolean updateSequence(@NonNull LocalSequence sequence) {
        boolean sequenceUpdated = sequenceDao.update(DataConverter.toSequenceEntity(sequence)) != 0;
        Log.d(TAG, String.format("updateSequence. Status: %s. Sequence id: %s.", sequenceUpdated, sequence.getID()));
        return sequenceUpdated;
    }

    @Override
    public Maybe<LocalSequence> getSequence(@NotNull String sequenceId) {
        return sequenceDao
                .findByID(sequenceId)
                .doOnSuccess(items -> Log.d(TAG, String.format("getSequence. Status: success. Sequence id: %s. Message: Sequence found.", sequenceId)))
                .doOnComplete(() -> Log.d(TAG, String.format("getSequence. Status: complete. Sequence id: %s. Message: Sequence not found.", sequenceId)))
                .doOnError(throwable -> Log.d(TAG,
                        String.format("getSequence. Status: error. Sequence id: %s.  Message: %s.",
                                sequenceId,
                                throwable.getLocalizedMessage())))
                .map(DataConverter::toLocalSequence);
    }

    @Override
    public Maybe<LocalSequence> getSequenceWithAll(@NotNull String sequenceId) {
        return sequenceDao
                .findByIDWithReward(sequenceId)
                .doOnSuccess(items -> Log.d(TAG, String.format("getSequenceWithReward. Status: success. Sequence id: %s. Message: Sequence found.", sequenceId)))
                .doOnComplete(() -> Log.d(TAG, String.format("getSequenceWithReward. Status: complete. Sequence id: %s. Message: Sequence not found.", sequenceId)))
                .doOnError(throwable -> Log.d(TAG,
                        String.format("getSequenceWithReward. Status: error. Sequence id: %s.  Message: %s.",
                                sequenceId,
                                throwable.getLocalizedMessage())))
                .map(item -> {
                    List<OSVLocation> osvLocations = locationLocalDataSource.getLocationsBySequenceId(sequenceId).blockingGet();
                    if (osvLocations != null) {
                        int locationsCount = osvLocations.size();
                        Log.d(TAG, String.format(
                                "getSequenceWithReward. Status: update location data. Message: %s locations for the sequence found in the persistence.",
                                locationsCount));
                        LocalSequence localSequence = DataConverter.toLocalSequence(scoreLocalDataSource, item);
                        localSequence.getCompressionDetails().setCoordinates(DataConverter.toLocations(osvLocations));
                        return localSequence;
                    }
                    return DataConverter.toLocalSequence(scoreLocalDataSource, item);
                });
    }

    @Override
    public Maybe<LocalSequence> getSequenceWithReward(@NotNull String sequenceId) {
        return sequenceDao
                .findByIDWithReward(sequenceId)
                .doOnSuccess(items -> Log.d(TAG, String.format("getSequenceWithReward. Status: success. Sequence id: %s. Message: Sequence found.", sequenceId)))
                .doOnComplete(() -> Log.d(TAG, String.format("getSequenceWithReward. Status: complete. Sequence id: %s. Message: Sequence not found.", sequenceId)))
                .doOnError(throwable -> Log.d(TAG,
                        String.format("getSequenceWithReward. Status: error. Sequence id: %s.  Message: %s.",
                                sequenceId,
                                throwable.getLocalizedMessage())))
                .map(item -> DataConverter.toLocalSequence(
                        scoreLocalDataSource,
                        item));
    }

    @Override
    public boolean deleteSequence(@NotNull String sequenceId) {
        boolean deleteResult = sequenceDao.deleteById(sequenceId) != 0;
        Log.d(TAG, String.format("deleteSequence. Status: %s. Sequence id: %s.", deleteResult, sequenceId));
        return deleteResult;
    }

    @Override
    public boolean updateObd(@NotNull String sequenceId, boolean obd) {
        boolean updateObd = sequenceDao.updateObd(sequenceId, obd) != 0;
        Log.d(TAG, String.format("updateObd. Status: %s. Sequence id: %s.", updateObd, sequenceId));
        return updateObd;
    }

    @Override
    public boolean updateOnlineId(@NotNull String sequenceId, long onlineID) {
        boolean updateOnlineId = sequenceDao.updateOnlineId(sequenceId, onlineID) != 0;
        Log.d(TAG, String.format("updateOnlineId. Status: %s. Sequence id: %s.", updateOnlineId, sequenceId));
        return updateOnlineId;
    }

    @Override
    public boolean updateDiskSize(@NotNull String sequenceId, long diskSize) {
        boolean updateDiskSize = sequenceDao.updateDiskSize(sequenceId, diskSize) != 0;
        Log.d(TAG, String.format("updateDiskSize. Status: %s. Sequence id: %s.", updateDiskSize, sequenceId));
        return updateDiskSize;
    }

    @Override
    public boolean updateDistance(@NotNull String sequenceId, double distance) {
        boolean updateDistance = sequenceDao.updateDistance(sequenceId, distance) != 0;
        Log.d(TAG, String.format("updateDistance. Status: %s. Sequence id: %s.", updateDistance, sequenceId));
        return updateDistance;
    }

    @Override
    public boolean updateDateTime(@NonNull String sequenceId, DateTime creationDate) {
        boolean updateDateTime = sequenceDao.updateDateTime(sequenceId, creationDate) != 0;
        Log.d(TAG, String.format("updateDateTime. Status: %s. Sequence id: %s.", updateDateTime, sequenceId));
        return updateDateTime;
    }

    @Override
    public boolean updateSequenceSizeInfo(@NotNull String sequenceId, long diskSize, int frameCount, int videoCount) {
        boolean updateSequenceSizeInfo = sequenceDao.updateSizeInfo(sequenceId, diskSize, frameCount, videoCount) != 0;
        Log.d(TAG, String.format("updateSequenceSizeInfo. Status: %s. Sequence id: %s.", updateSequenceSizeInfo, sequenceId));
        return updateSequenceSizeInfo;
    }

    @Override
    public boolean updateCompressionSizeInfo(@NonNull String sequenceId, int frameCount, int videoCount) {
        boolean updateSequenceCompressionSizeInfo = sequenceDao.updateCompressionNumbers(sequenceId, frameCount, videoCount) != 0;
        Log.d(TAG, String.format("updateCompressionSizeInfo. Status: %s. Sequence id: %s.", updateSequenceCompressionSizeInfo, sequenceId));
        return updateSequenceCompressionSizeInfo;
    }

    @Override
    public boolean updateAddressName(@NonNull String sequenceId, String addressName) {
        boolean updateAddressName = sequenceDao.updateAddressName(sequenceId, addressName) != 0;
        Log.d(TAG, String.format("updateAddressName. Status: %s. Sequence id: %s.", updateAddressName, sequenceId));
        return updateAddressName;
    }

    @Override
    public boolean updateConsistencyStatus(@NonNull String sequenceId, int consistencyStatus) {
        boolean updateConsistencyStatus = sequenceDao.updateConsistencyStatus(sequenceId, consistencyStatus) != 0;
        Log.d(TAG, String.format("updateConsistencyStatus. Status: %s. Sequence id: %s.", updateConsistencyStatus, sequenceId));
        return updateConsistencyStatus;
    }

    /**
     * @param include flag which if set to {@code} true will filter the sequences based on the given list of sequence consistency status. The value must be from
     * {@link SequenceDetailsLocal.SequenceConsistencyStatus} interface, otherwise it will exclude said given status.
     * @param sequenceConsistencyStatuses the sequence {@code consistency status} by which the sequences will be filtered by. The first param will ensure if the
     * sequences from persistence with same status will be included or excluded.
     * @return collection of sequence statuses
     */
    private List<Integer> getSequenceConsistencyStatuses(boolean include, @SequenceDetailsLocal.SequenceConsistencyStatus Integer... sequenceConsistencyStatuses) {
        List<Integer> dataSequencesStatuses = new ArrayList<>();
        if (include) {
            dataSequencesStatuses = new ArrayList<>(Arrays.asList(sequenceConsistencyStatuses));
        } else {
            dataSequencesStatuses.add(SequenceDetailsLocal.SequenceConsistencyStatus.VALID);
            dataSequencesStatuses.add(SequenceDetailsLocal.SequenceConsistencyStatus.DATA_MISSING);
            dataSequencesStatuses.add(SequenceDetailsLocal.SequenceConsistencyStatus.EXTERNAL_DATA_MISSING);
            dataSequencesStatuses.add(SequenceDetailsLocal.SequenceConsistencyStatus.METADATA_MISSING);
            for (Integer sequenceConsistencyStatus : sequenceConsistencyStatuses) {
                dataSequencesStatuses.remove(sequenceConsistencyStatus);
            }
        }
        return dataSequencesStatuses;
    }
}