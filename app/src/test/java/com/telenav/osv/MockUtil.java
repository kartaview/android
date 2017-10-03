package com.telenav.osv;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Utility methods that unit tests can use to do common android library mocking that might be needed.
 */
public class MockUtil {

    private static final ScheduledExecutorService mainThread = Executors.newSingleThreadScheduledExecutor();

    private static final ScheduledExecutorService backgroundThread = Executors.newSingleThreadScheduledExecutor();

    private MockUtil() {
    }

    /**
     * Mocks main thread handler post() and postDelayed() for use in Android unit tests
     * <p>
     * To use this:
     * <ol>
     * <li>Call this method in an {@literal @}Before method of your test.</li>
     * <li>Place Looper.class in the {@literal @}PrepareForTest annotation before your test class.</li>
     * <li>any class under test that needs to call {@code new Handler(Looper.getMainLooper())} should be placed
     * in the {@literal @}PrepareForTest annotation as well.</li>
     * </ol>
     * @throws Exception
     */
    public static void mockMainThreadHandler() throws Exception {
        //PowerMockito.mockStatic(Looper.class);
        //Looper mockMainThreadLooper = mock(Looper.class);
        //when(Looper.getMainLooper()).thenReturn(mockMainThreadLooper);
        //Handler mockMainThreadHandler = mock(Handler.class);
        //Answer<Boolean> handlerPostAnswer = invocation -> {
        //  Runnable runnable = invocation.getArgument(0);//, Runnable.class);
        //  Long delay = 0L;
        //  if (invocation.getArguments().length > 1) {
        //    delay = invocation.getArgument(1);//, Long.class);
        //  }
        //  if (runnable != null) {
        //    mainThread.schedule(runnable, delay, TimeUnit.MILLISECONDS);
        //  }
        //  return true;
        //};
        //doAnswer(handlerPostAnswer).when(mockMainThreadHandler).post(any(Runnable.class));
        //doAnswer(handlerPostAnswer).when(mockMainThreadHandler).postDelayed(any(Runnable.class), anyLong());
        //PowerMockito.whenNew(Handler.class).withArguments(mockMainThreadLooper).thenReturn(mockMainThreadHandler);
    }

    public static String getJsonFromFile(Object obj, String filename) {
        try {
            ClassLoader classLoader = obj.getClass().getClassLoader();
            URL resource = classLoader.getResource("res/" + filename);
            File file = new File(resource.getPath());
            return new Scanner(file).useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {

        }
        return null;
    }

    /**
     * Mocks another thread handler post() and postDelayed() for use in Android unit tests
     * <p>
     * To use this:
     * <ol>
     * <li>Call this method in an {@literal @}Before method of your test.</li>
     * <li>Place Looper.class in the {@literal @}PrepareForTest annotation before your test class.</li>
     * <li>any class under test that needs to call {@code new Handler(Looper.getMainLooper())} should be placed
     * in the {@literal @}PrepareForTest annotation as well.</li>
     * </ol>
     * @throws Exception
     */
    public static void mockBackgroundThreadHandler() throws Exception {
        //PowerMockito.mockStatic(Looper.class);
        //Looper mockBackgroundThreadLooper = mock(Looper.class);
        //when(Looper.getMainLooper()).thenReturn(mockBackgroundThreadLooper);
        //Handler mockMainThreadHandler = mock(Handler.class);
        //Answer<Boolean> handlerPostAnswer = invocation -> {
        //  Runnable runnable = invocation.getArgument(0);//, Runnable.class);
        //  Long delay = 0L;
        //  if (invocation.getArguments().length > 1) {
        //    delay = invocation.getArgument(1);//, Long.class);
        //  }
        //  if (runnable != null) {
        //    mainThread.schedule(runnable, delay, TimeUnit.MILLISECONDS);
        //  }
        //  return true;
        //};
        //doAnswer(handlerPostAnswer).when(mockMainThreadHandler).post(any(Runnable.class));
        //doAnswer(handlerPostAnswer).when(mockMainThreadHandler).postDelayed(any(Runnable.class), anyLong());
        //PowerMockito.whenNew(Handler.class).withArguments(mockBackgroundThreadLooper).thenReturn(mockMainThreadHandler);
    }
}