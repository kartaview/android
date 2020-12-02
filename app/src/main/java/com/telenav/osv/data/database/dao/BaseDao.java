package com.telenav.osv.data.database.dao;

import java.util.List;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;

/**
 * Base dao which holds all CRUD functions supported automatically by the {@code Room} library such as:
 * <ul>
 * <li>{@link #insertAll(Object[])}</li>
 * <li>{@link #deleteAll(Object[])}</li>
 * <li>{@link #updateAll(Object[])}</li>
 * </ul>
 * @author horatiuf
 */
@Entity
public interface BaseDao<T> {

    /**
     * Persists the given entities.
     * <p>If any of the objects already exists the conflict strategy used is replacing the existing value with the given values.</p>
     * <p>This can be used for also a single value.</p>
     * @param entities arbitrary number of entities to be persisted.
     * @see Insert
     */
    @Insert(onConflict = OnConflictStrategy.FAIL)
    List<Long> insertAll(T... entities);

    /**
     * Persists the given entity.
     * <p>If any of the objects already exists the conflict strategy used is replacing the existing value with the given values.</p>
     * <p>This can be used for also a single value.</p>
     * @param entity the entity to be persisted.
     * @see Insert
     */
    @Insert(onConflict = OnConflictStrategy.FAIL)
    long insert(T entity);

    /**
     * Remove the entities which match the given values.
     * <p>This can be used for also a single value.</p>
     * @param entities the arbitrary number of entities to be removed. Can be used for a single value.
     * @see Delete
     */
    @Delete
    int deleteAll(T... entities);

    /**
     * Updates the entities which matches the given ids.
     * <p>If any of the objects already exists the conflict strategy used is replacing the existing value with the given values.</p>
     * <p>This can be used for also a single value.</p>
     * @param entites arbitrary number of entities to be updated.
     * @see Update
     */
    @Update
    int updateAll(T... entites);

    /**
     * Updates the entity which matches the given id.
     * <p>If any of the objects already exists the conflict strategy used is replacing the existing value with the given values.</p>
     * @param entity the entity to be updated.
     * @see Update
     */
    @Update
    int update(T entity);
}
