package com.telenav.osv.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import android.content.Context;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.item.OSVFile;

/**
 * Created by Kalman on 2/8/16.
 */
public class Log {
    private static final long TEN_DAYS = 10L * 24L * 60L * 60L * 1000L;

    public static boolean DEBUG = false;

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM_dd_hh_mm");

    public static String externalFilesDir = "/sdcard/Android/data/com.telenav.osv/files/";

    private static File logFile;

    public static void d(String tag, String message) {
        if (DEBUG) {
            appendLog(System.currentTimeMillis() + " D/" + tag + ": " + message);
        }
        android.util.Log.d(tag, message);
    }

    public static void w(String tag, String message) {
        if (DEBUG) {
            appendLog(System.currentTimeMillis() + " W/" + tag + ": " + message);
        }
        android.util.Log.w(tag, message);
    }

    public static void e(String tag, String message) {
        android.util.Log.e(tag, message);
        if (DEBUG) {
            appendLog(System.currentTimeMillis() + " E/" + tag + ": " + message);
        }
    }

    public static void v(String tag, String message) {
        if (DEBUG) {
            appendLog(System.currentTimeMillis() + " V/" + tag + ": " + message);
        }
        android.util.Log.v(tag, message);
    }

    public static void i(String tag, String message) {
        if (DEBUG) {
            appendLog(System.currentTimeMillis() + " I/" + tag + ": " + message);
        }
        android.util.Log.i(tag, message);
    }

    public static String getStackTraceString(Throwable throwable) {
        return android.util.Log.getStackTraceString(throwable);
    }

    public static void d(String tag, String message, Exception e) {
        if (DEBUG) {
            appendLog(System.currentTimeMillis() + " D/" + tag + ": " + message + " " + android.util.Log.getStackTraceString(e));
        }
        android.util.Log.d(tag, message, e);
    }

    public static void e(String tag, String message, Exception e) {
        if (DEBUG) {
            appendLog(System.currentTimeMillis() + " E/" + tag + ": " + message + " " + android.util.Log.getStackTraceString(e));
        }
        android.util.Log.e(tag, message, e);
    }

    public static void i(String tag, String message, Exception e) {
        if (DEBUG) {
            appendLog(System.currentTimeMillis() + " I/" + tag + ": " + message + " " + android.util.Log.getStackTraceString(e));
        }
        android.util.Log.i(tag, message, e);
    }

    public static void appendLog(String text) {
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
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            //e.printStackTrace();
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
            OSVFile avRecordLog = new OSVFile(files,"av_recording_log.txt");
            OSVFile avPlayerLog = new OSVFile(files,"av_player_log.txt");
            if (avRecordLog.exists() && Utils.fileSize(avRecordLog) > 1024*1024*20){
                avRecordLog.delete();
            }
            if (avPlayerLog.exists() && Utils.fileSize(avPlayerLog) > 1024*1024*20){
                avPlayerLog.delete();
            }
        } catch (Exception e) {
            if (DEBUG) {
                android.util.Log.d("Log", "deleteOldLogs: " + e.getLocalizedMessage());
            }
        }
    }
}
