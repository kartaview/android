package com.telenav.osv.ui.binding.viewmodel.profile.newimpl;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import com.telenav.osv.application.ValueFormatter;

/**
 * todo only used in new profile fragment impl.
 * Responsible for creating the view models while with a lifecycle owner to be used with LiveData
 *
 * This class should be used together with ViewModelProvider.get(LifecycleOwner, Factory) method to instantiate the view models.
 * This way it will respect the scope of the creation and enable the lifecycle library to clean up resources such as observers.
 *
 * Custom scopes can be implemented by implementing the LifecycleRegistryOwner interface,
 * which can be injected later on through this factory class.
 * Created by kalmanb on 9/7/17.
 */
public class ListItemViewModelFactory extends ViewModelProvider.NewInstanceFactory {

  private final Application application;

  private final LifecycleOwner owner;

  private final ValueFormatter valueFormatter;

  ListItemViewModelFactory(Application application, LifecycleOwner owner, ValueFormatter valueFormatter) {
    this.application = application;
    this.owner = owner;
    this.valueFormatter = valueFormatter;
  }

  @Override
  public <T extends ViewModel> T create(Class<T> modelClass) {
    if (modelClass.isAssignableFrom(TrackListItemViewModel.class)) {
      //noinspection unchecked
      return (T) new TrackListItemViewModel(application, owner);
    } else if (modelClass.isAssignableFrom(ByodTrackListHeaderViewModel.class)) {
      //noinspection unchecked
      return (T) new ByodTrackListHeaderViewModel(application, owner, valueFormatter);
    }
    throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
  }
}
