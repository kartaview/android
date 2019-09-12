package com.telenav.osv.data.database;

import android.content.Context;
import com.telenav.osv.data.database.migration.Migration1To2;
import com.telenav.osv.data.database.migration.Migration2To3;
import com.telenav.osv.data.database.migration.Migration3To4;
import com.telenav.osv.data.database.migration.Migration4To5;
import com.telenav.osv.data.frame.database.dao.FrameDao;
import com.telenav.osv.data.frame.database.entity.FrameEntity;
import com.telenav.osv.data.location.database.dao.LocationDao;
import com.telenav.osv.data.location.database.entity.LocationEntity;
import com.telenav.osv.data.score.database.dao.ScoreDao;
import com.telenav.osv.data.score.database.entity.ScoreEntity;
import com.telenav.osv.data.sequence.database.dao.SequenceDao;
import com.telenav.osv.data.sequence.database.entity.SequenceEntity;
import com.telenav.osv.data.video.database.dao.VideoDao;
import com.telenav.osv.data.video.database.entity.VideoEntity;
import com.telenav.osv.utils.ConverterHelper;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Custom {@code RoomDatabase} abstract implementation. This will hold the DAO objects for all structure data. The class will be singleton in order to have a single source of
 * truth to the database.
 * <p> DAO layer:
 * <ul>
 * <li>Score - {@link #scoreDao()}</li>
 * <li>Score - {@link #videoDao()}</li>
 * <li>Score - {@link #sequenceDao()}</li>
 * <li>Score - {@link #locationDao()}</li>
 * <li>Score - {@link #frameDao()}</li>
 * </ul>
 * @author horatiuf
 */
@Database(
        version = 5,
        entities = {
                SequenceEntity.class,
                VideoEntity.class,
                FrameEntity.class,
                ScoreEntity.class,
                LocationEntity.class})
@TypeConverters(ConverterHelper.class)
public abstract class OSCDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "Sequences";

    private static final Object syncObject = new Object();

    private static OSCDatabase instance;

    /**
     * Default constructor for the current class.
     */
    public OSCDatabase() {
        //private constructor to prevent instantiation.
    }

    /**
     * @param context used to instantiate the database custom implementation if it does not exist.
     * @return instance to the current custom {@code RoomDatabase} abstract implementation. This instance is persisted using {@code Singleton} pattern in order to have only one
     * access to the database at a time.
     */
    public static OSCDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (syncObject) {
                instance = Room.databaseBuilder(context, OSCDatabase.class, DATABASE_NAME)
                        .addMigrations(
                                new Migration1To2(),
                                new Migration2To3(),
                                new Migration3To4(),
                                new Migration4To5())
                        .build();
            }
        }
        return instance;
    }

    /**
     * @return instance to the {@code ScoreDao} which will be automatically populated by the {@code Room} library.
     */
    public abstract ScoreDao scoreDao();

    /**
     * @return instance to the {@code VideoDao} which will be automatically populated by the {@code Room} library.
     */
    public abstract VideoDao videoDao();

    /**
     * @return instance to the {@code LocationDao} which will be automatically populated by the {@code Room} library.
     */
    public abstract LocationDao locationDao();

    /**
     * @return instance to the {@code SequenceDao} which will be automatically populated by the {@code Room} library.
     */
    public abstract SequenceDao sequenceDao();

    /**
     * @return instance to the {@code FrameDao} which will be automatically populated by the {@code Room} library.
     */
    public abstract FrameDao frameDao();
}
