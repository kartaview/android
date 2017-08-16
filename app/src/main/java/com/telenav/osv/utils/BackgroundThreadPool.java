package com.telenav.osv.utils;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * widely used thread pool for small priority background tasks
 * Created by Kalman on 11/05/2017.
 */
public class BackgroundThreadPool {
    private static final int KEEP_ALIVE = 10;

    private static BackgroundThreadPool mInstance;

    private ThreadPoolExecutor mThreadPoolExec;

    private BackgroundThreadPool() {
        int coreNum = Runtime.getRuntime().availableProcessors();
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        mThreadPoolExec = new ThreadPoolExecutor(
                coreNum,
                coreNum,
                KEEP_ALIVE,
                TimeUnit.SECONDS,
                workQueue,
                new ThreadFactoryBuilder().setDaemon(false).setNameFormat("BackgroundThreadPool").setPriority(Thread.NORM_PRIORITY).build());
    }

    public static synchronized void post(Runnable runnable) {
        if (mInstance == null) {
            mInstance = new BackgroundThreadPool();
        }
        mInstance.mThreadPoolExec.execute(runnable);
    }

    public static void finish() {
        mInstance.mThreadPoolExec.shutdown();
    }

    public static void cancelTask(Runnable runnable) {
        if (runnable != null) {
            mInstance.mThreadPoolExec.remove(runnable);
        }
    }
}