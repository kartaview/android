package com.telenav.osv.ui.binding.viewmodel.profile.newimpl;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;

/**
 * todo only used in new profile fragment impl.
 * Created by kalmanb on 9/29/17.
 */
public class TrackListItemViewModel extends ListItemViewModel {

    public TrackListItemViewModel(Application application, LifecycleOwner owner) {
        super(application);
        this.owner = owner;
    }
}
