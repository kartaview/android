package com.telenav.osv.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * The DataBase of the OSV sequences
 * Created by Kalman on 10/7/2015.
 */
public class SequenceDB {
    /**
     * Name of the only table, containing photo details
     */
    public final static String FRAME_TABLE = "Frame";

    public final static String FRAME_SEQ_ID = "sequenceId";

    public final static String FRAME_VIDEO_ID = "videoId";

    public final static String FRAME_SEQ_INDEX = "sequenceIndex";

    public final static String FRAME_FILE_PATH = "filePath";

    public final static String FRAME_LAT = "latitude";

    public final static String FRAME_LON = "longitude";

    public final static String FRAME_ACCURACY = "accuracy";

    public final static String FRAME_ORIENTATION = "orientation";

    public static final String FRAME_UNIQUE_CONSTRAINT = "uniqueConstraint";

    public static final String VIDEO_UNIQUE_CONSTRAINT = "uniqueConstraint";

//  -------------------------------------------------------------

    public static final String SEQUENCE_TABLE = "Sequence";

    public final static String SEQUENCE_ID = "sequenceId";

    public final static String SEQUENCE_ONLINE_ID = "onlineSequenceId";

    public final static String SEQUENCE_LAT = "latitude";

    public final static String SEQUENCE_LON = "longitude";

    public final static String SEQUENCE_PATH = "thumb";

    public final static String SEQUENCE_COUNT = "count";

    public final static String SEQUENCE_ORIG_COUNT = "origCount";

    public final static String SEQUENCE_VIDEO_COUNT = "videoCount";

    public final static String SEQUENCE_PANO = "panorama";

    public final static String SEQUENCE_EXTERNAL = "external";

    public final static String SEQUENCE_VERSION = "version";

    public static final String SEQUENCE_OBD = "obd";

    public static final String SEQUENCE_STATUS = "status";

//  ---------------------------------------------------------------

    public final static String VIDEO_TABLE = "Video";

    public final static String VIDEO_SEQ_ID = "sequenceId";

    public final static String VIDEO_INDEX = "videoIndex";

    public final static String VIDEO_FILE_PATH = "filePath";

    public final static String VIDEO_FRAME_COUNT = "count";

    private static final String TAG = "SequenceDB";

    public static SequenceDB instance;

    private SequenceDBHelper dbHelper;

    private SQLiteDatabase database;

    /**
     * @param context
     */
    public SequenceDB(Context context) {
        dbHelper = new SequenceDBHelper(context);
        try {
            database = dbHelper.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            Log.d(TAG, "SequenceDB: " + e.getLocalizedMessage());
        }
        SequenceDB.instance = this;
    }

    public long insertSequence(int seqId, double lat, double lon, String path, boolean pano, boolean external, String version, boolean obd) {
        ContentValues values = new ContentValues();
        values.put(SEQUENCE_ID, seqId);
        values.put(SEQUENCE_ONLINE_ID, -1);
        values.put(SEQUENCE_LAT, lat);
        values.put(SEQUENCE_LON, lon);
        values.put(SEQUENCE_PATH, path);
        values.put(SEQUENCE_COUNT, -1);
        values.put(SEQUENCE_ORIG_COUNT, -1);
        values.put(SEQUENCE_VIDEO_COUNT, -1);
        values.put(SEQUENCE_PANO, pano);
        values.put(SEQUENCE_EXTERNAL, external);
        values.put(SEQUENCE_VERSION, version);
        values.put(SEQUENCE_OBD, obd);
        values.put(SEQUENCE_STATUS, Sequence.STATUS_NEW);

        return database.insertOrThrow(SEQUENCE_TABLE, null, values);
    }

    public long insertVideo(int seqId,int videoIndex, String filePath){
        ContentValues values = new ContentValues();
        values.put(VIDEO_SEQ_ID, seqId);
        values.put(VIDEO_INDEX, videoIndex);
        values.put(VIDEO_FILE_PATH, filePath);
        values.put(VIDEO_FRAME_COUNT, -1);
        return database.insertOrThrow(VIDEO_TABLE, null, values);
    }

    public long insertVideoIfNotAdded(int seqId, int videoIndex, String filePath) {
        if (getNumberOfVideos(seqId, videoIndex) <= 0) {
            ContentValues values = new ContentValues();
            values.put(VIDEO_SEQ_ID, seqId);
            values.put(VIDEO_INDEX, videoIndex);
            values.put(VIDEO_FILE_PATH, filePath);
            values.put(VIDEO_FRAME_COUNT, -1);
            return database.insert(VIDEO_TABLE, null, values);
        }
        return 0;
    }

    public long insertPhoto(int seqId,int videoIndex, int seqIndex, String filePath, double lat, double lon, float accuracy, int orientation) {
        ContentValues values = new ContentValues();
        values.put(FRAME_SEQ_ID, seqId);
        values.put(FRAME_VIDEO_ID, videoIndex);
        values.put(FRAME_SEQ_INDEX, seqIndex);
        values.put(FRAME_FILE_PATH, filePath);
        values.put(FRAME_LAT, lat);
        values.put(FRAME_LON, lon);
        values.put(FRAME_ACCURACY, accuracy);
        values.put(FRAME_ORIENTATION, orientation);
        return database.insertOrThrow(FRAME_TABLE, null, values);
    }

    public Cursor getAllSequences() {
        try {
            String[] cols = new String[]{SEQUENCE_ID, SEQUENCE_ONLINE_ID, SEQUENCE_LAT, SEQUENCE_LON, SEQUENCE_PATH, SEQUENCE_COUNT, SEQUENCE_ORIG_COUNT, SEQUENCE_VIDEO_COUNT, SEQUENCE_PANO,
                    SEQUENCE_EXTERNAL, SEQUENCE_VERSION, SEQUENCE_OBD, SEQUENCE_STATUS};
            Cursor mCursor = database.query(true, SEQUENCE_TABLE, cols, null
                    , null, SEQUENCE_ID, null, null, null);
            if (mCursor != null && mCursor.getCount() > 0) {
                mCursor.moveToFirst();
            }
            return mCursor;
        } catch (SQLiteException e) {
            if (e.getLocalizedMessage().contains("no such column") && e.getLocalizedMessage().contains("status")) {
                database.execSQL("ALTER TABLE " + SequenceDB.SEQUENCE_TABLE + " ADD COLUMN " + SequenceDB.SEQUENCE_STATUS + " int");
            }
        }
        //retry
        String[] cols = new String[]{SEQUENCE_ID, SEQUENCE_ONLINE_ID, SEQUENCE_LAT, SEQUENCE_LON, SEQUENCE_PATH, SEQUENCE_COUNT, SEQUENCE_ORIG_COUNT, SEQUENCE_VIDEO_COUNT, SEQUENCE_PANO,
                SEQUENCE_EXTERNAL, SEQUENCE_VERSION, SEQUENCE_OBD, SEQUENCE_STATUS};
        Cursor mCursor = database.query(true, SEQUENCE_TABLE, cols, null
                , null, SEQUENCE_ID, null, null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public boolean checkSequenceExists(int sequenceId) {
        return DatabaseUtils.queryNumEntries(database, SEQUENCE_TABLE, SEQUENCE_ID + " = ?", new String[]{"" + sequenceId}) > 0;
    }

    public Cursor getFrames(int sequenceId) {
        String[] cols = new String[]{FRAME_SEQ_ID, FRAME_VIDEO_ID, FRAME_VIDEO_ID, FRAME_SEQ_INDEX, FRAME_FILE_PATH, FRAME_LAT, FRAME_LON, FRAME_ACCURACY, FRAME_ORIENTATION};
        Cursor mCursor = database.query(true, FRAME_TABLE, cols, FRAME_SEQ_ID + " = ?"
                , new String[]{"" + sequenceId}, null, null, null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

    public Cursor getVideos(int sequenceId) {
        String[] cols = new String[]{VIDEO_SEQ_ID, VIDEO_INDEX, VIDEO_FILE_PATH, VIDEO_FRAME_COUNT};
        Cursor mCursor = database.query(true, VIDEO_TABLE, cols, VIDEO_SEQ_ID + " = ?"
                , new String[]{"" + sequenceId}, null, null, null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

    public Cursor getAllFrames() {
        String[] cols = new String[]{FRAME_SEQ_ID, FRAME_VIDEO_ID, FRAME_SEQ_INDEX, FRAME_FILE_PATH, FRAME_LAT, FRAME_LON, FRAME_ACCURACY, FRAME_ORIENTATION};
        Cursor mCursor = database.query(true, FRAME_TABLE, cols, null
                , null, null, null, null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

    private Cursor getAllVideos() {
        String[] cols = new String[]{VIDEO_SEQ_ID, VIDEO_FILE_PATH, VIDEO_FRAME_COUNT, VIDEO_INDEX};
        Cursor mCursor = database.query(true, VIDEO_TABLE, cols, null
                , null, null, null, null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

    public long getNumberOfFrames() {
        return DatabaseUtils.queryNumEntries(database, FRAME_TABLE);
    }

    public long getNumberOfFrames(int localSequenceId) {
        return DatabaseUtils.queryNumEntries(database, FRAME_TABLE, FRAME_SEQ_ID + "=?", new String[]{"" + localSequenceId});
    }

    public long getNumberOfVideos(int localSequenceId) {
        return DatabaseUtils.queryNumEntries(database, VIDEO_TABLE, VIDEO_SEQ_ID + "=?", new String[]{"" + localSequenceId});
    }

    private long getNumberOfVideos(int localSequenceId, int videoIndex) {
        return DatabaseUtils.queryNumEntries(database, VIDEO_TABLE, VIDEO_SEQ_ID + "=? AND " + VIDEO_INDEX + "=?", new String[]{"" + localSequenceId, "" + videoIndex});
    }

    public int getOriginalFrameCount(int sequenceId) {
        String[] cols = new String[]{SEQUENCE_ORIG_COUNT};
        Cursor mCursor = database.query(true, SEQUENCE_TABLE, cols, SEQUENCE_ID + " = ?"
                , new String[]{"" + sequenceId}, null, null, null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            mCursor.moveToFirst();
            int res = mCursor.getInt(mCursor.getColumnIndex(SEQUENCE_ORIG_COUNT));
            mCursor.close();
            return res;
        }
        if (mCursor != null){
            mCursor.close();
        }
        return 0;
    }

    public int updateSequenceOnlineId(int sequenceId, int onlineSequenceId) {
        ContentValues cv = new ContentValues();
        cv.put(SEQUENCE_ONLINE_ID, onlineSequenceId);
        return database.update(SEQUENCE_TABLE, cv, SEQUENCE_ID + " = ?", new String[]{"" + sequenceId});
    }

    public int updateSequenceLocation(int sequenceId, double lat, double lon) {
        ContentValues cv = new ContentValues();
        cv.put(SEQUENCE_LAT, lat);
        cv.put(SEQUENCE_LON, lon);
        return database.update(SEQUENCE_TABLE, cv, SEQUENCE_ID + " = ?", new String[]{"" + sequenceId});
    }

    public int updateSequenceFrameCount(int sequenceId) {
        ContentValues cv = new ContentValues();
        int imageCount = (int) getNumberOfFrames(sequenceId);
        int originalCount = getOriginalFrameCount(sequenceId);
        cv.put(SEQUENCE_COUNT, imageCount);
        if (originalCount < imageCount) {
            cv.put(SEQUENCE_ORIG_COUNT, imageCount);
        } else {
            if (imageCount != originalCount) {
                cv.put(SEQUENCE_STATUS, Sequence.STATUS_INTERRUPTED);
            } else if (imageCount != 0) {
                cv.put(SEQUENCE_STATUS, Sequence.STATUS_NEW);
            }
        }
        return database.update(SEQUENCE_TABLE, cv, SEQUENCE_ID + " = ?", new String[]{"" + sequenceId});
    }

    private int deleteSequenceRecord(int sequenceId) {
        return database.delete(SEQUENCE_TABLE, SEQUENCE_ID + " = ?", new String[]{"" + sequenceId});
    }
    private int deleteVideoRecord(int sequenceId) {
        return database.delete(VIDEO_TABLE, VIDEO_SEQ_ID + " = ?", new String[]{"" + sequenceId});
    }

    /**
     * @param sequenceIdLocal
     * @param videoIndex startIndex of the video
     * @return
     */
    public int deleteVideo(int sequenceIdLocal, int videoIndex) {
        int res = database.delete(FRAME_TABLE, FRAME_SEQ_ID + " = ? AND " + FRAME_VIDEO_ID + " = ?"
                , new String[]{"" + sequenceIdLocal, "" + videoIndex});
        res = res + database.delete(VIDEO_TABLE, VIDEO_SEQ_ID + " = ? AND " + VIDEO_INDEX + " = ?"
                , new String[]{"" + sequenceIdLocal, "" + videoIndex});
        return res;
    }

    public int resetOnlineSequenceId(int onlineSequenceId) {
        ContentValues cv = new ContentValues();
        cv.put(SEQUENCE_ONLINE_ID, "-1");
        return database.update(SEQUENCE_TABLE, cv, SEQUENCE_ONLINE_ID + " = ?", new String[]{"" + onlineSequenceId});
    }

    public int deleteRecords(int sequenceIdLocal) {
        int res;
        res = database.delete(FRAME_TABLE, FRAME_SEQ_ID + " = ?", new String[]{"" + sequenceIdLocal});
        res = res + database.delete(VIDEO_TABLE, VIDEO_SEQ_ID + " = ?", new String[]{"" + sequenceIdLocal});
        res = res + database.delete(SEQUENCE_TABLE, SEQUENCE_ID + " = ?", new String[]{"" + sequenceIdLocal});
        return res;
    }

    public Sequence createNewSequence(Context context, double lat, double lon, boolean pano, boolean external, String version, boolean obd) {
        OSVFile osv = Utils.generateOSVFolder(context);
        int i = 0;
        while (true) {
            OSVFile file = new OSVFile(osv.getPath(), "/SEQ_" + i);
            if (!file.exists()) {
                if (!checkSequenceExists(i)) {
                    insertSequence(i, lat, lon, file.getPath(), pano, external, version, obd);
                    file.mkdir();
                    return new Sequence(file);

                }
            }
            i++;
        }
    }

    /**
     * deletes the image, deletes the parent folder if it remains empty
     * @param video
     * @param sequenceIdLocal
     * @param sequenceIndex
     * @return true if parent folder is also deleted
     */
    public void deleteVideo(OSVFile video, int sequenceIdLocal, int sequenceIndex) {
        int result = deleteVideo(sequenceIdLocal, sequenceIndex);
        if (result == 0) {
            Log.d(TAG, "deleteVideo: No records deleted, no records found with sequenceId: " + sequenceIdLocal + " and sequenceIndex: " + sequenceIndex);
        }
        if (!video.delete()) {
            Log.d(TAG, "deleteVideo: delete unsuccessful: " + video.getName());
        }
    }

    public void consistencyCheck(Context context) {
        try {
            Cursor cur = getAllSequences();
            cur.close();
        } catch (SQLiteException e) {
            if (e.getLocalizedMessage().contains("no such column") && e.getLocalizedMessage().contains("status")) {
                database.execSQL("ALTER TABLE " + SequenceDB.SEQUENCE_TABLE + " ADD COLUMN " + SequenceDB.SEQUENCE_STATUS + " int");
            }
        }
        Cursor sequences = getAllSequences();
        while (sequences != null && sequences.getCount() > 0 && !sequences.isAfterLast()) {
            int id = sequences.getInt(sequences.getColumnIndex(SEQUENCE_ID));
            updateSequenceFrameCount(id);
            if (getNumberOfFrames(id) <= 0) {
                int res = deleteSequenceRecord(id);
                res = res + deleteVideoRecord(id);
                Log.d(TAG, "consistencyCheck: deleted sequence with 0 items, id = " + sequences.getInt(sequences.getColumnIndex(SEQUENCE_ID)) + ", result = " + res);
            }
            sequences.moveToNext();
        }
        if (sequences != null) {
            sequences.close();
        }
        Cursor cursor = getAllVideos();
        if (cursor.getCount() > 0) {
            while (!cursor.isAfterLast()) {
                String path = cursor.getString(cursor.getColumnIndex(VIDEO_FILE_PATH));
                OSVFile file = new OSVFile(path);
                int sequenceId = cursor.getInt(cursor.getColumnIndex(VIDEO_SEQ_ID));
                int index = cursor.getInt(cursor.getColumnIndex(VIDEO_INDEX));
//                double lat = cursor.getDouble(cursor.getColumnIndex(VIDEO_FRAME_COUNT));
//                double lon = cursor.getDouble(cursor.getColumnIndex(VIDEO_LON));
//                float accuracy = cursor.getFloat(cursor.getColumnIndex(VIDEO_ACCURACY));
//                int orientation = cursor.getInt(cursor.getColumnIndex(VIDEO_ORIENTATION));
////                int fileindex = ImageFile.getImageIndex(file);
//                if (file.exists() && startIndex != fileindex && fileindex != -1) {
//                    deleteVideo(sequenceId, startIndex);
//                    try {
//                        insertPhoto(sequenceId, fileindex, file.getPath(), lat, lon, accuracy, orientation);
//                    } catch (Exception e) {
//                        Log.d(TAG, "consistencyCheck: " + Log.getStackTraceString(e));
//                    }
//                }
                if (file.exists() && Utils.fileSize(file) < 1000){
                    file.delete();
                }
                if (!file.exists()) {
                    deleteVideo(sequenceId, index);
                    if (getNumberOfFrames(sequenceId) <= 0) {
                        deleteSequenceRecord(sequenceId);
                    }
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        OSVFile osv = Utils.generateOSVFolder(context);
        for (OSVFile sequence : osv.listFiles()) {
            int id = Sequence.getSequenceId(sequence);
            if (getNumberOfFrames(id) <= 0 && getNumberOfVideos(id)<=0) {
                boolean result = sequence.delete();
            }
        }
        interruptUploading();
        fixStatuses();
    }

    public void deleteHistory() {
        Cursor sequences = getAllSequences();
        while (sequences != null && sequences.getCount() > 0 && !sequences.isAfterLast()) {
            int id = sequences.getInt(sequences.getColumnIndex(SEQUENCE_ID));
            updateSequenceFrameCount(id);
            if (getNumberOfFrames(id) <= 0) {
                int res = deleteSequenceRecord(id);
                res = res + deleteVideoRecord(id);
                Log.d(TAG, "deleteHistory: deleted sequence with 0 items, id = " + sequences.getInt(sequences.getColumnIndex(SEQUENCE_ID)) + ", result = " + res);
            }
            sequences.moveToNext();
        }
        if (sequences != null) {
            sequences.close();
        }
        fixStatuses();
    }

    public boolean isOBDSequence(int sequenceId) {
        return DatabaseUtils.queryNumEntries(database, SEQUENCE_TABLE, SEQUENCE_ID + " = ? AND " + SEQUENCE_OBD + " > 0", new String[]{"" + sequenceId}) > 0;
    }

    public int getOnlineId(int sequenceId) {
        String[] cols = new String[]{SEQUENCE_ID, SEQUENCE_ONLINE_ID};
        Cursor mCursor = database.query(true, SEQUENCE_TABLE, cols, SEQUENCE_ID + " = ?"
                , new String[]{"" + sequenceId}, null, null, null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            mCursor.moveToFirst();
            int val = mCursor.getInt(mCursor.getColumnIndex(SEQUENCE_ONLINE_ID));
            mCursor.close();
            return val;
        }
        if (mCursor != null){
            mCursor.close();
        }
        return -1;
    }

    public void setOBDForSequence(int sequenceId, boolean obd) {
        ContentValues cv = new ContentValues();
        cv.put(SEQUENCE_OBD, obd);
        database.update(SEQUENCE_TABLE, cv, SEQUENCE_ID + " = ?", new String[]{"" + sequenceId});
    }

    public int getStatus(int sequenceId) {
        String[] cols = new String[]{SEQUENCE_STATUS};
        Cursor mCursor = database.query(true, SEQUENCE_TABLE, cols, SEQUENCE_ID + " = ?"
                , new String[]{"" + sequenceId}, null, null, null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            mCursor.moveToFirst();
            int res = mCursor.getInt(mCursor.getColumnIndex(SEQUENCE_STATUS));
            mCursor.close();
            return res;
        }
        if (mCursor != null){
            mCursor.close();
        }
        return -1;
    }

    public void interruptUploading() {
        ContentValues cv = new ContentValues();
        cv.put(SEQUENCE_STATUS, Sequence.STATUS_INTERRUPTED);
        int res = database.update(SEQUENCE_TABLE, cv, SEQUENCE_STATUS + " = ?", new String[]{"" + Sequence.STATUS_UPLOADING});
        Log.d(TAG, "interruptUploading: number of reset UPLOADING statuses = " + res);
        cv.clear();
        cv.put(SEQUENCE_STATUS, Sequence.STATUS_NEW);
        res = database.update(SEQUENCE_TABLE, cv, SEQUENCE_STATUS + " = ?", new String[]{"" + Sequence.STATUS_INDEXING});
        Log.d(TAG, "interruptUploading: number of reset INDEXING statuses = " + res);
    }

    public void setStatus(int sequenceId, int status) {
        ContentValues cv = new ContentValues();
        cv.put(SEQUENCE_STATUS, status);
        database.update(SEQUENCE_TABLE, cv, SEQUENCE_ID + " = ?", new String[]{"" + sequenceId});
    }

    public void fixStatuses() {
        ContentValues cv = new ContentValues();
        Cursor sequences = getAllSequences();
        while (sequences != null && sequences.getCount() > 0 && !sequences.isAfterLast()) {
            cv.clear();
            int sequenceId = sequences.getInt(sequences.getColumnIndex(SEQUENCE_ID));
            int status = sequences.getInt(sequences.getColumnIndex(SEQUENCE_STATUS));
            int imageCount = (int) getNumberOfFrames(sequenceId);
            int originalCount = getOriginalFrameCount(sequenceId);
            boolean untouched = imageCount > 0 && (imageCount == originalCount || Math.abs(imageCount - originalCount) < 10);
            if (untouched) {
                if (status != Sequence.STATUS_NEW) {
                    cv.put(SEQUENCE_STATUS, Sequence.STATUS_NEW);
                    Log.d(TAG, "fixStatuses: setting NEW");
                }
            } else {
                if (imageCount == 0) {
                    cv.put(SEQUENCE_STATUS, Sequence.STATUS_FINISHED);
                    Log.d(TAG, "fixStatuses: setting FINISHED");
                } else {
                    cv.put(SEQUENCE_STATUS, Sequence.STATUS_INTERRUPTED);
                    Log.d(TAG, "fixStatuses: setting INTERRUPTED");
                }
            }
            if (cv.size() > 0) {
                database.update(SEQUENCE_TABLE, cv, SEQUENCE_ID + " = ?", new String[]{"" + sequenceId});
            }
            sequences.moveToNext();
        }
        if (sequences != null) {
            sequences.close();
        }
    }

    public String getSequenceVersion(int sequenceId) {
        String[] cols = new String[]{SEQUENCE_VERSION};
        Cursor mCursor = database.query(true, SEQUENCE_TABLE, cols, SEQUENCE_ID + " = ?"
                , new String[]{"" + sequenceId}, null, null, null, null);
        try {
            if (mCursor != null && mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                String res = mCursor.getString(mCursor.getColumnIndex(SEQUENCE_VERSION));
                mCursor.close();
                return res;
            }
            if (mCursor != null){
                mCursor.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "getSequenceVersion: " + Log.getStackTraceString(e));
        }
        return "";
    }
}