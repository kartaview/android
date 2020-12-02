package com.telenav.osv.data.database.migration;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Custom implementation of Room {@code Migration} class from version 2 to 3.
 * <p>
 * Added 'safe' field into the 'Sequence' table which shows if a sequence is either video/jpeg format in photo compressions. Safe in this case refers to jpeg format.
 * @author horatiuf
 */
public class Migration2To3 extends Migration {

    /**
     * The start version for the migration.
     */
    private static final int START_VERSION = 2;

    /**
     * The end version for the migration.
     */
    private static final int END_VERSION = 3;

    /**
     * Creates a new migration between {@code startVersion} and {@code endVersion}.
     */
    public Migration2To3() {
        super(START_VERSION, END_VERSION);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE 'Sequence' ADD 'safe' INTEGER");
    }
}
