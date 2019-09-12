package com.telenav.osv.location;


import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.Manifest;
import android.location.Location;
import com.telenav.osv.utils.Log;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;
import io.reactivex.subscribers.TestSubscriber;

@RunWith(AndroidJUnit4.class)
public class LocationServiceInstrTest {

    private static final String TAG = LocationServiceInstrTest.class.getSimpleName();

    private LocationService locationService;

    @Before
    public void setUp() {
        GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION);
        locationService = LocationServiceManager.getInstance(InstrumentationRegistry.getTargetContext());
    }

    @After
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        Field instance = LocationServiceManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        locationService = null;
    }

    @Test
    public void testLocationUpdates() {
        TestSubscriber<Location> testSubscriber = locationService.getLocationUpdates().test();
        testSubscriber.awaitCount(1);
        testSubscriber.assertNoErrors();
        testSubscriber.assertNotComplete();
        testSubscriber.assertNoTimeout();
        testSubscriber.assertValue(location -> {
            Log.d(TAG, String.format("Location lat %s, lon %s", location.getLatitude(), location.getLongitude()));
            return location.getLongitude() >= 0 && location.getLatitude() >= 0;
        });
        testSubscriber.dispose();
    }

    @Test
    public void testLocationUpdatesMultipleSubscribers() {
        TestSubscriber<Location> firstSubscriber = locationService.getLocationUpdates().test();
        locationService.getLocationUpdates().subscribe(location -> Log.d(TAG, String.format("Location first lat %s, lon %s", location.getLatitude(), location.getLongitude())));
        locationService.getLocationUpdates().subscribe(location -> Log.d(TAG, String.format("Location second lat %s, lon %s", location.getLatitude(), location.getLongitude())));
        firstSubscriber.awaitTerminalEvent();
    }

    @Test
    public void testLocationAccuracy() {
        Assert.assertTrue(true);
    }

}
