package com.telenav.osv.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 10/7/2015.
 */
class SequenceDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Sequences";

    private static final int DATABASE_VERSION = 4;

    // Database creation sql statement
    private static final String DB_CREATE_PHOTO_TABLE = "create table if not exists " + SequenceDB.FRAME_TABLE + "("
            + SequenceDB.FRAME_SEQ_ID + " integer, "
            + SequenceDB.FRAME_VIDEO_ID + " integer, "
            + SequenceDB.FRAME_SEQ_INDEX + " integer, "
            + SequenceDB.FRAME_FILE_PATH + " varchar(100), "
            + SequenceDB.FRAME_LAT + " real, "
            + SequenceDB.FRAME_LON + " real, "
            + SequenceDB.FRAME_ACCURACY + " float, "
            + SequenceDB.FRAME_ORIENTATION + " integer, "
            + "CONSTRAINT " + SequenceDB.FRAME_UNIQUE_CONSTRAINT + " PRIMARY KEY (" + SequenceDB.FRAME_SEQ_ID + "," + SequenceDB.FRAME_SEQ_INDEX + ","
            + SequenceDB.FRAME_VIDEO_ID + ")"
            + "FOREIGN KEY (" + SequenceDB.FRAME_SEQ_ID + ") REFERENCES " + SequenceDB.SEQUENCE_TABLE + " (" + SequenceDB.SEQUENCE_ID + ")"
            + "FOREIGN KEY (" + SequenceDB.FRAME_VIDEO_ID + ") REFERENCES " + SequenceDB.VIDEO_TABLE + " (" + SequenceDB.VIDEO_INDEX + ")"
            + ");";

    private static final String DB_CREATE_VIDEO_TABLE = "create table if not exists " + SequenceDB.VIDEO_TABLE + "("
            + SequenceDB.VIDEO_SEQ_ID + " integer, "
            + SequenceDB.VIDEO_INDEX + " integer, "
            + SequenceDB.VIDEO_FILE_PATH + " varchar(100), "
            + SequenceDB.VIDEO_FRAME_COUNT + " int, "
            + "CONSTRAINT " + SequenceDB.VIDEO_UNIQUE_CONSTRAINT + " PRIMARY KEY (" + SequenceDB.VIDEO_SEQ_ID + "," + SequenceDB.VIDEO_INDEX + ")"
            + "FOREIGN KEY (" + SequenceDB.VIDEO_SEQ_ID + ") REFERENCES " + SequenceDB.SEQUENCE_TABLE + " (" + SequenceDB.SEQUENCE_ID + ")"
            + ");";

    private static final String DB_CREATE_SCORE_TABLE = "create table if not exists " + SequenceDB.SCORE_TABLE + "("
            + SequenceDB.SCORE_SEQ_ID + " integer, "
            + SequenceDB.SCORE_COVERAGE + " integer, "
            + SequenceDB.SCORE_OBD_COUNT + " integer, "
            + SequenceDB.SCORE_COUNT + " integer, "
            + "CONSTRAINT " + SequenceDB.SCORE_UNIQUE_CONSTRAINT + " PRIMARY KEY (" + SequenceDB.SCORE_SEQ_ID + "," + SequenceDB.SCORE_COVERAGE + ")"
            + "FOREIGN KEY (" + SequenceDB.SCORE_SEQ_ID + ") REFERENCES " + SequenceDB.SEQUENCE_TABLE + " (" + SequenceDB.SEQUENCE_ID + "),"
            + "CHECK (" + SequenceDB.SCORE_COVERAGE + " > -2 AND " + SequenceDB.SCORE_COVERAGE + " < 11)"
            + ");";

    private static final String DB_CREATE_SEQUENCE_TABLE = "create table if not exists " + SequenceDB.SEQUENCE_TABLE + "("
            + SequenceDB.SEQUENCE_ID + " integer primary key,"
            + SequenceDB.SEQUENCE_ONLINE_ID + " integer, "
            + SequenceDB.SEQUENCE_LAT + " real, "
            + SequenceDB.SEQUENCE_LON + " real, "
            + SequenceDB.SEQUENCE_PATH + " varchar(300), "
            + SequenceDB.SEQUENCE_COUNT + " integer, "
            + SequenceDB.SEQUENCE_ORIG_COUNT + " integer, "
            + SequenceDB.SEQUENCE_VIDEO_COUNT + " integer, "
            + SequenceDB.SEQUENCE_PANO + " integer, "
            + SequenceDB.SEQUENCE_EXTERNAL + " integer, "
            + SequenceDB.SEQUENCE_VERSION + " varchar(30), "
            + SequenceDB.SEQUENCE_OBD + " integer, "
            + SequenceDB.SEQUENCE_STATUS + " integer,"
            + SequenceDB.SEQUENCE_SAFE + " integer "
            + ");";

    private static final String TAG = "SequenceDBHelper";

    private Context mContext;

    public SequenceDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
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
        Log.d(TAG, "onUpgrade: from v" + oldVersion + " to v" + newVersion);
        if (oldVersion < 2) {
            database.execSQL(DB_CREATE_SCORE_TABLE);
        }
        if (oldVersion < 3) {
            database.execSQL("ALTER TABLE " + SequenceDB.SEQUENCE_TABLE + " ADD " + SequenceDB.SEQUENCE_SAFE + " INTEGER");
        }
        if (oldVersion < 4) {
            database.execSQL("ALTER TABLE " + SequenceDB.SCORE_TABLE + " RENAME TO " + SequenceDB.SCORE_TABLE + "1");
            database.execSQL(DB_CREATE_SCORE_TABLE);
            database.execSQL("INSERT INTO " + SequenceDB.SCORE_TABLE + " (" + SequenceDB.SCORE_SEQ_ID + ", " + SequenceDB.SCORE_COVERAGE + ", " + SequenceDB.SCORE_OBD_COUNT + "," +
                    " " + SequenceDB.SCORE_COUNT + ") SELECT " + SequenceDB.SCORE_SEQ_ID + ", " + SequenceDB.SCORE_COVERAGE + ", " + SequenceDB.SCORE_OBD_COUNT + ", " +
                    SequenceDB.SCORE_COUNT + " FROM " + SequenceDB.SCORE_TABLE + "1" + ";");
            database.execSQL("DROP TABLE " + SequenceDB.SCORE_TABLE + "1");
        }
    }
//
//    private void moveFolders() {
//        try {
//            if (Utils.checkSDCard(mContext)) {
//                boolean external = ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE);
//                OSVFile osv = Utils.generateOSVFolder(mContext);
//                if (osv.listFiles().length > 0) {
////                                                       files     /   com.tnav.osv   /   data      /     Android    /   sdcard1
//                    OSVFile osvCopy = new OSVFile(osv.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(), "OSV_BACKUP");
//                    boolean success = osv.renameTo(osvCopy);
//                    if (!success) {
//                        OSVFile osvCopy2 = new OSVFile(osv.getParentFile(), "OSV_BACKUP");
//                        osv.renameTo(osvCopy2);
//                    }
//                    if (external) {
//                        OSVApplication.sOSVBackupExt = osvCopy.getAbsolutePath();
//                    } else {
//                        OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
//                    }
//                }
//                ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE, !external);
//                external = !external;
//                osv = Utils.generateOSVFolder(mContext);
//                if (osv.listFiles().length > 0) {
////                                                       files     /   com.tnav.osv   /   data      /     Android    /   sdcard2
//                    OSVFile osvCopy = new OSVFile(osv.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(), "OSV_BACKUP");
//                    boolean success = osv.renameTo(osvCopy);
//                    if (!success) {
//                        OSVFile osvCopy2 = new OSVFile(osv.getParentFile(), "OSV_BACKUP");
//                        osv.renameTo(osvCopy2);
//                    }
//                    if (external) {
//                        OSVApplication.sOSVBackupExt = osvCopy.getAbsolutePath();
//                    } else {
//                        OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
//                    }
//                }
//                //reset preference
//                ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE, external);
//            } else {
//                OSVFile osv = Utils.generateOSVFolder(mContext);
//                if (osv.listFiles().length > 0) {
////                                                       files     /   com.tnav.osv   /   data      /     Android    /   sdcard
//                    OSVFile osvCopy = new OSVFile(osv.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(), "OSV_BACKUP");
//                    osv.renameTo(osvCopy);
//                    OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
//                }
//            }
//        } catch (Exception e) {
//            try {
//                if (Utils.checkSDCard(mContext)) {
//                    boolean external = ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE);
//                    OSVFile osv = Utils.generateOSVFolder(mContext);
//                    if (osv.listFiles().length > 0) {
////                                                       files     /   com.tnav.osv   /   data      /     Android    /   sdcard1
//                        OSVFile osvCopy = new OSVFile(osv.getParentFile(), "OSV_BACKUP");
//                        osv.renameTo(osvCopy);
//                        if (external) {
//                            OSVApplication.sOSVBackupExt = osvCopy.getAbsolutePath();
//                        } else {
//                            OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
//                        }
//                    }
//
//                    ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE, !external);
//                    osv = Utils.generateOSVFolder(mContext);
//                    if (osv.listFiles().length > 0) {
//                        OSVFile osvCopy = new OSVFile(osv.getParentFile(), "OSV_BACKUP");
//                        osv.renameTo(osvCopy);
//                        if (!external) {
//                            OSVApplication.sOSVBackupExt = osvCopy.getAbsolutePath();
//                        } else {
//                            OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
//                        }
//                    }
//                    ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE, external);
//                } else {
//                    OSVFile osv = Utils.generateOSVFolder(mContext);
//                    if (osv.listFiles().length > 0) {
//                        OSVFile osvCopy = new OSVFile(osv.getParentFile(), "OSV_BACKUP");
//                        osv.renameTo(osvCopy);
//                        OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
//                    }
//                }
//            } catch (Exception ex) {
//                Log.e(TAG, "onUpgrade: " + Log.getStackTraceString(e));
//            }
//        }
//    }
}