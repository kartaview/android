package com.telenav.osv.data.video.database.dao;

import java.util.List;
import com.telenav.osv.data.database.dao.BaseDao;
import com.telenav.osv.data.video.database.entity.VideoEntity;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * The DAO used in order to access, insert and remove {@code VideoEntity} objects.
 * <p>
 * Access:
 * <ul>
 * <li>{@link #findAll()}</li>
 * <li>{@link #findAllByIds(String[])}</li>
 * <li>{@link #findByID(String)}</li>
 * <li>{@link #findBySequenceID(String)}</li>
 * </ul>
 * Remove:
 * <ul>
 * <li>{@link #deleteById(String)}</li>
 * <li>{@link #deleteBySequenceId(String)}</li>
 * <li>{@link #deleteAll()}</li>
 * </ul>
 * @author horatiuf
 * @see BaseDao
 */
@Dao
public interface VideoDao extends BaseDao<VideoEntity> {

    @Override
    @Insert(onConflict = OnConflictStrategy.FAIL)
    List<Long> insertAll(VideoEntity... entities);

    @Override()
    @Insert(onConflict = OnConflictStrategy.FAIL)
    long insert(VideoEntity entity);

    /**
     * @return a collection representing all available entities of specified type by the concrete implementation.
     */
    @Query("SELECT id FROM video where sequence_id = :sequenceID")
    Single<List<String>> findAllIdsBySequenceId(String sequenceID);

    /**
     * @return a {@code Flowable} representing all available entities of specified type by the concrete implementation.
     */
    @Query("SELECT * FROM video")
    Single<List<VideoEntity>> findAll();

    /**
     * @param entityIds a collection of ids by which the entities will be filtered by.
     * @return a collection of entities which matched the given ids.
     */
    @Query("SELECT * FROM video WHERE id IN (:entityIds)")
    Maybe<List<VideoEntity>> findAllByIds(String[] entityIds);

    /**
     * @param sequenceID the sequence identifier by which the entities will be filtered by.
     * @return a collection of entities which matched the given sequence id.
     */
    @Query("SELECT * FROM video WHERE sequence_id = :sequenceID")
    Maybe<List<VideoEntity>> findAllBySequenceID(String sequenceID);

    /**
     * @param videoId the identifier in order to find a specific {@code VideoEntity}.
     * @return either the {@code VideoEntity} which matched the given id, or an empty value otherwise.
     */
    @Query("SELECT * FROM video WHERE id = :videoId")
    Maybe<VideoEntity> findByID(String videoId);

    /**
     * @param sequenceId the sequence identifier by which the video entities will be filtered by.
     * @return the number of rows which have a specific {@code sequenceId}.
     */
    @Query("SELECT COUNT(sequence_id) FROM video WHERE sequence_id = :sequenceId")
    int findNumberOfRows(String sequenceId);

    /**
     * @param sequenceID the sequence identifier in order to find a specific {@code VideoEntity}.
     * @return either the {@code VideoEntity} which matched the given sequence id, or an empty value otherwise.
     */
    @Query("SELECT * FROM video WHERE sequence_id = :sequenceID")
    Maybe<VideoEntity> findBySequenceID(String sequenceID);

    /**
     * @param videoId the identifier in order to update a specific {@code VideoEntity}.
     * @param frameCount the frame count which will be updated.
     * @return the number of rows which where updated by the query.
     */
    @Query("UPDATE video SET frame_count = :frameCount WHERE id = :videoId")
    int updateFrameCount(String videoId, int frameCount);

    /**
     * Remove a {@code VideoEntity} which match the given parameter.
     * @param ID the identifier for a {@code VideoEntity} to be removed.
     */
    @Query("DELETE FROM video WHERE id = :ID")
    int deleteById(String ID);

    /**
     * Remove a {@code VideoEntity} which match the given parameter.
     * @param sequenceId the identifier for a {@code SequenceEntity} by which the {@code VideoEntity} will match in order to be removed.
     */
    @Query("DELETE FROM video WHERE sequence_id = :sequenceId")
    int deleteBySequenceId(String sequenceId);

    /**
     * Remove all {@code VideoEntity} existing in the persistence.
     */
    @Query("DELETE FROM video")
    int deleteAll();
}
