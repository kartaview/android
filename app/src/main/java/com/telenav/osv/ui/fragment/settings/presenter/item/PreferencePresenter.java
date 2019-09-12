package com.telenav.osv.ui.fragment.settings.presenter.item;

import android.content.Context;
import com.telenav.osv.ui.fragment.settings.model.SettingsItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

/**
 * Presenter which is responsible to create a {@link Preference} view from a given data model.
 */
public class PreferencePresenter {

    /**
     * The click action for the current preference.
     */
    private Preference.OnPreferenceClickListener clickListener;

    /**
     * Default constructor for the current class.
     * @param clickListener the click action for the current preference.
     */
    public PreferencePresenter(@Nullable Preference.OnPreferenceClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /**
     * Defines the {@code Preference} from data model.
     * @param item data object containing all the required information for a preference.
     * @param context the context from {@code PreferenceManager} used to create the preference object.
     * @return a customised {@code Preference}.
     */
    public Preference getPreference(@NonNull Context context, @NonNull SettingsItem item) {
        Preference preference = new Preference(context);
        onConfigure(preference, item);
        return preference;
    }

    /**
     * Configures the common elements for a {@code Preference} view.
     * @param preference the view which should be customised.
     * @param item data model containing the information for the preference.
     */
    void onConfigure(@NonNull Preference preference, @NonNull SettingsItem item) {
        if (item.getPreferenceKey() != null && !item.getPreferenceKey().isEmpty()) {
            preference.setKey(item.getPreferenceKey());
        }
        if (item.getIcon() != 0) {
            preference.setIcon(item.getIcon());
        }
        if (item.getSummary() != null) {
            preference.setSummary(item.getSummary());
        }
        if (item.getTitle() != null) {
            preference.setTitle(item.getTitle());
        }
        if (clickListener != null) {
            preference.setOnPreferenceClickListener(clickListener);
        }
    }
}