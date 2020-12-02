package com.telenav.osv.ui.fragment.settings.viewmodel;

import java.util.List;
import android.app.Application;
import android.content.Intent;
import android.util.Pair;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Base class for Settings {@code ViewModel} which holds all the common functionality for all the screens.
 */
public abstract class SettingsBaseViewModel extends AndroidViewModel {

    /**
     * Observable which notifies the observer when a new settings list is available.
     */
    protected MutableLiveData<List<SettingsGroup>> settingsDataObservable;

    /**
     * Observable which notifies the observer when a new screen should be open.
     */
    protected MutableLiveData<Pair<String, Fragment>> openScreenObservable;

    /**
     * Observable which notifies the observer when a new activity screen should be open.
     */
    protected MutableLiveData<Intent> openActivityScreenObservable;

    /**
     * Observable which notifies the observer when a preference summary was changed.
     */
    protected MutableLiveData<Pair<String, String>> summaryPreferenceObservable;

    /**
     * Instance for user's application preferences.
     */
    protected ApplicationPreferences appPrefs;

    protected SettingsBaseViewModel(@NonNull Application application, @NonNull ApplicationPreferences appPrefs) {
        super(application);
        this.appPrefs = appPrefs;
    }

    /**
     * @return a {@code String} representing the screen title which should be displayed in the toolbar.
     */
    @StringRes
    public abstract int getTitle();

    /**
     * Method called when an observer is register to receive settings data.
     */
    public abstract void start();

    /**
     * @return an {@code LiveData} observable which emits the settings data.
     */
    public LiveData<List<SettingsGroup>> getSettingsDataObservable() {
        if (settingsDataObservable == null) {
            settingsDataObservable = new MutableLiveData<>();
        }
        return settingsDataObservable;
    }

    /**
     * @return an {@code LiveData} observable which emits the screen and it's tag that should be displayed next.
     */
    public LiveData<Pair<String, Fragment>> getOpenScreenObservable() {
        if (openScreenObservable == null) {
            openScreenObservable = new MutableLiveData<>();
        }
        return openScreenObservable;
    }

    /**
     * @return an {@code LiveData} observable which emits the intent for the next screen.
     */
    public LiveData<Intent> getOpenActivityScreenObservable() {
        if (openActivityScreenObservable == null) {
            openActivityScreenObservable = new MutableLiveData<>();
        }
        return openActivityScreenObservable;
    }

    /**
     * @return an {@link LiveData} observable which emits a pair of preference key and new preference summary.
     */
    public LiveData<Pair<String, String>> getSummaryPreferenceObservable() {
        if (summaryPreferenceObservable == null) {
            summaryPreferenceObservable = new MutableLiveData<>();
        }
        return summaryPreferenceObservable;
    }

    /**
     * Default implementation for {@code onResume} lifecycle method in {@code viewModel}.
     */
    public void onResume() {

    }

    /**
     * Default implementation for {@code onPause} lifecycle method in {@code viewModel}.
     */
    public void onPause() {
        if (openScreenObservable != null) {
            openScreenObservable.setValue(null);
        }
        if (openActivityScreenObservable != null) {
            openActivityScreenObservable.setValue(null);
        }
        if (settingsDataObservable != null) {
            settingsDataObservable.setValue(null);
        }
    }
}