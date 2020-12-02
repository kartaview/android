package com.telenav.osv.data.database.migration;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Custom implementation of Room {@code Migration} class from version 2 to 3.
 * <p>
 * Added 'safe' field into the 'Sequence' table which shows if a sequence is either video/jpeg format in photo compressions. Safe in this case refers to jpeg format.
 * @author horatiuf
 */
public class Migration1To2 extends Migration {

    /**
     * The start version for the migration.
     */
    private static final int START_VERSION = 1;

    /**
     * The end version for the migration.
     */
    private static final int END_VERSION = 2;

    /**
     * Sql statement in order to create the initial score table, pre-room database schema migration.
     */
    private static final String DB_CREATE_SCORE_TABLE =
            "create table if not exists 'Score'('sequenceId' integer, 'coverage'" +
                    " integer, 'obdCount' integer, 'count' integer, " + "CONSTRAINT " +
                    "'scoreUniqueConstraint' PRIMARY KEY ('sequenceId', 'coverage')" +
                    "FOREIGN KEY ('sequenceId') REFERENCES 'Sequence' ('sequenceId')," +
                    "CHECK ('coverage' > -2 AND 'coverage' < 11));";

    /**
     * Creates a new migration between {@code startVersion} and {@code endVersion}.
     */
    public Migration1To2() {
        super(START_VERSION, END_VERSION);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        database.execSQL(DB_CREATE_SCORE_TABLE);
    }
}
