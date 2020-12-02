package com.telenav.osv.upload;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.content.Context;
import com.telenav.osv.utils.ServiceUtils;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

/**
 * @author horatiuf
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class UploadManagerTest {

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    UploadManager uploadManager;

    private Context instrumentationCtx;

    @Before
    public void setUp() throws Exception {
        uploadManager = UploadManagerImpl.getInstance();
        instrumentationCtx = InstrumentationRegistry.getTargetContext();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void start() throws Exception {
        uploadManager.start();

        Assert.assertEquals(true, ServiceUtils.isServiceRunning(ServiceUploadImpl.class.getName(), instrumentationCtx));
    }

}