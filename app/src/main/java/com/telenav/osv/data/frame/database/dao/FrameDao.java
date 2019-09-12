package com.telenav.osv.data.frame.database.dao;

import java.util.List;
import com.telenav.osv.data.database.dao.BaseDao;
import com.telenav.osv.data.frame.database.entity.FrameEntity;
import com.telenav.osv.data.frame.database.entity.FrameWithLocationEntity;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * The DAO used in order to access, insert and remove {@code FrameEntity} objects.
 * <p>
 * Access:
 * <ul>
 * <li>{@link #findAll()}</li>
 * <li>{@link #findAllByIds(int[])}</li>
 * <li>{@link #findByID(String)}</li>
 * <li>{@link #findBySequenceID(String)}</li>
 * <li>{@link #findAllWithLocationBySequenceID(String)}</li>
 * </ul>
 * Remove:
 * <ul>
 * <li>{@link #deleteById(String)}</li>
 * <li>{@link #deleteBySequenceId(String)}</li>
 * <li>{@link #deleteAll()}</li>
 * </ul>
 * @author horatiuf
 * @see BaseDao
 * @see FrameEntity
 */
@Dao
public interface FrameDao extends BaseDao<FrameEntity> {

    /**
     * @return a collection representing all available {@code FrameEntity}.
     */
    @Query("SELECT * FROM frame")
    Maybe<List<FrameEntity>> findAll();

    /**
     * @param entityIds a collection of ids by which the entities will be filtered by.
     * @return a collection of entities which matched the given ids.
     */
    @Query("SELECT * FROM frame WHERE id IN (:entityIds)")
    Maybe<List<FrameEntity>> findAllByIds(int[] entityIds);

    /**
     * @param ID the identifier in order to find a specific {@code FrameEntity}.
     * @return either the {@code FrameEntity} which matched the given id, or an empty value otherwise.
     */
    @Query("SELECT * FROM frame WHERE id = :ID")
    Maybe<FrameEntity> findByID(String ID);

    /**
     * @return a collection representing all available entities of specified type by the concrete implementation.
     */
    @Query("SELECT id FROM frame where sequence_id = :sequenceID")
    Single<List<String>> findAllIdsBySequenceId(String sequenceID);

    /**
     * @param ID the identifier in order to find a specific {@code FrameEntity}.
     * @return either the {@code FrameWithLocationEntity} which matched the given id, or an empty value otherwise.
     */
    @Query("SELECT frame.*, " +
            "location.id as loc_id, " +
            "location.lat as loc_lat, " +
            "location.lon as loc_lon " +
            "FROM frame JOIN location ON frame.id = location.frame_id WHERE frame.id = :ID")
    Maybe<FrameWithLocationEntity> findWithLocationByID(String ID);

    /**
     * @param sequenceID the sequence identifier by which the entities will be filtered by.
     * @return a collection of entities which matched the given sequence id.
     */
    @Query("SELECT * FROM frame WHERE sequence_id = :sequenceID")
    Maybe<List<FrameEntity>> findAllBySequenceID(String sequenceID);

    /**
     * @param sequenceID the sequence identifier by which the entities will be filtered by.
     * @return a collection of entities which matched the given sequence id. The location entity will be embedded into the objects.
     */
    @Transaction
    @Query("SELECT frame.*, " +
            "location.id as loc_id, " +
            "location.lat as loc_lat, " +
            "location.lon as loc_lon " +
            "FROM frame JOIN location ON frame.id = location.frame_id WHERE frame.sequence_id = :sequenceID")
    Maybe<List<FrameWithLocationEntity>> findAllWithLocationBySequenceID(String sequenceID);

    /**
     * @param sequenceID the sequence identifier in order to find a specific {@code FrameEntity}.
     * @return either the {@code FrameEntity} which matched the given sequence id, or an empty value otherwise.
     */
    @Query("SELECT * FROM frame WHERE sequence_id = :sequenceID")
    Maybe<FrameEntity> findBySequenceID(String sequenceID);

    /**
     * Remove a {@code FrameEntity} which match the given parameter.
     * @param ID the identifier for a {@code FrameEntity} to be removed.
     */
    @Query("DELETE FROM frame WHERE id = :ID")
    int deleteById(String ID);

    /**
     * Remove a {@code FrameEntity} which match the given parameter.
     * @param sequenceId the identifier for a {@code SequenceEntity} by which the {@code FrameEntity} will match in order to be removed.
     */
    @Query("DELETE FROM frame WHERE sequence_id = :sequenceId")
    int deleteBySequenceId(String sequenceId);

    /**
     * @param sequenceId the sequence identifier by which the video entities will be filtered by.
     * @return the number of rows which have a specific {@code sequenceId}.
     */
    @Query("SELECT COUNT(sequence_id) FROM frame WHERE sequence_id = :sequenceId")
    int findNumberOfRows(String sequenceId);

    /**
     * Remove all {@code FrameEntity} existing in the persistence.
     */
    @Query("DELETE FROM frame")
    int deleteAll();
}
