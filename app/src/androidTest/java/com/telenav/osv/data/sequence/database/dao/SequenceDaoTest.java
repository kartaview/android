package com.telenav.osv.data.sequence.database.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.content.Context;
import com.telenav.osv.data.DataTestHelper;
import com.telenav.osv.data.database.OSCDatabase;
import com.telenav.osv.data.score.database.dao.ScoreDao;
import com.telenav.osv.data.score.database.entity.ScoreEntity;
import com.telenav.osv.data.sequence.database.entity.SequenceEntity;
import com.telenav.osv.data.sequence.database.entity.SequenceWithRewardEntity;
import com.telenav.osv.utils.StringUtils;
import androidx.core.util.Pair;
import androidx.room.Room;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author horatiuf
 */
@RunWith(AndroidJUnit4.class)
public class SequenceDaoTest {

    private OSCDatabase database;

    private SequenceDao sequenceDao;

    private ScoreDao scoreDao;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, OSCDatabase.class).build();
        scoreDao = database.scoreDao();
        sequenceDao = database.sequenceDao();
    }

    @After
    public void closeDb() {
        database.close();
    }

    @Test
    public void insert() {
        SequenceEntity sequenceEntity = DataTestHelper.assertInsertGetSequence(sequenceDao);
        SequenceEntity entityFound = sequenceDao.findByID(sequenceEntity.getSequenceId()).blockingGet();
        DataTestHelper.assertSequencesDataEntity(sequenceEntity, entityFound);
    }

    @Test
    public void findEmptySequencesAll() {
        List<SequenceEntity> sequenceEntities = sequenceDao.findAll().blockingGet();

        assertEquals(sequenceEntities.size(), 0);
    }

    @Test
    public void findSequences() {
        List<SequenceEntity> sequenceEntities = DataTestHelper.generateSequenceEntities(3);

        List<Long> longs = sequenceDao.insertAll(sequenceEntities.toArray(new SequenceEntity[0]));

        List<SequenceEntity> dbEntities = sequenceDao.findAll().blockingGet();

        assertNotEquals(longs.size(), 0);
        assertEquals(sequenceEntities.size(), dbEntities.size());
        for (int i = 0; i < sequenceEntities.size(); i++) {
            DataTestHelper.assertSequencesDataEntity(sequenceEntities.get(i), dbEntities.get(i));
        }
    }

    @Test
    public void findSequencesIds() {
        Map<String, SequenceEntity> sequenceEntities = DataTestHelper.generateSequenceEntitiesMap(3);

        List<Long> longs = sequenceDao.insertAll(sequenceEntities.values().toArray(new SequenceEntity[0]));

        List<String> dbEntitiesIds = sequenceDao.findAllIds().blockingGet();

        assertNotEquals(longs.size(), 0);
        assertEquals(sequenceEntities.size(), dbEntitiesIds.size());

        dbEntitiesIds.forEach(dbId -> {
            SequenceEntity sequenceEntity = sequenceEntities.get(dbId);
            assertNotNull(sequenceEntity);
            assertNotEquals(sequenceEntity.getSequenceId(), StringUtils.EMPTY_STRING);
            assertEquals(dbId, sequenceEntity.getSequenceId());
        });
    }

    @Test
    public void findAllWithRewards() {
        List<SequenceEntity> sequenceEntities = DataTestHelper.generateSequenceEntities(2);
        List<Long> longs = sequenceDao.insertAll(sequenceEntities.toArray(new SequenceEntity[0]));
        assertNotEquals(longs.size(), 0);

        Map<String, Pair<SequenceEntity, List<ScoreEntity>>> map = new HashMap<>();
        for (SequenceEntity sequenceEntity : sequenceEntities) {
            List<ScoreEntity> scoreEntities = DataTestHelper.generateScoreEntities(sequenceEntity.getSequenceId(), 5);
            map.put(sequenceEntity.getSequenceId(), new Pair<>(sequenceEntity, scoreEntities));
            List<Long> scoreInserts = scoreDao.insertAll(scoreEntities.toArray(new ScoreEntity[0]));
            assertNotEquals(scoreInserts.size(), 0);
        }

        List<SequenceWithRewardEntity> sequenceWithRewardEntities = sequenceDao.findAllWithRewards().blockingGet();
        for (SequenceWithRewardEntity sequenceWithRewardEntity : sequenceWithRewardEntities) {
            SequenceEntity dbSequence = sequenceWithRewardEntity.getSequenceEntity();
            Pair<SequenceEntity, List<ScoreEntity>> sequenceScorePair = map.get(dbSequence.getSequenceId());
            DataTestHelper.assertSequencesDataEntity(dbSequence, sequenceScorePair.first);
            List<ScoreEntity> scoreEntities = sequenceScorePair.second;
            List<ScoreEntity> dbScoreEntities = sequenceWithRewardEntity.getScoreEntities();
            assertNotEquals(dbScoreEntities.size(), 0);
            for (int i = 0; i < scoreEntities.size(); i++) {
                DataTestHelper.assertPairScoreEntity(sequenceWithRewardEntity.getScoreEntities().get(i), scoreEntities.get(i));
            }
        }
    }

    @Test
    public void findAllByIds() {
        Map<String, SequenceEntity> sequenceEntitiesMap = DataTestHelper.generateSequenceEntitiesMap(2);

        List<Long> longs = sequenceDao.insertAll(sequenceEntitiesMap.values().toArray(new SequenceEntity[0]));
        assertNotEquals(longs.size(), 0);


        List<SequenceEntity> dbSequenceEntities = sequenceDao.findAllByIds(sequenceEntitiesMap.keySet().toArray(new String[0])).blockingGet();
        DataTestHelper.assertSequenceEntitiesMapList(sequenceEntitiesMap, dbSequenceEntities);
    }

    @Test
    public void findByID() {
        SequenceEntity sequenceEntity = DataTestHelper.generateSequenceEntity();

        long rowId = sequenceDao.insert(sequenceEntity);

        assertNotEquals(rowId, 0);

        SequenceEntity dbSequenceEntity = database.sequenceDao().findByID(sequenceEntity.getSequenceId()).blockingGet();
        assertNotNull(dbSequenceEntity);
        DataTestHelper.assertSequencesDataEntity(sequenceEntity, dbSequenceEntity);
    }

    @Test
    public void findByIDWithReward() {
        SequenceEntity sequenceEntity = DataTestHelper.generateSequenceEntity();
        List<ScoreEntity> scoreEntities = DataTestHelper.generateScoreEntities(sequenceEntity.getSequenceId(), 10);


        long seqRowId = sequenceDao.insert(sequenceEntity);
        List<Long> longs = scoreDao.insertAll(scoreEntities.toArray(new ScoreEntity[0]));

        assertNotEquals(seqRowId, 0);
        assertNotEquals(longs.size(), 0);

        SequenceWithRewardEntity sequenceWithRewardEntity = sequenceDao.findByIDWithReward(sequenceEntity.getSequenceId()).blockingGet();

        assertNotNull(sequenceWithRewardEntity);
        DataTestHelper.assertScoreEntitiesList(scoreEntities, sequenceWithRewardEntity.getScoreEntities());
    }

    @Test
    public void findBySequenceID() {
        SequenceEntity sequenceEntity = DataTestHelper.assertInsertGetSequence(sequenceDao);

        SequenceEntity dbSequenceEntity = sequenceDao.findBySequenceID(sequenceEntity.getSequenceId()).blockingGet();

        DataTestHelper.assertSequencesDataEntity(sequenceEntity, dbSequenceEntity);

    }

    @Test
    public void deleteById() {
        SequenceEntity sequenceEntity = DataTestHelper.assertInsertGetSequence(sequenceDao);

        long rowId = sequenceDao.deleteById(sequenceEntity.getSequenceId());

        assertEquals(rowId, 1);
    }

    @Test
    public void deleteByIdNonExistent() {
        long rowId = sequenceDao.deleteById(UUID.randomUUID().toString());

        assertEquals(rowId, 0);
    }

    @Test
    public void updateObd() {
        SequenceEntity sequenceEntity = DataTestHelper.assertInsertGetSequence(sequenceDao);

        int updatedRows = sequenceDao.updateObd(sequenceEntity.getSequenceId(), !sequenceEntity.isObd());

        assertEquals(updatedRows, 1);

        SequenceEntity dbSequenceEntity = sequenceDao.findBySequenceID(sequenceEntity.getSequenceId()).blockingGet();
        assertNotEquals(dbSequenceEntity.isObd(), sequenceEntity.isObd());
        assertEquals(dbSequenceEntity.isObd(), !sequenceEntity.isObd());
    }

    @Test
    public void updateOnlineId() {
        SequenceEntity sequenceEntity = DataTestHelper.assertInsertGetSequence(sequenceDao);

        long newOnlineId = new Random().nextLong();
        int updatedRows = sequenceDao.updateOnlineId(sequenceEntity.getSequenceId(), newOnlineId);

        assertEquals(updatedRows, 1);

        SequenceEntity dbSequenceEntity = sequenceDao.findBySequenceID(sequenceEntity.getSequenceId()).blockingGet();
        assertNotEquals(dbSequenceEntity.getOnlineID(), sequenceEntity.getOnlineID());
        assertEquals((long) dbSequenceEntity.getOnlineID(), newOnlineId);
    }

    @Test
    public void updateDiskSize() {
        SequenceEntity sequenceEntity = DataTestHelper.assertInsertGetSequence(sequenceDao);

        long diskSize = new Random().nextLong();
        int updatedRows = sequenceDao.updateDiskSize(sequenceEntity.getSequenceId(), diskSize);

        assertEquals(updatedRows, 1);

        SequenceEntity dbSequenceEntity = sequenceDao.findBySequenceID(sequenceEntity.getSequenceId()).blockingGet();
        assertNotEquals(dbSequenceEntity.getDiskSize(), sequenceEntity.getDiskSize());
        assertEquals((long) dbSequenceEntity.getDiskSize(), diskSize);
    }

    @Test
    public void updateSizeInfo() {
        SequenceEntity sequenceEntity = DataTestHelper.assertInsertGetSequence(sequenceDao);

        Random random = new Random();
        long diskSize = random.nextLong();
        int frameCount = random.nextInt();
        int videoCount = random.nextInt();
        int updatedRows = sequenceDao.updateSizeInfo(sequenceEntity.getSequenceId(), diskSize, frameCount, videoCount);

        assertEquals(updatedRows, 1);

        SequenceEntity dbSequenceEntity = sequenceDao.findBySequenceID(sequenceEntity.getSequenceId()).blockingGet();
        assertNotEquals(dbSequenceEntity.getDiskSize(), sequenceEntity.getDiskSize());
        assertNotEquals(dbSequenceEntity.getLocationsCount(), sequenceEntity.getLocationsCount());
        assertNotEquals(dbSequenceEntity.getVideoCount(), sequenceEntity.getVideoCount());
        assertEquals((long) dbSequenceEntity.getDiskSize(), diskSize);
        assertEquals((int) dbSequenceEntity.getLocationsCount(), frameCount);
        assertEquals((int) dbSequenceEntity.getVideoCount(), videoCount);
    }

    @Test
    public void updateAddressName() {
        SequenceEntity sequenceEntity = DataTestHelper.assertInsertGetSequence(sequenceDao);

        String addressName = UUID.randomUUID().toString();
        int updatedRows = sequenceDao.updateAddressName(sequenceEntity.getSequenceId(), addressName);

        assertEquals(updatedRows, 1);

        SequenceEntity dbSequenceEntity = sequenceDao.findBySequenceID(sequenceEntity.getSequenceId()).blockingGet();
        assertNotEquals(dbSequenceEntity.getAddressName(), sequenceEntity.getAddressName());
        assertEquals(dbSequenceEntity.getAddressName(), addressName);
    }

    @Test
    public void updateConsistencyStatus() {
        SequenceEntity sequenceEntity = DataTestHelper.assertInsertGetSequence(sequenceDao);

        int consistencyStatus = 99;
        int updatedRows = sequenceDao.updateConsistencyStatus(sequenceEntity.getSequenceId(), consistencyStatus);

        assertEquals(updatedRows, 1);

        SequenceEntity dbSequenceEntity = sequenceDao.findBySequenceID(sequenceEntity.getSequenceId()).blockingGet();
        assertNotEquals(dbSequenceEntity.getConsistencyStatus(), sequenceEntity.getConsistencyStatus());
        assertEquals((int) dbSequenceEntity.getConsistencyStatus(), consistencyStatus);
    }

    @Test
    public void update() {
        SequenceEntity sequenceEntity = DataTestHelper.assertInsertGetSequence(sequenceDao);

        SequenceEntity sequenceFromWhichDataToBeUpdated = DataTestHelper.generateSequenceEntity();

        SequenceEntity sequenceToBeUpdated = new SequenceEntity(
                sequenceEntity.getSequenceId(),
                sequenceFromWhichDataToBeUpdated.isObd(),
                sequenceFromWhichDataToBeUpdated.getLatitude(),
                sequenceFromWhichDataToBeUpdated.getLongitude(),
                sequenceFromWhichDataToBeUpdated.getAddressName(),
                sequenceFromWhichDataToBeUpdated.getDistance(),
                sequenceFromWhichDataToBeUpdated.getAppVersion(),
                sequenceFromWhichDataToBeUpdated.getCreationTime(),
                sequenceFromWhichDataToBeUpdated.getLocationsCount(),
                sequenceFromWhichDataToBeUpdated.getVideoCount(),
                sequenceFromWhichDataToBeUpdated.getDiskSize(),
                sequenceFromWhichDataToBeUpdated.getFilePath(),
                sequenceFromWhichDataToBeUpdated.getOnlineID(),
                sequenceFromWhichDataToBeUpdated.getConsistencyStatus(),
                sequenceFromWhichDataToBeUpdated.getBoundingNorthLat(),
                sequenceFromWhichDataToBeUpdated.getBoundingSouthLat(),
                sequenceFromWhichDataToBeUpdated.getBoundingWestLon(),
                sequenceFromWhichDataToBeUpdated.getBoundingEastLon());
        int update = sequenceDao.update(sequenceToBeUpdated);

        assertEquals(update, 1);

        SequenceEntity dbSequenceEntity = sequenceDao.findBySequenceID(sequenceEntity.getSequenceId()).blockingGet();
        DataTestHelper.assertSequencesDataEntity(dbSequenceEntity, sequenceToBeUpdated);
    }

    @Test
    public void countAllEmpty() {
        assertEquals(sequenceDao.countAll(), 0);
    }

    @Test
    public void countAllInsert() {
        List<SequenceEntity> sequenceEntities = DataTestHelper.assertInsertGetSequences(3, sequenceDao);
        assertEquals(sequenceDao.countAll(), sequenceEntities.size());
    }

    @Test
    public void deleteAll() {
        List<SequenceEntity> sequenceEntities = DataTestHelper.assertInsertGetSequences(3, sequenceDao);

        int number = sequenceDao.deleteAll();

        assertEquals(number, sequenceEntities.size());
    }
}