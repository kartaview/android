package com.telenav.osv.ui.fragment.settings.model;

import javax.annotation.Nullable;
import android.content.Context;
import com.telenav.osv.ui.fragment.settings.presenter.item.PreferencePresenter;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.preference.Preference;

/**
 * Data model for {@code Preference} which represents a settings item.
 */
public class SettingsItem {

    /**
     * The title of the item.
     */
    private String title;

    /**
     * The summary of the item.
     */
    private String summary;

    /**
     * The preference key for storing data in {@code SharedPreferences}.
     */
    private String preferenceKey;

    /**
     * The resource id for the item's icon.
     */
    private int icon;

    /**
     * The class responsible to create the {@code Preference}.
     */
    private PreferencePresenter presenter;


    /**
     * Constructor fo the settings {@code Preference}.
     * @param title represents the title of the item.
     * @param summary represents the summary of the item.
     * @param preferenceKey represents the key for storing data into {@code SharedPreferences}.
     * @param icon represents the resource id of the item's icon.
     * @param presenter the class which is responsible to create the {@code Preference}.
     */
    public SettingsItem(@Nullable String title, @Nullable String summary, @Nullable String preferenceKey, @DrawableRes int icon, @NonNull PreferencePresenter presenter) {
        this.title = title;
        this.summary = summary;
        this.preferenceKey = preferenceKey;
        this.icon = icon;
        this.presenter = presenter;
    }

    /**
     * @return a {@code String} representing {@link SettingsItem#title}.
     */
    @Nullable
    public String getTitle() {
        return title;
    }

    /**
     * @return a {@code String} representing {@link SettingsItem#summary}.
     */
    @Nullable
    public String getSummary() {
        return summary;
    }

    /**
     * @return a {@code String} representing {@link SettingsItem#preferenceKey}.
     */
    @Nullable
    public String getPreferenceKey() {
        return preferenceKey;
    }

    /**
     * @return a {@code int} representing {@link SettingsItem#icon}.
     */
    @DrawableRes
    public int getIcon() {
        return icon;
    }

    /**
     * @param context the context defined by the {@code PreferenceManager}.
     * @return the {@code Preference} which should be displayed for the current item.
     */
    public Preference getPreference(Context context) {
        return presenter.getPreference(context, this);
    }
}