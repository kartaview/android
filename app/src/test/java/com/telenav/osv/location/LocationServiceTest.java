package com.telenav.osv.location;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import android.content.Context;
import android.location.Location;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.filter.FilterFactory;
import com.telenav.osv.location.filter.LocationFilterType;
import junit.framework.Assert;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subscribers.TestSubscriber;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LocationProvider.class, AccuracyQualityChecker.class})
public class LocationServiceTest {

    private static final String LOCATION_PROVIDER = "Test";

    private static final long DELAY_TIME_7_SEC = 7;

    @Mock
    private Context context;

    @Mock
    private LocationProvider locationProvider;

    @Mock
    private AccuracyQualityChecker accuracyQualityChecker;

    private LocationService locationService;

    private TestScheduler testScheduler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(LocationProvider.class);
        PowerMockito.when(LocationProvider.get(context)).thenReturn(locationProvider);
        PowerMockito.whenNew(AccuracyQualityChecker.class).withNoArguments().thenReturn(accuracyQualityChecker);
        locationService = Injection.provideLocationService(context);
        testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);
    }

    @After
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        RxJavaPlugins.reset();
        Field instance = LocationServiceManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        locationService = null;
    }

    @Test
    public void testLastKnownLocationOnSuccess() {
        Location location = new Location(LOCATION_PROVIDER);
        PowerMockito.when(locationProvider.getLastKnownLocation()).thenReturn(Maybe.create(emitter -> {
            emitter.onSuccess(location);
        }));
        TestObserver<Location> testObserver = locationService.getLastKnownLocation().test();
        testObserver.assertSubscribed();
        testObserver.assertValue(location);
        testObserver.dispose();
    }

    @Test
    public void testLastKnownLocationOnError() {
        PowerMockito.when(locationProvider.getLastKnownLocation()).thenReturn(Maybe.create(MaybeEmitter::onComplete));
        locationService.getLastKnownLocation().test().assertComplete();
    }

    @Test
    public void testLocationUpdates() {
        Location location = new Location(LOCATION_PROVIDER);
        PowerMockito.when(locationProvider.getLocationUpdates()).thenReturn(Flowable.create(emitter -> {
            emitter.onNext(location);
            emitter.onComplete();
        }, BackpressureStrategy.DROP));
        TestSubscriber<Location> testSubscriber = locationService.getLocationUpdates().test();
        testSubscriber.assertSubscribed();
        testScheduler.advanceTimeBy(DELAY_TIME_7_SEC, TimeUnit.SECONDS);
        testSubscriber.assertValue(location);
        testSubscriber.assertComplete()
                .assertValueCount(1);
        testSubscriber.dispose();
    }

    @Test
    public void testLocationUpdatesWithMultipleObservers() {
        Location location = new Location(LOCATION_PROVIDER);
        final boolean[] isCancelled = {false};
        Flowable<Location> flowable = Flowable.create((FlowableOnSubscribe<Location>) emitter -> {
            emitter.onNext(location);
            emitter.onComplete();
            emitter.setCancellable(() -> isCancelled[0] = true);
        }, BackpressureStrategy.DROP).share();
        PowerMockito.when(locationProvider.getLocationUpdates()).thenReturn(flowable);
        TestSubscriber<Location> testSubscriber = locationService.getLocationUpdates().test();
        TestSubscriber<Location> testSubscriber1 = locationService.getLocationUpdates().test();
        testScheduler.advanceTimeBy(DELAY_TIME_7_SEC, TimeUnit.SECONDS);
        testSubscriber.assertValueCount(1);
        testSubscriber.assertValue(location);
        testSubscriber1.assertValueCount(1);
        testSubscriber1.assertValue(location);
        testSubscriber.dispose();
        testSubscriber1.dispose();
        Assert.assertTrue(isCancelled[0]);
    }

    @Test
    public void testLocationUpdatesFilterZeroValues() {
        Location location = new Location(LOCATION_PROVIDER); // (lat, lon)  = (0,0), the event shouldn't be received.
        PowerMockito.when(locationProvider.getLocationUpdates()).thenReturn(Flowable.create((FlowableOnSubscribe<Location>) emitter -> {
            emitter.onNext(location);
            emitter.onComplete();
        }, BackpressureStrategy.DROP).filter(FilterFactory.getLocationFilter(LocationFilterType.FILTER_ZERO_VALUES)));
        TestSubscriber<Location> testSubscriber = locationService.getLocationUpdates().test();
        testSubscriber.assertSubscribed();
        testScheduler.advanceTimeBy(DELAY_TIME_7_SEC, TimeUnit.SECONDS);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(0);
        testSubscriber.dispose();
    }

    @Test
    public void testAccuracyType() {
        PowerMockito.when(accuracyQualityChecker.getAccuracyType()).thenReturn(Observable.just(AccuracyType.ACCURACY_GOOD));
        accuracyQualityChecker.getAccuracyType().test()
                .assertSubscribed()
                .assertValue(AccuracyType.ACCURACY_GOOD);
    }
}
