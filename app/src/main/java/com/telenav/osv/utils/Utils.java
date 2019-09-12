package com.telenav.osv.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.security.auth.x500.X500Principal;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.Point;
import android.hardware.Camera;
import android.location.Location;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;
import com.crashlytics.android.Crashlytics;
import com.faraji.environment3.Environment3;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.SKDeveloperKeyException;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.SKMapsInitSettings;
import com.skobbler.ngx.SKMapsInitializationListener;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.data.location.model.OSVLocation;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.network.payrate.model.PayRateData;
import com.telenav.osv.network.payrate.model.PayRateItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import io.fabric.sdk.android.Fabric;

@SuppressWarnings("ResultOfMethodCallIgnored")
@SuppressLint("SimpleDateFormat")
public class Utils {

    /**
     * The index for days for the duration breakdown.
     */
    public static final int DURATION_BREAKDOWN_DAYS_INDEX = 0;

    /**
     * The index for hours for the duration breakdown.
     */
    public static final int DURATION_BREAKDOWN_HOURS_INDEX = 1;

    /**
     * The index for minutes for the duration breakdown.
     */
    public static final int DURATION_BREAKDOWN_MIN_INDEX = 2;

    /**
     * The index for seconds for the duration breakdown.
     */
    public static final int DURATION_BREAKDOWN_SEC_INDEX = 3;

    public static final SimpleDateFormat onlineDateFormat = new SimpleDateFormat("yyyy-MM-dd  (hh:mm)");

    public static final SimpleDateFormat onlineDriverDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public static final String numericDateFormatPattern = "MM-dd hh:mm";

    public static final SimpleDateFormat numericDateFormat = new SimpleDateFormat("MM-dd hh:mm");

    public static final SimpleDateFormat numericCardDateFormat = new SimpleDateFormat("MMM dd, hh:mm");

    public static final SimpleDateFormat numericPaymentDateFormat = new SimpleDateFormat("MMM dd ''yy");

    public static final SimpleDateFormat paymentServerDateFormat = new SimpleDateFormat("dd MMM yy");

    public static final SimpleDateFormat niceDateFormat = new SimpleDateFormat("MMMM dd | h:mm a");

    public static final int REQUEST_ENABLE_BT = 1;

    public static final String APP_FOLDER_PATTERN = "%s/OSV";

    public static final int STORAGE_SIZE_MINIMUM_LENGTH = 1;

    public static final int INDEX_STORAGE_EXTERNAL = 1;

    public static final String OSV_FOLDER_NAME = "/OSV";

    // Orientation hysteresis amount used in rounding, in degrees
    private static final int ORIENTATION_HYSTERESIS = 10;

    private static final long UNAVAILABLE = -1L;

    private static final long PREPARING = -2L;

    private static final long UNKNOWN_SIZE = -3L;

    private static final int[] SCORES = new int[]{10, 5, 5, 3, 3, 2, 2, 2, 2, 2, 1};

    private static final String TAG = "Utils";

    private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

    private static final int METADATA_TXT_FILE_INDEX = 1;

    /**
     * true if multiple map instances can be created
     */
    public static boolean isMultipleMapSupportEnabled;

    public static boolean DEBUG = false;

    /**
     * Tells if internet is currently available on the device
     * @param currentContext a
     * @return a
     */
    public static boolean isInternetAvailable(Context currentContext) {
        ConnectivityManager conectivityManager = (ConnectivityManager) currentContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                if (networkInfo.isConnected()) {
                    return true;
                }
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                if (networkInfo.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Initializes the SKMaps framework
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean initializeLibrary(final Activity context, SKMapsInitializationListener initListener) {

        // get object holding map initialization settings
        SKMapsInitSettings initMapSettings = new SKMapsInitSettings();

        final String mapResourcesPath =
                ((OSVApplication) context.getApplicationContext()).getAppPrefs().getStringPreference("mapResourcesPath");
        // set path to map resources and initial map style
        initMapSettings.setMapResourcesPath(mapResourcesPath);
        initMapSettings.setCurrentMapViewStyle(new SKMapViewStyle(mapResourcesPath + "grayscalestyle/", "grayscalestyle.json"));

        if (context.getApplicationContext() != null) {
            try {
                SKMaps.getInstance().initializeSKMaps((Application) context.getApplicationContext(), initListener, initMapSettings);
                return true;
            } catch (SKDeveloperKeyException exception) {
                exception.printStackTrace();
                showApiKeyErrorDialog(context);
                return false;
            }
        } else {
            initListener.onLibraryInitialized(false);
            return false;
        }
    }

    public static boolean isDebugBuild(Context ctx) {
        boolean debug = false;
        try {
            @SuppressLint("PackageManagerGetSignatures") PackageInfo pinfo =
                    ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            Signature signatures[] = pinfo.signatures;

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            for (Signature signature : signatures) {
                ByteArrayInputStream stream = new ByteArrayInputStream(signature.toByteArray());
                X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);
                debug = cert.getSubjectX500Principal().equals(DEBUG_DN);
                if (debug) {
                    break;
                }
            }
        } catch (Exception e) {
            //debuggable variable will remain false
        }
        return debug;
    }

    /**
     * Convert a millisecond duration to an array of days, hours, minutes, seconds in that specific order.
     * @param millis The duration in milliseconds
     */
    public static void durationBreakdown(long millis, long[] outDurationBreakdown) {
        if (millis < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        outDurationBreakdown[DURATION_BREAKDOWN_DAYS_INDEX] = days;
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        outDurationBreakdown[DURATION_BREAKDOWN_HOURS_INDEX] = hours;
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        outDurationBreakdown[DURATION_BREAKDOWN_MIN_INDEX] = minutes;
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        outDurationBreakdown[DURATION_BREAKDOWN_SEC_INDEX] = seconds;
    }

    public static boolean isDebugEnabled(Context ctx) {
        DEBUG = false;
        if (ctx != null) {
            DEBUG = ((OSVApplication) ctx.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED);
        }
        return DEBUG;
    }

    public static int getExactScreenOrientation(Context context) {
        Display defaultDisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = defaultDisplay.getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        defaultDisplay.getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    // Logging.writeLog(TAG, "Unknown screen orientation. Defaulting to " + "portrait.", Logging.LOG_DEBUG);
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

    public static int getScreenOrientation(Context context) {
        Display defaultDisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        return defaultDisplay.getRotation();
    }

    /**
     * Deletes all files and directories from <>file</> except PreinstalledMaps
     * @param file a
     */
    public static void deleteFileOrDirectory(OSVFile file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (String aChildren : children) {
                if (new OSVFile(file, aChildren).isDirectory() && !aChildren.equals("PreinstalledMaps") && !aChildren.equals("Maps")) {
                    deleteFileOrDirectory(new OSVFile(file, aChildren));
                } else {
                    new OSVFile(file, aChildren).delete();
                }
            }
        }
    }

    /**
     * Rounds the orientation so that the UI doesn't rotate if the user
     * holds the device towards the floor or the sky
     * @param orientation New orientation
     * @param orientationHistory Previous orientation
     * @return Rounded orientation
     */
    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }

    /**
     * Converts the specified DP to PIXELS according to current screen density
     * @param context a
     * @param dp a
     * @return a
     */
    public static float dpToPx(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) ((dp * displayMetrics.density) + 0.5);
    }

    public static OSVFile generateOSVFolder(Context context) {
        OSVFile osv = new OSVFile(String.format(APP_FOLDER_PATTERN, getSelectedStoragePath(context)));
        if (!osv.exists()) {
            osv.mkdir();
        }
        return osv;
    }

    public static long getOSVDiskSize(Context context) {
        List<String> storagePaths = new ArrayList<>();
        String internalStoragePath = getInternalStoragePath(context);
        if (internalStoragePath != null) {
            storagePaths.add(internalStoragePath);
        }

        long fileSize = 0;
        for (String path : storagePaths) {
            OSVFile osv = new OSVFile(String.format(APP_FOLDER_PATTERN, path));
            if (osv.exists()) {
                fileSize += fileSize(osv);
            }
        }

        return fileSize;
    }

    @Nullable
    public static String getExternalStoragePath(Context context) {
        File[] storages = ContextCompat.getExternalFilesDirs(context, null);
        if (storages.length > STORAGE_SIZE_MINIMUM_LENGTH && storages[INDEX_STORAGE_EXTERNAL] != null) {
            return storages[INDEX_STORAGE_EXTERNAL].getPath();
        }

        return null;
    }

    public static String getInternalStoragePath(Context context) {
        File[] storages = ContextCompat.getExternalFilesDirs(context, null);
        if (storages.length > 0 && storages[0] != null) {
            return storages[0].getPath();
        }

        return Environment3.getInternalStorage().getFile().getPath();
    }

    public static String getSelectedStoragePath(Context context) {
        boolean external =
                ((OSVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE);
        if (external) {
            return getExternalStoragePath(context);
        } else {
            return getInternalStoragePath(context);
        }
    }

    public static long folderSize(OSVFile directory) {
        if (directory == null) {
            return 0;
        }
        long length = 0;
        try {
            for (OSVFile file : directory.listFiles()) {
                if (file.isFile()) {
                    length += file.length();
                } else {
                    length += folderSize(file);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "folderSize: " + e.getLocalizedMessage());
        }
        return length;
    }

    //    /**
    //     * used on the older version upgrade
    //     * @param context a
    //     * @param delete a
    //     */
    //    public static void moveToPublic(final Context context, final boolean delete) {
    //        new Thread(new Runnable() {
    //            @Override
    //            public void run() {
    //                OSVFile osvOld = new OSVFile(context.getFilesDir().getFilePath(), "OSV");
    //                if (!osvOld.exists()) {
    //                    osvOld.mkdir();
    //                }
    //
    //                OSVFile osv = new OSVFile(getSelectedStorage(context).getFilePath() + "/OSV");
    //                if (!osv.exists()) {
    //                    osv.mkdir();
    //                }
    //                for (OSVFile folder : osvOld.listFiles()) {
    //                    OSVFile fileTo = new OSVFile(getSelectedStorage(context).getFilePath() + "/OSV", folder.getName());
    //                    if (!fileTo.exists()) {
    //                        fileTo.mkdir();
    //                    }
    //                    FileChannel source = null;
    //                    FileChannel destination = null;
    //                    for (OSVFile img : folder.listFiles()) {
    //                        try {
    //                            try {
    //                                source = new FileInputStream(img).getChannel();
    //                                destination = new FileOutputStream(new OSVFile(fileTo, img.getName())).getChannel();
    //                                destination.transferFrom(source, 0, source.size());
    //                                if (delete) {
    //                                    Log.d(TAG, "moveToPublic: deleted " + folder.getName() + "/" + img.getName() + " : " + img.delete
    // ());
    //                                }
    //                            } catch (Exception e) {
    //                                Log.w(TAG, "moveToPublic: " + e.getLocalizedMessage());
    //                            } finally {
    //                                if (source != null) {
    //                                    source.close();
    //                                }
    //                                if (destination != null) {
    //                                    destination.close();
    //                                }
    //                            }
    //                        } catch (Exception e) {
    //                            Log.w(TAG, "moveToPublic: " + e.getLocalizedMessage());
    //                        }
    //
    //                        Log.d(TAG, "moveToPublic: copied " + folder.getName() + "/" + img.getName());
    //                    }
    //                    if (folder.listFiles().length == 0 && delete) {
    //                        folder.delete();
    //                    }
    //                }
    //                if (delete) {
    //                    osvOld.delete();
    //                }
    //                Log.d(TAG, "moveToPublic: copy done");
    //            }
    //        }).start();
    //
    //    }

    public static long fileSize(OSVFile file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        long length = 0;
        try {
            if (file.isFile()) {
                length = file.length();
            } else {
                length = folderSize(file);
            }
        } catch (Exception e) {
            Log.w(TAG, "fileSize: " + e.getLocalizedMessage());
        }
        return length;
    }

    public static File[] findFilesByExtension(File dir, final List<String> extensions) {
        return dir.listFiles(pathname -> {
            for (String ext : extensions) {
                if (pathname.getName().endsWith(ext)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * @param dir the directory where the search for metadata file extension will be performed. This will internally use {@link #findFilesByExtension(File, List)}.
     * @return {@code true} if metadata files exist, {@code false} otherwise.
     */
    public static boolean doesMetadataExist(File dir) {
        File[] metadata = Utils.findFilesByExtension(dir, new ArrayList<String>() {
            {
                add(SequenceDetailsCompressionBase.SequenceFilesExtensions.METADATA_DEFAULT);
            }

            {
                add(SequenceDetailsCompressionBase.SequenceFilesExtensions.METADATA_TXT);
            }
        });
        boolean metadataExists = metadata.length != 0;
        Log.d(TAG, String.format("doesMetadataExist. Status: %s. Message: Checking for metadata file extensions.", metadataExists));
        return metadataExists;
    }

    public static long getMetadataSize(File dir) {
        File[] metadata = Utils.findFilesByExtension(dir, new ArrayList<String>() {
            {
                add(SequenceDetailsCompressionBase.SequenceFilesExtensions.METADATA_DEFAULT);
            }

            {
                add(SequenceDetailsCompressionBase.SequenceFilesExtensions.METADATA_TXT);
            }
        });
        if (metadata.length != 0) {
            if (metadata[0] != null) {
                return fileSize(new OSVFile(metadata[0].getPath()));
            }
            if (metadata[METADATA_TXT_FILE_INDEX] != null) {
                return fileSize(new OSVFile(metadata[1].getPath()));
            }
        }

        return 0;
    }

    public static File[] findFilesByExtension(File dir, String ext) {
        return dir.listFiles(pathname -> pathname.getName().endsWith(ext));
    }

    public static long creationTime(OSVFile file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        long creationTime = 0;
        try {
            if (file.isFile()) {
                creationTime = file.lastModified();
            } else {
                creationTime = creationTime(file);
            }
        } catch (Exception e) {
            Log.w(TAG, "fileSize: " + e.getLocalizedMessage());
        }
        return creationTime;
    }

    public static long getAvailableSpace(Context mContext) {
        String state = Environment.getExternalStorageState();
        //        Log.d(TAG, "External storage state=" + state);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }
        String path = getSelectedStoragePath(mContext);
        OSVFile dir = new OSVFile(path);
        if (!dir.exists() && !dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }

        try {
            StatFs stat = new StatFs(path);
            Log.d(TAG, "getStorageSpace: " + ((stat.getAvailableBlocksLong() * stat.getBlockSizeLong()) / 1024 / 1024));
            return (stat.getAvailableBlocksLong() * stat.getBlockSizeLong()) / 1024 / 1024;
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
            if (Fabric.isInitialized()) {
                Crashlytics.logException(e);
            }
        }
        return UNKNOWN_SIZE;
    }

    public static boolean checkSDCard(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return Environment3.isSecondaryExternalStorageAvailable();
        } else {
            File[] storages = ContextCompat.getExternalFilesDirs(context, null);
            Log.d(TAG, "getSelectedStoragePath: " + Arrays.toString(storages));
            return storages.length > 1 && storages[1] != null;
        }
    }

    public static boolean isInsideBoundingBox(double latPoint, double lonPoint, double latTopLeft, double longTopLeft, double latBottomRight,
                                              double longBottomRight) {
        return (latPoint <= latTopLeft) && (latPoint >= latBottomRight) && (lonPoint >= longTopLeft) && (lonPoint <= longBottomRight);
    }

    public static boolean isDebuggableFlag(Context context) {
        return (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
    }

    public static boolean isGPSEnabled(Context context) {
        if (context == null) {
            return false;
        }
        android.location.LocationManager locationManager =
                (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean checkGooglePlayServices(final Activity activity) {
        final int googlePlayServicesCheck = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        switch (googlePlayServicesCheck) {
            case ConnectionResult.SUCCESS:
                return true;
            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_INVALID:
            case ConnectionResult.SERVICE_MISSING:
                return false;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(googlePlayServicesCheck, activity, 0);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        activity.finish();
                    }
                });
                dialog.show();
        }
        return false;
    }

    /**
     * get score for coverage
     * @param coverage - value from 0 to 10 (10 included)
     * @return score
     */
    public static int getValueOnSegment(int coverage) {
        if (coverage < 0) {
            return -1;
        }
        coverage = Math.min(SCORES.length - 1, coverage);
        coverage = Math.max(0, coverage);
        return SCORES[coverage];
    }

    public static boolean isCharging(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent == null ? -1 : intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    public static void trace() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (StackTraceElement e : trace) {
            Log.d(TAG, "####### " + e);
        }
    }

    /**
     * Pretty prints the elements & keys of the given map to a string.
     * @param stringMap the map which we need to be pretty printed
     * @return a string containing the elements of the map
     */
    public static String mapToString(@NonNull Map<String, String> stringMap) {
        StringBuilder stringBuilder = new StringBuilder("{\n");
        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            stringBuilder.append("{ ");
            stringBuilder.append(entry.getKey());
            stringBuilder.append(", ");
            stringBuilder.append(entry.getValue());
            stringBuilder.append(" }\n");
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    /**
     * @param context the application context.
     * @return {@code true} if the {@code GooglePlayServices} is available on device, {@code false} otherwise.
     */
    public static boolean isGooglePlayServicesAvailable(final Context context) {
        final int googlePlayServicesCheck = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        switch (googlePlayServicesCheck) {
            case ConnectionResult.SUCCESS:
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                return true;
        }
        return false;
    }

    /**
     * @param locations the list representing the locations.
     * @return {@code List<SKCoordinate>} representing the translated location objects into skcoordinate objects.
     */
    public static List<SKCoordinate> toSKCoordinatesFromLocation(List<Location> locations) {
        List<SKCoordinate> skCoordinateList = new ArrayList<>();
        if (locations != null) {
            for (Location location : locations) {
                SKCoordinate skCoordinate = new SKCoordinate();
                skCoordinate.setLongitude(location.getLongitude());
                skCoordinate.setLatitude(location.getLatitude());
                skCoordinateList.add(skCoordinate);
            }
        }
        return skCoordinateList;
    }

    /**
     * @param osvLocations the list representing the {@code OSVLocation} objects.
     * @return {@code List<SKCoordinate>} representing the translated location objects into skcoordinate objects.
     */
    public static List<SKCoordinate> toSKCoordinatesFromOSVLocations(List<OSVLocation> osvLocations) {
        List<SKCoordinate> skCoordinateList = new ArrayList<>();
        if (osvLocations != null) {
            for (OSVLocation osvLocation : osvLocations) {
                SKCoordinate skCoordinate = new SKCoordinate();
                Location location = osvLocation.getLocation();
                skCoordinate.setLongitude(location.getLongitude());
                skCoordinate.setLatitude(location.getLatitude());
                skCoordinateList.add(skCoordinate);
            }
        }
        return skCoordinateList;
    }

    /**
     * @param payRateData the {@code PayRateData} object which has all the information related to pay rate.
     * @return max pay rate data from {@code PayRateData} object.
     */
    public static float getMaxPayRate(PayRateData payRateData) {
        ArrayList<PayRateItem> obdOrderedPayRates = new ArrayList<>(payRateData.getPayRates());
        ArrayList<PayRateItem> nonObdOrderedPayRates = new ArrayList<>(obdOrderedPayRates);

        Collections.sort(obdOrderedPayRates, (item1, item2) -> (int) (item1.obdPayRateValue - item2.obdPayRateValue));
        Collections.sort(nonObdOrderedPayRates, (item1, item2) -> (int) (item1.nonObdPayRateValue - item2.nonObdPayRateValue));

        return Math.max(obdOrderedPayRates.get(0).obdPayRateValue, nonObdOrderedPayRates.get(0).nonObdPayRateValue);
    }

    /**
     * Checks which resolutions are available also for teh encoder output and
     * keeps only those which are supported by both, camera and encoder.
     * @param resolutions the camera resolutions.
     */
    public static void checkResolutionsAvailabilityForEncoder(List<Camera.Size> resolutions) {
        MediaCodecInfo codecInfo = selectCodec();
        if (codecInfo == null) {
            return;
        }
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);
        MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();
        Iterator<Camera.Size> i = resolutions.iterator();
        while (i.hasNext()) {
            Camera.Size size = i.next();
            if (!videoCapabilities.isSizeSupported(size.width, size.height)) {
                i.remove();
            }
        }
    }

    public static MediaCodecInfo.VideoCapabilities getEncoderVideoCapabilities() {
        MediaCodecInfo codecInfo = selectCodec();
        if (codecInfo == null) {
            return null;
        }
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);
        return capabilities.getVideoCapabilities();
    }

    /**
     * @return {@code true} if the format YUV420 Semi-Planar is available for encoding, {@code false} otherwise.
     */
    public static boolean isYUV420SemiPlanarSupportedByEncoder() {
        MediaCodecInfo codecInfo = selectCodec();
        if (codecInfo == null) {
            return false;
        }
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);
        //Check which formats are supported by the encoder
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            Log.d(TAG, String.format("setEncodingFormat. Format available: %s", capabilities.colorFormats[i]));
            if (capabilities.colorFormats[i] == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param context the application context used for accessing system window properties.
     * @return a {@code Size} which represents the landscape screen dimensions.
     */
    public static Size getLandscapeScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        Size screenSize;
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getRealSize(point);
            screenSize = new Size(point.x, point.y);
        } else {
            DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
            screenSize = new Size(displayMetrics.widthPixels, displayMetrics.heightPixels);
        }
        if (screenSize.getHeight() > screenSize.getWidth()) {
            screenSize.swapValues();
        }
        return screenSize;
    }

    /**
     * Shows the api key not set dialog.
     */
    private static void showApiKeyErrorDialog(Activity currentActivity) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(currentActivity);

        alertDialog.setTitle("Error");
        alertDialog.setMessage("API_KEY not set");
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton(currentActivity.getResources().getString(R.string.ok_label), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });

        alertDialog.show();
    }

    /**
     * Selects the codec info for the H264 encoder.
     * @return {@code MediaCodecInfo} which contains the information regarding the supported color formats.
     */
    private static MediaCodecInfo selectCodec() {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
}