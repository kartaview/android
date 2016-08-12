package com.telenav.osv.application;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * This class handles preference changes for the app
 * Created by Kalman on 10/7/2015.
 */
public class ApplicationPreferences {

    public static final String CURRENT_VERSION_CODE = "currentVersionCode";

    /**
     * preference name
     */
    public static final String PREFS_NAME = "osvAppPrefs";

    /**
     * used for modifying values in a SharedPreferences prefs
     */
    private SharedPreferences.Editor prefsEditor;

    /**
     * reference to preference
     */
    private SharedPreferences prefs;

    /**
     * the context
     */
    private Context context;

    public ApplicationPreferences(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();
    }

    public int getIntPreference(String key) {
        return prefs.getInt(key, 0);
    }

    public String getStringPreference(String key) {
        return prefs.getString(key, "");
    }

    public boolean getBooleanPreference(String key) {
        return prefs.getBoolean(key, false);
    }

    public boolean getBooleanPreference(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public float getFloatPreference(String key) {
        return prefs.getFloat(key, 0);
    }

    public float getFloatPreference(String key, float defaultValue) {
        return prefs.getFloat(key, defaultValue);
    }

    public void setCurrentVersionCode(int versionCode) {
        prefsEditor.putInt(CURRENT_VERSION_CODE, versionCode);
        prefsEditor.commit();
    }

    public void saveStringPreference(String key, String value) {
        prefsEditor.putString(key, value);
        prefsEditor.commit();
    }

    public void saveBooleanPreference(String key, boolean value) {
        prefsEditor.putBoolean(key, value);
        prefsEditor.commit();
    }

    public void saveFloatPreference(String key, float value) {
        prefsEditor.putFloat(key, value);
        prefsEditor.commit();
    }

    public void saveIntPreference(String key, int value) {
        prefsEditor.putInt(key, value);
        prefsEditor.commit();
    }
}
