package com.telenav.osv.data;

/**
 * Class for handling preferences easily
 * Created by kalmanb on 9/14/17.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public interface RunPreferences {

    boolean isFirstRun();

    int getFfmpegCrashCounter();

    void setFfmpegCrashCounter(int value);

    int getRestartCounter();

    void setRestartCounter(int value);

    boolean getCrashed();

    void setCrashed(boolean value);
}
