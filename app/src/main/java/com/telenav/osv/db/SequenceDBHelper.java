package com.telenav.osv.db;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.crashlytics.android.Crashlytics;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import io.fabric.sdk.android.Fabric;

/**
 * Created by Kalman on 10/7/2015.
 */
public class SequenceDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Sequences";

    private static final int DATABASE_VERSION = 1;

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
            + SequenceDB.SEQUENCE_STATUS + " integer"
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
    }

    // Method is called during an upgrade of the database,
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: from v" + oldVersion + " to v" + newVersion);
//        if (oldVersion < 21) {
//            try {
//                moveFolders();
//            } catch (Exception e){
//                Log.d(TAG, "onUpgrade: " + Log.getStackTraceString(e));
//            }
//            try {
//                database.execSQL("DROP TABLE " + SequenceDB.FRAME_TABLE + ";");
//            } catch (Exception e) {
//                e.printStackTrace();
//                if (Fabric.isInitialized()) {
//                    Crashlytics.logException(e);
//                }
//            }
//            try {
//                database.execSQL("DROP TABLE " + SequenceDB.VIDEO_TABLE + ";");
//            } catch (Exception e) {
//                e.printStackTrace();
//                if (Fabric.isInitialized()) {
//                    Crashlytics.logException(e);
//                }
//            }
//            try {
//                database.execSQL("DROP TABLE " + SequenceDB.SEQUENCE_TABLE + ";");
//            } catch (Exception e) {
//                e.printStackTrace();
//                if (Fabric.isInitialized()) {
//                    Crashlytics.logException(e);
//                }
//            }
//            database.execSQL(DB_CREATE_SEQUENCE_TABLE);
//            database.execSQL(DB_CREATE_VIDEO_TABLE);
//            database.execSQL(DB_CREATE_PHOTO_TABLE);
//        }
    }

    private void moveFolders() {
        try {
            if (Utils.checkSDCard(mContext)) {
                boolean external = ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE);
                OSVFile osv = Utils.generateOSVFolder(mContext);
                if (osv.listFiles().length > 0) {
//                                                       files     /   com.tnav.osv   /   data      /     Android    /   sdcard1
                    OSVFile osvCopy = new OSVFile(osv.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(), "OSV_BACKUP");
                    boolean success = osv.renameTo(osvCopy);
                    if (!success) {
                        OSVFile osvCopy2 = new OSVFile(osv.getParentFile(), "OSV_BACKUP");
                        osv.renameTo(osvCopy2);
                    }
                    if (external) {
                        OSVApplication.sOSVBackupExt = osvCopy.getAbsolutePath();
                    } else {
                        OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
                    }
                }
                ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE, !external);
                external = !external;
                osv = Utils.generateOSVFolder(mContext);
                if (osv.listFiles().length > 0) {
//                                                       files     /   com.tnav.osv   /   data      /     Android    /   sdcard2
                    OSVFile osvCopy = new OSVFile(osv.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(), "OSV_BACKUP");
                    boolean success = osv.renameTo(osvCopy);
                    if (!success) {
                        OSVFile osvCopy2 = new OSVFile(osv.getParentFile(), "OSV_BACKUP");
                        osv.renameTo(osvCopy2);
                    }
                    if (external) {
                        OSVApplication.sOSVBackupExt = osvCopy.getAbsolutePath();
                    } else {
                        OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
                    }
                }
                //reset preference
                ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE, external);
            } else {
                OSVFile osv = Utils.generateOSVFolder(mContext);
                if (osv.listFiles().length > 0) {
//                                                       files     /   com.tnav.osv   /   data      /     Android    /   sdcard
                    OSVFile osvCopy = new OSVFile(osv.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(), "OSV_BACKUP");
                    osv.renameTo(osvCopy);
                    OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            try {
                if (Utils.checkSDCard(mContext)) {
                    boolean external = ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE);
                    OSVFile osv = Utils.generateOSVFolder(mContext);
                    if (osv.listFiles().length > 0) {
//                                                       files     /   com.tnav.osv   /   data      /     Android    /   sdcard1
                        OSVFile osvCopy = new OSVFile(osv.getParentFile(), "OSV_BACKUP");
                        osv.renameTo(osvCopy);
                        if (external) {
                            OSVApplication.sOSVBackupExt = osvCopy.getAbsolutePath();
                        } else {
                            OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
                        }
                    }

                    ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE, !external);
                    osv = Utils.generateOSVFolder(mContext);
                    if (osv.listFiles().length > 0) {
                        OSVFile osvCopy = new OSVFile(osv.getParentFile(), "OSV_BACKUP");
                        osv.renameTo(osvCopy);
                        if (!external) {
                            OSVApplication.sOSVBackupExt = osvCopy.getAbsolutePath();
                        } else {
                            OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
                        }
                    }
                    ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE, external);
                } else {
                    OSVFile osv = Utils.generateOSVFolder(mContext);
                    if (osv.listFiles().length > 0) {
                        OSVFile osvCopy = new OSVFile(osv.getParentFile(), "OSV_BACKUP");
                        osv.renameTo(osvCopy);
                        OSVApplication.sOSVBackup = osvCopy.getAbsolutePath();
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "onUpgrade: " + Log.getStackTraceString(e));
            }
        }
    }
}