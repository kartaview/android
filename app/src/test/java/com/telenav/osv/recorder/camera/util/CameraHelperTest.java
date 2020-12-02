package com.telenav.osv.recorder.camera.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import com.telenav.osv.utils.Size;

@RunWith(RobolectricTestRunner.class)
public class CameraHelperTest {

    @Test
    public void testChooseOptimalPreviewSize() {
        List<Size> previewSizesCandidates = new ArrayList<>();
        previewSizesCandidates.add(new Size(768, 432));
        previewSizesCandidates.add(new Size(960, 540));
        previewSizesCandidates.add(new Size(1280, 800));
        previewSizesCandidates.add(new Size(1400, 900));
        //choose the preview size which is big enough for the given surface size.
        int surfaceWidth = 720;
        int surfaceHeight = 1280;
        Size size = CameraHelper.chooseOptimalPreviewSize(previewSizesCandidates, surfaceWidth, surfaceHeight);
        Assert.assertEquals(1280, size.getWidth());
        Assert.assertEquals(800, size.getHeight());
        surfaceWidth = 1280;
        surfaceHeight = 720;
        size = CameraHelper.chooseOptimalPreviewSize(previewSizesCandidates, surfaceWidth, surfaceHeight);
        Assert.assertEquals(1280, size.getWidth());
        Assert.assertEquals(800, size.getHeight());
        //choose the largest preview size when the surface size is bigger than all the preview sizes.
        surfaceWidth = 1000;
        surfaceHeight = 1500;
        size = CameraHelper.chooseOptimalPreviewSize(previewSizesCandidates, surfaceWidth, surfaceHeight);
        Assert.assertEquals(1400, size.getWidth());
        Assert.assertEquals(900, size.getHeight());
    }

    //ToDo: @CameliaOanta check why the values do not work anymore
    /*@Test
    public void testOrientation() {
        //sensor orientation is 90 for most devices
        Assert.assertEquals(90, CameraHelper.getOrientation(Surface.ROTATION_0, 90));
        Assert.assertEquals(91, CameraHelper.getOrientation(Surface.ROTATION_90, 90));
        Assert.assertEquals(270, CameraHelper.getOrientation(Surface.ROTATION_180, 90));
        Assert.assertEquals(180, CameraHelper.getOrientation(Surface.ROTATION_270, 90));
        //or 270 for some devices (e.g Nexus 5X)
        Assert.assertEquals(180, CameraHelper.getOrientation(Surface.ROTATION_90, 270));
        Assert.assertEquals(270, CameraHelper.getOrientation(Surface.ROTATION_0, 270));
        Assert.assertEquals(90, CameraHelper.getOrientation(Surface.ROTATION_180, 270));
        Assert.assertEquals(0, CameraHelper.getOrientation(Surface.ROTATION_270, 270));
    }*/

    @Test
    public void testConverterFromNV21ToYUV420Planar() {
        int width = 4;
        int height = 3;
        int size = width * height + (width * height) / 2;
        Random random = new Random();
        byte[] data = new byte[size];
        for (byte i = 0; i < data.length; i++) {
            data[i] = (byte) random.nextInt(100);
        }
        System.out.println();
        byte[] expected = Arrays.copyOf(data, data.length);

        for (int i = 0; i < data.length; i++) {
            System.out.print(data[i] + " ");
        }
        byte[] result = CameraHelper.convertFromNV21ToYUV420Planar(data, width, height);
        System.out.println();

        for (int i = 0; i < result.length; i++) {
            System.out.print(result[i] + " ");
        }
        int uIndex = width * height + 1;
        int vIndex = width * height;
        for (int i = width * height; i < data.length - width * height / 4; i++) {
            Assert.assertEquals(expected[uIndex], result[i]);
            uIndex = uIndex + 2;
        }
        for (int i = width * height + width * height / 4; i < data.length; i++) {
            Assert.assertEquals(expected[vIndex], result[i]);
            vIndex = vIndex + 2;
        }
    }

    @Test
    public void testConverterFromNV21ToYUV420SemiPlanar() {
        int width = 4;
        int height = 3;
        int size = width * height + (width * height) / 2;
        byte[] data = new byte[size];
        Random random = new Random();
        for (byte i = 0; i < data.length; i++) {
            data[i] = (byte) random.nextInt(100);
        }
        byte[] expected = Arrays.copyOf(data, data.length);
        System.out.println();

        for (int i = 0; i < data.length; i++) {
            System.out.print(data[i] + " ");
        }
        byte[] result = CameraHelper.convertFromNV21ToYUV420SemiPlanar(data, width, height);
        System.out.println();

        for (int i = 0; i < result.length; i++) {
            System.out.print(result[i] + " ");
        }
        for (int i = 0; i < width * height; i++) {
            Assert.assertEquals(data[i], result[i]);
        }
        for (int i = width * height; i < data.length; i = i + 2) {
            Assert.assertEquals(expected[i + 1], result[i]);
            Assert.assertEquals(expected[i], result[i + 1]);
        }
    }
}
