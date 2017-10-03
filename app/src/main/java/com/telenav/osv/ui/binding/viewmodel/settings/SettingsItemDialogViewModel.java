package com.telenav.osv.ui.binding.viewmodel.settings;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.databinding.ObservableField;
import com.telenav.osv.ui.binding.viewmodel.SingleLiveEvent;

/**
 * view model for setting item with only one text field
 * Created by kalmanb on 9/7/17.
 */
public class SettingsItemDialogViewModel extends SettingsItemViewModel {

    public final ObservableField<String> subtitle = new ObservableField<>();

    public final ObservableField<String> title = new ObservableField<>();

    public final ObservableField<String> value = new ObservableField<>();

    private SingleLiveEvent event;

    public SettingsItemDialogViewModel(Application application, LifecycleOwner owner) {
        super(application);
        this.owner = owner;
    }

    public SettingsItemDialogViewModel setTitle(String title) {
        this.title.set(title);
        return this;
    }

    public SettingsItemDialogViewModel setTitle(MutableLiveData<String> title) {
        title.observe(owner, this::setTitle);
        return this;
    }

    public SettingsItemDialogViewModel setSubTitle(String subtitle) {
        this.subtitle.set(subtitle);
        return this;
    }

    public SettingsItemDialogViewModel setValue(MutableLiveData<String> value) {
        value.observe(owner, this.value::set);
        return this;
    }

    public SettingsItemDialogViewModel setEvent(SingleLiveEvent<Void> event) {
        this.event = event;
        return this;
    }

    public void onClick() {
        event.call();
    }
}
