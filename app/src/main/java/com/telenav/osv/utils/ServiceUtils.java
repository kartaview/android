package com.telenav.osv.utils;

import java.util.List;
import android.app.ActivityManager;
import android.content.Context;

/**
 * Helper methods related to {@link android.app.Service} component.
 * @author horatiuf
 */

public class ServiceUtils {
    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ServiceUtils.class.getSimpleName();

    /**
     * @param serviceClassName the class name representing the concrete {@code Service} implementation.
     * @param context the {@code Context} by which {@code ActivityManager} can be referenced in order to get a list of all running services.
     * @return {@code true} if a service identified by a class name, otherwise {@code false}.
     */
    public static boolean isServiceRunning(String serviceClassName, Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)) {
                return true;
            }
        }
        return false;
    }
}
