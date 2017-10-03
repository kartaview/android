package com.telenav.osv.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import com.telenav.osv.utils.Log;
import java.util.HashSet;
import java.util.Set;

import static com.telenav.osv.data.PreferenceTypes.K_INTRO_SHOWN_OLD;
import static com.telenav.osv.data.PreferenceTypes.K_SHOW_INTRO;

/**
 * This class handles preference changes for the app
 * Created by Kalman on 10/7/2015.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public class ApplicationPreferences {

  public static final String CURRENT_VERSION_CODE = "currentVersionCode";

  /**
   * preference name
   */
  private static final String PREFS_NAME = "osvAppPrefs";

  /**
   * used for modifying values in a SharedPreferences prefs
   */
  private SharedPreferences.Editor prefsEditor;

  /**
   * reference to preference
   */
  private SharedPreferences prefs;

  @SuppressLint("CommitPrefEdits")
  public ApplicationPreferences(Context context) {
    super();
    prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    prefsEditor = prefs.edit();
    if (prefs.contains(K_INTRO_SHOWN_OLD)) {
      Log.d("Preferences", "ApplicationPreferences: upgrading walkthrough pref");
      saveBooleanPreference(K_SHOW_INTRO, false);
      prefs.edit().remove(K_INTRO_SHOWN_OLD).apply();
    }
  }

  public int getIntPreference(String key) {
    return prefs.getInt(key, 0);
  }

  public int getIntPreference(String key, int def) {
    return prefs.getInt(key, def);
  }

  public String getStringPreference(String key) {
    return prefs.getString(key, "");
  }

  public String getStringPreference(String key, String defValue) {
    return prefs.getString(key, defValue);
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

  public Set<String> getStringSetPreference(String key, Set<String> defaultValue) {
    return prefs.getStringSet(key, defaultValue);
  }

  public Set<String> getStringSetPreference(String key) {
    return prefs.getStringSet(key, new HashSet<>());
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

  public void saveFloatPreference(String key, float value, boolean later) {
    prefsEditor.putFloat(key, value);
    if (later) {
      prefsEditor.apply();
    } else {
      prefsEditor.commit();
    }
  }

  public void saveIntPreference(String key, int value) {
    prefsEditor.putInt(key, value);
    prefsEditor.commit();
  }

  public void saveStringSetPreference(String key, Set<String> strings) {
    prefsEditor.putStringSet(key, strings);
    prefsEditor.commit();
  }

  public PrefLiveData.PrefIntLiveData getLiveIntPreference(String key) {
    return new PrefLiveData.PrefIntLiveData(prefs, key, 0);
  }

  public PrefLiveData.PrefIntLiveData getLiveIntPreference(String key, int defValue) {
    return new PrefLiveData.PrefIntLiveData(prefs, key, defValue);
  }

  public PrefLiveData.PrefStringLiveData getLiveStringPreference(String key) {
    return new PrefLiveData.PrefStringLiveData(prefs, key, "");
  }

  public PrefLiveData.PrefStringLiveData getLiveStringPreference(String key, String defValue) {
    return new PrefLiveData.PrefStringLiveData(prefs, key, defValue);
  }

  public PrefLiveData.PrefLongLiveData getLiveLongPreference(String key) {
    return new PrefLiveData.PrefLongLiveData(prefs, key, 0L);
  }

  public PrefLiveData.PrefLongLiveData getLiveLongPreference(String key, long defValue) {
    return new PrefLiveData.PrefLongLiveData(prefs, key, defValue);
  }

  public PrefLiveData.PrefStringSetLiveData getLiveStringSetPreference(String key, Set<String> defValue) {
    return new PrefLiveData.PrefStringSetLiveData(prefs, key, defValue);
  }

  public PrefLiveData.PrefBooleanLiveData getLiveBooleanPreference(String key) {
    return new PrefLiveData.PrefBooleanLiveData(prefs, key, false);
  }

  public PrefLiveData.PrefBooleanLiveData getLiveBooleanPreference(String key, boolean defValue) {
    return new PrefLiveData.PrefBooleanLiveData(prefs, key, defValue);
  }
}
