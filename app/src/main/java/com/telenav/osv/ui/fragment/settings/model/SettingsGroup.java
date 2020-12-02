package com.telenav.osv.ui.fragment.settings.model;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import com.telenav.osv.ui.fragment.settings.presenter.group.CategoryPresenter;
import com.telenav.osv.utils.StringUtils;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceGroup;

/**
 * Data model for {@code PreferenceCategory} which represents a group of settings with common functionality.
 */
public class SettingsGroup {

    /**
     * The title of the category.
     */
    private String title;

    /**
     * The list of settings with common functionality for this category.
     */
    private List<SettingsItem> items;

    /**
     * The class responsible to create the {@code Group} preference.
     */
    private CategoryPresenter presenter;

    /**
     * Constructor for the {@code Category} group.
     * @param title represents the title of the category.
     * @param items represents the list of settings with common functionality.
     * @param presenter the class which is responsible to create the {@code Category}.
     */
    public SettingsGroup(@NonNull String title, @NonNull List<SettingsItem> items, @NonNull CategoryPresenter presenter) {
        this.title = title;
        this.items = items;
        this.presenter = presenter;
    }

    /**
     * Constructor for the {@code RadioGroup} and {@code Footer} preferences.
     * @param presenter the class which is responsible to create the preference.
     */
    public SettingsGroup(@NonNull CategoryPresenter presenter) {
        this.title = StringUtils.EMPTY_STRING;
        this.items = new ArrayList<>();
        this.presenter = presenter;
    }

    /**
     * @return a {@code String} representing {@link SettingsGroup#title}.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return a list of settings items representing {@link SettingsGroup#items}.
     */
    public List<SettingsItem> getItems() {
        return items;
    }

    /**
     * @param context the context defined by the {@code PreferenceManager}.
     * @return the {@code PreferenceGroup} which should be displayed for the current group.
     */
    public PreferenceGroup getPreference(Context context) {
        return presenter.getPreference(context, this);
    }
}