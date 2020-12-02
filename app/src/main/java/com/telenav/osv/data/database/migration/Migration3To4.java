package com.telenav.osv.data.database.migration;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Custom implementation of Room {@code Migration} class from version 3 to 4.
 * <p>
 * Alters the 'Score' table and move all the data from the old 'Score' table to the new 'Score' table.
 * @author horatiuf
 */
public class Migration3To4 extends Migration {

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
     * The start version for the migration.
     */
    private static final int START_VERSION = 3;

    /**
     * The end version for the migration.
     */
    private static final int END_VERSION = 4;

    /**
     * Creates a new migration between {@code startVersion} and {@code endVersion}.
     */
    public Migration3To4() {
        super(START_VERSION, END_VERSION);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE 'Score' RENAME TO 'Score1'");
        database.execSQL(DB_CREATE_SCORE_TABLE);
        database.execSQL("INSERT INTO 'Score'('sequenceId', 'coverage', 'obdCount' , 'count') " +
                "SELECT 'sequenceId', 'coverage' , 'obdCount', 'count' " +
                "FROM 'Score1';");
        database.execSQL("DROP TABLE 'Score1'");

    }
}
