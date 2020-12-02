package com.telenav.osv.recorder.camera.util;

import org.junit.Assert;
import org.junit.Test;
import com.telenav.osv.utils.Size;

public class AspectRatioTest {


    @Test
    public void testCreateAspectRatio() {
        AspectRatio aspectRatio = AspectRatio.createAspectRatio(4, 3);
        Assert.assertEquals(4, aspectRatio.getX());
        Assert.assertEquals(3, aspectRatio.getY());
    }

    @Test
    public void testEquals() {
        AspectRatio aspectRatio = AspectRatio.createAspectRatio(4, 3);
        AspectRatio aspectRatio1 = AspectRatio.createAspectRatio(8, 6);
        AspectRatio aspectRatio2 = null;
        //test null size
        Assert.assertFalse(aspectRatio.equals(aspectRatio2));
        aspectRatio2 = AspectRatio.createAspectRatio(1, 1);
        //test equal ratios
        Assert.assertTrue(aspectRatio.equals(aspectRatio1));
        //test not equal ratios
        Assert.assertFalse(aspectRatio.equals(aspectRatio2));
        //test with different objects
        Assert.assertFalse(aspectRatio.equals(2));
    }

    @Test
    public void testToString() {
        AspectRatio aspectRatio = AspectRatio.createAspectRatio(4, 3);
        Assert.assertEquals("4:3", aspectRatio.toString());
    }

    @Test
    public void testCompareRatios() {
        AspectRatio aspectRatio = AspectRatio.createAspectRatio(4, 3);
        AspectRatio aspectRatio1 = AspectRatio.createAspectRatio(4, 3);
        AspectRatio aspectRatio2 = AspectRatio.createAspectRatio(1, 1);
        //compare when the ratios are equal
        Assert.assertEquals(0, aspectRatio.compareTo(aspectRatio1));
        //compare when the first ratio is grater than the other
        Assert.assertEquals(1, aspectRatio.compareTo(aspectRatio2));
        //compare when the first ratios is less than the other
        Assert.assertEquals(-1, aspectRatio2.compareTo(aspectRatio));
    }

    @Test
    public void testGreatestCommonDivisor() {
        AspectRatio aspectRatio = AspectRatio.createAspectRatio(100, 200);
        AspectRatio aspectRatio1 = AspectRatio.createAspectRatio(232, 435);
        Assert.assertEquals(1, aspectRatio.getX());
        Assert.assertEquals(2, aspectRatio.getY());
        Assert.assertEquals(8, aspectRatio1.getX());
        Assert.assertEquals(15, aspectRatio1.getY());
    }

    @Test
    public void testMatchRatios() {
        AspectRatio aspectRatio = AspectRatio.createAspectRatio(4, 3);
        Size size = new Size(800, 600);
        Assert.assertTrue(aspectRatio.matches(size));
        size = new Size(400, 310);
        Assert.assertFalse(aspectRatio.matches(size));
    }
}
