package com.telenav.osv.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import javax.security.auth.x500.X500Principal;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;
import com.faraji.environment3.Environment3;
import com.faraji.environment3.NoSecondaryStorageException;
import com.google.common.io.ByteStreams;
import com.skobbler.ngx.SKDeveloperKeyException;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.SKMapsInitSettings;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.navigation.SKAdvisorSettings;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.item.OSVFile;


public class Utils {

    // Orientation hysteresis amount used in rounding, in degrees
    public static final int ORIENTATION_HYSTERESIS = 5;

    public static final long UNAVAILABLE = -1L;

    public static final long PREPARING = -2L;

    public static final long UNKNOWN_SIZE = -3L;

    public static final float METER_TO_FEET = 3.28084f;

    public static final SimpleDateFormat onlineDateFormat = new SimpleDateFormat("yyyy-MM-dd  (hh:mm)");

    public static final SimpleDateFormat numericDateFormat = new SimpleDateFormat("MM-dd hh:mm");

    public static final SimpleDateFormat niceDateFormat = new SimpleDateFormat("MMMM dd | h:mm a");

    private static final float KILOMETER_TO_MILE = 0.621371f;

    private static final String TAG = "Utils";

    private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

    public static String EXTERNAL_STORAGE_PATH = "no external";

    /**
     * true if multiple map instances can be created
     */
    public static boolean isMultipleMapSupportEnabled;

    public static boolean DEBUG = false;

    // Screen size holder
    private static Point mScreenSize = new Point();

    private static int mRotation = 0;

    /**
     * Formats a given distance value (given in meters)
     * @param distInMeters
     * @return
     */
    public static String[] formatDistanceFromMeters(Context context, int distInMeters) {
        boolean isUS = !((OSVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
        if (isUS) {
            distInMeters = (int) (distInMeters * METER_TO_FEET);
        }
        DecimalFormat df2 = new DecimalFormat("#.#");
        if (distInMeters < 500) {
            return new String[]{distInMeters + "", (isUS ? " ft" : " m")};
        } else {
            return new String[]{df2.format((double) distInMeters / (isUS ? 5280 : 1000)) + "", (isUS ? " mi" : " km")};
        }
    }

    /**
     * Formats a given distance value (given in meters)
     * @param dist
     * @return
     */
    public static String[] formatDistanceFromKiloMeters(Context context, double dist) {
        boolean isUS = !((OSVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
        if (isUS) {
            dist = (int) (dist * KILOMETER_TO_MILE);
        }
        DecimalFormat df2 = new DecimalFormat("#.#");
        return new String[]{df2.format(dist) + "", (isUS ? " mi" : " km")};
    }

    /**
     * Copies files from assets to destination folder
     * @param assetManager
     * @param sourceFolder
     * @throws IOException
     */
    public static void copyAssetsToFolder(AssetManager assetManager, String sourceFolder, String destinationFolder)
            throws IOException {
        final String[] assets = assetManager.list(sourceFolder);

        final OSVFile destFolderFile = new OSVFile(destinationFolder);
        if (!destFolderFile.exists()) {
            destFolderFile.mkdirs();
        }
        copyAsset(assetManager, sourceFolder, destinationFolder, assets);
    }

    /**
     * Copies files from assets to destination folder
     * @param assetManager
     * @param sourceFolder
     * @param assetsNames
     * @throws IOException
     */
    public static void copyAsset(AssetManager assetManager, String sourceFolder, String destinationFolder,
                                 String... assetsNames) throws IOException {

        for (String assetName : assetsNames) {
            OutputStream destinationStream = new FileOutputStream(new OSVFile(destinationFolder + "/" + assetName));
            String[] files = assetManager.list(sourceFolder + "/" + assetName);
            if (files == null || files.length == 0) {

                InputStream asset = assetManager.open(sourceFolder + "/" + assetName);
                try {
                    ByteStreams.copy(asset, destinationStream);
                } finally {
                    asset.close();
                    destinationStream.close();
                }
            }
        }
    }

    /**
     * Tells if internet is currently available on the device
     * @param currentContext
     * @return
     */
    public static boolean isInternetAvailable(Context currentContext) {
        ConnectivityManager conectivityManager =
                (ConnectivityManager) currentContext.getSystemService(Context.CONNECTIVITY_SERVICE);
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
    public static boolean initializeLibrary(final Activity context) {

        // get object holding map initialization settings
        SKMapsInitSettings initMapSettings = new SKMapsInitSettings();

        final String mapResourcesPath = ((OSVApplication) context.getApplicationContext()).getAppPrefs().getStringPreference("mapResourcesPath");
        // set path to map resources and initial map style
        initMapSettings.setMapResourcesPaths(mapResourcesPath,
                new SKMapViewStyle(mapResourcesPath + "grayscalestyle/", "grayscalestyle.json"));

        final SKAdvisorSettings advisorSettings = initMapSettings.getAdvisorSettings();
        advisorSettings.setAdvisorConfigPath(mapResourcesPath + "/Advisor");
        advisorSettings.setResourcePath(mapResourcesPath + "/Advisor/Languages");
        advisorSettings.setLanguage(SKAdvisorSettings.SKAdvisorLanguage.LANGUAGE_EN);
        advisorSettings.setAdvisorVoice("en");
        initMapSettings.setAdvisorSettings(advisorSettings);


        try {
            SKMaps.getInstance().initializeSKMaps(context, initMapSettings);
            return true;
        } catch (SKDeveloperKeyException exception) {
            exception.printStackTrace();
            showApiKeyErrorDialog(context);
            return false;
        }
    }

    public static boolean isDebugBuild(Context ctx) {
        boolean debug = false;
        try {
            PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            Signature signatures[] = pinfo.signatures;

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            for (int i = 0; i < signatures.length; i++) {
                ByteArrayInputStream stream = new ByteArrayInputStream(signatures[i].toByteArray());
                X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);
                debug = cert.getSubjectX500Principal().equals(DEBUG_DN);
                if (debug)
                    break;
            }
        } catch (Exception e) {
            //debuggable variable will remain false
        }
        return debug;
    }

    public static boolean isDebugEnabled(Context ctx) {
        DEBUG = false;
        if (ctx != null) {
            DEBUG = ((OSVApplication) ctx.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED);
        }
        return DEBUG;
    }

    /**
     * Shows the api key not set dialog.
     */
    public static void showApiKeyErrorDialog(Activity currentActivity) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                currentActivity);

        alertDialog.setTitle("Error");
        alertDialog.setMessage("API_KEY not set");
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton(
                currentActivity.getResources().getString(R.string.ok_label),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                });

        alertDialog.show();
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
        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                width > height) {
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

    /**
     * Deletes all files and directories from <>file</> except PreinstalledMaps
     * @param file
     */
    public static void deleteFileOrDirectory(OSVFile file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i = 0; i < children.length; i++) {
                if (new OSVFile(file, children[i]).isDirectory() && !children[i].equals("PreinstalledMaps") && !children[i].equals("Maps")) {
                    deleteFileOrDirectory(new OSVFile(file, children[i]));
                } else {
                    new OSVFile(file, children[i]).delete();
                }
            }
        }
    }

    /**
     * Returns the orientation of the display
     * In our case, since we're locked in Landscape, it should always
     * be 90
     * @param context
     * @return Orientation angle of the display
     */
    public static int getDisplayRotation(Context context) {
        if (context != null) {
            int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                    .getRotation();
            switch (rotation) {
                case Surface.ROTATION_0:
                    mRotation = 0;
                    break;
                case Surface.ROTATION_90:
                    mRotation = 90;
                    break;
                case Surface.ROTATION_180:
                    mRotation = 180;
                    break;
                case Surface.ROTATION_270:
                    mRotation = 270;
                    break;
                default:
                    mRotation = 0;
            }
        }

        return mRotation;
    }

    /**
     * Rounds the orientation so that the UI doesn't rotate if the user
     * holds the device towards the floor or the sky
     * @param orientation New orientation
     * @param orientationHistory Previous orientation
     * @return Rounded orientation
     */
    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
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
     * @param context
     * @param dp
     * @return
     */
    public static float dpToPx(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) ((dp * displayMetrics.density) + 0.5);
    }

    public static OSVFile generateOSVFolder(Context context) {
        OSVFile osv = new OSVFile(getSelectedStorage(context).getPath() + "/OSV");
        if (!osv.exists()) {
            osv.mkdir();
        }
        return osv;
    }

    /**
     * used on the older version upgrade
     * @param context
     * @param delete
     */
    public static void moveToPublic(final Context context, final boolean delete) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                OSVFile osvOld = new OSVFile(context.getFilesDir().getPath(), "OSV");
                if (!osvOld.exists()) {
                    osvOld.mkdir();
                }

                OSVFile osv = new OSVFile(getSelectedStorage(context).getPath() + "/OSV");
                if (!osv.exists()) {
                    osv.mkdir();
                }
                for (OSVFile folder : osvOld.listFiles()) {
                    OSVFile fileTo = new OSVFile(getSelectedStorage(context).getPath() + "/OSV", folder.getName());
                    if (!fileTo.exists()) {
                        fileTo.mkdir();
                    }
                    FileChannel source = null;
                    FileChannel destination = null;
                    for (OSVFile img : folder.listFiles()) {
                        try {
                            try {
                                source = new FileInputStream(img).getChannel();
                                destination = new FileOutputStream(new OSVFile(fileTo, img.getName())).getChannel();
                                destination.transferFrom(source, 0, source.size());
                                if (delete) {
                                    Log.d(TAG, "moveToPublic: deleted " + folder.getName() + "/" + img.getName() + " : " + img.delete());
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "moveToPublic: " + e.getLocalizedMessage());
                            } finally {
                                if (source != null) {
                                    source.close();
                                }
                                if (destination != null) {
                                    destination.close();
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "moveToPublic: " + e.getLocalizedMessage());
                        }

                        Log.d(TAG, "moveToPublic: copied " + folder.getName() + "/" + img.getName());
                    }
                    if (folder.listFiles().length == 0 && delete) {
                        folder.delete();
                    }
                }
                if (delete) {
                    osvOld.delete();
                }
                Log.d(TAG, "moveToPublic: copy done");
            }
        }).start();

    }

    public static File getSelectedStorage(Context context) {
        if (Build.VERSION.SDK_INT < 19 && Environment3.isSecondaryExternalStorageAvailable()) {
            try {
                return Environment3.getSecondaryExternalFilesDir(context, null);
            } catch (NoSecondaryStorageException e) {
                Log.d(TAG, "getSelectedStorage: " + Log.getStackTraceString(e));
            }
        }
        File[] storages = ContextCompat.getExternalFilesDirs(context, null);
        if (storages.length > 1) {
            File file = storages[1];
            if (file != null) {
                EXTERNAL_STORAGE_PATH = file.getPath();
            }
        }
//        Log.d(TAG, "getSelectedStorage: " + Arrays.toString(storages));
        boolean external = ((OSVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE);
        if (storages.length > 1 && external && storages[1] != null) {
//            Log.d(TAG, "getSelectedStorage: external");
            return storages[1];
        }
        if (external) {
            ((OSVApplication) context.getApplicationContext()).getAppPrefs().saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE, false);
        }
//        Log.d(TAG, "getSelectedStorage: internal");
        File result = storages[0];
        if (result == null) {
            result = context.getExternalFilesDir(null);
        }
        if (result == null) {
            return Environment3.getInternalStorage().getFile();
        }
        return result;
    }

    public static File getInternalStorage(Context context) {
        File[] storages = ContextCompat.getExternalFilesDirs(context, null);
        if (storages.length > 1) {
            File file = storages[1];
            if (file != null) {
                EXTERNAL_STORAGE_PATH = file.getPath();
            }
        }
        File result = storages[0];
        if (result == null) {
            result = context.getExternalFilesDir(null);
        }
        if (result == null) {
            return Environment3.getInternalStorage().getFile();
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
                if (file.isFile())
                    length += file.length();
                else
                    length += folderSize(file);
            }
        } catch (Exception e) {
            Log.d(TAG, "folderSize: " + e.getLocalizedMessage());
        }
        return length;
    }

    public static long fileSize(OSVFile file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        long length = 0;
        try {
            if (file.isFile())
                length = file.length();
            else
                length = folderSize(file);
        } catch (Exception e) {
            Log.d(TAG, "fileSize: " + e.getLocalizedMessage());
        }
        return length;
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
        String path = getSelectedStorage(mContext).getPath();
        OSVFile dir = new OSVFile(path);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }

        try {
            StatFs stat = new StatFs(path);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.d(TAG, "getStorageSpace: " + ((stat.getAvailableBlocksLong() * stat.getBlockSizeLong()) / 1024 / 1024));
                return (stat.getAvailableBlocksLong() * stat.getBlockSizeLong()) / 1024 / 1024;
            } else {
                Log.d(TAG, "getStorageSpace: " + ((long) (stat.getAvailableBlocks() * stat.getBlockSize()) / 1024 / 1024));
                return (stat.getAvailableBlocks() * stat.getBlockSize()) / 1024 / 1024;
            }
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);

//            if (Fabric.isInitialized()) {
//                Crashlytics.logException(e);
//            }
        }
        return UNKNOWN_SIZE;
    }

//    public static float getPictureSize(Context context) {
//        if (mImageSize == -1 || mImageSize == 0) {
//            OSVFile[] folderList = generateOSVFolder(context).listFiles();
//            if (folderList.length > 0) {
//                long total = 0;
//                float nr = folderList[0].listFiles().length;
//                for (OSVFile img : folderList[0].listFiles()) {
//                    total = total + img.length();
//                }
//                if (nr > 0) {
//                    mImageSize = (float) total / nr / 1024f / 1024f;
//                    context.getSharedPreferences("osvAppPrefs", Context.MODE_PRIVATE).edit().putFloat(PreferenceTypes.K_IMAGE_SIZE, mImageSize).commit();
//                }
//            }
//            mImageSize = context.getSharedPreferences("osvAppPrefs", Context.MODE_PRIVATE).getFloat(PreferenceTypes.K_IMAGE_SIZE, 2);
//        }
//        return mImageSize;
//    }

    public static long getStorageSpace(Context context) {
        String state = Environment.getExternalStorageState();
//        Log.d(TAG, "External storage state=" + state);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }

        OSVFile dir = new OSVFile(getSelectedStorage(context).getPath());
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }

        try {
            StatFs stat = new StatFs(getSelectedStorage(context).getPath());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.d(TAG, "getStorageSpace: " + ((stat.getBlockCountLong() * stat.getBlockSizeLong()) / 1024 / 1024));
                return (stat.getBlockCountLong() * stat.getBlockSizeLong()) / 1024 / 1024;
            } else {
                Log.d(TAG, "getStorageSpace: " + ((long) (stat.getBlockCount() * stat.getBlockSize()) / 1024 / 1024));
                return (long) (stat.getBlockCount() * stat.getBlockSize()) / 1024 / 1024;
            }
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return UNKNOWN_SIZE;
    }

    public static String formatSize(double value) {
        String sizeText = "0 MB";
        if (value > 0) {
            double size = value / (double) 1024 / (double) 1024;
            String type = " MB";
            DecimalFormat df2 = new DecimalFormat("#.#");
            if (size > 1024) {
                size = (size / (double) 1024);
                type = " GB";
                sizeText = "" + df2.format(size) + type;
            } else {
                sizeText = "" + df2.format(size) + type;
            }
        }
        return sizeText;
    }

    public static boolean checkSDCard(Context context) {
        if (Build.VERSION.SDK_INT < 19) {
            return Environment3.isSecondaryExternalStorageAvailable();
        } else {
            File[] storages = ContextCompat.getExternalFilesDirs(context, null);
            Log.d(TAG, "getSelectedStorage: " + Arrays.toString(storages));
            return storages.length > 1 && storages[1] != null;
        }
    }

    public static String[] formatSpeedFromKmph(Context context, int speed) {
        boolean isUS = !((OSVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
        int ret = speed;
        if (isUS) {
            ret = (int) (speed * KILOMETER_TO_MILE);
            return new String[]{ret + "", "mph"};
        } else {
            return new String[]{ret + "", "km/h"};
        }
    }

    public static boolean isInsideBoundingBox(double latPoint, double lonPoint, double latTopLeft, double longTopLeft, double latBottomRight, double longBottomRight) {
        if ((latPoint <= latTopLeft) && (latPoint >= latBottomRight) && (lonPoint >= longTopLeft) && (lonPoint <= longBottomRight)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isDebuggableFlag(Context context) {
        return (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
    }
}