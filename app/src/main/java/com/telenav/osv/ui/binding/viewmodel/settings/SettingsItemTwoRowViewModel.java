package com.telenav.osv.ui.binding.viewmodel.settings;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.databinding.ObservableField;
import com.telenav.osv.ui.binding.viewmodel.SingleLiveEvent;

/**
 * view model for setting item with two text fields
 * Created by kalmanb on 9/7/17.
 */
public class SettingsItemTwoRowViewModel extends SettingsItemViewModel {

    public final ObservableField<String> title = new ObservableField<>();

    public final ObservableField<String> subtitle = new ObservableField<>();

    private SingleLiveEvent<Void> event;

    private MutableLiveData<Boolean> hiddenFunctionality;

    private long lastHitTime;

    private int counter;

    private SingleLiveEvent<Integer> delayedAction;

    public SettingsItemTwoRowViewModel(Application application, LifecycleOwner owner) {
        super(application);
        this.owner = owner;
    }

    public SettingsItemTwoRowViewModel setHiddenFunctionality(MutableLiveData<Boolean> event) {
        hiddenFunctionality = event;
        return this;
    }

    public SettingsItemTwoRowViewModel setTitle(String title) {
        this.title.set(title);
        return this;
    }

    public SettingsItemTwoRowViewModel setSubtitle(String subtitle) {
        this.subtitle.set(subtitle);
        return this;
    }

    public SettingsItemTwoRowViewModel setSubtitle(LiveData<String> subtitle) {
        subtitle.observe(owner, this.subtitle::set);
        return this;
    }

    public SettingsItemTwoRowViewModel setEvent(SingleLiveEvent<Void> event) {
        this.event = event;
        return this;
    }

    public void onClick() {
        if (event != null) {
            event.call();
        } else {
            if (hiddenFunctionality != null && hiddenFunctionality.getValue() != null && !hiddenFunctionality.getValue() && manageAction()) {
                hiddenFunctionality.setValue(true);
            }
        }
    }

    public SettingsItemTwoRowViewModel setDelayedAction(SingleLiveEvent<Integer> delayedAction) {
        this.delayedAction = delayedAction;
        return this;
    }

    private boolean manageAction() {
        if (System.currentTimeMillis() - lastHitTime <= 2000) {
            lastHitTime = System.currentTimeMillis();
            counter++;
            if (counter > 3) {
                delayedAction.setValue(10 - counter);
            }
            return counter >= 10;
        }
        counter = 0;
        lastHitTime = System.currentTimeMillis();
        return false;
    }
}
