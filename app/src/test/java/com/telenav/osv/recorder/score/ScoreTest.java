package com.telenav.osv.recorder.score;

import android.content.Context;
import android.location.Location;

import com.telenav.RandomUtils;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.common.model.KVLatLng;
import com.telenav.osv.data.collector.datatype.datatypes.SpeedObject;
import com.telenav.osv.data.collector.datatype.util.LibraryUtil;
import com.telenav.osv.data.score.datasource.ScoreDataSource;
import com.telenav.osv.event.OSVEvent;
import com.telenav.osv.item.Polyline;
import com.telenav.osv.item.Segment;
import com.telenav.osv.location.LocationService;
import com.telenav.osv.obd.manager.ObdManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.util.UUID;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.observers.TestObserver;

/**
 * Created by cameliao on 2/2/18.
 */

@RunWith(RobolectricTestRunner.class)
public class ScoreTest {

    private static final int POLYLINE_IDENTIFIER = 1;

    private Score score;

    @Mock
    private Context context;

    @Mock
    private ApplicationPreferences appPrefs;

    @Mock
    private ScoreDataSource scoreDataSource;

    @Mock
    private PositionMatcher positionMatcher;

    @Mock
    private LocationService locationService;

    private ObdManager obdManager;

    private FlowableEmitter<Location> locationFlowableEmitter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(locationService.getLocationUpdates())
                .thenReturn(Flowable.create(emitter -> locationFlowableEmitter = emitter, BackpressureStrategy.DROP));
        obdManager = ObdManager.getInstance(context, appPrefs);
        score = new ScoreManager(scoreDataSource, positionMatcher, locationService, obdManager);
    }

    @After
    public void tearDown() throws Exception {
        setRecording(false);
        score.release();
        Field instance = ObdManager.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    public void testScoreUpdateWithoutStaringTheRecording() {
        Location location = new Location("");
        TestObserver<OSVEvent> testObserver = score.getScoreUpdates().test();
        locationFlowableEmitter.onNext(location);
        testObserver.assertNoValues();
        testObserver.dispose();
    }

    @Test
    public void testScoreUpdateWhenLocationChanged() {
        Location location = new Location("");
        location.setLatitude(1);
        location.setLongitude(1);
        KVLatLng kvLatLng = new KVLatLng(location, RandomUtils.generateInt());
        Segment segment = createSegment(0);
        Mockito.when(positionMatcher.onLocationChanged(kvLatLng)).thenReturn(segment);
        setRecording(true);
        TestObserver<OSVEvent> testObserver = score.getScoreUpdates().test();
        locationFlowableEmitter.onNext(location);
        //Todo: @CameliaOana fix issues with assert on the observable
        /*testObserver.assertValueCount(1);
        testObserver.assertValue(osvEvent -> {
            ScoreChangedEvent scoreChangedEvent = (ScoreChangedEvent) osvEvent;
            return scoreChangedEvent.getScore() == 0 && scoreChangedEvent.getMultiplier() == 10;
        });*/
        testObserver.dispose();
    }

    @Test
    public void testScoreUpdateWhenAPhotoWasTaken() {
        Segment segment = createSegment(0);
        Location location = new Location("");
        location.setLatitude(1);
        location.setLongitude(1);
        KVLatLng kvLatLng = new KVLatLng(location, RandomUtils.generateInt());
        Mockito.when(positionMatcher.onPictureTaken(kvLatLng)).thenReturn(segment);
        setRecording(true);
        TestObserver<OSVEvent> testObserver = score.getScoreUpdates().test();
        score.onPictureTaken(location);
        //Todo: @CameliaOana fix issues with assert on the observable
        /*testObserver.assertValueCount(1);
        testObserver.assertValue(osvEvent -> {
            ScoreChangedEvent scoreChangedEvent = (ScoreChangedEvent) osvEvent;
            return scoreChangedEvent.getScore() == 10 && scoreChangedEvent.getMultiplier() == 10;
        });*/
        testObserver.dispose();
    }

    @Test
    public void testScoreUpdateWhenAPhotoWasTakenWithOBD() {
        Location location = new Location("");
        Segment segment = createSegment(0);
        Mockito.when(positionMatcher.onPictureTaken(generateKVLatLng())).thenReturn(segment);
        setRecording(true);
        SpeedObject speedObject = new SpeedObject(10, LibraryUtil.OBD_READ_SUCCESS, LibraryUtil.SPEED) {
            @Override
            public int getSpeed() {
                return super.getSpeed();
            }

            @Override
            public String getSensorType() {
                return LibraryUtil.SPEED;
            }

            @Override
            public int getStatusCode() {
                return LibraryUtil.OBD_READ_SUCCESS;
            }
        };
        obdManager.onNewEvent(speedObject);
        TestObserver<OSVEvent> testObserver = score.getScoreUpdates().test();
        score.onPictureTaken(location);
        //Todo: @CameliaOana fix issues with assert on the observable
        /*testObserver.assertValueCount(1);
        testObserver.assertValue(osvEvent -> {
            ScoreChangedEvent scoreChangedEvent = (ScoreChangedEvent) osvEvent;
            return scoreChangedEvent.getScore() == 20 && scoreChangedEvent.getMultiplier() == 20;
        });*/
        testObserver.dispose();
    }

    @Test
    public void testScoreUpdateWithoutSettingTheSoreUpdates() {
        Location location = new Location("");
        Segment segment = createSegment(0);
        Mockito.when(positionMatcher.onLocationChanged(generateKVLatLng())).thenReturn(segment);
        setRecording(true);
        TestObserver<OSVEvent> testObserver = score.getScoreUpdates().test();
        locationFlowableEmitter.onNext(location);
        testObserver.assertNoValues();
        testObserver.dispose();
    }

    private void setRecording(boolean startRecording) {
        score.onRecordingStateChanged(startRecording, UUID.randomUUID().toString());
    }

    private Segment createSegment(int coverage) {
        Polyline polyline = new Polyline(POLYLINE_IDENTIFIER);
        polyline.coverage = coverage;
        return new Segment(RandomUtils.generateDouble(), generateKVLatLng(), polyline, generateKVLatLng(), generateKVLatLng(), RandomUtils.generateDouble(), RandomUtils.generateInt());
    }

    private KVLatLng generateKVLatLng() {
        return new KVLatLng(RandomUtils.generateDouble(), RandomUtils.generateDouble(), RandomUtils.generateInt());
    }
}
