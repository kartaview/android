package com.telenav.osv.ui.binding.viewmodel.settings;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

/**
 * Responsible for creating the view models while with a lifecycle owner to be used with LiveData
 * <p>
 * This class should be used together with ViewModelProvider.get(LifecycleOwner, Factory) method to instantiate the view models.
 * This way it will respect the scope of the creation and enable the lifecycle library to clean up resources such as observers.
 * <p>
 * Custom scopes can be implemented by implementing the LifecycleRegistryOwner interface,
 * which can be injected later on through this factory class.
 * Created by kalmanb on 9/7/17.
 */
public class SettingViewModelFactory extends ViewModelProvider.NewInstanceFactory {

    private final Application application;

    private final LifecycleOwner owner;

    SettingViewModelFactory(Application application, LifecycleOwner owner) {
        this.application = application;
        this.owner = owner;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SettingsItemSwitchViewModel.class)) {
            //noinspection unchecked
            return (T) new SettingsItemSwitchViewModel(application, owner);
        } else if (modelClass.isAssignableFrom(SettingsItemCategoryViewModel.class)) {
            //noinspection unchecked
            return (T) new SettingsItemCategoryViewModel(application, owner);
        } else if (modelClass.isAssignableFrom(SettingsItemDialogViewModel.class)) {
            //noinspection unchecked
            return (T) new SettingsItemDialogViewModel(application, owner);
        } else if (modelClass.isAssignableFrom(SettingsItemDividerViewModel.class)) {
            //noinspection unchecked
            return (T) new SettingsItemDividerViewModel(application, owner);
        } else if (modelClass.isAssignableFrom(SettingsItemOneRowViewModel.class)) {
            //noinspection unchecked
            return (T) new SettingsItemOneRowViewModel(application, owner);
        } else if (modelClass.isAssignableFrom(SettingsItemTwoRowViewModel.class)) {
            //noinspection unchecked
            return (T) new SettingsItemTwoRowViewModel(application, owner);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
