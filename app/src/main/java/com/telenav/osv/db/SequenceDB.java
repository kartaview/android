package com.telenav.osv.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * The DataBase of the OSV sequences
 * Created by Kalman on 10/7/2015.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class SequenceDB {
    public final static String FRAME_SEQ_INDEX = "sequenceIndex";

    public final static String FRAME_FILE_PATH = "filePath";

    public final static String FRAME_LAT = "latitude";

    public final static String FRAME_LON = "longitude";

    public final static String FRAME_ACCURACY = "accuracy";

    public final static String SEQUENCE_ID = "sequenceId";

    public final static String SEQUENCE_LAT = "latitude";

    public final static String SEQUENCE_LON = "longitude";

    public final static String SEQUENCE_PATH = "thumb";

    public final static String SEQUENCE_EXTERNAL = "external";

    public static final String SEQUENCE_SAFE = "safe";

    public final static String VIDEO_INDEX = "videoIndex";

//  -------------------------------------------------------------

    public final static String VIDEO_FILE_PATH = "filePath";

    public final static String VIDEO_FRAME_COUNT = "count";

    public final static String SCORE_COVERAGE = "coverage";

    public final static String SCORE_OBD_COUNT = "obdCount";

    public final static String SCORE_COUNT = "count";

    /**
     * Name of the only table, containing photo details
     */
    final static String FRAME_TABLE = "Frame";

    final static String FRAME_SEQ_ID = "sequenceId";

    final static String FRAME_VIDEO_ID = "videoId";

    final static String FRAME_ORIENTATION = "orientation";

    static final String FRAME_UNIQUE_CONSTRAINT = "uniqueConstraint";

    static final String VIDEO_UNIQUE_CONSTRAINT = "uniqueConstraint";

    static final String SCORE_UNIQUE_CONSTRAINT = "scoreUniqueConstraint";

    static final String SEQUENCE_TABLE = "Sequence";

    final static String SEQUENCE_ONLINE_ID = "onlineSequenceId";

    final static String SEQUENCE_COUNT = "count";

//  ---------------------------------------------------------------

    final static String SEQUENCE_ORIG_COUNT = "origCount";

    final static String SEQUENCE_VIDEO_COUNT = "videoCount";

    final static String SEQUENCE_PANO = "panorama";

    final static String SEQUENCE_VERSION = "version";

    static final String SEQUENCE_OBD = "obd";

    //  ---------------------------------------------------------------

    static final String SEQUENCE_STATUS = "status";

    final static String VIDEO_TABLE = "Video";

    final static String VIDEO_SEQ_ID = "sequenceId";

    final static String SCORE_TABLE = "Score";

    final static String SCORE_SEQ_ID = "sequenceId";

    //  ---------------------------------------------------------------

    private static final String TAG = "SequenceDB";

    public static SequenceDB instance;

    private SQLiteDatabase database;

    /**
     * @param context context
     */
    private SequenceDB(Context context) {
        SequenceDBHelper dbHelper = new SequenceDBHelper(context);
        try {
            database = dbHelper.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            Log.d(TAG, "SequenceDB: " + e.getLocalizedMessage());
        }
        SequenceDB.instance = this;
    }

    public static void instantiate(Context context) {
        if (instance == null) {
            SequenceDB.instance = new SequenceDB(context);
        }
    }

    private void insertSequence(int seqId, double lat, double lon, String path, boolean pano, boolean external, String version, boolean obd, boolean safe) {
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
        values.put(SEQUENCE_SAFE, safe);
        values.put(SEQUENCE_STATUS, LocalSequence.STATUS_NEW);

        database.insertOrThrow(SEQUENCE_TABLE, null, values);
    }

//    public long insertVideo(int seqId, int videoIndex, String filePath) {
//        ContentValues values = new ContentValues();
//        values.put(VIDEO_SEQ_ID, seqId);
//        values.put(VIDEO_INDEX, videoIndex);
//        values.put(VIDEO_FILE_PATH, filePath);
//        values.put(VIDEO_FRAME_COUNT, -1);
//        return database.insertOrThrow(VIDEO_TABLE, null, values);
//    }

    public void insertVideoIfNotAdded(int seqId, int videoIndex, String filePath) {
        if (!isVideoAdded(seqId, videoIndex)) {
            ContentValues values = new ContentValues();
            values.put(VIDEO_SEQ_ID, seqId);
            values.put(VIDEO_INDEX, videoIndex);
            values.put(VIDEO_FILE_PATH, filePath);
            values.put(VIDEO_FRAME_COUNT, -1);
            database.insert(VIDEO_TABLE, null, values);
        }
    }

    public void insertPhoto(int seqId, int videoIndex, int seqIndex, String filePath, double lat, double lon, float accuracy, int orientation) {
        ContentValues values = new ContentValues();
        values.put(FRAME_SEQ_ID, seqId);
        values.put(FRAME_VIDEO_ID, videoIndex);
        values.put(FRAME_SEQ_INDEX, seqIndex);
        values.put(FRAME_FILE_PATH, filePath);
        values.put(FRAME_LAT, lat);
        values.put(FRAME_LON, lon);
        values.put(FRAME_ACCURACY, accuracy);
        values.put(FRAME_ORIENTATION, orientation);
        database.insertOrThrow(FRAME_TABLE, null, values);
    }

    public void insertScore(int seqId, boolean obd, int coverage) {
        String countColumn = (obd ? SCORE_OBD_COUNT : SCORE_COUNT);
        ContentValues values = new ContentValues();
        if (!isScoreRowAdded(seqId, coverage)) {
            values.put(SCORE_SEQ_ID, seqId);
            values.put(SCORE_COVERAGE, coverage);
            values.put(countColumn, 1);
            database.insertOrThrow(SCORE_TABLE, null, values);
        } else {
            database.execSQL("UPDATE " + SCORE_TABLE + " SET " + countColumn + " = " + countColumn + " + 1 "
                    + "WHERE " + SCORE_SEQ_ID + " = " + seqId + " AND " + SCORE_COVERAGE + " = " + coverage);
        }
    }

    public Cursor getAllSequences() {
        try {
            String[] cols = new String[]{SEQUENCE_ID, SEQUENCE_ONLINE_ID, SEQUENCE_LAT, SEQUENCE_LON, SEQUENCE_PATH, SEQUENCE_COUNT, SEQUENCE_ORIG_COUNT, SEQUENCE_VIDEO_COUNT,
                    SEQUENCE_PANO,
                    SEQUENCE_EXTERNAL, SEQUENCE_VERSION, SEQUENCE_OBD, SEQUENCE_SAFE, SEQUENCE_STATUS};
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
        String[] cols = new String[]{SEQUENCE_ID, SEQUENCE_ONLINE_ID, SEQUENCE_LAT, SEQUENCE_LON, SEQUENCE_PATH, SEQUENCE_COUNT, SEQUENCE_ORIG_COUNT, SEQUENCE_VIDEO_COUNT,
                SEQUENCE_PANO,
                SEQUENCE_EXTERNAL, SEQUENCE_VERSION, SEQUENCE_OBD, SEQUENCE_SAFE, SEQUENCE_STATUS};
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

//    public Cursor getAllFrames() {
//        String[] cols = new String[]{FRAME_SEQ_ID, FRAME_VIDEO_ID, FRAME_SEQ_INDEX, FRAME_FILE_PATH, FRAME_LAT, FRAME_LON, FRAME_ACCURACY, FRAME_ORIENTATION};
//        Cursor mCursor = database.query(true, FRAME_TABLE, cols, null
//                , null, null, null, null, null);
//        if (mCursor != null && mCursor.getCount() > 0) {
//            mCursor.moveToFirst();
//        }
//        return mCursor; // iterate to get each value.
//    }

    private Cursor getAllVideos() {
        String[] cols = new String[]{VIDEO_SEQ_ID, VIDEO_FILE_PATH, VIDEO_FRAME_COUNT, VIDEO_INDEX};
        Cursor mCursor = database.query(true, VIDEO_TABLE, cols, null
                , null, null, null, null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

//    public long getNumberOfFrames() {
//        return DatabaseUtils.queryNumEntries(database, FRAME_TABLE);
//    }

    public long getNumberOfFrames(int localSequenceId) {
        return DatabaseUtils.queryNumEntries(database, FRAME_TABLE, FRAME_SEQ_ID + "=?", new String[]{"" + localSequenceId});
    }

    public long getNumberOfVideos(int localSequenceId) {
        return DatabaseUtils.queryNumEntries(database, VIDEO_TABLE, VIDEO_SEQ_ID + "=?", new String[]{"" + localSequenceId});
    }

    private boolean isVideoAdded(int localSequenceId, int videoIndex) {
        return DatabaseUtils.queryNumEntries(database, VIDEO_TABLE, VIDEO_SEQ_ID + "=? AND " + VIDEO_INDEX + "=?", new String[]{"" + localSequenceId, "" + videoIndex}) > 0;
    }

    private boolean isScoreRowAdded(int localSequenceId, int coverage) {
        return DatabaseUtils.queryNumEntries(database, SCORE_TABLE, SCORE_SEQ_ID + "=? AND " + SCORE_COVERAGE + "=?", new String[]{"" + localSequenceId, "" + coverage}) > 0;
    }

//    public long getVideoFrameCount(int localSequenceId, int videoIndex) {
//        String[] cols = new String[]{VIDEO_FRAME_COUNT};
//        Cursor mCursor = database.query(true, VIDEO_TABLE, cols, VIDEO_SEQ_ID + " = ? AND " + VIDEO_INDEX + " = ?"
//                , new String[]{"" + localSequenceId, "" + videoIndex}, null, null, null, null);
//        if (mCursor != null && mCursor.getCount() > 0) {
//            mCursor.moveToFirst();
//            int res = mCursor.getInt(mCursor.getColumnIndex(VIDEO_FRAME_COUNT));
//            mCursor.close();
//            return res;
//        }
//        if (mCursor != null) {
//            mCursor.close();
//        }
//        return 0;
//    }

    private long countVideoFrames(int localSequenceId, int videoIndex) {
        return DatabaseUtils.queryNumEntries(database, FRAME_TABLE, FRAME_SEQ_ID + " = ? AND " + FRAME_VIDEO_ID + " = ?", new String[]{"" + localSequenceId, "" + videoIndex});
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
        if (mCursor != null) {
            mCursor.close();
        }
        return 0;
    }

    public int updateSequenceOnlineId(int sequenceId, int onlineSequenceId) {
        ContentValues cv = new ContentValues();
        cv.put(SEQUENCE_ONLINE_ID, onlineSequenceId);
        return database.update(SEQUENCE_TABLE, cv, SEQUENCE_ID + " = ? AND " + SEQUENCE_ONLINE_ID + " = -1", new String[]{"" + sequenceId});
    }

    public void updateSequenceLocation(int sequenceId, double lat, double lon) {
        ContentValues cv = new ContentValues();
        cv.put(SEQUENCE_LAT, lat);
        cv.put(SEQUENCE_LON, lon);
        database.update(SEQUENCE_TABLE, cv, SEQUENCE_ID + " = ?", new String[]{"" + sequenceId});
    }

    public void updateSequenceFrameCount(int sequenceId) {
        ContentValues cv = new ContentValues();
        int imageCount = (int) getNumberOfFrames(sequenceId);
        int originalCount = getOriginalFrameCount(sequenceId);
        cv.put(SEQUENCE_COUNT, imageCount);
        if (originalCount < imageCount) {
            cv.put(SEQUENCE_ORIG_COUNT, imageCount);
        } else {
            if (imageCount != originalCount) {
                cv.put(SEQUENCE_STATUS, LocalSequence.STATUS_INTERRUPTED);
            } else if (imageCount != 0) {
                cv.put(SEQUENCE_STATUS, LocalSequence.STATUS_NEW);
            }
        }
        database.update(SEQUENCE_TABLE, cv, SEQUENCE_ID + " = ?", new String[]{"" + sequenceId});
        Cursor videos = getVideos(sequenceId);
        while (videos != null && videos.getCount() > 0 && !videos.isAfterLast()) {
            int index = videos.getInt(videos.getColumnIndex(VIDEO_INDEX));
            int count = (int) countVideoFrames(sequenceId, index);
            cv = new ContentValues();
            cv.put(VIDEO_FRAME_COUNT, count);
            database.update(VIDEO_TABLE, cv, VIDEO_SEQ_ID + " = ? AND " + VIDEO_INDEX + " = ?", new String[]{"" + sequenceId, "" + index});
            videos.moveToNext();
        }
        if (videos != null) {
            videos.close();
        }
    }

    private int deleteSequenceRecord(int sequenceId) {
        return database.delete(SEQUENCE_TABLE, SEQUENCE_ID + " = ?", new String[]{"" + sequenceId});
    }

    private int deleteVideoRecord(int sequenceId) {
        return database.delete(VIDEO_TABLE, VIDEO_SEQ_ID + " = ?", new String[]{"" + sequenceId});
    }

    /**
     * @param sequenceIdLocal sequence id
     * @param videoIndex startIndex of the video
     * @return if deleted
     */
    public int deleteVideo(int sequenceIdLocal, int videoIndex) {
        int res = database.delete(FRAME_TABLE, FRAME_SEQ_ID + " = ? AND " + FRAME_VIDEO_ID + " = ?"
                , new String[]{"" + sequenceIdLocal, "" + videoIndex});
        res = res + database.delete(VIDEO_TABLE, VIDEO_SEQ_ID + " = ? AND " + VIDEO_INDEX + " = ?"
                , new String[]{"" + sequenceIdLocal, "" + videoIndex});
        return res;
    }

    /**
     * @param sequenceIdLocal sequence id
     * @param photoIndex startIndex of the video
     * @return if done
     */
    private int deletePhoto(int sequenceIdLocal, int photoIndex) {
        return database.delete(FRAME_TABLE, FRAME_SEQ_ID + " = ? AND " + FRAME_SEQ_INDEX + " = ?"
                , new String[]{"" + sequenceIdLocal, "" + photoIndex});
    }

    public int resetOnlineSequenceId(int onlineSequenceId) {
        ContentValues cv = new ContentValues();
        cv.put(SEQUENCE_ONLINE_ID, "-1");
        return database.update(SEQUENCE_TABLE, cv, SEQUENCE_ONLINE_ID + " = ?", new String[]{"" + onlineSequenceId});
    }

    public void deleteRecords(int sequenceIdLocal) {
        database.delete(FRAME_TABLE, FRAME_SEQ_ID + " = ?", new String[]{"" + sequenceIdLocal});
        database.delete(VIDEO_TABLE, VIDEO_SEQ_ID + " = ?", new String[]{"" + sequenceIdLocal});
        database.delete(SCORE_TABLE, SCORE_SEQ_ID + " = ?", new String[]{"" + sequenceIdLocal});
        database.delete(SEQUENCE_TABLE, SEQUENCE_ID + " = ?", new String[]{"" + sequenceIdLocal});
    }

    @SuppressWarnings("SameParameterValue")
    public LocalSequence createNewSequence(Context context, double lat, double lon, boolean pano, boolean external, String version, boolean obd, boolean safe) {
        OSVFile osv = Utils.generateOSVFolder(context);
        int i = 0;
        int limit = 100;
        while (limit > 0) {
            OSVFile file = new OSVFile(osv.getPath(), "/SEQ_" + i);
            if (!file.exists()) {
                if (!checkSequenceExists(i)) {
                    boolean result = file.mkdir();
                    if (result && file.exists()) {
                        insertSequence(i, lat, lon, file.getPath(), pano, external, version, obd, safe);
                        return new LocalSequence(file);
                    } else {
                        Log.d(TAG, "createNewSequence: could not create directory " + file.getAbsolutePath());
                    }
                    limit--;
                }
            }
            i++;
        }
        return null;
    }

    /**
     * deletes the image, deletes the parent folder if it remains empty
     * @param video video
     * @param sequenceIdLocal sequence id
     * @param sequenceIndex index
     */
    public void deleteVideo(OSVFile video, int sequenceIdLocal, int sequenceIndex) {
        int result = deleteVideo(sequenceIdLocal, sequenceIndex);
        if (result == 0) {
            Log.w(TAG, "deleteVideo: No records deleted, no records found with sequenceId: " + sequenceIdLocal + " and sequenceIndex: " + sequenceIndex);
        }
        if (!video.delete()) {
            Log.w(TAG, "deleteVideo: delete unsuccessful: " + video.getName());
        }
    }

    /**
     * deletes the image, deletes the parent folder if it remains empty
     * @param photo photo
     * @param sequenceIdLocal sequence id
     * @param sequenceIndex index
     */
    public void deletePhoto(OSVFile photo, int sequenceIdLocal, int sequenceIndex) {
        int result = deletePhoto(sequenceIdLocal, sequenceIndex);
        if (result == 0) {
            Log.w(TAG, "deletePhoto: No records deleted, no records found with sequenceId: " + sequenceIdLocal + " and sequenceIndex: " + sequenceIndex);
        }
        if (!photo.delete()) {
            Log.w(TAG, "deletePhoto: delete unsuccessful: " + photo.getName());
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
//                if (file.exists() && fileIndex != fileindex && fileindex != -1) {
//                    deleteVideo(sequenceId, fileIndex);
//                    try {
//                        insertPhoto(sequenceId, fileindex, file.getPath(), lat, lon, accuracy, orientation);
//                    } catch (Exception e) {
//                        Log.d(TAG, "consistencyCheck: " + Log.getStackTraceString(e));
//                    }
//                }
                if (file.exists() && Utils.fileSize(file) < 1000) {
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
            int id = LocalSequence.getSequenceId(sequence);
            if (getNumberOfFrames(id) <= 0 && getNumberOfVideos(id) <= 0) {
                sequence.delete();
            }
        }
        interruptUploading();
        fixStatuses();
    }

//    public void deleteHistory() {
//        Cursor sequences = getAllSequences();
//        while (sequences != null && sequences.getCount() > 0 && !sequences.isAfterLast()) {
//            int id = sequences.getInt(sequences.getColumnIndex(SEQUENCE_ID));
//            updateSequenceFrameCount(id);
//            if (getNumberOfFrames(id) <= 0) {
//                int res = deleteSequenceRecord(id);
//                res = res + deleteVideoRecord(id);
//                Log.d(TAG, "deleteHistory: deleted sequence with 0 items, id = " + sequences.getInt(sequences.getColumnIndex(SEQUENCE_ID)) + ", result = " + res);
//            }
//            sequences.moveToNext();
//        }
//        if (sequences != null) {
//            sequences.close();
//        }
//        fixStatuses();
//    }

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
        if (mCursor != null) {
            mCursor.close();
        }
        return -1;
    }

    @SuppressWarnings("SameParameterValue")
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
        if (mCursor != null) {
            mCursor.close();
        }
        return -1;
    }

    public void interruptUploading() {
        ContentValues cv = new ContentValues();
        cv.put(SEQUENCE_STATUS, LocalSequence.STATUS_INTERRUPTED);
        int res = database.update(SEQUENCE_TABLE, cv, SEQUENCE_STATUS + " = ?", new String[]{"" + LocalSequence.STATUS_UPLOADING});
        Log.d(TAG, "interruptUploading: number of reset UPLOADING statuses = " + res);
        cv.clear();
        cv.put(SEQUENCE_STATUS, LocalSequence.STATUS_NEW);
        res = database.update(SEQUENCE_TABLE, cv, SEQUENCE_STATUS + " = ?", new String[]{"" + LocalSequence.STATUS_INDEXING});
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
                if (status != LocalSequence.STATUS_NEW) {
                    cv.put(SEQUENCE_STATUS, LocalSequence.STATUS_NEW);
                    Log.d(TAG, "fixStatuses: setting NEW");
                }
            } else {
                cv.put(SEQUENCE_STATUS, LocalSequence.STATUS_INTERRUPTED);
                Log.d(TAG, "fixStatuses: setting INTERRUPTED");
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
            if (mCursor != null) {
                mCursor.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "getSequenceVersion: " + Log.getStackTraceString(e));
        }
        if (mCursor != null) {
            mCursor.close();
        }
        return "";
    }

    public Cursor getScores(int sequenceId) {
        String[] cols = new String[]{SCORE_COVERAGE, SCORE_OBD_COUNT, SCORE_COUNT};
        Cursor mCursor = database.query(false, SCORE_TABLE, cols, SCORE_SEQ_ID + " = ?"
                , new String[]{"" + sequenceId}, null, null, null, null);
        if (mCursor != null && mCursor.getCount() > 0) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public boolean isSequenceSafe(int sequenceId) {
        return DatabaseUtils.queryNumEntries(database, SEQUENCE_TABLE, SEQUENCE_ID + " = ? AND " + SEQUENCE_SAFE + " > 0", new String[]{"" + sequenceId}) > 0;
    }
}