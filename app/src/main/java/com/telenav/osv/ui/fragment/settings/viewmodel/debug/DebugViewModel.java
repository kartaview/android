package com.telenav.osv.ui.fragment.settings.viewmodel.debug;

import java.util.ArrayList;
import java.util.List;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Pair;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.Injection;
import com.telenav.osv.network.FactoryServerEndpointUrl;
import com.telenav.osv.ui.fragment.settings.SettingsFragment;
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup;
import com.telenav.osv.ui.fragment.settings.model.SettingsItem;
import com.telenav.osv.ui.fragment.settings.presenter.group.CategoryPresenter;
import com.telenav.osv.ui.fragment.settings.presenter.item.PreferencePresenter;
import com.telenav.osv.ui.fragment.settings.presenter.item.SwitchPresenter;
import com.telenav.osv.ui.fragment.settings.viewmodel.SettingsBaseViewModel;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

/**
 * The {@code ViewModel} responsible to handle the functionality for the {@code Debug} menu.
 */
public class DebugViewModel extends SettingsBaseViewModel {

    /**
     * The key for the current server option.
     * This key is required in order to update the summary for this preference.
     */
    private static final String KEY_CURRENT_SERVER = "KeyCurrentServer";

    /**
     * The tag for the {@code Server} submenu.
     */
    private static final String SUBMENU_TAG_SERVER = "ServerSubmenu";

    /**
     * Instance for the factory url used to get and set the environment.
     */
    private FactoryServerEndpointUrl factoryServerEndpointUrl;

    public DebugViewModel(@NonNull Application application, @NonNull ApplicationPreferences appPrefs) {
        super(application, appPrefs);
        //ToDo: remove endpoint factory injection directly in constructor and add it as a parameter.
        factoryServerEndpointUrl = Injection.provideNetworkFactoryUrl(appPrefs);
    }

    @Override
    public int getTitle() {
        return R.string.settings_debug;
    }

    @Override
    public void start() {
        List<SettingsGroup> settingsCategories = new ArrayList<>();
        settingsCategories.add(createAuthCategory());
        settingsCategories.add(createServerCategory());
        settingsCategories.add(createRecordingCategory());
        settingsDataObservable.setValue(settingsCategories);
    }

    @Override
    public void onResume() {
        super.onResume();
        summaryPreferenceObservable.setValue(new Pair<>(KEY_CURRENT_SERVER, getCurrentServer()));
    }

    /**
     * @return a group model for the server category.
     */
    private SettingsGroup createServerCategory() {
        List<SettingsItem> items = new ArrayList<>();
        Resources resources = getApplication().getResources();
        items.add(new SettingsItem(resources.getString(R.string.settings_debug_current_server),
                getCurrentServer(),
                KEY_CURRENT_SERVER,
                R.drawable.ic_settings_debug_current_server,
                new PreferencePresenter(preference -> {
                    openScreenObservable.setValue(new Pair<>(SUBMENU_TAG_SERVER, SettingsFragment.newInstance(DebugServerViewModel.class)));
                    return true;
                })));
        return new SettingsGroup(resources.getString(R.string.settings_debug_server), items, new CategoryPresenter());
    }

    /**
     * @return a group model for the authentication category.
     */
    private SettingsGroup createAuthCategory() {
        List<SettingsItem> items = new ArrayList<>();
        Resources resources = getApplication().getResources();
        items.add(new SettingsItem(resources.getString(R.string.settings_debug_auth_remember_login_title),
                resources.getString(R.string.settings_debug_auth_remember_login_summary),
                PreferenceTypes.K_DEBUG_SAVE_AUTH,
                R.drawable.ic_settings_debug_remember_credentials,
                new SwitchPresenter(preference -> {
                    if (!((SwitchPreferenceCompat) preference).isChecked()) {
                        //TODO: Check this when the Authentication process is refactored.
                        //clear auth cache
                        Context context = getApplication().getApplicationContext();
                        CookieSyncManager.createInstance(context);
                        CookieManager.getInstance().removeAllCookie();
                        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                        editor.clear();
                        editor.apply();
                    }
                    return false;
                })));
        return new SettingsGroup(resources.getString(R.string.settings_debug_auth), items, new CategoryPresenter());
    }

    /**
     * @return a group model for the recording category.
     */
    private SettingsGroup createRecordingCategory() {
        List<SettingsItem> items = new ArrayList<>();
        Resources resources = getApplication().getResources();
        items.add(new SettingsItem(resources.getString(R.string.recording_tagging_settings_title),
                resources.getString(R.string.recording_tagging_settings_message),
                PreferenceTypes.K_DEBUG_RECORDING_TAGGING,
                R.drawable.ic_settings_map,
                new SwitchPresenter()));
        items.add(new SettingsItem(resources.getString(R.string.settings_debug_automatic_image_capturing_title),
                resources.getString(R.string.settings_debug_automatic_image_capturing_summary),
                PreferenceTypes.K_DEBUG_BENCHMARK_SHUTTER_LOGIC,
                0,
                new SwitchPresenter()));
        return new SettingsGroup(resources.getString(R.string.settings_category_recording), items, new CategoryPresenter());
    }

    /**
     * @return a {@code String} representing the name of the current stored server.
     */
    private String getCurrentServer() {
        return factoryServerEndpointUrl.getServerEndpointWithoutProtocol();
    }
}