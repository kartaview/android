package com.telenav.osv.ui.fragment.settings.presenter.item;

import android.content.Context;
import com.telenav.osv.R;
import com.telenav.osv.ui.fragment.settings.model.SettingsItem;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

/**
 * Presenter which is responsible to create a {@link SwitchPreferenceCompat} view from a given data model.
 */
public class SwitchPresenter extends PreferencePresenter {

    /**
     * Listener called when teh preference value is changed.
     */
    private Preference.OnPreferenceChangeListener changeListener;

    /**
     * Default constructor for the current class.
     */
    public SwitchPresenter() {
        super(null);
    }

    /**
     * Constructor for the current class in order to provide a custom click listener.
     * @param clickListener the click action for the current preference.
     */
    public SwitchPresenter(@NonNull Preference.OnPreferenceClickListener clickListener) {
        super(clickListener);
    }

    /**
     * Constructor for the current class in order to provide a custom listener for value changes.
     * @param changeListener the listener when the preferences value changes.
     */
    public SwitchPresenter(@NonNull Preference.OnPreferenceChangeListener changeListener) {
        super(null);
        this.changeListener = changeListener;
    }

    @Override
    public Preference getPreference(@NonNull Context context, @NonNull SettingsItem item) {
        Preference preference = new SwitchPreferenceCompat(context);
        onConfigure(preference, item);
        preference.setWidgetLayoutResource(R.layout.settings_item_switch);
        if (changeListener != null) {
            preference.setOnPreferenceChangeListener(changeListener);
        }
        return preference;
    }
}