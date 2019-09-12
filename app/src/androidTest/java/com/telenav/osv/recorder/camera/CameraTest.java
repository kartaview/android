package com.telenav.osv.recorder.camera;

import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.Manifest;
import android.content.Context;
import com.telenav.osv.recorder.camera.util.CameraError;
import com.telenav.osv.utils.Size;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;
import io.reactivex.observers.TestObserver;

/**
 * Created by cameliao on 2/2/18.
 */

@RunWith(AndroidJUnit4.class)
public class CameraTest {

    private static final long ASYNC_WAIT_SYNCHRONIZATION_TIME = 20;

    private static final int PICTURE_RESOLUTION_WIDTH = 2880;

    private static final int PICTURE_RESOLUTION_HEIGHT = 2160;

    private static final int SCREEN_SIZE_WIDTH = 1080;

    private static final int SCREEN_SIZE_HEIGHT = 2160;

    private Camera camera;

    private TestObserver testObserver;

    @Before
    public void setUp() {
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Context context = InstrumentationRegistry.getTargetContext();
        camera = new CameraManager(context, new Size(PICTURE_RESOLUTION_WIDTH, PICTURE_RESOLUTION_HEIGHT), new Size(SCREEN_SIZE_WIDTH, SCREEN_SIZE_HEIGHT));
    }

    @After
    public void tearDown() {
        if (camera != null) {
            camera.closeCamera();
        }
        if (testObserver != null && !testObserver.isDisposed()) {
            testObserver.dispose();
        }
    }

    @Test
    public void testOpenCamera() {
        openCamera();
    }

    @Test
    public void testCloseCamera() {
        openCamera();
        camera.closeCamera();
        Assert.assertFalse(camera.isCameraOpen());
    }

    @Test
    public void testTakePictureWithoutOpeningTheCamera() {
        testObserver = camera.takePicture().test();
        Assert.assertTrue(testObserver
                .awaitTerminalEvent(ASYNC_WAIT_SYNCHRONIZATION_TIME, TimeUnit.SECONDS));
        testObserver.assertSubscribed()
                .assertError(throwable -> ((Throwable) throwable).getMessage().equals(CameraError.ERROR_CAMERA_IS_NOT_OPENED));
    }

    @Test
    public void testTakePicture() {
        openCamera();
        testObserver = camera.takePicture().test();
        Assert.assertTrue(testObserver
                .awaitTerminalEvent(ASYNC_WAIT_SYNCHRONIZATION_TIME, TimeUnit.SECONDS));
        testObserver.assertSubscribed()
                .assertComplete();
    }

    //Todo: Fix the test
   /* @Test
    public void testGetSupportedPictureResolutions() {
        Map<Integer, Size> pictureResolutions = camera.getSupportedPictureResolutions();
        Assert.assertTrue(pictureResolutions.size() > 0);
    }*/

    private void openCamera() {
        testObserver = camera.openCamera().test();
        Assert.assertTrue(testObserver
                .awaitTerminalEvent(ASYNC_WAIT_SYNCHRONIZATION_TIME, TimeUnit.SECONDS));
        testObserver.assertComplete()
                .assertNoErrors();
        Assert.assertTrue(camera.isCameraOpen());
    }
}