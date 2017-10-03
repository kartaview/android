package com.telenav.osv.di.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import com.telenav.osv.ui.binding.ViewModelFactory;
import com.telenav.osv.ui.binding.viewmodel.profile.newimpl.ProfileByodViewModel;
import com.telenav.osv.ui.binding.viewmodel.settings.SettingsViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel.class)
    abstract ViewModel bindSettingsViewModel(SettingsViewModel vm);

    @Binds
    @IntoMap
    @ViewModelKey(ProfileByodViewModel.class)//todo only used in new profile fragment impl.
    abstract ViewModel bindProfileByodViewModel(ProfileByodViewModel vm);

    @Binds
    abstract ViewModelProvider.Factory bindViewModelFactory(ViewModelFactory factory);
}
