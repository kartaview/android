package com.telenav.osv.recorder.shutter;

import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.content.Context;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.common.Injection;
import com.telenav.osv.recorder.shutter.shutterlogic.AutoShutterLogic;
import com.telenav.osv.recorder.shutter.shutterlogic.GpsShutterLogic;
import com.telenav.osv.recorder.shutter.shutterlogic.IdleShutterLogic;
import com.telenav.osv.recorder.shutter.shutterlogic.ObdShutterLogic;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

/**
 * Created by cameliao on 2/2/18.
 */

@RunWith(AndroidJUnit4.class)
public class ShutterTest {

    private Shutter shutter;

    private boolean calledOnComplete;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        shutter = new ShutterManager(Injection.provideLocationService(context), Injection.provideObdManager(context, new ApplicationPreferences(context)), false);
    }

    @After
    public void tearDown() {
        shutter.destroy();
    }

    @Test
    public void testObdSpeedChanged() throws Exception {
        Field fieldCurrentLogic = ShutterManager.class.getDeclaredField("currentLogic");
        fieldCurrentLogic.setAccessible(true);
        Assert.assertTrue(fieldCurrentLogic.get(shutter) instanceof ObdShutterLogic);
    }

    @Test
    public void testObdSpeedTimeOut() throws Exception {
        final Object syncObject = new Object();
        synchronized (syncObject) {
            syncObject.wait(6000);
        }
        Field fieldCurrentLogic = ShutterManager.class.getDeclaredField("currentLogic");
        fieldCurrentLogic.setAccessible(true);
        Assert.assertTrue(fieldCurrentLogic.get(shutter) instanceof IdleShutterLogic);
    }

    @Test
    public void testLocationTimeOut() throws Exception {
        final Object syncObject = new Object();
        synchronized (syncObject) {
            syncObject.wait(6000);
        }
        Field fieldCurrentLogic = ShutterManager.class.getDeclaredField("currentLogic");
        fieldCurrentLogic.setAccessible(true);
        Assert.assertTrue(fieldCurrentLogic.get(shutter) instanceof AutoShutterLogic);
    }

    @Test
    public void testLocationChanged() throws Exception {
        Field fieldCurrentLogic = ShutterManager.class.getDeclaredField("currentLogic");
        fieldCurrentLogic.setAccessible(true);
        Assert.assertTrue(fieldCurrentLogic.get(shutter) instanceof GpsShutterLogic);
    }

    @Test
    public void testRecordingStateChanged() throws Exception {
        shutter.onRecordingStateChanged(true);
        Field fieldCurrentLogic = ShutterManager.class.getDeclaredField("currentLogic");
        fieldCurrentLogic.setAccessible(true);
        Assert.assertTrue(fieldCurrentLogic.get(shutter) instanceof AutoShutterLogic);
        shutter.onRecordingStateChanged(false);
        Assert.assertTrue(fieldCurrentLogic.get(shutter) instanceof IdleShutterLogic);
    }

    @Test
    public void testSubscribeTakePhoto() throws Exception {
        final Object syncObject = new Object();
        calledOnComplete = false;
        shutter.getTakeImageObservable().subscribe(distance -> {
            calledOnComplete = true;
            synchronized (syncObject) {
                syncObject.notify();
            }
        });
        Field fieldCurrentLogic = ShutterManager.class.getDeclaredField("currentLogic");
        fieldCurrentLogic.setAccessible(true);
        Assert.assertTrue(fieldCurrentLogic.get(shutter) instanceof AutoShutterLogic);
        synchronized (syncObject) {
            syncObject.wait(10000);
        }
        if (!calledOnComplete) {
            Assert.assertTrue(false);
        } else {
            Assert.assertTrue(true);
        }
    }
}
