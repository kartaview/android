package com.telenav.osv.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import android.annotation.SuppressLint;
import android.content.Context;
import com.crashlytics.android.Crashlytics;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.item.OSVFile;
import io.fabric.sdk.android.Fabric;

/**
 * Internal logging chained through crashlytics logging
 * Created by Kalman on 2/8/16.
 */
public class Log {

    public static final String RECORD_STATUS = "recording";

    public static final String CURRENT_SCREEN = "currentScreen";

    public static final String SDK_ENABLED = "sdkEnabled";

    public static final String POINTS_ENABLED = "pointsEnabled";

    public static final String UPLOAD_STATUS = "uploading";

    public static final String SAFE_RECORDING = "safeMode";

    public static final String STATIC_FOCUS = "staticFocus";

    public static final String CAMERA_API_NEW = "cameraApiNew";

    public static final String LOG_FILE = "logFileName";

    public static final String PLAYBACK = "playback";

    public static final String USER_TYPE = "userType";

    public static final String OLD_VERSION = "oldVersion";

    public static final String NEW_VERSION = "oldVersion";

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM_dd_hh_mm");

    private static final long TEN_DAYS = 10L * 24L * 60L * 60L * 1000L;

    private static final String[] TYPES = new String[]{"D", "D", "V", "D", "I", "W", "E"};

    private static final long TWO_DAYS = 48L * 60L * 60L * 1000L;

    @SuppressLint("SdCardPath")
    public static String externalFilesDir = "/sdcard/Android/data/com.telenav.streetview/files/";

    private static File logFile;

    public static File getLogFile() {
        if (logFile == null) {
            logFile = new File(externalFilesDir, "log_" + dateFormat.format(OSVApplication.runTime) + ".txt");
        }
        return logFile;
    }

    public static void d(String tag, String message) {
        appendLog(android.util.Log.DEBUG, tag, message);
    }

    public static void w(String tag, String message) {
        appendLog(android.util.Log.WARN, tag, message);
    }

    public static void e(String tag, String message) {
        appendLog(android.util.Log.ERROR, tag, message);
    }

    public static void v(String tag, String message) {
        appendLog(android.util.Log.VERBOSE, tag, message);
    }

    public static void i(String tag, String message) {
        appendLog(android.util.Log.INFO, tag, message);
    }

    public static String getStackTraceString(Throwable throwable) {
        return android.util.Log.getStackTraceString(throwable);
    }

    public static void d(String tag, String message, Exception e) {
        appendLog(android.util.Log.DEBUG, tag, message + " " + android.util.Log.getStackTraceString(e));
        android.util.Log.d(tag, message, e);
    }

    public static void e(String tag, String message, Exception e) {
        appendLog(android.util.Log.ERROR, tag, message + " " + android.util.Log.getStackTraceString(e));
        android.util.Log.e(tag, message, e);
    }

    public static void i(String tag, String message, Exception e) {
        appendLog(android.util.Log.INFO, tag, message + " " + android.util.Log.getStackTraceString(e));
        android.util.Log.i(tag, message, e);
    }

    private static void appendLog(int priority, String tag, String text) {
        if (logFile == null) {
            logFile = new File(externalFilesDir, "log_" + dateFormat.format(OSVApplication.runTime) + ".txt");
        }
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(String.valueOf(System.currentTimeMillis())).append(" ");
            buf.append(TYPES[priority]).append("/").append(tag).append(": ");
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
        if (Fabric.isInitialized()) {
            try {
                Crashlytics.log(priority, tag, text);
            } catch (Exception ignored) {}
        } else {
            android.util.Log.println(priority, tag, text);
        }
    }

    public static void deleteOldLogs(Context context) {
        try {
            File files = new File(context.getExternalFilesDir(null).getPath());
            for (File f : files.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.contains("log") && !filename.contains("av_");
                }
            })) {
                if (System.currentTimeMillis() - f.lastModified() > TEN_DAYS) {
                    f.delete();
                }
            }
            OSVFile avRecordLog = new OSVFile(files, "av_recording_log.txt");
            OSVFile avPlayerLog = new OSVFile(files, "av_player_log.txt");
            if (avRecordLog.exists() && Utils.fileSize(avRecordLog) > 1024 * 1024 * 20) {
                avRecordLog.delete();
            }
            if (avPlayerLog.exists() && Utils.fileSize(avPlayerLog) > 1024 * 1024 * 20) {
                avPlayerLog.delete();
            }
        } catch (Exception e) {
            android.util.Log.d("Log", "deleteOldLogs: " + e.getLocalizedMessage());
        }
    }

    public static ArrayList<OSVFile> getLogFiles(Context context) {
        ArrayList<OSVFile> files = new ArrayList<>();
        OSVFile folder = new OSVFile(context.getExternalFilesDir(null).getPath());
        Collections.addAll(files, folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                OSVFile file = new OSVFile(dir, filename);
                return (filename.contains("log") || filename.contains("av_")) && System.currentTimeMillis() - file.lastModified() <= TWO_DAYS;
            }
        }));
        return files;
    }
}
