package com.telenav.osv.ui.binding.viewmodel.profile.newimpl;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import com.telenav.osv.ui.binding.viewmodel.RepeatableViewModel;

/**
 * todo only used in new profile fragment impl.
 * abstract view model for setting item
 * Created by kalmanb on 9/7/17.
 */
public class ListItemViewModel extends RepeatableViewModel {

  public ListItemViewModel(Application application) {
    super(application);
  }

  @Override
  public void setOwner(LifecycleOwner lifecycleOwner) {
    this.owner = lifecycleOwner;
  }
}
