package com.telenav.osv.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.joda.time.DateTime;
import com.telenav.osv.data.frame.database.entity.FrameEntity;
import com.telenav.osv.data.location.database.entity.LocationEntity;
import com.telenav.osv.data.score.database.dao.ScoreDao;
import com.telenav.osv.data.score.database.entity.ScoreEntity;
import com.telenav.osv.data.sequence.database.dao.SequenceDao;
import com.telenav.osv.data.sequence.database.entity.SequenceEntity;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.data.video.database.dao.VideoDao;
import com.telenav.osv.data.video.database.entity.VideoEntity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Helper for testing all room data entites. This will mostly generate mock data for persistence.
 * @author horatiuf
 */
public class DataTestHelper {

    private static final double DELTA = 1e-15;

    public static SequenceEntity generateSequenceEntity() {
        Random random = new Random();
        return new SequenceEntity(
                UUID.randomUUID().toString(),
                random.nextBoolean(),
                random.nextDouble(),
                random.nextDouble(),
                UUID.randomUUID().toString(),
                random.nextDouble(),
                UUID.randomUUID().toString(),
                new DateTime(),
                random.nextInt(),
                random.nextInt(),
                random.nextLong(),
                UUID.randomUUID().toString(),
                random.nextLong(),
                random.nextInt(SequenceDetailsLocal.SequenceConsistencyStatus.METADATA_MISSING),
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble()
        );
    }

    public static ScoreEntity generateScoreEntity(String seqId, int coverage) {
        Random random = new Random();
        return new ScoreEntity(
                UUID.randomUUID().toString(),
                random.nextInt(),
                random.nextInt(),
                coverage,
                seqId);
    }

    public static VideoEntity generateVideoEntity(String seqId, int videoIndex) {
        return new VideoEntity(
                UUID.randomUUID().toString(),
                videoIndex,
                UUID.randomUUID().toString(),
                new Random().nextInt(),
                seqId);
    }

    public static FrameEntity generateFrameEntity(String seqId, int index) {
        return new FrameEntity(
                UUID.randomUUID().toString(),
                new DateTime(),
                UUID.randomUUID().toString(),
                index,
                seqId);
    }

    public static LocationEntity generateLocationEntity(String seqId, String videoId, String frameId) {
        Random random = new Random();
        return new LocationEntity(
                UUID.randomUUID().toString(),
                random.nextDouble(),
                random.nextDouble(),
                seqId,
                videoId,
                frameId);
    }

    public static List<SequenceEntity> generateSequenceEntities(int sequenceNo) {
        List<SequenceEntity> sequenceEntities = new ArrayList<>();
        for (int i = 0; i < sequenceNo; i++) {
            sequenceEntities.add(generateSequenceEntity());
        }
        return sequenceEntities;
    }

    public static Map<String, SequenceEntity> generateSequenceEntitiesMap(int sequenceNo) {
        Map<String, SequenceEntity> sequenceEntitiesMap = new HashMap<>();
        for (int i = 0; i < sequenceNo; i++) {
            SequenceEntity sequenceEntity = generateSequenceEntity();
            sequenceEntitiesMap.put(sequenceEntity.getSequenceId(), sequenceEntity);
        }
        return sequenceEntitiesMap;
    }

    public static List<ScoreEntity> generateScoreEntities(String seqId, int scoreNoPerSequence) {
        List<ScoreEntity> scoreEntities = new ArrayList<>();
        for (int i = 0; i < scoreNoPerSequence; i++) {
            scoreEntities.add(generateScoreEntity(seqId, i));
        }
        return scoreEntities;
    }

    public static void assertScoresDataEntity(ScoreEntity firstScore, ScoreEntity secondScore) {
        assertEquals(firstScore.getSequenceID(), secondScore.getSequenceID());
        assertEquals(firstScore.getCoverage(), secondScore.getCoverage());
        assertEquals(firstScore.getFrameCount(), secondScore.getFrameCount());
        assertEquals(firstScore.getObdFrameCount(), secondScore.getObdFrameCount());
        assertEquals(firstScore.getScoreId(), secondScore.getScoreId());
    }

    public static void assertSequencesDataEntity(SequenceEntity firstSequence, SequenceEntity secondSequence) {
        assertNotNull(firstSequence);
        assertNotNull(secondSequence);
        assertEquals(firstSequence.getAddressName(), secondSequence.getAddressName());
        assertEquals(firstSequence.getAppVersion(), secondSequence.getAppVersion());
        assertEquals(firstSequence.getCreationTime(), secondSequence.getCreationTime());
        assertEquals(firstSequence.getOnlineID(), secondSequence.getOnlineID());
        assertEquals(firstSequence.getSequenceId(), secondSequence.getSequenceId());
        assertEquals(firstSequence.getVideoCount(), secondSequence.getVideoCount());
        assertEquals(firstSequence.getLocationsCount(), secondSequence.getLocationsCount());
        assertEquals(firstSequence.getDiskSize(), secondSequence.getDiskSize());
        assertEquals(firstSequence.getFilePath(), secondSequence.getFilePath());
        assertEquals(firstSequence.getLatitude(), secondSequence.getLatitude(), DELTA);
        assertEquals(firstSequence.getLongitude(), secondSequence.getLongitude(), DELTA);
        assertEquals(firstSequence.getFilePath(), secondSequence.getFilePath());
        assertEquals(firstSequence.isObd(), secondSequence.isObd());
        assertEquals(firstSequence.getDistance(), secondSequence.getDistance(), DELTA);
        assertEquals(firstSequence.getBoundingEastLon(), secondSequence.getBoundingEastLon(), DELTA);
        assertEquals(firstSequence.getBoundingWestLon(), secondSequence.getBoundingWestLon(), DELTA);
        assertEquals(firstSequence.getBoundingSouthLat(), secondSequence.getBoundingSouthLat(), DELTA);
        assertEquals(firstSequence.getBoundingNorthLat(), secondSequence.getBoundingNorthLat(), DELTA);
    }

    public static void assertPairScoreEntity(ScoreEntity firstEntity, ScoreEntity secondEntity) {
        assertNotNull(firstEntity);
        assertNotNull(secondEntity);
        assertEquals(firstEntity.getCoverage(), secondEntity.getCoverage());
        assertEquals(firstEntity.getFrameCount(), secondEntity.getFrameCount());
        assertEquals(firstEntity.getObdFrameCount(), secondEntity.getObdFrameCount());
        assertEquals(firstEntity.getScoreId(), secondEntity.getScoreId());
        assertEquals(firstEntity.getSequenceID(), secondEntity.getSequenceID());
    }

    public static void assertVideoDataEntity(VideoEntity firstEntity, VideoEntity secondEntity) {
        assertNotNull(firstEntity);
        assertNotNull(secondEntity);
        assertEquals(firstEntity.getIndex(), secondEntity.getIndex());
        assertEquals(firstEntity.getFilePath(), secondEntity.getFilePath());
        assertEquals(firstEntity.getVideoId(), secondEntity.getVideoId());
        assertEquals(firstEntity.getFrameCount(), secondEntity.getFrameCount());
        assertEquals(firstEntity.getSequenceID(), secondEntity.getSequenceID());
    }

    public static void assertFrameDataEntity(FrameEntity firstEntity, FrameEntity secondEntity) {
        assertNotNull(firstEntity);
        assertNotNull(secondEntity);
        assertEquals(firstEntity.getIndex(), secondEntity.getIndex());
        assertEquals(firstEntity.getFrameId(), secondEntity.getFrameId());
        assertEquals(firstEntity.getDateTime(), secondEntity.getDateTime());
        assertEquals(firstEntity.getFilePath(), secondEntity.getFilePath());
        assertEquals(firstEntity.getSequenceID(), secondEntity.getSequenceID());
    }

    public static void assertLocationDataEntity(LocationEntity firstEntity, LocationEntity secondEntity) {
        assertNotNull(firstEntity);
        assertNotNull(secondEntity);
        assertEquals(firstEntity.getVideoID(), secondEntity.getVideoID());
        assertEquals(firstEntity.getSequenceID(), secondEntity.getSequenceID());
        assertEquals(firstEntity.getLongitude(), secondEntity.getLongitude(), DELTA);
        assertEquals(firstEntity.getLatitude(), secondEntity.getLatitude(), DELTA);
        assertEquals(firstEntity.getLocationId(), secondEntity.getLocationId());
    }

    public static void assertScoreEntitiesList(List<ScoreEntity> firstEntites, List<ScoreEntity> secondEntities) {
        assertListEqual(firstEntites, secondEntities);

        for (int i = 0; i < firstEntites.size(); i++) {
            DataTestHelper.assertPairScoreEntity(firstEntites.get(i), secondEntities.get(i));
        }
    }

    public static void assertSequenceEntitiesMapList(Map<String, SequenceEntity> entitiesMap, List<SequenceEntity> entitiesList) {
        assertMapListNotEmpty(entitiesMap, entitiesList);
        for (int i = 0; i < entitiesMap.size(); i++) {
            SequenceEntity secondEntity = entitiesList.get(i);
            assertSequencesDataEntity(entitiesMap.get(secondEntity.getSequenceId()), secondEntity);
        }
    }

    public static void assertScoreEntitiesList(Map<String, ScoreEntity> entitiesMap, List<ScoreEntity> entitiesList) {
        assertMapListNotEmpty(entitiesMap, entitiesList);
        entitiesList.forEach(item -> assertScoresDataEntity(entitiesMap.get(item.getScoreId()), item));
    }

    public static void assertVideoEntitiesMapList(Map<String, VideoEntity> entitiesMap, List<VideoEntity> entitiesList) {
        assertMapListNotEmpty(entitiesMap, entitiesList);
        entitiesList.forEach(item -> assertVideoDataEntity(entitiesMap.get(item.getVideoId()), item));
    }

    public static void assertFrameEntitiesMapList(Map<String, FrameEntity> entitiesMap, List<FrameEntity> entitiesList) {
        assertMapListNotEmpty(entitiesMap, entitiesList);
        entitiesList.forEach(item -> assertFrameDataEntity(entitiesMap.get(item.getFrameId()), item));
    }

    public static void assertLocationEntitiesMapList(Map<String, LocationEntity> entitiesMap, List<LocationEntity> entitiesList) {
        assertMapListNotEmpty(entitiesMap, entitiesList);
        entitiesList.forEach(item -> assertLocationDataEntity(entitiesMap.get(item.getLocationId()), item));
    }

    public static <T extends Object> void assertListEqual(List<T> firstList, List<T> secondList) {
        assertNotNull(firstList);
        assertNotNull(secondList);
        assertNotEquals(firstList.size(), 0);
        assertNotEquals(secondList.size(), 0);
    }

    public static List<SequenceEntity> assertInsertGetSequences(int noOfSequences, SequenceDao sequenceDao) {
        List<SequenceEntity> sequenceEntities = generateSequenceEntities(noOfSequences);

        List<Long> longs = sequenceDao.insertAll(sequenceEntities.toArray(new SequenceEntity[0]));
        assertNotEquals(longs.size(), 0);
        assertEquals(longs.size(), noOfSequences);

        return sequenceEntities;
    }

    public static Map<String, ScoreEntity> assertInsertGetScoresMap(int noOfSequences, int scorePerSequence, ScoreDao scoreDao, SequenceDao sequenceDao) {
        List<SequenceEntity> sequenceEntities = assertInsertGetSequences(noOfSequences, sequenceDao);

        Map<String, ScoreEntity> scoreEntities = new HashMap<>();
        for (SequenceEntity sequenceEntity : sequenceEntities) {
            for (int i = 0; i < scorePerSequence; i++) {
                ScoreEntity scoreEntity = assertInsertGetScore(sequenceEntity.getSequenceId(), scoreDao, i);
                scoreEntities.put(scoreEntity.getScoreId(), scoreEntity);
            }
        }

        return scoreEntities;
    }

    public static Map<String, VideoEntity> assertInsertGetVideosMap(int noOfSequences, int videosPerSequence, VideoDao videoDao, SequenceDao sequenceDao) {
        List<SequenceEntity> sequenceEntities = assertInsertGetSequences(noOfSequences, sequenceDao);

        Map<String, VideoEntity> videoEntities = new HashMap<>();
        for (SequenceEntity sequenceEntity : sequenceEntities) {
            for (int i = 0; i < videosPerSequence; i++) {
                VideoEntity videoEntity = assertInsertGetVideo(sequenceEntity.getSequenceId(), videoDao, i);
                videoEntities.put(videoEntity.getVideoId(), videoEntity);
            }
        }

        return videoEntities;
    }

    public static Map<String, List<VideoEntity>> assertInsertGetSequenceIdVideosListMap(int noOfSequences, int videosPerSequence, VideoDao videoDao, SequenceDao sequenceDao) {
        List<SequenceEntity> sequenceEntities = assertInsertGetSequences(noOfSequences, sequenceDao);

        Map<String, List<VideoEntity>> videoEntities = new HashMap<>();
        for (SequenceEntity sequenceEntity : sequenceEntities) {
            List<VideoEntity> videoList = new ArrayList<>();
            for (int i = 0; i < videosPerSequence; i++) {
                VideoEntity videoEntity = assertInsertGetVideo(sequenceEntity.getSequenceId(), videoDao, i);
                videoList.add(videoEntity);
            }
            videoEntities.put(sequenceEntity.getSequenceId(), videoList);
        }

        return videoEntities;
    }

    public static SequenceEntity assertInsertGetSequence(SequenceDao sequenceDao) {
        SequenceEntity sequenceEntity = generateSequenceEntity();
        long seqRowId = sequenceDao.insert(sequenceEntity);
        assertNotEquals(seqRowId, 0);

        return sequenceEntity;
    }

    public static ScoreEntity assertInsertGetAutogeneratedSequenceScore(SequenceDao sequenceDao, ScoreDao scoreDao, int coverage) {
        SequenceEntity sequenceEntity = assertInsertGetSequence(sequenceDao);
        ScoreEntity scoreEntity = generateScoreEntity(sequenceEntity.getSequenceId(), coverage);
        long seqRowId = scoreDao.insert(scoreEntity);
        assertNotEquals(seqRowId, 0);

        return scoreEntity;
    }

    public static VideoEntity assertInsertGetAutogeneratedSequenceVideo(SequenceDao sequenceDao, VideoDao videoDao, int videoIndex) {
        SequenceEntity sequenceEntity = assertInsertGetSequence(sequenceDao);
        VideoEntity videoEntity = generateVideoEntity(sequenceEntity.getSequenceId(), videoIndex);
        long seqRowId = videoDao.insert(videoEntity);
        assertNotEquals(seqRowId, 0);

        return videoEntity;
    }

    public static ScoreEntity assertInsertGetScore(String seqId, ScoreDao scoreDao, int coverage) {
        ScoreEntity scoreEntity = generateScoreEntity(seqId, coverage);
        long seqRowId = scoreDao.insert(scoreEntity);
        assertNotEquals(seqRowId, 0);

        return scoreEntity;
    }

    public static VideoEntity assertInsertGetVideo(String seqId, VideoDao videoDao, int videoIndex) {
        VideoEntity videoEntity = generateVideoEntity(seqId, videoIndex);
        long seqRowId = videoDao.insert(videoEntity);
        assertNotEquals(seqRowId, 0);

        return videoEntity;
    }

    private static <T extends Object> void assertMapListNotEmpty(Map<String, T> entitiesMap, List<T> entitiesList) {
        assertNotNull(entitiesMap);
        assertNotEquals(entitiesMap.size(), 0);
        assertNotNull(entitiesList);
        assertNotEquals(entitiesList.size(), 0);
        assertEquals(entitiesMap.size(), entitiesList.size());
    }
}
