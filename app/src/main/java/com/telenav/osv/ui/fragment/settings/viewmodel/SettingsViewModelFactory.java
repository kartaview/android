package com.telenav.osv.ui.fragment.settings.viewmodel;

import java.lang.reflect.InvocationTargetException;
import android.app.Application;
import com.telenav.osv.application.ApplicationPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * Custom factory class for Settings {@code ViewModels}.
 * This is responsible to create the {@code ViewModels} with custom constructor parameters.
 */
public class SettingsViewModelFactory extends ViewModelProvider.AndroidViewModelFactory {

    /**
     * Instance to the user's application preferences.
     */
    private ApplicationPreferences applicationPreferences;

    /**
     * Instance of the application.
     */
    private Application application;

    public SettingsViewModelFactory(@NonNull Application application, @NonNull ApplicationPreferences applicationPreferences) {
        super(application);
        this.application = application;
        this.applicationPreferences = applicationPreferences;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            //create view model with the custom parameters
            return modelClass.getConstructor(Application.class, ApplicationPreferences.class).newInstance(application, applicationPreferences);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return super.create(modelClass);
    }
}