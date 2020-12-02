package com.telenav.osv.data.location.database.dao;

import java.util.List;
import com.telenav.osv.data.database.dao.BaseDao;
import com.telenav.osv.data.location.database.entity.LocationEntity;
import androidx.room.Dao;
import androidx.room.Query;
import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * The DAO used in order to access, insert and remove {@code LocationEntity} objects.
 * <p>
 * Access:
 * <ul>
 * <li>{@link #findAll()}</li>
 * <li>{@link #findAllByIds(int[])}</li>
 * <li>{@link #findByID(String)}</li>
 * <li>{@link #findAllBySequenceID(String)}</li>
 * <li>{@link #findAllBySequenceID(String)}</li>
 * </ul>
 * Remove:
 * <ul>
 * <li>{@link #deleteById(String)} ()}</li>
 * <li>{@link #deleteBySequenceId(String)}</li>
 * <li>{@link #deleteAll()}</li>
 * </ul>
 * @author horatiuf
 * @see BaseDao
 */
@Dao
public interface LocationDao extends BaseDao<LocationEntity> {

    /**
     * @return a {@code Single} representing all available entities of specified type by the concrete implementation.
     */
    @Query("SELECT * FROM location")
    Single<List<LocationEntity>> findAll();

    /**
     * @param entityIds a collection of ids by which the entities will be filtered by.
     * @return a collection of entities which matched the given ids.
     */
    @Query("SELECT * FROM location WHERE id IN (:entityIds)")
    Maybe<List<LocationEntity>> findAllByIds(String[] entityIds);

    /**
     * @param ID the identifier in order to find a specific {@code locationEntity}.
     * @return either the {@code LocationEntity} which matched the given id, or an empty value otherwise.
     */
    @Query("SELECT * FROM location WHERE id = :ID")
    Maybe<LocationEntity> findByID(String ID);

    /**
     * @param sequenceID the sequence identifier in order to find a specific {@code LocationEntity}.
     * @return either the collection of {@code LocationEntity} which matched the given sequence id, or an empty value otherwise.
     */
    @Query("SELECT * FROM location WHERE sequence_id = :sequenceID")
    Maybe<List<LocationEntity>> findAllBySequenceID(String sequenceID);

    /**
     * @param videoID the video identifier in order to find a specific {@code LocationEntity}.
     * @return either the collection of {@code LocationEntity} which matched the given video id, or an empty value otherwise.
     */
    @Query("SELECT * FROM location WHERE sequence_id = :videoID")
    Maybe<List<LocationEntity>> findByVideoID(String videoID);

    /**
     * @param frameID the frame identifier in order to find a specific {@code LocationEntity}.
     * @return either the {@code LocationEntity} which matched the given sequence id, or an empty value otherwise.
     */
    @Query("SELECT * FROM location WHERE frame_id = :frameID")
    Maybe<LocationEntity> findByFrameID(String frameID);

    /**
     * @param sequenceId the sequence identifier by which the location entities will be filtered by.
     * @return the number of rows which have the exact given {@code sequenceId}.
     */
    @Query("SELECT COUNT(sequence_id) FROM location WHERE sequence_id = :sequenceId")
    int findNumberOfRows(String sequenceId);

    /**
     * Remove a {@code LocationEntity} which match the given parameter.
     * @param ID the identifier for a {@code LocationEntity} to be removed.
     */
    @Query("DELETE FROM location WHERE id = :ID")
    int deleteById(String ID);

    /**
     * Remove a {@code LocationEntity} which match the given parameter.
     * @param sequenceId the identifier for a {@code SequenceEntity} by which the {@code LocationEntity} will match in order to be removed.
     */
    @Query("DELETE FROM location WHERE sequence_id = :sequenceId")
    int deleteBySequenceId(String sequenceId);

    /**
     * Remove all {@code LocationEntity} existing in the persistence.
     */
    @Query("DELETE FROM location")
    int deleteAll();
}
