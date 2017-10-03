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
public class SettingsItemOneRowViewModel extends SettingsItemViewModel {

    public final ObservableField<String> title = new ObservableField<>();

    private SingleLiveEvent event;

    public SettingsItemOneRowViewModel(Application application, LifecycleOwner owner) {
        super(application);
        this.owner = owner;
    }

    public void onClick() {
        if (event != null) {
            event.call();
        }
    }

    public SettingsItemOneRowViewModel setTitle(MutableLiveData<String> title) {
        title.observe(owner, this.title::set);
        return this;
    }

    public SettingsItemOneRowViewModel setTitle(String title) {
        this.title.set(title);
        return this;
    }

    public SettingsItemOneRowViewModel setEvent(SingleLiveEvent event) {
        this.event = event;
        return this;
    }
}
