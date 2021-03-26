package com.telenav.osv.data.video.database.dao;

import android.content.Context;

import androidx.room.Room;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.telenav.osv.data.DataTestHelper;
import com.telenav.osv.data.database.KVDatabase;
import com.telenav.osv.data.sequence.database.dao.SequenceDao;
import com.telenav.osv.data.video.database.entity.VideoEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author horatiuf
 */
@RunWith(AndroidJUnit4.class)
public class VideoDaoTest {

    private SequenceDao sequenceDao;

    private VideoDao videoDao;

    private KVDatabase database;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, KVDatabase.class).build();
        videoDao = database.videoDao();
        sequenceDao = database.sequenceDao();
    }

    @After
    public void closeDb() {
        database.close();
    }

    @Test
    public void insertAll() {
        DataTestHelper.assertInsertGetVideosMap(4, 22, videoDao, sequenceDao);
    }

    @Test
    public void insert() {
        DataTestHelper.assertInsertGetAutogeneratedSequenceVideo(sequenceDao, videoDao, 0);
    }

    @Test
    public void findAll() {
        Map<String, VideoEntity> videoEntityMap = DataTestHelper.assertInsertGetVideosMap(4, 22, videoDao, sequenceDao);

        List<VideoEntity> videoEntities = videoDao.findAll().blockingGet();

        DataTestHelper.assertVideoEntitiesMapList(videoEntityMap, videoEntities);
    }

    @Test
    public void getVideosByIds() {
        Map<String, VideoEntity> videoEntityMap = DataTestHelper.assertInsertGetVideosMap(4, 22, videoDao, sequenceDao);
        DataTestHelper.assertInsertGetVideosMap(3, 13, videoDao, sequenceDao);

        List<VideoEntity> videoEntities = videoDao.findAllByIds(videoEntityMap.keySet().toArray(new String[0])).blockingGet();
        DataTestHelper.assertVideoEntitiesMapList(videoEntityMap, videoEntities);
    }

    @Test
    public void findAllBySequenceID() {
        Map<String, List<VideoEntity>> videoEntityMap = DataTestHelper.assertInsertGetSequenceIdVideosListMap(4, 22, videoDao, sequenceDao);

        for (String sequenceId : videoEntityMap.keySet()) {
            List<VideoEntity> videoEntities = videoDao.findAllBySequenceID(sequenceId).blockingGet();

            Map<String, VideoEntity> videoEntityMapForSequence = new HashMap<>();
            videoEntityMap.get(sequenceId).forEach(item -> videoEntityMapForSequence.put(item.getVideoId(), item));
            DataTestHelper.assertVideoEntitiesMapList(videoEntityMapForSequence, videoEntities);
        }
    }

    @Test
    public void findByID() {
        VideoEntity videoEntity = DataTestHelper.assertInsertGetAutogeneratedSequenceVideo(sequenceDao, videoDao, 0);

        VideoEntity dbEntity = videoDao.findByID(videoEntity.getVideoId()).blockingGet();

        DataTestHelper.assertVideoDataEntity(videoEntity, dbEntity);
    }

    @Test
    public void findBySequenceID() {
        VideoEntity videoEntity = DataTestHelper.assertInsertGetAutogeneratedSequenceVideo(sequenceDao, videoDao, 0);
        VideoEntity dbEntity = videoDao.findBySequenceID(videoEntity.getSequenceID()).blockingGet();
        DataTestHelper.assertVideoDataEntity(videoEntity, dbEntity);
    }

    @Test
    public void updateFrameCount() {
        VideoEntity videoEntity = DataTestHelper.assertInsertGetAutogeneratedSequenceVideo(sequenceDao, videoDao, 0);

        int frameCount = new Random().nextInt();
        int updateNo = videoDao.updateFrameCount(videoEntity.getVideoId(), frameCount);
        VideoEntity dbEntity = videoDao.findByID(videoEntity.getVideoId()).blockingGet();

        assertEquals(updateNo, 1);
        assertEquals(frameCount, (int) dbEntity.getFrameCount());
        assertNotEquals(videoEntity.getFrameCount(), videoEntity);
    }

    @Test
    public void deleteByVideoId() {
        VideoEntity videoEntity = DataTestHelper.assertInsertGetAutogeneratedSequenceVideo(sequenceDao, videoDao, 0);
        int deleteNo = videoDao.deleteById(videoEntity.getVideoId());
        assertEquals(deleteNo, 1);
    }

    @Test
    public void deleteBySequenceId() {
        VideoEntity videoEntity = DataTestHelper.assertInsertGetAutogeneratedSequenceVideo(sequenceDao, videoDao, 0);
        int deleteNo = videoDao.deleteBySequenceId(videoEntity.getSequenceID());
        assertEquals(deleteNo, 1);
    }

    @Test
    public void deleteAll() {
        int noOfSequences = 4;
        int videosPerSequences = 22;
        DataTestHelper.assertInsertGetVideosMap(noOfSequences, videosPerSequences, videoDao, sequenceDao);
        int deleteNo = videoDao.deleteAll();
        assertEquals(deleteNo, noOfSequences * videosPerSequences);
    }
}