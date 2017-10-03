package com.telenav.osv.ui.binding.viewmodel.settings;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;

/**
 * view model for setting category item
 * Created by kalmanb on 9/7/17.
 */
public class SettingsItemCategoryViewModel extends SettingsItemViewModel {

    public final ObservableField<String> title = new ObservableField<>();

    public final ObservableBoolean visible = new ObservableBoolean();

    public SettingsItemCategoryViewModel(Application application, LifecycleOwner owner) {
        super(application);
        this.owner = owner;
        visible.set(true);
    }

    public SettingsItemCategoryViewModel setTitle(String title) {
        this.title.set(title);
        return this;
    }

    public SettingsItemCategoryViewModel setVisibility(LiveData<Boolean> visibilityPref) {
        visibilityPref.observe(owner, this::setVisible);
        return this;
    }

    public void setVisible(boolean value) {
        visible.set(value);
    }
}
