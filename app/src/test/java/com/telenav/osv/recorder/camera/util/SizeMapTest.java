package com.telenav.osv.recorder.camera.util;

import org.junit.Assert;
import org.junit.Test;
import com.telenav.osv.utils.Size;

public class SizeMapTest {

    @Test
    public void testAdd() {
        SizeMap sizeMap = new SizeMap();
        Size s1 = new Size(1, 1);
        Size s2 = new Size(4, 3);
        Size s3 = new Size(16, 9);
        Assert.assertTrue(sizeMap.add(s1));
        Assert.assertTrue(sizeMap.add(s2));
        Assert.assertTrue(sizeMap.add(s3));
        Assert.assertEquals(sizeMap.ratios().size(), 3);
    }

    @Test
    public void testAddDuplicateRatio() {
        SizeMap sizeMap = new SizeMap();
        Size s1 = new Size(4, 3);
        Size s2 = new Size(800, 600);
        Assert.assertTrue(sizeMap.add(s1));
        Assert.assertTrue(sizeMap.add(s2));
        Assert.assertEquals(sizeMap.ratios().size(), 1);
        Assert.assertEquals(sizeMap.sizes(AspectRatio.createAspectRatio(4, 3)).size(), 2);
    }

    @Test
    public void testAddDuplicateSize() {
        SizeMap sizeMap = new SizeMap();
        Size s1 = new Size(4, 3);
        Size s2 = new Size(4, 3);
        Assert.assertTrue(sizeMap.add(s1));
        Assert.assertFalse(sizeMap.add(s2));

    }

    @Test
    public void testRemoveRatio() {
        SizeMap sizeMap = new SizeMap();
        Size s1 = new Size(4, 3);
        Size s2 = new Size(800, 600);
        Size s3 = new Size(1, 1);
        Size s4 = new Size(10, 10);
        sizeMap.add(s1);
        sizeMap.add(s2);
        sizeMap.add(s3);
        sizeMap.add(s4);
        Assert.assertEquals(sizeMap.ratios().size(), 2);
        sizeMap.remove(AspectRatio.createAspectRatio(4, 3));
        Assert.assertEquals(sizeMap.ratios().size(), 1);
    }

    @Test
    public void testGetAllSizes() {
        SizeMap sizeMap = new SizeMap();
        Size s1 = new Size(4, 3);
        Size s2 = new Size(8, 6);
        sizeMap.add(s1);
        sizeMap.add(s2);
        Assert.assertEquals(sizeMap.allSizes().size(), 2);
    }

    @Test
    public void testClearRatios() {
        SizeMap sizeMap = new SizeMap();
        Size s = new Size(4, 3);
        sizeMap.add(s);
        Assert.assertEquals(sizeMap.ratios().size(), 1);
        sizeMap.clear();
        Assert.assertEquals(sizeMap.ratios().size(), 0);
    }
}
