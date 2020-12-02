package com.telenav.osv.upload;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

/**
 * @author horatiuf
 */

public class UploadService extends Service {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = UploadService.class.getSimpleName();

    Executor executor;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "UploadService ==> onCreate");
        executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        executor.execute(() -> {
            // mock some network operation
            try {
                System.out.println("command is executing");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        return START_NOT_STICKY;
    }
}
