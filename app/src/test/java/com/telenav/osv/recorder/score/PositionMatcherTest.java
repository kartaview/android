package com.telenav.osv.recorder.score;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.skobbler.ngx.SKCoordinate;

public class PositionMatcherTest {


    private PositionMatcher positionMatcher;

    @Before
    public void setUp() {
        positionMatcher = new PositionMatcher(null);
    }

    @After
    public void tearDown() {
        positionMatcher = null;
    }

    @Test
    public void getBearingTest() {
        Random random = new Random();
        List<SKCoordinate> coordinates = new ArrayList<>();
        List<Double> bearings = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            SKCoordinate skCoordinate = new SKCoordinate(random.nextDouble(), random.nextDouble());
            coordinates.add(skCoordinate);
            bearings.add(positionMatcher.getBearing(skCoordinate));
        }

        Assert.assertEquals(coordinates.size(), bearings.size());
    }

}