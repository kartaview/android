package com.telenav.osv.recorder.score;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import android.content.Context;
import android.location.Location;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.RandomUtils;
import com.telenav.datacollectormodule.datatype.datatypes.SpeedObject;
import com.telenav.datacollectormodule.datatype.util.LibraryUtil;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.data.score.datasource.ScoreDataSource;
import com.telenav.osv.event.OSVEvent;
import com.telenav.osv.item.Polyline;
import com.telenav.osv.item.Segment;
import com.telenav.osv.location.LocationService;
import com.telenav.osv.network.payrate.model.PayRateCoverageInterval;
import com.telenav.osv.network.payrate.model.PayRateData;
import com.telenav.osv.network.payrate.model.PayRateItem;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.recorder.score.event.ByodDriverPayRateUpdatedEvent;
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
    public void testSetUserType() throws Exception {
        Field fieldUserType = ScoreManager.class.getDeclaredField("isUserByod20");
        fieldUserType.setAccessible(true);
        Assert.assertEquals(false, fieldUserType.get(score));
        score.setUserType(true);
        Assert.assertEquals(true, fieldUserType.get(score));
        score.setUserType(false);
        Assert.assertEquals(false, fieldUserType.get(score));
    }

    @Test
    public void testPayRateData() throws Exception {
        Field fieldPayRateData = ScoreManager.class.getDeclaredField("payRateData");
        fieldPayRateData.setAccessible(true);
        PayRateData payRateData = createPayRateData();
        score.setPayRateData(payRateData);
        PayRateData actualPayRateData = (PayRateData) fieldPayRateData.get(score);
        Assert.assertEquals(payRateData, actualPayRateData);
        Assert.assertEquals(payRateData.getCurrency(), actualPayRateData.getCurrency());
        Assert.assertEquals(payRateData.getPayRates().get(0), actualPayRateData.getPayRates().get(0));
        Assert.assertEquals(payRateData, score.getPayRateData());
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
        Segment segment = createSegment(0);
        Mockito.when(positionMatcher.onLocationChanged(location)).thenReturn(segment);
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
        Location location = new Location("");
        Segment segment = createSegment(0);
        Mockito.when(positionMatcher.onPictureTaken(location)).thenReturn(segment);
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
        Mockito.when(positionMatcher.onPictureTaken(location)).thenReturn(segment);
        setRecording(true);
        SpeedObject speedObject = new SpeedObject(10, LibraryUtil.OBD_READ_SUCCESS, LibraryUtil.SPEED) {
            @Override
            public double getSpeed() {
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
    public void testPayRateUpdateWithoutSettingThePayRates() {
        Location location = new Location("");
        Segment segment = createSegment(0);
        Mockito.when(positionMatcher.onLocationChanged(location)).thenReturn(segment);
        setRecording(true);
        score.setUserType(true);
        TestObserver<OSVEvent> testObserver = score.getScoreUpdates().test();
        locationFlowableEmitter.onNext(location);
        testObserver.assertNoValues();
        testObserver.dispose();
    }

    @Test
    public void testPayRateUpdateWhenLocationChanged() {
        Location location = new Location("");
        location.setLatitude(1);
        location.setLongitude(1);
        Segment segment = createSegment(0);
        Mockito.when(positionMatcher.onLocationChanged(location)).thenReturn(segment);
        setRecording(true);
        PayRateData payRateData = createPayRateData();
        score.setPayRateData(payRateData);
        score.setUserType(true);
        TestObserver<OSVEvent> testObserver = score.getScoreUpdates().test();
        locationFlowableEmitter.onNext(location);
        testObserver.assertValueCount(1);
        testObserver.dispose();
    }

    @Test
    public void testPayRateUpdateWhenAPhotoWasTaken() {
        Location location = new Location("");
        Segment segment = createSegment(0);
        Mockito.when(positionMatcher.onPictureTaken(location)).thenReturn(segment);
        setRecording(true);
        PayRateData payRateData = createPayRateData();
        score.setPayRateData(payRateData);
        score.setUserType(true);
        TestObserver<OSVEvent> testObserver = score.getScoreUpdates().test();
        score.onPictureTaken(location);
        testObserver.assertValueCount(1);
        testObserver.assertValue(osvEvent -> {
            ByodDriverPayRateUpdatedEvent byodDriverPayRateUpdatedEvent = (ByodDriverPayRateUpdatedEvent) osvEvent;
            return byodDriverPayRateUpdatedEvent.getCurrency().equals("USD");
        });
        testObserver.dispose();
    }

    private PayRateData createPayRateData() {
        PayRateData payRateData = new PayRateData();
        payRateData.setCurrency("USD");
        List<PayRateItem> payRateItemList = new ArrayList<>();
        payRateItemList.add(new PayRateItem(new PayRateCoverageInterval(RandomUtils.generateInt(), RandomUtils.generateInt()), RandomUtils.generateInt(), RandomUtils.generateInt
                ()));
        payRateData.setPayRates(payRateItemList);
        return payRateData;
    }

    private void setRecording(boolean startRecording) {
        score.onRecordingStateChanged(startRecording, UUID.randomUUID().toString());
    }

    private Segment createSegment(int coverage) {
        SKCoordinate skCoordinate = new SKCoordinate(0, 0);
        Polyline polyline = new Polyline(POLYLINE_IDENTIFIER);
        polyline.coverage = coverage;
        return new Segment(RandomUtils.generateDouble(), skCoordinate, polyline, skCoordinate, skCoordinate, RandomUtils.generateDouble(), RandomUtils.generateInt());
    }
}
