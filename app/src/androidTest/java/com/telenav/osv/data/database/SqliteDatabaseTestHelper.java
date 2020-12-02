package com.telenav.osv.data.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.telenav.osv.data.frame.legacy.FrameLegacyModelTest;
import com.telenav.osv.data.score.legacy.ScoreHistoryLegacyTest;
import com.telenav.osv.data.sequence.legacy.LocalSequenceLegacyModelTest;
import com.telenav.osv.data.video.legacy.VideoLegacyTestModel;

import java.util.List;
import java.util.Map;

import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.DB_CREATE_PHOTO_TABLE;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.DB_CREATE_SCORE_TABLE;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.DB_CREATE_SEQUENCE_TABLE;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.DB_CREATE_VIDEO_TABLE;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.FRAME_ACCURACY;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.FRAME_FILE_PATH;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.FRAME_LAT;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.FRAME_LON;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.FRAME_SEQ_ID;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.FRAME_SEQ_INDEX;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.FRAME_TABLE;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.FRAME_VIDEO_ID;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SCORE_COUNT;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SCORE_COVERAGE;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SCORE_OBD_COUNT;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SCORE_SEQ_ID;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SCORE_TABLE;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SEQUENCE_COUNT;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SEQUENCE_ID;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SEQUENCE_OBD;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SEQUENCE_ONLINE_ID;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SEQUENCE_PATH;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SEQUENCE_TABLE;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SEQUENCE_VERSION;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.SEQUENCE_VIDEO_COUNT;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.VIDEO_FILE_PATH;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.VIDEO_FRAME_COUNT;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.VIDEO_INDEX;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.VIDEO_SEQ_ID;
import static com.telenav.osv.data.database.SqlLiteOpenHelperTest.VIDEO_TABLE;

/**
 * @author horatiuf
 */
public class SqliteDatabaseTestHelper {
    public static void insertSequence(LocalSequenceLegacyModelTest sequenceLegacyModelTest, SqlLiteOpenHelperTest helper) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = createDefaultSequenceContentValues(sequenceLegacyModelTest);
        values.put(SEQUENCE_ONLINE_ID, sequenceLegacyModelTest.getOnlineId());
        values.put(SEQUENCE_VIDEO_COUNT, sequenceLegacyModelTest.getVideoCount());
        values.put(SEQUENCE_OBD, sequenceLegacyModelTest.hasObd());
        db.insertOrThrow(SEQUENCE_TABLE, null, values);
        db.close();
    }

    public static void insertSequenceWithoutNullableFields(LocalSequenceLegacyModelTest sequenceLegacyModelTest, SqlLiteOpenHelperTest helper) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.insertOrThrow(SEQUENCE_TABLE, null, createDefaultSequenceContentValues(sequenceLegacyModelTest));
        db.close();
    }

    public static void insertScore(ScoreHistoryLegacyTest scoreHistoryLegacyTest, int sequenceId, SqlLiteOpenHelperTest helper) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = createDefaultScoreContentValues(scoreHistoryLegacyTest, sequenceId);
        values.put(SCORE_COUNT, scoreHistoryLegacyTest.photoCount);
        values.put(SCORE_OBD_COUNT, scoreHistoryLegacyTest.obdPhotoCount);
        db.insertOrThrow(SCORE_TABLE, null, values);
        db.close();
    }

    public static void insertScoreWithoutNullableFields(ScoreHistoryLegacyTest scoreHistoryLegacyTest, int sequenceId, SqlLiteOpenHelperTest helper) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.insertOrThrow(SCORE_TABLE, null, createDefaultScoreContentValues(scoreHistoryLegacyTest, sequenceId));
        db.close();
    }

    public static void insertVideo(VideoLegacyTestModel videoLegacyTestModel, SqlLiteOpenHelperTest helper) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(VIDEO_FILE_PATH, videoLegacyTestModel.getFile());
        values.put(VIDEO_FRAME_COUNT, videoLegacyTestModel.getFrameCount());
        values.put(VIDEO_INDEX, videoLegacyTestModel.getFileIndex());
        values.put(VIDEO_SEQ_ID, videoLegacyTestModel.getSequenceId());
        db.insertOrThrow(VIDEO_TABLE, null, values);
        db.close();
    }

    public static void insertPhoto(FrameLegacyModelTest frameLegacyModelTest, SqlLiteOpenHelperTest helper) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FRAME_FILE_PATH, frameLegacyModelTest.getFilePath());
        values.put(FRAME_ACCURACY, frameLegacyModelTest.getAccuracy());
        values.put(FRAME_LAT, frameLegacyModelTest.getLat());
        values.put(FRAME_LON, frameLegacyModelTest.getLon());
        values.put(FRAME_SEQ_ID, frameLegacyModelTest.getSequenceId());
        values.put(FRAME_SEQ_INDEX, frameLegacyModelTest.getSeqIndex());
        values.put(FRAME_VIDEO_ID, frameLegacyModelTest.getVideoIndex());
        db.insertOrThrow(FRAME_TABLE, null, values);
        db.close();
    }

    public static void insertScores(Map<Integer, ScoreHistoryLegacyTest> scoreHistoryLegacyTestMap, int sequenceId, SqlLiteOpenHelperTest helper, boolean includeNullableFields) {
        for (ScoreHistoryLegacyTest scoreHistoryLegacyTest : scoreHistoryLegacyTestMap.values()) {
            if (includeNullableFields) {
                insertScore(scoreHistoryLegacyTest, sequenceId, helper);
            } else {
                insertScoreWithoutNullableFields(scoreHistoryLegacyTest, sequenceId, helper);
            }
        }
    }

    public static void insertVideos(Map<String, VideoLegacyTestModel> videosLegacyTestMap, SqlLiteOpenHelperTest helper) {
        for (VideoLegacyTestModel videoLegacyTestModel : videosLegacyTestMap.values()) {
            insertVideo(videoLegacyTestModel, helper);
        }
    }

    public static void insertPhotos(Map<String, FrameLegacyModelTest> frameLegacyModelTestMap, SqlLiteOpenHelperTest helper) {
        for (FrameLegacyModelTest frameLegacyModelTest : frameLegacyModelTestMap.values()) {
            insertPhoto(frameLegacyModelTest, helper);
        }
    }

    public static void insertSequences(List<LocalSequenceLegacyModelTest> sequenceLegacyModelTests, SqlLiteOpenHelperTest helper, boolean includeNullableFields) {
        for (LocalSequenceLegacyModelTest sequenceLegacyModelTest : sequenceLegacyModelTests) {
            if (includeNullableFields) {
                insertSequence(sequenceLegacyModelTest, helper);
            } else {
                insertSequenceWithoutNullableFields(sequenceLegacyModelTest, helper);
            }
            Map<Integer, ScoreHistoryLegacyTest> scoreHistoryLegacyTestMap = sequenceLegacyModelTest.getScoreHistories();
            if (scoreHistoryLegacyTestMap != null && !scoreHistoryLegacyTestMap.isEmpty()) {
                insertScores(scoreHistoryLegacyTestMap, sequenceLegacyModelTest.getId(), helper, includeNullableFields);
            }
            Map<String, VideoLegacyTestModel> videoLegacyTestMap = sequenceLegacyModelTest.getVideoLegacyTestModels();
            if (videoLegacyTestMap != null && !videoLegacyTestMap.isEmpty()) {
                //no flag passed since there are no nullable fields in the legacy models
                insertVideos(videoLegacyTestMap, helper);
            }
            Map<String, FrameLegacyModelTest> frameLegacyModelTestMap = sequenceLegacyModelTest.getFrameLegacyModelTestMap();
            if (frameLegacyModelTestMap != null && !frameLegacyModelTestMap.isEmpty()) {
                //no flag passed since there are no nullable fields in the legacy models
                insertPhotos(frameLegacyModelTestMap, helper);
            }
        }
    }

    public static void createTable(SqlLiteOpenHelperTest helper) {
        SQLiteDatabase db = helper.getWritableDatabase();

        db.execSQL(DB_CREATE_SEQUENCE_TABLE);
        db.execSQL(DB_CREATE_VIDEO_TABLE);
        db.execSQL(DB_CREATE_PHOTO_TABLE);
        db.execSQL(DB_CREATE_SCORE_TABLE);
        db.close();
    }

    public static void clearDatabase(SqlLiteOpenHelperTest helper) {
        SQLiteDatabase db = helper.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS Frame");
        db.execSQL("DROP TABLE IF EXISTS Video");
        db.execSQL("DROP TABLE IF EXISTS Score");
        db.execSQL("DROP TABLE IF EXISTS Sequence");
        db.execSQL("DROP TABLE IF EXISTS Location");
        db.close();
    }

    private static ContentValues createDefaultSequenceContentValues(LocalSequenceLegacyModelTest sequenceLegacyModelTest) {
        ContentValues defaultSequenceContentValues = new ContentValues();
        defaultSequenceContentValues.put(SEQUENCE_ID, sequenceLegacyModelTest.getId());
        //TODO: Fix test
//        defaultSequenceContentValues.put(SEQUENCE_LAT, sequenceLegacyModelTest.getLocation().getLatitude());
//        defaultSequenceContentValues.put(SEQUENCE_LON, sequenceLegacyModelTest.getLocation().getLongitude());
        defaultSequenceContentValues.put(SEQUENCE_PATH, sequenceLegacyModelTest.getPath());
        defaultSequenceContentValues.put(SEQUENCE_COUNT, sequenceLegacyModelTest.getFrameCount());
        defaultSequenceContentValues.put(SEQUENCE_VERSION, sequenceLegacyModelTest.getAppVersion());
        return defaultSequenceContentValues;
    }

    private static ContentValues createDefaultScoreContentValues(ScoreHistoryLegacyTest scoreHistoryLegacyTest, int sequenceId) {
        ContentValues values = new ContentValues();
        values.put(SCORE_COVERAGE, scoreHistoryLegacyTest.coverage);
        values.put(SCORE_SEQ_ID, sequenceId);
        return values;
    }
}
