package com.telenav.osv.data.database.migration;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.telenav.osv.data.database.KVDatabase;
import com.telenav.osv.data.database.SqlLiteOpenHelperTest;
import com.telenav.osv.data.database.SqliteDatabaseTestHelper;
import com.telenav.osv.data.frame.database.entity.FrameEntity;
import com.telenav.osv.data.frame.legacy.FrameLegacyModelTest;
import com.telenav.osv.data.location.database.entity.LocationEntity;
import com.telenav.osv.data.score.database.entity.ScoreEntity;
import com.telenav.osv.data.score.legacy.ScoreHistoryLegacyTest;
import com.telenav.osv.data.sequence.database.entity.SequenceEntity;
import com.telenav.osv.data.sequence.legacy.LocalSequenceLegacyModelTest;
import com.telenav.osv.data.video.database.entity.VideoEntity;
import com.telenav.osv.data.video.legacy.VideoLegacyTestModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author horatiuf
 */
@RunWith(AndroidJUnit4.class)
public class Migration4To5Test {
    private static final String TEST_DB = "migration-test";

    private static final double DELTA = 1e-15;

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
            KVDatabase.class.getCanonicalName(),
            new FrameworkSQLiteOpenHelperFactory());

    private SqlLiteOpenHelperTest sqliteTestDbHelper;

    public Migration4To5Test() {
    }

    @Before
    public void setUp() {
        // To test migrations from version 1 of the database, we need to create the database
        // with version 1 using SQLite API
        sqliteTestDbHelper = new SqlLiteOpenHelperTest(InstrumentationRegistry.getTargetContext(), TEST_DB, 4);
        // We're creating the table for every test, to ensure that the table is in the correct state
        SqliteDatabaseTestHelper.createTable(sqliteTestDbHelper);
    }

    @After
    public void tearDown() {
        // Clear the database after every test
        SqliteDatabaseTestHelper.clearDatabase(sqliteTestDbHelper);
    }


    @Test
    public void testMaximumCompleteSequenceDataConsistencyForMigration4To5() throws IOException {
        testCompleteSequenceDataConsistencyForMigration4To5(3, 1500, 450, 5000, true);
    }

    @Test
    public void testMinimumCompleteSequenceDataWithoutVideoConsistencyForMigration4To5() throws IOException {
        testCompleteSequenceDataConsistencyForMigration4To5(1, 500, 0, 300, true);
    }

    @Test
    public void testMinimumCompleteSequenceDataConsistencyForMigration4To5() throws IOException {
        testCompleteSequenceDataConsistencyForMigration4To5(1, 500, 2, 300, true);
    }

    @Test
    public void testMinimumCompleteSequenceDataConsistencyForMigrationTo4To5WithoutNullableFields() throws IOException {
        testCompleteSequenceDataConsistencyForMigration4To5(1, 500, 2, 300, false);
    }

    @Test
    public void testRealCaseForMigration4To5() throws IOException {
        Map<Integer, LocalSequenceLegacyModelTest> localSequenceLegacyModelTestMap = new HashMap<>();
        localSequenceLegacyModelTestMap.put(1, generateSequenceLegacyTestModel(1, 1000, 4, 40));
        localSequenceLegacyModelTestMap.put(2, generateSequenceLegacyTestModel(2, 400, 2, 150));
        localSequenceLegacyModelTestMap.put(3, generateSequenceLegacyTestModel(3, 800, 0, 300));
        localSequenceLegacyModelTestMap.put(4, generateSequenceLegacyTestModel(4, 750, 2, 60));
        SqliteDatabaseTestHelper.insertSequences(
                new ArrayList<>(localSequenceLegacyModelTestMap.values()),
                sqliteTestDbHelper,
                true);

        testFullSequenceDataConsistencyForMigration4To5(localSequenceLegacyModelTestMap, true);
    }

    private void assertScoreConsistency(ScoreEntity dbScore, int legacySequenceId, ScoreHistoryLegacyTest legacyScore, boolean includeNullableFields) {
        assertNotNull(dbScore.getScoreId());
        assertTrue(dbScore.getScoreId() instanceof String);
        assertEquals(dbScore.getSequenceID(), String.valueOf(legacySequenceId));
        assertEquals((int) dbScore.getCoverage(), legacyScore.coverage);
        if (includeNullableFields) {
            assertEquals((int) dbScore.getFrameCount(), legacyScore.photoCount);
            assertEquals((int) dbScore.getObdFrameCount(), legacyScore.obdPhotoCount);
        } else {
            assertNull(dbScore.getFrameCount());
            assertNull(dbScore.getObdFrameCount());
        }
    }

    private void assertSequenceConsistency(LocalSequenceLegacyModelTest legacySequence, SequenceEntity dbSequence, boolean includeNullableFields) {
        assertEquals(dbSequence.getSequenceId(), String.valueOf(legacySequence.getId()));
        //TODO: Fix test
//        assertEquals(dbSequence.getLatitude(), legacySequence.getLocation().getLatitude(), DELTA);
//        assertEquals(dbSequence.getLongitude(), legacySequence.getLocation().getLongitude(), DELTA);
        assertEquals(dbSequence.getFilePath(), legacySequence.getPath());
        assertEquals(dbSequence.getAppVersion(), legacySequence.getAppVersion());
        assertEquals((int) dbSequence.getLocationsCount(), legacySequence.getFrameCount());
        assertEquals((int) dbSequence.getVideoCount(), legacySequence.getVideoCount());
        if (includeNullableFields) {
            assertEquals(dbSequence.isObd(), legacySequence.hasObd());
            assertEquals((long) dbSequence.getOnlineID(), legacySequence.getOnlineId());
            assertEquals((int) dbSequence.getVideoCount(), legacySequence.getVideoCount());
        } else {
            assertNull(dbSequence.isObd());
            assertNull(dbSequence.getOnlineID());
        }
        assertEquals(dbSequence.getBoundingWestLon(), 0.0, DELTA);
        assertEquals(dbSequence.getBoundingEastLon(), 0.0, DELTA);
        assertEquals(dbSequence.getBoundingSouthLat(), 0.0, DELTA);
        assertEquals(dbSequence.getBoundingNorthLat(), 0.0, DELTA);
    }

    private void assertVideoConsistency(VideoEntity dbVideo, int legacySequenceId, VideoLegacyTestModel legacyVideo) {
        assertNotNull(dbVideo.getVideoId());
        assertTrue(dbVideo.getVideoId() instanceof String);
        assertEquals(dbVideo.getSequenceID(), String.valueOf(legacySequenceId));
        assertEquals(dbVideo.getFilePath(), legacyVideo.getFile());
        assertEquals((int) dbVideo.getIndex(), legacyVideo.getFileIndex());
        assertEquals((int) dbVideo.getFrameCount(), legacyVideo.getFrameCount());
    }

    private void assertFrameConsistency(FrameEntity dbFrame, int legacySequenceId, FrameLegacyModelTest legacyFrame) {
        assertNotNull(dbFrame.getFrameId());
        assertTrue(dbFrame.getFrameId() instanceof String);
        assertEquals(dbFrame.getSequenceID(), String.valueOf(legacySequenceId));
        assertEquals(dbFrame.getFilePath(), legacyFrame.getFilePath());
        assertEquals((int) dbFrame.getIndex(), legacyFrame.getSeqIndex());
    }

    private void testCompleteSequenceDataConsistencyForMigration4To5(int sequenceNo, int scoreNo, int videoNo, int frameNo, boolean includeNullableFields) throws IOException {
        Map<Integer, LocalSequenceLegacyModelTest> localSequenceLegacyModelTestMap = generateSequenceMap(sequenceNo, scoreNo, videoNo, frameNo);
        SqliteDatabaseTestHelper.insertSequences(
                new ArrayList<>(localSequenceLegacyModelTestMap.values()),
                sqliteTestDbHelper,
                includeNullableFields);

        testFullSequenceDataConsistencyForMigration4To5(localSequenceLegacyModelTestMap, includeNullableFields);
    }

    private void testFullSequenceDataConsistencyForMigration4To5(Map<Integer, LocalSequenceLegacyModelTest> sequencesMap, boolean includeNullableFields) throws IOException {
        helper.runMigrationsAndValidate(TEST_DB, 5, true, new Migration4To5());
        // Get the latest, migrated, version of the database
        KVDatabase latestDb = getMigratedRoomDatabase();
        List<SequenceEntity> sequenceEntities = latestDb.sequenceDao().findAll().blockingGet();

        assertNotNull(sequenceEntities);
        assertNotEquals(sequenceEntities.size(), 0);
        for (SequenceEntity sequenceEntity : sequenceEntities) {
            LocalSequenceLegacyModelTest legacySequence = sequencesMap.get(Integer.parseInt(sequenceEntity.getSequenceId()));
            assertSequenceConsistency(legacySequence, sequenceEntity, includeNullableFields);
            if (legacySequence.getScoreHistories().size() != 0) {
                List<ScoreEntity> scoreEntities = latestDb.scoreDao().findAllBySequenceID(sequenceEntity.getSequenceId()).blockingGet();

                assertNotNull(scoreEntities);
                assertNotEquals(scoreEntities.size(), 0);
                for (ScoreEntity scoreEntity : scoreEntities) {
                    assertScoreConsistency(scoreEntity, legacySequence.getId(), legacySequence.getScoreHistories().get(scoreEntity.getCoverage()), includeNullableFields);
                }
            }
            if (legacySequence.getVideoCount() != 0) {
                List<VideoEntity> videoEntities = latestDb.videoDao().findAllBySequenceID(sequenceEntity.getSequenceId()).blockingGet();

                assertNotNull(videoEntities);
                assertNotEquals(videoEntities.size(), 0);
                for (VideoEntity videoEntity : videoEntities) {
                    assertVideoConsistency(videoEntity, legacySequence.getId(), legacySequence.getVideoLegacyTestModels().get(videoEntity.getVideoId()));
                }
            }

            if (legacySequence.getFrameCount() != 0) {
                List<FrameEntity> frameEntities = latestDb.frameDao().findAllBySequenceID(sequenceEntity.getSequenceId()).blockingGet();

                assertNotNull(frameEntities);
                assertNotEquals(frameEntities.size(), 0);
                for (FrameEntity frameEntity : frameEntities) {
                    FrameLegacyModelTest legacyFrame = legacySequence.getFrameLegacyModelTestMap().get(frameEntity.getFrameId());
                    assertFrameConsistency(frameEntity, legacySequence.getId(), legacyFrame);
                    LocationEntity locationEntity = latestDb.locationDao().findByFrameID(frameEntity.getFrameId()).blockingGet();
                    assertLocationConsistency(legacySequence.getId(), locationEntity, legacyFrame);
                }
            }
        }
    }

    private void assertLocationConsistency(int legacySequenceId, LocationEntity dbLocation, FrameLegacyModelTest legacyFrame) {
        assertNotNull(dbLocation.getLocationId());
        assertTrue(dbLocation.getLocationId() instanceof String);
        assertEquals(dbLocation.getLatitude(), legacyFrame.getLat(), DELTA);
        assertEquals(dbLocation.getLongitude(), legacyFrame.getLon(), DELTA);
        assertEquals(dbLocation.getSequenceID(), String.valueOf(legacySequenceId));
        assertNotNull(dbLocation.getVideoID());
        assertTrue(dbLocation.getVideoID() instanceof String);
    }

    private KVDatabase getMigratedRoomDatabase() {
        KVDatabase database = Room.databaseBuilder(InstrumentationRegistry.getTargetContext(),
                KVDatabase.class, TEST_DB)
                .addMigrations(new Migration4To5())
                .build();
        // close the database and release any stream resources when the test finishes
        helper.closeWhenFinished(database);
        return database;
    }

    private Map<String, VideoLegacyTestModel> generateVideoMap(int videoNumber, int seqId) {
        Map<String, VideoLegacyTestModel> videoLegacyTestModels = new HashMap<>();
        for (int i = 0; i < videoNumber; i++) {
            VideoLegacyTestModel videoLegacyTestModel = generateVideoLegacyTestModel(seqId, i);
            videoLegacyTestModels.put(videoLegacyTestModel.getSequenceId() + "," + videoLegacyTestModel.getFileIndex(), videoLegacyTestModel);
        }
        return videoLegacyTestModels;
    }

    private Map<String, FrameLegacyModelTest> generateFrameMap(int frameNo, int videoIndex, int seqId, int sequenceStartingIndex) {
        Map<String, FrameLegacyModelTest> videoLegacyTestModels = new HashMap<>();
        for (int i = 0; i < frameNo; i++) {
            FrameLegacyModelTest frameLegacyModelTest = generateFrame(seqId, videoIndex, i + sequenceStartingIndex);
            videoLegacyTestModels.put(frameLegacyModelTest.getSequenceId() + ","
                            + frameLegacyModelTest.getSeqIndex() + ","
                            + frameLegacyModelTest.getVideoIndex(),
                    frameLegacyModelTest);
        }
        return videoLegacyTestModels;
    }

    private VideoLegacyTestModel generateVideoLegacyTestModel(int seqId, int index) {
        Random random = new Random();
        return new VideoLegacyTestModel(
                index,
                UUID.randomUUID().toString(),
                seqId,
                random.nextInt(10000),
                UUID.randomUUID().toString());
    }

    private Map<Integer, LocalSequenceLegacyModelTest> generateSequenceMap(int number, int scoreHistoryNo, int videoNo, int frameNo) {
        Map<Integer, LocalSequenceLegacyModelTest> sequenceLegacyModelTestMap = new HashMap<>();
        for (int i = 1; i <= number; i++) {
            sequenceLegacyModelTestMap.put(i, generateSequenceLegacyTestModel(i, scoreHistoryNo, videoNo, frameNo));
        }
        return sequenceLegacyModelTestMap;
    }

    private LocalSequenceLegacyModelTest generateSequenceLegacyTestModel(int sequenceId, int scoreHistoryNo, int videoNo, int frameNo) {
        LocalSequenceLegacyModelTest localSequenceLegacyModelTest = generateSequence(sequenceId, videoNo, frameNo);
        if (scoreHistoryNo != 0) {
            localSequenceLegacyModelTest.setScoreHistory(generateScoreHistory(scoreHistoryNo));
        }
        if (videoNo != 0) {
            Map<String, VideoLegacyTestModel> videoLegacyTestModels = generateVideoMap(videoNo, sequenceId);
            localSequenceLegacyModelTest.setVideoLegacyTestModels(videoLegacyTestModels);
            localSequenceLegacyModelTest.setVideoCount(videoLegacyTestModels.size());
        }
        if (frameNo != 0) {
            Map<String, FrameLegacyModelTest> frameLegacyTestModels = new HashMap<>();
            // generates frames for videos, this is logic based on app segmentation
            if (videoNo != 0) {
                int sequenceFrame = frameNo;
                int framesPerVideo = sequenceFrame / videoNo;
                int index = sequenceFrame / framesPerVideo;
                for (int y = 0; y < index; y++) {
                    frameLegacyTestModels.putAll(generateFrameMap(framesPerVideo, y, sequenceId, framesPerVideo * y));
                    sequenceFrame -= framesPerVideo;
                }
                if (sequenceFrame > 0) {
                    frameLegacyTestModels.putAll(generateFrameMap(sequenceFrame, index, sequenceId, framesPerVideo * index));
                }
            } else {
                frameLegacyTestModels = generateFrameMap(frameNo, 0, sequenceId, 0);
            }
            localSequenceLegacyModelTest.setFrameLegacyModelTestMap(frameLegacyTestModels);
            localSequenceLegacyModelTest.setFrameCount(frameLegacyTestModels.size());
        }
        return localSequenceLegacyModelTest;
    }

    private FrameLegacyModelTest generateFrame(int sequenceId, int videoIndex, int seqIndex) {
        Random random = new Random();
        return new FrameLegacyModelTest(
                sequenceId,
                videoIndex,
                seqIndex,
                UUID.randomUUID().toString(),
                random.nextDouble(),
                random.nextDouble(),
                random.nextFloat());
    }

    private LocalSequenceLegacyModelTest generateSequence(int seqId, int videoCount, int frameCount) {
        Random random = new Random();
        LocalSequenceLegacyModelTest sequenceLegacyModelTest = new LocalSequenceLegacyModelTest(
                seqId,
                random.nextInt(10000),
                UUID.randomUUID().toString(),
                random.nextBoolean(),
                random.nextLong(),
                random.nextLong(),
                random.nextBoolean(),
                videoCount,
                UUID.randomUUID().toString());
//        sequenceLegacyModelTest.setLocation(new SKCoordinate(random.nextDouble(), random.nextDouble()));
        sequenceLegacyModelTest.setFrameCount(frameCount);
        sequenceLegacyModelTest.setHasObd(random.nextBoolean());
        return sequenceLegacyModelTest;
    }

    private Map<Integer, ScoreHistoryLegacyTest> generateScoreHistory(int number) {
        Map<Integer, ScoreHistoryLegacyTest> scoreHistoryMap = new HashMap<>();
        Random random = new Random();
        for (int i = 0; i < number; i++) {
            ScoreHistoryLegacyTest newScoreHistoryLegacyTest = new ScoreHistoryLegacyTest(
                    ThreadLocalRandom.current().nextInt(-1, 11),
                    random.nextInt(10000),
                    random.nextInt(10000)
            );
            if (scoreHistoryMap.containsKey(newScoreHistoryLegacyTest.coverage)) {
                ScoreHistoryLegacyTest scoreHistoryLegacyTest = scoreHistoryMap.get(newScoreHistoryLegacyTest.coverage);
                scoreHistoryLegacyTest.obdPhotoCount += newScoreHistoryLegacyTest.obdPhotoCount;
                scoreHistoryLegacyTest.photoCount += newScoreHistoryLegacyTest.photoCount;
            } else {
                scoreHistoryMap.put(newScoreHistoryLegacyTest.coverage, newScoreHistoryLegacyTest);
            }
        }
        return scoreHistoryMap;
    }
}