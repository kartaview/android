package com.telenav.osv.data.sequence.database.dao;

import java.util.List;
import org.joda.time.DateTime;
import com.telenav.osv.data.database.dao.BaseDao;
import com.telenav.osv.data.sequence.database.entity.SequenceEntity;
import com.telenav.osv.data.sequence.database.entity.SequenceWithRewardEntity;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * The DAO used in order to access, insert and remove {@code SequenceEntity} objects.
 * <p>
 * Access:
 * <ul>
 * <li>{@link #findAll()}</li>
 * <li>{@link #findAllByIds(String[])}</li>
 * <li>{@link #findByID(String)}</li>
 * <li>{@link #findBySequenceID(String)}</li>
 * <li>{@link #countAll()}</li>
 * </ul>
 * Update:
 * <ul>
 * <li>{@link #updateAddressName(String, String)}</li>
 * </ul>
 * Remove:
 * <ul>
 * <li>{@link #deleteById(String)}</li>
 * <li>{@link #deleteAll()}</li>
 * </ul>
 * @author horatiuf
 * @see BaseDao
 */
@Dao
public interface SequenceDao extends BaseDao<SequenceEntity> {

    /**
     * @return a collection representing all available entities of specified type by the concrete implementation.
     */
    @Query("SELECT * FROM sequence")
    Single<List<SequenceEntity>> findAll();

    /**
     * @return a collection representing all available entities of specified type by the concrete implementation.
     */
    @Query("SELECT id FROM sequence")
    Single<List<String>> findAllIds();

    /**
     * @param dataConsistencies the values for data consistency by which the sequences will be filtered by.
     * @return a collection representing all available entities of specified type by the concrete implementation which passes the given filter.
     */
    @Query("SELECT * FROM sequence WHERE consistency_status IN (:dataConsistencies)")
    Single<List<SequenceEntity>> findAll(List<Integer> dataConsistencies);

    /**
     * @return a collection representing all available entities of specified type by the concrete implementation which passes the given filter.
     * <p> The collection will include each sequence reward information if found.
     */
    @Transaction
    @Query("SELECT * FROM sequence")
    Single<List<SequenceWithRewardEntity>> findAllWithRewards();

    /**
     * @param dataConsistencies the values for data consistency by which the sequences will be filtered by.
     * @return a collection representing all available entities of specified type by the concrete implementation which passes the given filter.
     * <p> The collection will include each sequence reward information if found.
     */
    @Query("SELECT * FROM sequence WHERE consistency_status IN (:dataConsistencies)")
    Single<List<SequenceWithRewardEntity>> findAllWithRewards(List<Integer> dataConsistencies);

    /**
     * @param entityIds a collection of ids by which the entities will be filtered by.
     * @return a collection of entities which matched the given ids.
     */
    @Query("SELECT * FROM sequence WHERE id IN (:entityIds)")
    Maybe<List<SequenceEntity>> findAllByIds(String[] entityIds);

    /**
     * @param ID the identifier in order to find a specific {@code SequenceEntity}.
     * @return either the {@code SequenceEntity} which matched the given id, or an empty value otherwise.
     */
    @Query("SELECT * FROM sequence WHERE id = :ID")
    Maybe<SequenceEntity> findByID(String ID);

    /**
     * @param ID the identifier in order to find a specific {@code SequenceEntity}.
     * @return either the {@code SequenceEntity} which matched the given id, or an empty value otherwise.
     */
    @Query("SELECT * FROM sequence WHERE id = :ID")
    Maybe<SequenceWithRewardEntity> findByIDWithReward(String ID);

    /**
     * @param sequenceID the sequence identifier in order to find a specific {@code SequenceEntity}.
     * @return either the {@code SequenceEntity} which matched the given sequence id, or an empty value otherwise.
     */
    @Query("SELECT * FROM sequence WHERE id = :sequenceID")
    Maybe<SequenceEntity> findBySequenceID(String sequenceID);

    /**
     * Remove a {@code SequenceEntity} which match the given parameter.
     * @param sequenceId the identifier for a {@code SequenceEntity} to be removed.
     * @return {@code number} representing the deleted rows number.
     */
    @Query("DELETE FROM sequence WHERE id = :sequenceId")
    int deleteById(String sequenceId);

    /**
     * Updates the sequence details referring to compression numbers which can change at any moment.
     * @param sequenceId the sequence identifier.
     * @param locationsCount the {@code number} of frames to be updated in the sequence.
     * @param videoCount the {@code number} of videos to be updated in the sequence.
     * @return {@code number} representing the updated rows number.
     */
    @Query("UPDATE sequence SET frame_count = :locationsCount, video_count = :videoCount WHERE id = :sequenceId")
    int updateCompressionNumbers(String sequenceId, int locationsCount, int videoCount);

    /**
     * Updates the sequence details referring to obd which can change at any moment.
     * @param sequenceID the sequence identifier.
     * @param obd the {@code flag} which shows if the sequence is used with or without obd feature enabled.
     * @return {@code number} representing the updated rows number.
     */
    @Query("UPDATE sequence SET obd = :obd WHERE id = :sequenceID")
    int updateObd(String sequenceID, boolean obd);

    /**
     * Updates the sequence details refering to online id which can change at any moment.
     * @param sequenceID the sequence identifier.
     * @param onlineID the {@code online} identifier for the sequence.
     * @return {@code number} representing the updated rows number.
     */
    @Query("UPDATE sequence SET online_id = :onlineID WHERE id = :sequenceID")
    int updateOnlineId(String sequenceID, long onlineID);

    /**
     * Updates the sequence details referring to disk size which can change at any moment.
     * @param sequenceID the sequence identifier.
     * @param diskSize the {@code diskSize} for the sequence.
     * @return {@code number} representing the updated rows number.
     */
    @Query("UPDATE sequence SET disk_size = :diskSize WHERE id = :sequenceID")
    int updateDiskSize(String sequenceID, long diskSize);

    /**
     * Updates the sequence details referring to distance which can change at any moment.
     * @param sequenceID the sequence identifier.
     * @param distance the {@code distance} for the sequence.
     * @return {@code number} representing the updated rows number.
     */
    @Query("UPDATE sequence SET distance = :distance WHERE id = :sequenceID")
    int updateDistance(String sequenceID, double distance);

    /**
     * Updates the sequence details referring to distance which can change at any moment.
     * @param sequenceID the sequence identifier.
     * @param timestamp the {@code timestamp} which is the creation time for the sequence.
     * @return {@code number} representing the updated rows number.
     */
    @Query("UPDATE sequence SET timestamp = :timestamp WHERE id = :sequenceID")
    int updateDateTime(String sequenceID, DateTime timestamp);

    /**
     * Updates the sequence details which refer to size (disk size, frame count, video count) which can change at any moment.
     * @param sequenceID the sequence identifier.
     * @param diskSize the {@code disk size} for the sequence.
     * @param locationsCount the {@code frame} number for the sequence.
     * @param videoCount the {@code video} number for the sequence.
     * @return {@code number} representing the updated rows number.
     */
    @Query("UPDATE sequence SET disk_size = :diskSize, frame_count = :locationsCount, video_count = :videoCount WHERE id = :sequenceID")
    int updateSizeInfo(String sequenceID, long diskSize, int locationsCount, int videoCount);

    /**
     * Updates the sequence details referring to consistency for both data and file.
     * @param sequenceID the sequence identifier.
     * @param consistencyStatus the {@code consistency status} which is the validity of the sequence.
     * @return {@code number} representing the updated rows number.
     */
    @Query("UPDATE sequence SET consistency_status = :consistencyStatus WHERE id = :sequenceID")
    int updateConsistencyStatus(String sequenceID, int consistencyStatus);

    /**
     * Updates the sequence details referring to consistency for both data and file.
     * @param sequenceID the sequence identifier.
     * @param addressName the {@code address name} which is display name for the sequence.
     * @return {@code number} representing the updated rows number.
     */
    @Query("UPDATE sequence SET address_name = :addressName WHERE id = :sequenceID")
    int updateAddressName(String sequenceID, String addressName);

    /**
     * @return {@code number} of values which the sequences table has.
     */
    @Query("SELECT COUNT(*) FROM sequence")
    int countAll();

    /**
     * Remove all {@code SequenceEntity} existing in the persistence.
     * @return {@code number} representing the deleted rows number.
     */
    @Query("DELETE FROM sequence")
    int deleteAll();
}
