package com.telenav.osv.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author horatiuf
 */
public class SqlLiteOpenHelperTest extends SQLiteOpenHelper {

    public final static String FRAME_SEQ_INDEX = "sequenceIndex";

    public final static String FRAME_FILE_PATH = "filePath";

    public final static String FRAME_LAT = "latitude";

    public final static String FRAME_LON = "longitude";

    public final static String FRAME_ACCURACY = "accuracy";

    public final static String FRAME_SEQ_ID = "sequenceId";

    public final static String FRAME_VIDEO_ID = "videoId";

    public final static String FRAME_ORIENTATION = "orientation";

    public final static String SEQUENCE_ID = "sequenceId";

    public final static String SEQUENCE_LAT = "latitude";

    public final static String SEQUENCE_LON = "longitude";

    public final static String SEQUENCE_PATH = "thumb";

    public final static String SEQUENCE_EXTERNAL = "external";

    public static final String SEQUENCE_SAFE = "safe";

    public final static String VIDEO_INDEX = "videoIndex";

    public final static String VIDEO_FILE_PATH = "filePath";

    public final static String VIDEO_FRAME_COUNT = "count";

    //  -------------------------------------------------------------

    public final static String SCORE_COVERAGE = "coverage";

    public final static String SCORE_OBD_COUNT = "obdCount";

    public final static String SCORE_COUNT = "count";

    public static final String SEQUENCE_OBD = "obd";

    public static final int STATUS_NEW = 0;

    public static final int STATUS_INDEXING = 1;

    public static final int STATUS_UPLOADING = 2;

    public static final int STATUS_INTERRUPTED = 3;

    public final static String SEQUENCE_VERSION = "version";

    public static final String SEQUENCE_STATUS = "status";

    public final static String SEQUENCE_COUNT = "count";

    public final static String SEQUENCE_VIDEO_COUNT = "videoCount";

    //  ---------------------------------------------------------------

    public final static String SEQUENCE_ONLINE_ID = "onlineSequenceId";

    /**
     * Name of the only table, containing photo details
     */
    public final static String FRAME_TABLE = "Frame";

    public static final String FRAME_UNIQUE_CONSTRAINT = "uniqueConstraint";

    public static final String VIDEO_UNIQUE_CONSTRAINT = "uniqueConstraint";

    public static final String SCORE_UNIQUE_CONSTRAINT = "scoreUniqueConstraint";

    //  ---------------------------------------------------------------

    public static final String SEQUENCE_TABLE = "Sequence";

    public final static String SEQUENCE_ORIG_COUNT = "origCount";

    public final static String SEQUENCE_PANO = "panorama";

    public final static String VIDEO_TABLE = "Video";

    public final static String VIDEO_SEQ_ID = "sequenceId";

    public final static String SCORE_TABLE = "Score";

    public final static String SCORE_SEQ_ID = "sequenceId";

    // Database creation sql statement
    public static final String DB_CREATE_PHOTO_TABLE =
            "create table if not exists " + FRAME_TABLE + "(" + FRAME_SEQ_ID + " integer, " + FRAME_VIDEO_ID +
                    " integer, " + FRAME_SEQ_INDEX + " integer, " + FRAME_FILE_PATH + " varchar(100), " + FRAME_LAT +
                    " real, " + FRAME_LON + " real, " + FRAME_ACCURACY + " float, " + FRAME_ORIENTATION +
                    " integer, " + "CONSTRAINT " + FRAME_UNIQUE_CONSTRAINT + " PRIMARY KEY (" + FRAME_SEQ_ID + "," +
                    FRAME_SEQ_INDEX + "," + FRAME_VIDEO_ID + ")" + "FOREIGN KEY (" + FRAME_SEQ_ID + ") REFERENCES " +
                    SEQUENCE_TABLE + " (" + SEQUENCE_ID + ")" + "FOREIGN KEY (" + FRAME_VIDEO_ID + ") REFERENCES " +
                    VIDEO_TABLE + " (" + VIDEO_INDEX + ")" + ");";

    public static final String DB_CREATE_VIDEO_TABLE =
            "create table if not exists " + VIDEO_TABLE + "(" + VIDEO_SEQ_ID + " integer, " + VIDEO_INDEX +
                    " integer, " + VIDEO_FILE_PATH + " varchar(100), " + VIDEO_FRAME_COUNT + " int, " + "CONSTRAINT " +
                    VIDEO_UNIQUE_CONSTRAINT + " PRIMARY KEY (" + VIDEO_SEQ_ID + "," + VIDEO_INDEX + ")" +
                    "FOREIGN KEY (" + VIDEO_SEQ_ID + ") REFERENCES " + SEQUENCE_TABLE + " (" + SEQUENCE_ID + ")" +
                    ");";

    public static final String DB_CREATE_SCORE_TABLE =
            "create table if not exists " + SCORE_TABLE + "(" + SCORE_SEQ_ID + " integer, " + SCORE_COVERAGE +
                    " integer, " + SCORE_OBD_COUNT + " integer, " + SCORE_COUNT + " integer, " + "CONSTRAINT " +
                    SCORE_UNIQUE_CONSTRAINT + " PRIMARY KEY (" + SCORE_SEQ_ID + "," + SCORE_COVERAGE + ")" +
                    "FOREIGN KEY (" + SCORE_SEQ_ID + ") REFERENCES " + SEQUENCE_TABLE + " (" + SEQUENCE_ID + ")," +
                    "CHECK (" + SCORE_COVERAGE + " > -2 AND " + SCORE_COVERAGE + " < 11)" + ");";

    public static final String DB_CREATE_SEQUENCE_TABLE =
            "create table if not exists " + SEQUENCE_TABLE + "(" + SEQUENCE_ID + " integer primary key," +
                    SEQUENCE_ONLINE_ID + " integer, " + SEQUENCE_LAT + " real, " + SEQUENCE_LON + " real, " +
                    SEQUENCE_PATH + " varchar(300), " + SEQUENCE_COUNT + " integer, " + SEQUENCE_ORIG_COUNT +
                    " integer, " + SEQUENCE_VIDEO_COUNT + " integer, " + SEQUENCE_PANO + " integer, " +
                    SEQUENCE_EXTERNAL + " integer, " + SEQUENCE_VERSION + " varchar(30), " + SEQUENCE_OBD +
                    " integer, " + SEQUENCE_STATUS + " integer," + SEQUENCE_SAFE + " integer " + ");";

    public SqlLiteOpenHelperTest(Context context, String databaseName, int databaseVersion) {
        super(context, databaseName, null, databaseVersion);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DB_CREATE_SEQUENCE_TABLE);
        database.execSQL(DB_CREATE_VIDEO_TABLE);
        database.execSQL(DB_CREATE_PHOTO_TABLE);
        database.execSQL(DB_CREATE_SCORE_TABLE);
    }

    // Method is called during an upgrade of the database,
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
