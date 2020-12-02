package com.telenav.osv.data.database.migration;

import com.telenav.osv.utils.Log;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Custom implementation of Room {@code Migration} class from version 4 to 5.
 * <p>
 * Added support for room and changed all the tables in the schema to conform to the new database core feature.
 * @author horatiuf
 */
public class Migration4To5 extends Migration {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = Migration4To5.class.getSimpleName();

    /**
     * Sql statement in order to create the 'Sequence' table for version 5, which supports Room.
     */
    private static final String DB_CREATE_SEQUENCE_TABLE =
            "CREATE TABLE IF NOT EXISTS 'sequence' ('id' TEXT NOT NULL, 'obd' INTEGER, 'lat' REAL NOT NULL, 'lon' REAL NOT NULL, 'address_name' TEXT, 'distance' REAL NOT " +
                    "NULL, 'app_version' TEXT NOT NULL, 'timestamp' INTEGER NOT NULL, 'frame_count' INTEGER NOT NULL, 'video_count' INTEGER, 'disk_size' INTEGER NOT NULL, " +
                    "'file_path' TEXT NOT NULL, 'online_id' INTEGER, 'bounding_north_lat' REAL, 'bounding_south_lat' REAL, 'bounding_west_lon' REAL, 'bounding_east_lon' REAL, " +
                    "'consistency_status' INTEGER NOT NULL, PRIMARY KEY('id'))";

    /**
     * Sql statement in order to create the 'Score' table for version 5, which supports Room.
     */
    private static final String DB_CREATE_SCORE_TABLE =
            "CREATE TABLE IF NOT EXISTS 'score' ('id' TEXT NOT NULL, 'obd_frame_count' INTEGER, 'frame_count' INTEGER, 'coverage' INTEGER NOT NULL, 'sequence_id' TEXT " +
                    "NOT NULL, PRIMARY KEY('id'), FOREIGN KEY('sequence_id') REFERENCES 'sequence'('id') ON UPDATE NO ACTION ON DELETE CASCADE )";

    /**
     * Sql statement in order to create the 'Video' table for version 5, which supports Room.
     */
    private static final String DB_CREATE_VIDEO_TABLE =
            "CREATE TABLE IF NOT EXISTS 'video' ('id' TEXT NOT NULL, 'index' INTEGER NOT NULL, 'file_path' TEXT NOT NULL, 'frame_count' INTEGER NOT NULL, 'sequence_id' " +
                    "TEXT NOT NULL, PRIMARY KEY('id'), FOREIGN KEY('sequence_id') REFERENCES 'sequence'('id') ON UPDATE NO ACTION ON DELETE CASCADE )";

    /**
     * Sql statement in order to create the 'Frame' table for version 5, which supports Room and is newly altered from the altered 'Photo' table.
     */
    private static final String DB_CREATE_FRAME_TABLE =
            "CREATE TABLE IF NOT EXISTS 'frame' ('id' TEXT NOT NULL, 'timestamp' INTEGER, 'file_path' TEXT NOT NULL, 'index' INTEGER NOT NULL, 'sequence_id' TEXT NOT " +
                    "NULL, PRIMARY KEY('id'), FOREIGN KEY('sequence_id') REFERENCES 'sequence'('id') ON UPDATE NO ACTION ON DELETE CASCADE )";

    /**
     * Sql statement in order to create the 'Location' table for version 5, which supports Room and is newly created from the split 'Photo' table.
     */
    private static final String DB_CREATE_LOCATION_TABLE =
            "CREATE TABLE IF NOT EXISTS 'location' ('id' TEXT NOT NULL, 'lat' REAL, 'lon' REAL, 'sequence_id' TEXT NOT NULL, 'video_id' TEXT, 'frame_id' TEXT, PRIMARY KEY" +
                    "('id'), FOREIGN KEY('video_id') REFERENCES 'video'('id') ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY('frame_id') REFERENCES 'frame'('id') ON UPDATE " +
                    "NO ACTION ON DELETE CASCADE )";

    /**
     * The start version for the migration.
     */
    private static final int START_VERSION = 4;

    /**
     * The end version for the migration.
     */
    private static final int END_VERSION = 5;

    /**
     * Creates a new migration between {@code startVersion} and {@code endVersion}.
     */
    public Migration4To5() {
        super(START_VERSION, END_VERSION);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        migrateSequence4To5(database);
        migrateScore4To5(database);
        migrateVideo4To5(database);
        migrateFrameAndLocation4To5(database);
    }

    /**
     * Migrates the sequence table from version 4 to version 5.
     * @param database the database on which the migration will be performed.
     */
    private static void migrateSequence4To5(SupportSQLiteDatabase database) {
        //alter the previous sequence table name since there are to many change to manual change them one by one
        Log.d(TAG, "migrateSequence4To5. Status: initialising. Message: initiating sequences migration.");
        Log.d(TAG, "migrateSequence4To5. Status: renaming. Message: altering legacy sequence table name.");
        database.execSQL("ALTER TABLE 'Sequence' RENAME TO 'sequence_old'");
        Log.d(TAG, "migrateSequence4To5. Status: create. Message: creating version 5 sequence empty table.");
        database.execSQL(DB_CREATE_SEQUENCE_TABLE);
        Log.d(TAG, "migrateSequence4To5. Status: update data. Message: migrating data from legacy table to version 5 table.");
        //get video number
        database.execSQL("INSERT INTO 'sequence' ('id', " +
                "'obd', " +
                "'lat', " +
                "'lon', " +
                "'address_name', " +
                "'distance', " +
                "'app_version', " +
                "'timestamp' , " +
                "'frame_count', " +
                "'video_count', " +
                "'disk_size', " +
                "'file_path', " +
                "'online_id', " +
                "'consistency_status', " +
                "'bounding_north_lat', " +
                "'bounding_south_lat', " +
                "'bounding_west_lon', " +
                "'bounding_east_lon')" +
                "SELECT sequenceId," +
                "obd, " +
                "latitude, " +
                "longitude, " +
                "'' as address_name, " +
                "'0' as distance, " +
                "version, " +
                "'0' as timestamp, " +
                "count, " +
                "(SELECT count(Video.sequenceId) from Video WHERE Video.sequenceId = sequence_old.sequenceId) as video_count, " +
                "'0' as disk_size, " +
                "thumb, " +
                "onlineSequenceId, " +
                "'0' as consistency_status, " +
                "'0' as bounding_north_lat, " +
                "'0' as bounding_south_lat, " +
                "'0' as bounding_west_lon, " +
                "'0' as bounding_east_lon FROM sequence_old");
        Log.d(TAG, "migrateSequence4To5. Status: clear. Message: removing legacy table from device.");
        database.execSQL("DROP TABLE sequence_old");
        Log.d(TAG, "migrateSequence4To5. Status: finishing. Message: Migration successful.");
    }

    /**
     * Migrates the score table from version 4 to version 5.
     * @param database the database on which the migration will be performed.
     */
    private static void migrateScore4To5(SupportSQLiteDatabase database) {
        Log.d(TAG, "migrateScore4To5. Status: initialising. Message: initiating score migration.");
        Log.d(TAG, "migrateScore4To5. Status: renaming. Message: altering legacy score table name.");
        database.execSQL("ALTER TABLE 'Score' RENAME TO 'score_old'");
        Log.d(TAG, "migrateSequence4To5. Status: create. Message: creating version 5 score empty table.");
        database.execSQL(DB_CREATE_SCORE_TABLE);
        Log.d(TAG, "migrateScore4To5. Status: update data. Message: migrating data from legacy table to version 5 table.");
        database.execSQL("INSERT INTO 'score' ('id', " +
                "'obd_frame_count', " +
                "'frame_count', " +
                "'coverage', " +
                "'sequence_id') " +
                "SELECT (sequenceId || ',' || coverage) as id," +
                "obdCount, " +
                "count, " +
                "coverage, " +
                "sequenceId FROM score_old");
        Log.d(TAG, "migrateScore4To5. Status: clear. Message: removing legacy table from device.");
        database.execSQL("DROP TABLE score_old");
        Log.d(TAG, "migrateScore4To5. Status: finishing. Message: Migration successful.");
    }

    /**
     * Migrates the video table from version 4 to version 5.
     * @param database the database on which the migration will be performed.
     */
    private static void migrateVideo4To5(SupportSQLiteDatabase database) {
        Log.d(TAG, "migrateVideo4To5. Status: initialising. Message: initiating score migration.");
        Log.d(TAG, "migrateVideo4To5. Status: renaming. Message: altering legacy video table name.");
        database.execSQL("ALTER TABLE 'Video' RENAME TO 'video_old'");
        Log.d(TAG, "migrateVideo4To5. Status: create. Message: creating version 5 video empty table.");
        database.execSQL(DB_CREATE_VIDEO_TABLE);
        Log.d(TAG, "migrateVideo4To5. Status: update data. Message: migrating data from legacy table to version 5 table.");
        database.execSQL("INSERT INTO 'video'('id', " +
                "'index', " +
                "'file_path', " +
                "'frame_count', " +
                "'sequence_id') " +
                "SELECT sequenceId || ',' || videoIndex as id," +
                "videoIndex, " +
                "filePath, " +
                "count, " +
                "sequenceId FROM video_old");
        Log.d(TAG, "migrateVideo4To5. Status: clear. Message: removing legacy table from device.");
        database.execSQL("DROP TABLE video_old");
        Log.d(TAG, "migrateVideo4To5. Status: finishing. Message: Migration successful.");
    }

    /**
     * Migrates the 'Photo' table from version 4 to version 5. This will result into two new tables 'Frame' and 'Location'.
     * @param database the database on which the migration will be performed.
     */
    private static void migrateFrameAndLocation4To5(SupportSQLiteDatabase database) {
        Log.d(TAG, "migrateFrameAndLocation4To5. Status: initialising. Message: initiating score migration.");
        Log.d(TAG, "migrateFrameAndLocation4To5. Status: renaming. Message: altering legacy frame table name.");
        database.execSQL("ALTER TABLE 'Frame' RENAME TO 'frame_old'");
        Log.d(TAG, "migrateFrameAndLocation4To5. Status: create. Message: creating version 5 frame empty table.");
        database.execSQL(DB_CREATE_FRAME_TABLE);
        Log.d(TAG, "migrateFrameAndLocation4To5. Status: create. Message: creating version 5 location empty table.");
        database.execSQL(DB_CREATE_LOCATION_TABLE);
        Log.d(TAG, "migrateFrameAndLocation4To5. Status: indexing. Message: creating index for version 5 location table for video and frame ids.");
        database.execSQL("CREATE  INDEX 'index_location_video_id' ON 'location' ('video_id')");
        database.execSQL("CREATE  INDEX 'index_location_frame_id' ON 'location' ('frame_id')");
        Log.d(TAG, "migrateFrameAndLocation4To5. Status: update data. Message: migrating data from legacy table to version 5 frame table.");
        database.execSQL("INSERT INTO 'frame'('id', " +
                "'timestamp', " +
                "'file_path', " +
                "'index', " +
                "'sequence_id') " +
                "SELECT sequenceId || ',' || sequenceIndex || ',' || videoId as id," +
                "'0' as timestamp, " +
                "filePath, " +
                "sequenceIndex, " +
                "sequenceId FROM frame_old");
        Log.d(TAG, "migrateFrameAndLocation4To5. Status: update data. Message: migrating data from legacy table to version 5 location table.");
        database.execSQL("INSERT INTO 'location'('id', " +
                "'lat', " +
                "'lon', " +
                "'sequence_id', " +
                "'video_id', " +
                "'frame_id') " +
                "SELECT sequenceId || ',' || sequenceIndex || ',' || videoId as id," +
                "latitude, " +
                "longitude, " +
                "sequenceId, " +
                "videoId, " +
                "sequenceId || ',' || sequenceIndex || ',' || videoId as frame_id FROM frame_old");
        Log.d(TAG, "migrateFrameAndLocation4To5. Status: clear. Message: removing legacy table from device.");
        database.execSQL("DROP TABLE frame_old");
        Log.d(TAG, "migrateFrameAndLocation4To5. Status: finishing. Message: Migration successful.");
    }
}
