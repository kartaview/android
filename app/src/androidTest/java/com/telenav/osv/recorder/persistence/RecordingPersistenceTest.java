package com.telenav.osv.recorder.persistence;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import android.content.Context;
import android.location.Location;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;

/**
 * Created by cameliao on 2/2/18.
 */

@RunWith(AndroidJUnit4.class)
public class RecordingPersistenceTest {

    private RecordingPersistence recordingPersistence;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = InstrumentationRegistry.getTargetContext();
        recordingPersistence = RecordingPersistenceManager.getInstance(context);
    }

    @Test
    public void testSaveFrame() throws Exception {
        final Object syncObject = new Object();
        Location location = new Location("");
        location.setLongitude(41.6342);
        location.setLongitude(25.9086);
        Photo photo = new Photo(new byte[5], false, 0, location);

        final boolean[] onCompleteCalled = {false};
        recordingPersistence.saveFrame(photo).subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onComplete() {
                onCompleteCalled[0] = true;
                synchronized (syncObject) {
                    syncObject.notify();
                }
            }

            @Override
            public void onError(Throwable e) {

            }
        });

        synchronized (syncObject) {
            syncObject.wait(1000);
        }

        Assert.assertTrue(onCompleteCalled[0]);
    }
}
