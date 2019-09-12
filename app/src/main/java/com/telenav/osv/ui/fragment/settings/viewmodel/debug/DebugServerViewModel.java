package com.telenav.osv.ui.fragment.settings.viewmodel.debug;

import java.util.ArrayList;
import java.util.List;
import android.app.Application;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.Injection;
import com.telenav.osv.network.FactoryServerEndpointUrl;
import com.telenav.osv.ui.fragment.settings.RestartAppDialogFragment;
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup;
import com.telenav.osv.ui.fragment.settings.presenter.group.RadioGroupPresenter;
import com.telenav.osv.ui.fragment.settings.viewmodel.SettingsBaseViewModel;
import androidx.annotation.NonNull;

/**
 * {@code ViewModel} class responsible to handle the functionality for the {@code Server} submenu.
 */
public class DebugServerViewModel extends SettingsBaseViewModel {

    /**
     * Instance for the factory url used to get and set the environment.
     */
    private FactoryServerEndpointUrl factoryServerEndpointUrl;

    /**
     * Default constructor for the current class.
     */
    public DebugServerViewModel(@NonNull Application application, @NonNull ApplicationPreferences appPrefs) {
        super(application, appPrefs);
        //ToDo: remove endpoint factory injection directly in constructor and add it as a parameter.
        factoryServerEndpointUrl = Injection.provideNetworkFactoryUrl(appPrefs);
    }

    @Override
    public int getTitle() {
        return R.string.settings_debug_current_server;
    }

    @Override
    public void start() {
        List<SettingsGroup> settingsCategories = new ArrayList<>();
        settingsCategories.add(createServerURLCategory());
        settingsDataObservable.setValue(settingsCategories);
    }

    /**
     * @return a group model for the server options.
     */
    private SettingsGroup createServerURLCategory() {
        return new SettingsGroup(new RadioGroupPresenter(getServerOptions(), getServerGroupListener()));
    }

    /**
     * @return a list of {@code RadioButtons} representing the server options.
     */
    private List<RadioButton> getServerOptions() {
        List<RadioButton> radioButtons = new ArrayList<>();
        String[] serverEndpoints = factoryServerEndpointUrl.getServerEndpoints();
        for (int i = 0; i < serverEndpoints.length; i++) {
            RadioButton radioButton = (RadioButton) LayoutInflater.from(getApplication().getApplicationContext()).inflate(R.layout.settings_item_radio, null);
            radioButton.setText(serverEndpoints[i]);
            radioButton.setTag(i);
            radioButton.setId(View.generateViewId());
            if (i == getCurrentServerIndex()) {
                radioButton.setChecked(true);
            }
            radioButtons.add(radioButton);
        }
        return radioButtons;
    }

    /**
     * @return the index of the stored server in the {@link #appPrefs}.
     */
    private int getCurrentServerIndex() {
        ApplicationPreferences appPrefs = ((OSVApplication) getApplication()).getAppPrefs();
        return appPrefs.getIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE);
    }

    /**
     * @return a {@code listener} which is called when a server option is selected.
     */
    private RadioGroup.OnCheckedChangeListener getServerGroupListener() {
        return (radioGroup, i) -> {
            RadioButton radioButton = radioGroup.findViewById(i);
            if (radioButton != null) {
                int index = (int) radioButton.getTag();
                int previousServer = appPrefs.getIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE);
                if (index == previousServer) {
                    return;
                }
                appPrefs.saveIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE, index);
                //invalidate the current url for the server.
                factoryServerEndpointUrl.invalidate();
                RestartAppDialogFragment dialogFragment = new RestartAppDialogFragment();
                dialogFragment.setCancelable(false);
                dialogFragment.setOnDismissListener(() -> {
                    appPrefs.saveIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE, previousServer);
                    ((RadioButton) radioGroup.findViewWithTag(previousServer)).setChecked(true);
                });
                openScreenObservable.setValue(new Pair<>(RestartAppDialogFragment.TAG, dialogFragment));
            }
        };
    }
}