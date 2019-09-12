package com.telenav.osv.utils;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;

public class SizeTest {

    @Test
    public void testCreateSize() {
        Size size = new Size(10, 20);
        Assert.assertThat(size.getWidth(), is(10));
        Assert.assertThat(size.getHeight(), is(20));
    }

    @Test
    public void testSizeCompare() {
        Size s1 = new Size(10, 20);
        Size s2 = new Size(1, 2);
        //size 1 is bigger than size 2
        Assert.assertTrue(s1.compareTo(s2) > 0);
        //size 2 is smaller than size1
        Assert.assertTrue(s2.compareTo(s1) < 0);
        //size's values are equal
        s2 = s1;
        Assert.assertThat(s1.compareTo(s2), is(0));
    }

    @Test
    public void testToString() {
        Size size = new Size(10, 20);
        Assert.assertThat(size.toString(), is("10x20"));
    }

    @Test
    public void testEquals() {
        Size s1 = new Size(10, 20);
        Size s2 = null;
        //test null size
        Assert.assertFalse(s1.equals(s2));
        //test if the value are the same
        s2 = new Size(10, 20);
        Assert.assertTrue(s1.equals(s2));
        //test if the object is the same
        s2 = s1;
        Assert.assertTrue(s1.equals(s2));
        //test with different object
        Assert.assertFalse(s1.equals(2));

    }

    @Test
    public void testSwapValues() {
        Size size = new Size(10, 20);
        size.swapValues();
        Assert.assertThat(size.getWidth(), is(20));
        Assert.assertThat(size.getHeight(), is(10));
    }

    @Test
    public void testSizeToMp() {
        Size size = new Size(2560, 1920);
        Assert.assertThat(size.getRoundedMegaPixels(), is(5));
    }
}
