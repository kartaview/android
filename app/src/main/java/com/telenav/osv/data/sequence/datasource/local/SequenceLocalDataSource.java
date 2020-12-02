package com.telenav.osv.data.sequence.datasource.local;

import java.util.List;
import org.joda.time.DateTime;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal.SequenceConsistencyStatus;
import androidx.annotation.NonNull;
import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * Interface which represents the data source for {@link LocalSequence}. This represents a wrapper on top of the persistence layer in order to access, change, persist and remove
 * data related to sequences in the app.
 * <p>
 * Access methods:
 * <ul>
 * <li>{@link #getSequences()}</li>
 * <li>{@link #getSequences(boolean, Integer...)}</li>
 * <li>{@link #getSequenceWithReward(String)}</li>
 * <li>{@link #getSequenceWithAll(String)}</li>
 * <li>{@link #getSequence(String)}</li>
 * <li>{@link #getSequencesWithReward()}</li>
 * <li>{@link #getSequencesIds()}</li>
 * <li>{@link #getSequenceWithReward(String)}</li>
 * </ul>
 * Change methods:
 * <ul>
 * <li>{@link #updateDiskSize(String, long)}</li>
 * <li>{@link #updateObd(String, boolean)}</li>
 * <li>{@link #updateOnlineId(String, long)}</li>
 * <li>{@link #updateSequenceSizeInfo(String, long, int, int)}</li>
 * <li>{@link #updateSequence(LocalSequence)}</li>
 * </ul>
 * Persist method:
 * <ul>
 * <li>{@link #persistSequence(LocalSequence)}</li>
 * </ul>
 * Remove method:
 * <ul>
 * <li>{@link #deleteSequence(String)}</li>
 * </ul>
 * Helper method:
 * <ul>
 * <li>{@link #isPopulated()}</li>
 * </ul>
 * @author horatiuf
 */
public interface SequenceLocalDataSource {

    /**
     * @return {@code Single} with one of the following callbacks:
     * <ul>
     * <li>success - collection of {@code LocalSequence} found in the persistence. This will be without any data which is required from other tables which are
     * connected to the sequence.</li>
     * <li>error - error message received</li>
     * <p> Note: the location data source is required to be searched in order to properly update the number of locations for the compression info.
     */
    Single<List<LocalSequence>> getSequences();

    /**
     * @param include flag which if set to {@code} true will filter the sequences based on the given list of sequence consistency statuses. The value must be from
     * {@link SequenceConsistencyStatus} interface, otherwise it will exclude said given statuses.
     * @param sequenceConsistencyStatus the sequence {@code consistency status} by which the sequences will be filtered by. The first param will ensure if the
     * sequences from persistence with same status will be included or excluded.
     * @return {@code Single} with one of the following callbacks:
     * <ul>
     * <li>success - collection of {@code LocalSequence} found in the persistence. This will be without any data which is required from other tables which are
     * connected to the sequence.</li>
     * <li>error - error message received</li>
     * <p> Note: the location data source is required to be searched in order to properly update the number of locations for the compression info.
     */
    Single<List<LocalSequence>> getSequences(boolean include, @SequenceConsistencyStatus Integer... sequenceConsistencyStatus);

    /**
     * @param include flag which if set to {@code} true will filter the sequences based on the given list of sequence consistency status. The value must be from
     * {@link SequenceConsistencyStatus} interface, otherwise it will exclude said given status.
     * @param sequenceConsistencyStatus the sequence {@code consistency status} by which the sequences will be filtered by. The first param will ensure if the
     * sequences from persistence with same status will be included or excluded.
     * @return {@code Single} with one of the following callbacks:
     * <ul>
     * <li>success - collection of {@code LocalSequence} found in the persistence which also includes the reward data of each sequence if it exists.</li>
     * <li>error - error message received</li>
     * <p> Note: the location data source is required to be searched in order to properly update the number of locations for the compression info.
     */
    Single<List<LocalSequence>> getSequencesWithReward(boolean include, @SequenceConsistencyStatus Integer... sequenceConsistencyStatus);

    /**
     * @return {@code Single} with one of the following callbacks:
     * <ul>
     * <li>success - collection of LocalSequence {@code ids} found in the persistence. </li>
     * <li>error - error message received</li>
     **/
    Single<List<String>> getSequencesIds();

    /**
     * @return {@code Single} with one of the following callbacks:
     * <ul>
     * <li>success - collection of {@code LocalSequence} found in the persistence which also includes the reward data of each sequence if it exists.</li>
     * <li>error - error message received</li>
     * <p> Note: the location data source is required to be searched in order to properly update the number of locations for the compression info.
     */
    Single<List<LocalSequence>> getSequencesWithReward();

    /**
     * @param sequenceId the sequence identifier.
     * @return {@code Maybe} with one of the following callbacks:
     * <ul>
     * <li>success - {@code LocalSequence} based on the given sequence identifier</li>
     * <li>complete - {@code empty} response since the sequence was not found</li>
     * <li>error - error message received</li>
     */
    Maybe<LocalSequence> getSequence(@NonNull String sequenceId);

    /**
     * @param sequenceId the sequence identifier.
     * @return {@code Maybe} with one of the following callbacks:
     * <ul>
     * <li>success - {@code LocalSequence} based on the given sequence identifier with all nullable fields populated if they exist in persistence, i.e reward and location data</li>
     * <li>complete - {@code empty} response since the sequence was not found</li>
     * <li>error - error message received</li>
     */
    Maybe<LocalSequence> getSequenceWithAll(@NonNull String sequenceId);

    /**
     * @param sequenceId the sequence identifier.
     * @return {@code Maybe} with one of the following callbacks:
     * <ul>
     * <li>success - {@code LocalSequence} based on the given sequence identifier with reward field being populated if they exist in persistence</li>
     * <li>complete - {@code empty} response since the sequence was not found</li>
     * <li>error - error message received</li>
     */
    Maybe<LocalSequence> getSequenceWithReward(@NonNull String sequenceId);

    /**
     * @param sequenceId the sequence identifier.
     * @return {@code true} if the remove from persistence was successful, {@code false} otherwise.
     * <p>Note: This will also remove all data related to the sequence such as: score, frame and videos.</p>
     */
    boolean deleteSequence(@NonNull String sequenceId);

    /**
     * @param sequence the new sequence which will be persisted in the database.
     * @return {@code true} if the persistence was successful, {@code false} otherwise.
     */
    boolean persistSequence(@NonNull LocalSequence sequence);

    /**
     * @param sequenceId the identifier for the {@code LocalSequence}.
     * @param obd the {@code flag} which shows if the sequence is used with or without obd feature enabled.
     * @return {@code true} if the update was successful in the persistence, {@code false} otherwise.
     */
    boolean updateObd(@NonNull String sequenceId, boolean obd);

    /**
     * @param sequenceId the identifier for the {@code LocalSequence}.
     * @param onlineID the {@code online} identifier for the sequence.
     * @return {@code true} if the update was successful in the persistence, {@code false} otherwise.
     */
    boolean updateOnlineId(@NonNull String sequenceId, long onlineID);

    /**
     * @param sequenceId the identifier for the {@code LocalSequence}.
     * @param diskSize the physical device {@code size} for the sequence.
     * @return {@code true} if the update was successful in the persistence, {@code false} otherwise.
     */
    boolean updateDiskSize(@NonNull String sequenceId, long diskSize);

    /**
     * @param sequenceId the identifier for the {@code LocalSequence}.
     * @param distance the distance between all coordinates of the sequence.
     * @return {@code true} if the update was successful in the persistence, {@code false} otherwise.
     */
    boolean updateDistance(@NonNull String sequenceId, double distance);

    /**
     * @param sequenceId the identifier for the {@code LocalSequence}.
     * @param dateTime the datetime representing the creation time of the sequence.
     * @return {@code true} if the update was successful in the persistence, {@code false} otherwise.
     */
    boolean updateDateTime(@NonNull String sequenceId, DateTime dateTime);

    /**
     * @param sequenceId the identifier for the {@code LocalSequence}.
     * @param addressName the address name for the sequence.
     * @return {@code true} if the update was successful in the persistence, {@code false} otherwise.
     */
    boolean updateAddressName(@NonNull String sequenceId, String addressName);

    /**
     * @param sequenceId the identifier for the {@code LocalSequence}.
     * @param consistencyStatus the value representing the sequence consistency. Must be a value from {@link SequenceConsistencyStatus} interface.
     * @return {@code true} if the update was successful in the persistence, {@code false} otherwise.
     */
    boolean updateConsistencyStatus(@NonNull String sequenceId, @SequenceConsistencyStatus int consistencyStatus);

    /**
     * @param sequenceId the identifier for the {@code LocalSequence}.
     * @param diskSize the physical device {@code size} for the sequence.
     * @param frameCount the number of {@code frames} for the sequence.
     * @param videoCount the number of {@code videos} for the sequence.
     * @return {@code true} if the update was successful in the persistence, {@code false} otherwise.
     */
    boolean updateSequenceSizeInfo(@NonNull String sequenceId, long diskSize, int frameCount, int videoCount);

    /**
     * @param sequenceId the identifier for the {@code LocalSequence}.
     * @param frameCount the number representing the size of the frames of the sequence.
     * @param videoCount the number representing the size of the videos of the sequence.
     * @return {@code true} if the update was successful in the persistence, {@code false} otherwise.
     */
    boolean updateCompressionSizeInfo(@NonNull String sequenceId, int frameCount, int videoCount);

    /**
     * @return {@code true} if there are sequences in the persistence, {@code false} otherwise.
     */
    boolean isPopulated();

    /**
     * @param sequence the sequence which will be updated in the persistence.
     * @return {@code true} if the update was successful in the persistence, {@code false} otherwise.
     */
    boolean updateSequence(@NonNull LocalSequence sequence);
}
