package com.telenav.osv.data;

import java.util.Set;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

/**
 * LiveData wrapper for preferences
 * Created by kalmanb on 9/8/17.
 */
public abstract class PrefLiveData<T> extends MutableLiveData<T> {

    private static final String TAG = "PrefLiveData";

    final SharedPreferences sharedPrefs;

    final String key;

    final T defValue;

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {

                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals(PrefLiveData.this.key)) {
                        setValue(getValueFromPreferences(key, defValue));
                    }
                }
            };

    PrefLiveData(SharedPreferences sharedPrefs, String key, T defValue) {
        this.sharedPrefs = sharedPrefs;
        this.key = key;
        this.defValue = defValue;
    }

    @Override
    public void postValue(T value) {
        putValue(value);
        super.postValue(value);
    }

    @Nullable
    @Override
    public T getValue() {
        return getValueFromPreferences();
    }

    @Override
    public void setValue(T value) {
        putValue(value);
        super.setValue(value);
    }

    @Override
    protected void onActive() {
        super.onActive();
        postValue(getValueFromPreferences(key, defValue));
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    protected void onInactive() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        super.onInactive();
    }

    protected abstract void putValue(T value);

    public static class PrefIntLiveData extends PrefLiveData<Integer> {

        PrefIntLiveData(SharedPreferences sharedPrefs, String key, Integer defValue) {
            super(sharedPrefs, key, defValue);
        }

        @Override
        public Integer getValueFromPreferences(String key, Integer defValue) {
            return sharedPrefs.getInt(key, defValue);
        }

        @Override
        public Integer getValueFromPreferences() {
            return sharedPrefs.getInt(key, defValue);
        }

        @Override
        protected void putValue(Integer value) {
            sharedPrefs.edit().putInt(key, value).apply();
        }
    }

    public static class PrefStringLiveData extends PrefLiveData<String> {

        PrefStringLiveData(SharedPreferences sharedPrefs, String key, String defValue) {
            super(sharedPrefs, key, defValue);
        }

        @Override
        String getValueFromPreferences(String key, String defValue) {
            return sharedPrefs.getString(key, defValue);
        }

        @Override
        String getValueFromPreferences() {
            return sharedPrefs.getString(key, defValue);
        }

        @Override
        protected void putValue(String value) {
            sharedPrefs.edit().putString(key, value).apply();
        }
    }

    public static class PrefBooleanLiveData extends PrefLiveData<Boolean> {

        PrefBooleanLiveData(SharedPreferences sharedPrefs, String key, Boolean defValue) {
            super(sharedPrefs, key, defValue);
        }

        @Override
        Boolean getValueFromPreferences(String key, Boolean defValue) {
            return sharedPrefs.getBoolean(key, defValue);
        }

        @Override
        Boolean getValueFromPreferences() {
            return sharedPrefs.getBoolean(key, defValue);
        }

        @Override
        protected void putValue(Boolean value) {
            sharedPrefs.edit().putBoolean(key, value).apply();
        }
    }

    public static class PrefFloatLiveData extends PrefLiveData<Float> {

        PrefFloatLiveData(SharedPreferences sharedPrefs, String key, Float defValue) {
            super(sharedPrefs, key, defValue);
        }

        @Override
        Float getValueFromPreferences(String key, Float defValue) {
            return sharedPrefs.getFloat(key, defValue);
        }

        @Override
        Float getValueFromPreferences() {
            return sharedPrefs.getFloat(key, defValue);
        }

        @Override
        protected void putValue(Float value) {
            sharedPrefs.edit().putFloat(key, value).apply();
        }
    }

    public static class PrefLongLiveData extends PrefLiveData<Long> {

        PrefLongLiveData(SharedPreferences sharedPrefs, String key, Long defValue) {
            super(sharedPrefs, key, defValue);
        }

        @Override
        Long getValueFromPreferences(String key, Long defValue) {
            return sharedPrefs.getLong(key, defValue);
        }

        @Override
        Long getValueFromPreferences() {
            return sharedPrefs.getLong(key, defValue);
        }

        @Override
        protected void putValue(Long value) {
            sharedPrefs.edit().putLong(key, value).apply();
        }
    }

    public static class PrefStringSetLiveData extends PrefLiveData<Set<String>> {

        PrefStringSetLiveData(SharedPreferences sharedPrefs, String key, Set<String> defValue) {
            super(sharedPrefs, key, defValue);
        }

        @Override
        Set<String> getValueFromPreferences(String key, Set<String> defValue) {
            return sharedPrefs.getStringSet(key, defValue);
        }

        @Override
        Set<String> getValueFromPreferences() {
            return sharedPrefs.getStringSet(key, defValue);
        }

        @Override
        protected void putValue(Set<String> value) {
            sharedPrefs.edit().putStringSet(key, value).apply();
        }
    }

    abstract T getValueFromPreferences(String key, T defValue);

    abstract T getValueFromPreferences();
}