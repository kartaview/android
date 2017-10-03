package com.telenav.osv.ui.binding;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;
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
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final Map<Class<? extends ViewModel>, Provider<ViewModel>> creators;

    @Inject
    ViewModelFactory(Map<Class<? extends ViewModel>, Provider<ViewModel>> creators) {
        this.creators = creators;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        Provider<? extends ViewModel> creator = creators.get(modelClass);
        if (creator == null) {
            for (Map.Entry<Class<? extends ViewModel>, Provider<ViewModel>> entry : creators.entrySet()) {
                if (modelClass.isAssignableFrom(entry.getKey())) {
                    creator = entry.getValue();
                    break;
                }
            }
        }
        if (creator == null) {
            throw new IllegalArgumentException("unknown model class " + modelClass);
        }
        try {
            return (T) creator.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
