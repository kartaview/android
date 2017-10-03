package com.telenav.osv.ui.binding.viewmodel.settings;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;

/**
 * view model for setting item divider
 * Created by kalmanb on 9/7/17.
 */
public class SettingsItemDividerViewModel extends SettingsItemViewModel {

  public SettingsItemDividerViewModel(Application application, LifecycleOwner owner) {
    super(application);
    this.owner = owner;
  }
}
