package com.telenav.osv.ui.fragment.settings.viewmodel;

import android.app.Application;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup;
import com.telenav.osv.ui.fragment.settings.presenter.group.RadioGroupPresenter;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ViewModel} class responsible to handle the functionality for the  {@code Distance Unit} submenu.
 * @author cameliao
 */
public class DistanceUnitViewModel extends SettingsBaseViewModel {

    /**
     * Tag defined for the metric distance unit.
     */
    public static final int TAG_METRIC = 0;

    /**
     * Tag defined for the imperial distance unit.
     */
    public static final int TAG_IMPERIAL = 1;

    public DistanceUnitViewModel(@NonNull Application application, @NonNull ApplicationPreferences appPrefs) {
        super(application, appPrefs);
    }

    @Override
    public int getTitle() {
        return R.string.settings_distance_unit_title;
    }

    @Override
    public void start() {
        List<SettingsGroup> settingsCategories = new ArrayList<>();
        settingsCategories.add(createDistanceUnitCategory());
        settingsDataObservable.setValue(settingsCategories);
    }

    /**
     * @return the group model for the distance unit options.
     */
    private SettingsGroup createDistanceUnitCategory() {
        return new SettingsGroup(new RadioGroupPresenter(getDistanceUnitOptions(), getDistanceUnitGroupListener()));
    }

    /**
     * @return a list of {@code RadioButtons} representing the distance unit options.
     */
    private List<RadioButton> getDistanceUnitOptions() {
        List<RadioButton> radioButtons = new ArrayList<>();
        RadioButton metricRadioButton = (RadioButton) LayoutInflater.from(getApplication().getApplicationContext()).inflate(R.layout.settings_item_radio, null);
        metricRadioButton.setText(getApplication().getString(R.string.settings_metric_label));
        metricRadioButton.setTag(TAG_METRIC);
        metricRadioButton.setId(View.generateViewId());
        RadioButton imperialRadioButton = (RadioButton) LayoutInflater.from(getApplication().getApplicationContext()).inflate(R.layout.settings_item_radio, null);
        imperialRadioButton.setText(getApplication().getString(R.string.settings_imperial_label));
        imperialRadioButton.setTag(TAG_IMPERIAL);
        imperialRadioButton.setId(View.generateViewId());
        if (isDistanceUnitMetric()) {
            metricRadioButton.setChecked(true);
        } else {
            imperialRadioButton.setChecked(true);
        }
        radioButtons.add(metricRadioButton);
        radioButtons.add(imperialRadioButton);
        return radioButtons;
    }

    /**
     * @return a {@code listener} which is called when a distance unit option is selected.
     */
    private RadioGroup.OnCheckedChangeListener getDistanceUnitGroupListener() {
        return (radioGroup, i) -> {
            RadioButton radioButton = radioGroup.findViewById(i);
            if (radioButton != null) {
                int distanceUnitTag = (int) radioButton.getTag();
                ApplicationPreferences applicationPreferences = ((KVApplication) getApplication()).getAppPrefs();
                applicationPreferences.saveBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC, distanceUnitTag == TAG_METRIC);
            }
        };
    }

    /**
     * @return {@code true} if the distance unit is set to metric, {@code false} for imperial.
     */
    private boolean isDistanceUnitMetric() {
        ApplicationPreferences appPrefs = ((KVApplication) getApplication()).getAppPrefs();
        return appPrefs.getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
    }
}