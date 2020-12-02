package com.telenav.osv.ui.fragment.settings;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.common.model.base.KVBaseActivity;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.ObdContract;
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup;
import com.telenav.osv.ui.fragment.settings.model.SettingsItem;
import com.telenav.osv.ui.fragment.settings.viewmodel.SettingsBaseViewModel;
import com.telenav.osv.ui.fragment.settings.viewmodel.SettingsViewModel;
import com.telenav.osv.ui.fragment.settings.viewmodel.SettingsViewModelFactory;
import com.telenav.osv.utils.ActivityUtils;

import java.io.Serializable;

/**
 * Fragment responsible for displaying the Settings elements.
 * The fragment should be instantiated using the method {@link SettingsFragment#newInstance(Class)}.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements ObdContract.PermissionsListener {

    public static final String TAG = SettingsFragment.class.getSimpleName();

    /**
     * The key which is used for sending further the {@code ViewModel} class which should be used by the fragment.
     */
    private static final String KEY_VIEW_MODEL = "viewModel";

    /**
     * The {@code ViewModel} class which handles the view logic.
     */
    private SettingsBaseViewModel viewModel;

    /**
     * Method used to create a new instance of the {@link SettingsFragment}.
     * @param viewModelClass {@code ViewModel} class which is send further to the fragment and should extend {@link SettingsBaseViewModel}.
     * @return a new instance of the {@code SettingsFragment}.
     */
    public static SettingsFragment newInstance(@NonNull Class<? extends SettingsBaseViewModel> viewModelClass) {
        SettingsFragment settingsFragment = new SettingsFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_VIEW_MODEL, viewModelClass);
        settingsFragment.setArguments(bundle);
        return settingsFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        initViewModel();
        observeOnOpenScreen();
        observeOnActivityOpenScreen();
        observeOnSummaryChanges();
        if (viewModel instanceof SettingsViewModel) {
            observeOnCameraPermissionEvent();
        }
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(ApplicationPreferences.PREFS_NAME);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpToolbar();
        setDivider(new ColorDrawable(Color.TRANSPARENT));
        setDividerHeight(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.onResume();
        if (getView() != null) {
            getView().setBackgroundColor(getResources().getColor(R.color.default_white));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.onPause();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getPreferenceManager().getContext();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        viewModel.getSettingsDataObservable().observe(this, settings -> {
            if (settings == null) {
                return;
            }
            screen.removeAll();
            for (SettingsGroup category : settings) {
                displayCategory(category, screen, context);
            }
            setPreferenceScreen(screen);
        });
        viewModel.start();
    }

    @Override
    public void onPermissionGranted(int permissionCode) {
        viewModel.start();
    }

    private void setUpToolbar() {
        KVBaseActivity activity = (KVBaseActivity) getActivity();
        if (activity == null) {
            return;
        }
        activity.setStatusBarColor(getResources().getColor(R.color.default_purple));
        activity.getToolbar().updateToolbar(new ToolbarSettings.Builder()
                .setTitle(viewModel.getTitle())
                .setTextColor(getResources().getColor(R.color.default_white))
                .setNavigationIcon(R.drawable.vector_back_white, (v) -> activity.onBackPressed())
                .setBackgroundColor(getResources().getColor(R.color.default_purple))
                .build());
    }

    /**
     * Initializes the {@code ViewModel} using the fragment argument.
     */
    @SuppressWarnings("unchecked")
    private void initViewModel() {
        if (getArguments() != null && getArguments().containsKey(KEY_VIEW_MODEL)) {
            Serializable serializable = getArguments().getSerializable(KEY_VIEW_MODEL);
            if (serializable != null && serializable.getClass().isInstance(SettingsBaseViewModel.class)) {
                Class<? extends SettingsBaseViewModel> viewModelClass = (Class<? extends SettingsBaseViewModel>) serializable;
                ApplicationPreferences appPrefs = ((KVApplication) getActivity().getApplication()).getAppPrefs();
                viewModel = ViewModelProviders.of(this, new SettingsViewModelFactory(getActivity().getApplication(), appPrefs)).get(viewModelClass);
            }
        }
    }

    /**
     * Display the {@link PreferenceCategory} and {@link Preference} views on the given screen.
     * @param category the category containing the elements which should be displayed.
     * @param screen the screen on which the category should be added.\
     * @param context the context defined by {@code PreferenceManager}.
     */
    private void displayCategory(SettingsGroup category, PreferenceScreen screen, Context context) {
        PreferenceGroup preferenceCategory = category.getPreference(context);
        screen.addPreference(preferenceCategory);
        for (SettingsItem item : category.getItems()) {
            Preference preference = item.getPreference(context);
            preferenceCategory.addPreference(preference);
        }
    }

    /**
     * Observers for events in order to open a new screen.
     */
    private void observeOnOpenScreen() {
        viewModel.getOpenScreenObservable().observe(this, fragmentPair -> {
            if (fragmentPair != null && getFragmentManager() != null) {
                if (fragmentPair.second instanceof DialogFragment) {
                    ((DialogFragment) fragmentPair.second).show(getFragmentManager(), fragmentPair.first);
                } else {
                    ActivityUtils.replaceFragment(getFragmentManager(), fragmentPair.second, R.id.layout_activity_obd_fragment_container, true, fragmentPair.first);
                }
            }
        });
    }

    /**
     * Observers for events in order to open a new activity screen.
     */
    private void observeOnActivityOpenScreen() {
        viewModel.getOpenActivityScreenObservable().observe(this, intent -> {
            if (intent != null) {
                startActivity(intent);
            }
        });
    }

    /**
     * Observers for events in order to change the summary for a preference.
     */
    private void observeOnSummaryChanges() {
        viewModel.getSummaryPreferenceObservable().observe(this, pair -> {
            if (pair != null && getPreferenceScreen() != null) {
                Preference preference = getPreferenceScreen().findPreference(pair.first);
                if (preference != null) {
                    preference.setSummary(pair.second);
                }
            }
        });
    }

    /**
     * Registers an observer to listen for the required camera permission in order to retrieve the camera resolution.
     * When the event is received requests the camera permissions.
     */
    private void observeOnCameraPermissionEvent() {
        ((SettingsViewModel) viewModel).getCameraPermissionsObservable().observe(this, permissions -> {
            if (getActivity() != null && permissions != null) {
                ActivityCompat.requestPermissions(getActivity(), permissions, KVApplication.CAMERA_PERMISSION);
            }
        });
    }
}