package com.telenav.osv.location;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.reactivex.observers.TestObserver;

public class AccuracyQualityCheckerTest {

    private AccuracyQualityChecker checker;

    private TestObserver<Integer> accuracyTestObserver;

    @Before
    public void setUp() {
        checker = new AccuracyQualityChecker();
        accuracyTestObserver = checker.getAccuracyType().test();
    }

    @After
    public void tearDown() {
        accuracyTestObserver.dispose();
        checker = null;
    }

    @Test
    public void testLocationAccuracyGood() {
        accuracyTestObserver.assertSubscribed();
        checker.onAccuracyChanged(AccuracyType.ACCURACY_GOOD);
        accuracyTestObserver.assertValue(AccuracyType.ACCURACY_GOOD);
    }

    @Test
    public void testLocationAccuracyMedium() {
        accuracyTestObserver.assertSubscribed();
        checker.onAccuracyChanged(AccuracyType.ACCURACY_MEDIUM);
        accuracyTestObserver.assertValue(AccuracyType.ACCURACY_MEDIUM);
    }

    @Test
    public void testLocationAccuracyBad() {
        accuracyTestObserver.assertSubscribed();
        checker.onAccuracyChanged(AccuracyType.ACCURACY_BAD);
        accuracyTestObserver.assertValue(AccuracyType.ACCURACY_BAD);
    }
}