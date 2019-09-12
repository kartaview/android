package com.telenav.osv.data.score.database.dao;

import java.util.List;
import com.telenav.osv.data.database.dao.BaseDao;
import com.telenav.osv.data.score.database.entity.ScoreEntity;
import androidx.room.Dao;
import androidx.room.Query;
import io.reactivex.Maybe;

/**
 * The DAO used in order to access, insert, update and remove {@code ScoreEntity} objects.
 * <p>
 * Access:
 * <ul>
 * <li>{@link #findAll()}</li>
 * <li>{@link #findAllByIds(String[])}</li>
 * <li>{@link #findAllBySequenceID(String)}</li>
 * <li>{@link #findByID(String)}</li>
 * <li>{@link #findBySequenceID(String)}</li>
 * </ul>
 * Remove:
 * <ul>
 * <li>{@link #deleteById(String)} ()}</li>
 * <li>{@link #deleteBySequenceId(String)}</li>
 * <li>{@link #deleteAll()}</li>
 * </ul>
 * Update:
 * <ul>
 * <li>{@link #updateObdPhotoCount(String, int)}</li>
 * <li>{@link #updatePhotoCount(String, int)}</li>
 * </ul>
 * @author horatiuf
 * @see BaseDao
 */
@Dao
public interface ScoreDao extends BaseDao<ScoreEntity> {

    /**
     * @return a collection representing all available entities of specified type by the concrete implementation.
     */
    @Query("SELECT * FROM score")
    Maybe<List<ScoreEntity>> findAll();

    /**
     * @param entityIds a collection of ids by which the entities will be filtered by.
     * @return a collection of entities which matched the given ids if exists.
     */
    @Query("SELECT * FROM score WHERE id IN (:entityIds)")
    Maybe<List<ScoreEntity>> findAllByIds(String[] entityIds);

    /**
     * @param sequenceID the sequence identifier by which the entities will be filtered by.
     * @return a collection of entities which matched the given sequence id.
     */
    @Query("SELECT * FROM score WHERE sequence_id = :sequenceID")
    Maybe<List<ScoreEntity>> findAllBySequenceID(String sequenceID);

    /**
     * @param ID the identifier in order to find a specific {@code ScoreEntity}.
     * @return either the {@code ScoreEntity} which matched the given id, or an empty value otherwise.
     */
    @Query("SELECT * FROM score WHERE id = :ID")
    Maybe<ScoreEntity> findByID(String ID);

    /**
     * @param sequenceID the sequence identifier in order to find a specific {@code ScoreEntity}.
     * @return either the {@code ScoreEntity} which matched the given sequence id, or an empty value otherwise.
     */
    @Query("SELECT * FROM score WHERE sequence_id = :sequenceID")
    Maybe<ScoreEntity> findBySequenceID(String sequenceID);

    /**
     * Updates the score details referring to obd frame count which can change at any moment.
     * @param scoreID the score entity identifier.
     * @param obdPhotoCount the number of photos taken with obd to be updated.
     * @return {@code number} representing the updated rows number.
     */
    @Query("UPDATE score SET obd_frame_count = :obdPhotoCount WHERE id = :scoreID")
    int updateObdPhotoCount(String scoreID, int obdPhotoCount);

    /**
     * Updates the score details referring to frame count which can change at any moment.
     * @param scoreID the score entity identifier.
     * @param photoCount the number of photos taken to be updated.
     * @return {@code number} representing the updated rows number.
     */
    @Query("UPDATE score SET frame_count = :photoCount WHERE id = :scoreID")
    int updatePhotoCount(String scoreID, int photoCount);

    /**
     * Remove a {@code ScoreEntity} which match the given parameter.
     * @param ID the identifier for a {@code ScoreEntity} to be removed.
     * @return {@code number} representing the updated rows number.
     */
    @Query("DELETE FROM score WHERE id = :ID")
    int deleteById(String ID);

    /**
     * Remove a {@code ScoreEntity} which match the given parameter.
     * @param sequenceId the identifier for a {@code SequenceEntity} by which the {@code ScoreEntity} will match in order to be removed.
     */
    @Query("DELETE FROM score WHERE sequence_id = :sequenceId")
    int deleteBySequenceId(String sequenceId);

    /**
     * Remove all {@code ScoreEntity} existing in the persistence.
     */
    @Query("DELETE FROM score")
    int deleteAll();
}
