package com.telenav.osv.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import javax.security.auth.x500.X500Principal;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.OrientationEventListener;
import android.view.WindowManager;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.skobbler.ngx.SKDeveloperKeyException;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.SKMapsInitSettings;
import com.skobbler.ngx.SKMapsInitializationListener;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.telenav.osv.R;
import com.telenav.osv.data.DynamicPreferences;
import com.telenav.osv.data.MapPreferences;
import com.telenav.osv.item.OSVFile;
import io.fabric.sdk.android.Fabric;

@SuppressWarnings("ResultOfMethodCallIgnored")
@SuppressLint("SimpleDateFormat")
public class Utils {

    public static final SimpleDateFormat onlineDateFormat = new SimpleDateFormat("yyyy-MM-dd  (hh:mm)");

    public static final SimpleDateFormat onlineDriverDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public static final SimpleDateFormat numericDateFormat = new SimpleDateFormat("MM-dd hh:mm");

    public static final SimpleDateFormat numericCardDateFormat = new SimpleDateFormat("MMM dd, hh:mm");

    public static final SimpleDateFormat numericPaymentDateFormat = new SimpleDateFormat("MMM dd ''yy");

    public static final SimpleDateFormat paymentServerDateFormat = new SimpleDateFormat("dd MMM yy");

    public static final SimpleDateFormat niceDateFormat = new SimpleDateFormat("MMMM dd | h:mm a");

    public static final int REQUEST_ENABLE_BT = 1;

    // Orientation hysteresis amount used in rounding, in degrees
    private static final int ORIENTATION_HYSTERESIS = 10;

    private static final long UNAVAILABLE = -1L;

    private static final long PREPARING = -2L;

    private static final long UNKNOWN_SIZE = -3L;

    private static final int[] SCORES = new int[]{10, 5, 5, 3, 3, 2, 2, 2, 2, 2, 1};

    private static final String TAG = "Utils";

    private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

    /**
     * true if multiple map instances can be created
     */
    public static boolean isMultipleMapSupportEnabled;

    public static boolean DEBUG = false;

    private static String sExternalStoragePath = "";

    /**
     * Tells if internet is currently available on the device
     * @param currentContext a
     * @return a
     */
    public static boolean isInternetAvailable(Context currentContext) {
        ConnectivityManager connectivityManager = (ConnectivityManager) currentContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
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
        }
        return false;
    }

    /**
     * Initializes the SKMaps framework
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean initializeLibrary(final Activity context, MapPreferences prefs, SKMapsInitializationListener initListener) {

        // get object holding map initialization settings
        SKMapsInitSettings initMapSettings = new SKMapsInitSettings();

        final String mapResourcesPath = prefs.getMapResourcesPath();
        // set path to map resources and initial map style
        initMapSettings.setMapResourcesPath(mapResourcesPath);
        initMapSettings.setCurrentMapViewStyle(new SKMapViewStyle(mapResourcesPath + "grayscalestyle/", "grayscalestyle.json"));

        if (context.getApplicationContext() != null) {
            try {
                SKMaps.getInstance().initializeSKMaps((Application) context.getApplicationContext(), initListener, initMapSettings);
                return true;
            } catch (SKDeveloperKeyException exception) {
                Log.e("initializeLibrary:", Log.getStackTraceString(exception));
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

    public static int getScreenOrientation(Context context) {
        WindowManager windowManager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
        return windowManager != null ? windowManager.getDefaultDisplay().getRotation() : -1;
    }

    /**
     * Deletes all files and directories from <>file</> except PreinstalledMaps
     * @param file a
     */
    public static void deleteFileOrDirectory(OSVFile file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (String aChildren : children) {
                if (new OSVFile(file, aChildren).isDirectory() && !"PreinstalledMaps".equals(aChildren) && !"Maps".equals(aChildren)) {
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

    public static OSVFile generateOSVFolder(Context context, DynamicPreferences prefs) {
        OSVFile osv = new OSVFile(getSelectedStorage(context, prefs).getPath() + "/OSV");
        if (!osv.exists()) {
            osv.mkdir();
        }
        return osv;
    }

    public static File getSelectedStorage(Context context, DynamicPreferences prefs) {
        File[] storages = ContextCompat.getExternalFilesDirs(context, null);
        if (storages.length > 1) {
            File file = storages[1];
            if (file != null) {
                sExternalStoragePath = file.getPath();
            }
        }
        boolean external = prefs.isUsingExternalStorage();
        if (storages.length > 1 && external && storages[1] != null) {
            return storages[1];
        }
        if (external) {
            prefs.setUsingExternalStorage(false);
        }
        File result = storages[0];
        if (result == null) {
            result = context.getExternalFilesDir(null);
        }
        return result;
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

    public static long getAvailableSpace(Context mContext, DynamicPreferences prefs) {
        String state = Environment.getExternalStorageState();
        //        Log.d(TAG, "External storage state=" + state);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }
        String path = getSelectedStorage(mContext, prefs).getPath();
        OSVFile dir = new OSVFile(path);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
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
        File[] storages = ContextCompat.getExternalFilesDirs(context, null);
        Log.d(TAG, "getSelectedStorage: " + Arrays.toString(storages));
        return storages.length > 1 && storages[1] != null;
    }

    public static boolean isInsideBoundingBox(double latPoint, double lonPoint, double latTopLeft, double longTopLeft, double latBottomRight,
                                              double longBottomRight) {
        return (latPoint <= latTopLeft) && (latPoint >= latBottomRight) && (lonPoint >= longTopLeft) && (lonPoint <= longBottomRight);
    }

    public static boolean isDebuggableFlag(Context context) {
        return (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
    }

    public static boolean isGPSEnabled(Context context) {
        android.location.LocationManager locationManager =
                (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean checkGooglePlayServices(final Activity activity) {
        final int googlePlayServicesCheck = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);
        switch (googlePlayServicesCheck) {
            case ConnectionResult.SUCCESS:
                return true;
            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_INVALID:
            case ConnectionResult.SERVICE_MISSING:
                return false;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                GoogleApiAvailability.getInstance()
                        .showErrorDialogFragment(activity, googlePlayServicesCheck, 0, dialogInterface -> activity.finish());
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

    @SuppressWarnings("unused")
    public static void trace() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (StackTraceElement e : trace) {
            Log.d(TAG, "####### " + e);
        }
    }

    public static String getExternalStoragePath(Context context, DynamicPreferences prefs) {
        if ("".equals(sExternalStoragePath)) {
            getSelectedStorage(context, prefs);
        }
        return sExternalStoragePath;
    }

    public static String formatSize(double value) {
        String[] sizeText = formatSizeDetailed(value);
        return sizeText[0] + sizeText[1];
    }

    public static String[] formatSizeDetailed(double value) {
        String[] sizeText = new String[]{"0", " MB"};
        if (value > 0) {
            double size = value / (double) 1024 / (double) 1024;
            String type = " MB";
            DecimalFormat df2 = new DecimalFormat("#.#");
            if (size > 1024) {
                size = (size / (double) 1024);
                type = " GB";
                sizeText = new String[]{"" + df2.format(size), type};
            } else {
                sizeText = new String[]{"" + df2.format(size), type};
            }
        }
        return sizeText;
    }

    /**
     * Shows the api key not set dialog.
     */
    private static void showApiKeyErrorDialog(Activity currentActivity) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(currentActivity);

        alertDialog.setTitle("Error");
        alertDialog.setMessage("API_KEY not set");
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton(currentActivity.getResources().getString(R.string.ok_label),
                (dialog, which) -> android.os.Process
                        .killProcess(android.os.Process.myPid()));

        alertDialog.show();
    }
}