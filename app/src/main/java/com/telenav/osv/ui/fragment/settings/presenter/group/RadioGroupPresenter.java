package com.telenav.osv.ui.fragment.settings.presenter.group;


import java.util.List;
import android.content.Context;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.telenav.osv.ui.fragment.settings.custom.RadioGroupPreference;
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceGroup;

/**
 * Presenter responsible to create a {@link RadioGroupPreference} view from a given class model.
 */
public class RadioGroupPresenter extends CategoryPresenter {

    /**
     * The radio button list representing the group options.
     */
    private List<RadioButton> options;

    /**
     * The group click listener notified when an option was selected.
     */
    private RadioGroup.OnCheckedChangeListener listener;

    /**
     * Default constructor for the current class.
     * @param options the options which are set to the group.
     * @param listener the group click listener notified when an option was selected.
     */
    public RadioGroupPresenter(List<RadioButton> options, RadioGroup.OnCheckedChangeListener listener) {
        this.options = options;
        this.listener = listener;
    }

    @Override
    public PreferenceGroup getPreference(@NonNull Context context, @NonNull SettingsGroup group) {
        RadioGroupPreference radioGroupPreference = new RadioGroupPreference(context, options);
        radioGroupPreference.setOnCheckedChangeListener(listener);
        return radioGroupPreference;
    }
}