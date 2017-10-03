package com.telenav.osv.ui.binding.viewmodel.settings;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import com.telenav.osv.ui.binding.viewmodel.RepeatableViewModel;

/**
 * abstract view model for setting item
 * Created by kalmanb on 9/7/17.
 */
public class SettingsItemViewModel extends RepeatableViewModel {

  public SettingsItemViewModel(Application application) {
    super(application);
  }

  @Override
  public void setOwner(LifecycleOwner lifecycleOwner) {
    this.owner = lifecycleOwner;
  }
}
