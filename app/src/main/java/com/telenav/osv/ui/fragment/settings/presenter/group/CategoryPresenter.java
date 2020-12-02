package com.telenav.osv.ui.fragment.settings.presenter.group;

import android.content.Context;
import com.telenav.osv.R;
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

/**
 * Presenter which is responsible to create a {@link PreferenceCategory} view from a given model.
 */
public class CategoryPresenter {

    /**
     * Defines the {@code PreferenceCategory} from data model.
     * @param group data object containing all the required information for a group view.
     * @param context the context from {@code PreferenceManager} used to create the preference object.
     * @return a customised {@code PreferenceCategory}.
     */
    public PreferenceGroup getPreference(@NonNull Context context, @NonNull SettingsGroup group) {
        PreferenceGroup preferenceCategory = new PreferenceCategory(context);
        preferenceCategory.setLayoutResource(R.layout.settings_item_category);
        preferenceCategory.setTitle(group.getTitle());
        return preferenceCategory;
    }
}