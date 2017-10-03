package com.telenav.osv.ui.binding.viewmodel.settings;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;

/**
 * view model for setting item with switch
 * Created by kalmanb on 9/7/17.
 */
public class SettingsItemSwitchViewModel extends SettingsItemViewModel {

    private static final String TAG = "SettingsItemSwitchViewModel";

    public final ObservableField<String> title = new ObservableField<>();

    public final ObservableField<String> subtitle = new ObservableField<>();

    public final ObservableBoolean checked = new ObservableBoolean();

    private MutableLiveData<Boolean> pref;

    public SettingsItemSwitchViewModel(Application application, LifecycleOwner owner) {
        super(application);
        this.owner = owner;
    }

    public SettingsItemSwitchViewModel setTitle(String title) {
        this.title.set(title);
        return this;
    }

    public SettingsItemSwitchViewModel setSubtitle(String subtitle) {
        this.subtitle.set(subtitle);
        return this;
    }

    public SettingsItemSwitchViewModel setPreference(MutableLiveData<Boolean> pref) {
        this.pref = pref;
        pref.observe(owner, enabled -> checked.set(enabled != null && enabled));
        return this;
    }

    public void onChecked(boolean checked) {
        pref.setValue(checked);
    }

    public void onToggleChecked() {
        pref.setValue(!checked.get());
    }
}
