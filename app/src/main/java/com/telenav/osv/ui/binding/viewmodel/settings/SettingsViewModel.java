package com.telenav.osv.ui.binding.viewmodel.settings;

import javax.inject.Inject;
import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.content.Context;
import com.telenav.osv.BR;
import com.telenav.osv.R;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.ui.binding.viewmodel.AbstractViewModel;
import com.telenav.osv.ui.binding.viewmodel.SingleLiveEvent;
import com.telenav.osv.ui.custom.ViewHolder;
import com.telenav.osv.ui.list.BindingRecyclerAdapter;
import com.telenav.osv.ui.list.ObservableMergeList;
import com.telenav.osv.ui.list.ObservableOrderedSet;
import com.telenav.osv.utils.Log;
import me.tatarka.bindingcollectionadapter2.BindingRecyclerViewAdapter;
import me.tatarka.bindingcollectionadapter2.itembindings.OnItemBindClass;
import static com.telenav.osv.data.Preferences.URL_ENV;
import static com.telenav.osv.manager.obd.ObdManager.STATE_CONNECTED;
import static com.telenav.osv.manager.obd.ObdManager.STATE_CONNECTING;
import static com.telenav.osv.manager.obd.ObdManager.STATE_DISCONNECTED;
import static com.telenav.osv.manager.obd.ObdManager.TYPE_BLE;
import static com.telenav.osv.manager.obd.ObdManager.TYPE_BT;
import static com.telenav.osv.manager.obd.ObdManager.TYPE_WIFI;

/**
 * viewmodel for settings fragment
 * Created by kalmanb on 9/7/17.
 */
public class SettingsViewModel extends AbstractViewModel {

    private static final String TAG = "SettingsViewModel";

    /**
     * Custom adapter that logs calls.
     */
    public final BindingRecyclerAdapter<SettingsItemViewModel> adapter = new BindingRecyclerAdapter<>();

    public final SingleLiveEvent<Void> logEvent = new SingleLiveEvent<>();

    public final SingleLiveEvent<Void> reportEvent = new SingleLiveEvent<>();

    public final SingleLiveEvent<Void> feedbackEvent = new SingleLiveEvent<>();

    public final SingleLiveEvent<Void> policyEvent = new SingleLiveEvent<>();

    public final SingleLiveEvent<Void> termsEvent = new SingleLiveEvent<>();

    public final SingleLiveEvent<Void> obdConnectEvent = new SingleLiveEvent<>();

    public final SingleLiveEvent<Void> connectionSettingEvent = new SingleLiveEvent<>();

    public final SingleLiveEvent<Void> resolutionSettingEvent = new SingleLiveEvent<>();

    public final SingleLiveEvent<Void> tipsEvent = new SingleLiveEvent<>();

    public final SingleLiveEvent<Void> walkthroughEvent = new SingleLiveEvent<>();

    public final SingleLiveEvent<Void> serverSettingEvent = new SingleLiveEvent<>();

    /**
     * Binds multiple items types to different layouts based on class. This could have also be
     * written manually as
     * <pre>{@code
     * public final OnItemBind<Object> multipleItems = new OnItemBind<Object>() {
     *     @Override
     *     public void onItemBind(ItemBinding itemBinding, int position, Object item) {
     *         if (String.class.equals(item.getClass())) {
     *             itemBinding.set(BR.item, R.layout.item_header_footer);
     *         } else if (ItemViewModel.class.equals(item.getClass())) {
     *             itemBinding.set(BR.item, R.layout.item);
     *         }
     *     }
     * };
     * }</pre>
     */
    public final OnItemBindClass<SettingsItemViewModel> multipleItems = new OnItemBindClass<SettingsItemViewModel>()
            .map(SettingsItemDividerViewModel.class, BR.item, R.layout.item_settings_divider)
            .map(SettingsItemCategoryViewModel.class, BR.item, R.layout.item_settings_category)
            .map(SettingsItemDialogViewModel.class, BR.item, R.layout.item_settings_dialog)
            .map(SettingsItemOneRowViewModel.class, BR.item, R.layout.item_settings_one_row)
            .map(SettingsItemTwoRowViewModel.class, BR.item, R.layout.item_settings_two_row)
            .map(SettingsItemSwitchViewModel.class, BR.item, R.layout.item_settings_switch);

    /**
     * Custom view holders for RecyclerView
     */
    public final BindingRecyclerViewAdapter.ViewHolderFactory viewHolder = binding -> new ViewHolder(binding.getRoot());

    public final SingleLiveEvent<Integer> snackbarMessage = new SingleLiveEvent<>();

    private final MutableLiveData<Boolean> debugEvent = new MutableLiveData<>();

    private final ObservableOrderedSet<SettingsItemViewModel> account = new ObservableOrderedSet<>();

    private final ObservableOrderedSet<SettingsItemViewModel> uploadCharging = new ObservableOrderedSet<>();

    private final ObservableOrderedSet<SettingsItemViewModel> recordingMap = new ObservableOrderedSet<>();

    private final ObservableOrderedSet<SettingsItemViewModel> generalFirst = new ObservableOrderedSet<>();

    private final ObservableOrderedSet<SettingsItemViewModel> generalSecond = new ObservableOrderedSet<>();

    private final ObservableOrderedSet<SettingsItemViewModel> generalThird = new ObservableOrderedSet<>();

    private final ObservableOrderedSet<SettingsItemViewModel> gamification = new ObservableOrderedSet<>();

    private final ObservableOrderedSet<SettingsItemViewModel> camera = new ObservableOrderedSet<>();

    private final ObservableOrderedSet<SettingsItemViewModel> obd = new ObservableOrderedSet<>();

    private final ObservableOrderedSet<SettingsItemViewModel> improve = new ObservableOrderedSet<>();

    private final ObservableOrderedSet<SettingsItemViewModel> about = new ObservableOrderedSet<>();

    /**
     * Items merged with a header on top and footer on bottom.
     */
    public final ObservableMergeList<SettingsItemViewModel> headerFooterItems = new ObservableMergeList<SettingsItemViewModel>()
            .insertList(account)
            .insertList(generalFirst)
            .insertList(generalSecond)
            .insertList(generalThird)
            .insertList(camera)
            .insertList(obd)
            .insertList(improve)
            .insertList(about);

    private final ObservableOrderedSet<SettingsItemViewModel> debug = new ObservableOrderedSet<>();

    private final ObservableOrderedSet<SettingsItemViewModel> debugHidden = new ObservableOrderedSet<>();

    private final Preferences prefs;

    private MutableLiveData<String> obdType = new MutableLiveData<>();

    private MutableLiveData<String> obdStatus = new MutableLiveData<>();

    private MutableLiveData<String> obdAction = new MutableLiveData<>();

    private MutableLiveData<String> loginTitle = new MutableLiveData<>();

    @Inject
    public SettingsViewModel(Application application, Preferences prefs) {
        super(application);
        this.prefs = prefs;
        Log.d(TAG, "SettingsViewModel: ");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared: =================================================================================");
    }

    @Override
    public void setOwner(LifecycleOwner lifecycleOwner) {
        this.owner = lifecycleOwner;
        Context context = getApplication().getApplicationContext();
        //use factory directly to not reuse view models from vm store - scope must be different than fragments
        SettingViewModelFactory factory = new SettingViewModelFactory(getApplication(), owner);

        //initialize the settings screen preference items displayed in the list
        initializeItems(context, factory, prefs);

        //set up the dynamic show/hide functionality as well as dinamic titles/labels used in some items
        //based on the login state, display login/logout action label
        prefs.observeLogin().observe(owner, logged -> loginTitle
                .setValue(logged != null && logged ? context.getString(R.string.log_out) : context.getString(R.string.log_in)));

        prefs.getObdTypeLive().observe(owner, i -> {
            if (i != null) {
                obdType.setValue(context.getString(getObdTypeText(i)));
            }
        });
        prefs.getObdStatusLive().observe(owner, i -> {
            if (i != null) {
                obdStatus.setValue(context.getString(getObdStatusText(i)));
                obdAction.setValue(context.getString(getObdActionText(i)));
            }
        });
        //to add the buttons without seeing the animation
        //(observer gets notified after views get created so the recycler view will animate the insert into the list)
        if (prefs.isAutoUploadEnabled()) {
            generalFirst.insertCollection(uploadCharging);
        }
        prefs.getAutoUploadLive().observe(owner, value -> {
            if (value != null && value) {
                Log.d(TAG, "SettingsViewModel: add uploadCharging");
                generalFirst.insertCollection(uploadCharging);
            } else {
                Log.d(TAG, "SettingsViewModel: remove uploadCharging");
                generalFirst.removeCollection(uploadCharging);
            }
        });
        //to add the buttons without seeing the animation
        //(observer gets notified after views get created so the recycler view will animate the insert into the list)
        if (!prefs.isDriver()) {
            generalThird.insertCollection(gamification);
        }
        prefs.isDriverLive().observe(owner, value -> {
            if (value == null || !value) {
                generalThird.insertCollection(gamification);
            } else {
                generalThird.removeCollection(gamification);
            }
        });
        //to add the buttons without seeing the animation
        //(observer gets notified after views get created so the recycler view will animate the insert into the list)
        if (prefs.isMapEnabled()) {
            generalSecond.insertCollection(recordingMap);
        }
        prefs.getMapEnabledLive().observe(owner, value -> {
            if (value != null && value) {
                generalSecond.insertCollection(recordingMap);
            } else {
                generalSecond.removeCollection(recordingMap);
            }
        });
        debugEvent.setValue(prefs.isDebugEnabled());
        debugEvent.observe(owner, shown -> {
            Log.d(TAG, "SettingsViewModel: debug shown " + shown);
            if (shown != null && shown) {
                headerFooterItems.insertList(debug);
            } else {
                headerFooterItems.removeList(debug);
            }
        });
        prefs.getDebugEnabledLive().observe(owner, value -> {
            if (value != null && value) {
                headerFooterItems.insertList(debugHidden);
            } else {
                headerFooterItems.removeList(debugHidden);
            }
        });
    }

    private void initializeItems(Context context, SettingViewModelFactory factory, Preferences prefs) {
        Log.d(TAG, "initializeItems: ");
        account
                .insertItem(factory.create(SettingsItemCategoryViewModel.class)
                        .setTitle(context.getString(R.string.account_label)))
                .insertItem(factory.create(SettingsItemOneRowViewModel.class)
                        .setEvent(logEvent)
                        .setTitle(loginTitle));
        generalFirst
                .insertItem(factory.create(SettingsItemCategoryViewModel.class)
                        .setTitle(context.getString(R.string.general_settings_label)))
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getDataUploadLive())
                        .setTitle(context.getString(R.string.upload_on_data_label))
                        .setSubtitle(context.getString(R.string.data_setting_subtitle)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getAutoUploadLive())
                        .setTitle(context.getString(R.string.upload_automatically_label))
                        .setSubtitle(context.getString(R.string.auto_setting_subtitle)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class));
        uploadCharging.insertItem(factory.create(SettingsItemSwitchViewModel.class)
                .setPreference(prefs.getChargingUploadLive())
                .setTitle(context.getString(R.string.upload_charging_label))
                .setSubtitle(context.getString(R.string.charging_setting_subtitle)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class));

        generalSecond
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getExtStorageLive())
                        .setTitle(context.getString(R.string.storage_label))
                        .setSubtitle(context.getString(R.string.storage_subtitle)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getUsingMetricUnitsLive())
                        .setTitle(context.getString(R.string.metric_label))
                        .setSubtitle(context.getString(R.string.metric_subtitle)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getMapEnabledLive())
                        .setTitle(context.getString(R.string.map_sdk_label))
                        .setSubtitle(context.getString(R.string.map_sdk_subtitle)));
        recordingMap
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getMiniMapEnabledLive())
                        .setTitle(context.getString(R.string.map_visibility_label))
                        .setSubtitle(context.getString(R.string.map_visibility_subtitle)));//only once it will be added
        gamification
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getGamificationEnabledLive())
                        .setTitle(context.getString(R.string.gamification_title))
                        .setSubtitle(context.getString(R.string.gamification_sub_title)));
        camera
                .insertItem(factory.create(SettingsItemCategoryViewModel.class)
                        .setTitle(context.getString(R.string.camera_label)))
                .insertItem(factory.create(SettingsItemTwoRowViewModel.class)
                        .setEvent(resolutionSettingEvent)
                        .setTitle(context.getString(R.string.resolution_label))
                        .setSubtitle(context.getString(R.string.resolution_text)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getSafeModeLive())
                        .setTitle(context.getString(R.string.safe_mode_title))
                        .setSubtitle(context.getString(R.string.safe_mode_sub_title)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getStaticFocusLive())
                        .setTitle(context.getString(R.string.focus_mode_title))
                        .setSubtitle(context.getString(R.string.focus_mode_sub_title)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getNewCameraApiLive())
                        .setTitle(context.getString(R.string.api_mode_title))
                        .setSubtitle(context.getString(R.string.api_mode_sub_title)));
        obd
                .insertItem(factory.create(SettingsItemCategoryViewModel.class)
                        .setTitle(context.getString(R.string.obd_label)))
                .insertItem(factory.create(SettingsItemDialogViewModel.class)
                        .setEvent(connectionSettingEvent)
                        .setTitle(context.getString(R.string.connection_type))
                        .setSubTitle(context.getString(R.string.obd_selector_subtitle))
                        .setValue(obdType))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemDialogViewModel.class)
                        .setEvent(obdConnectEvent)
                        .setTitle(obdStatus).setSubTitle(context.getString(R.string.obd_subtitle))
                        .setValue(obdAction));
        improve
                .insertItem(factory.create(SettingsItemCategoryViewModel.class)
                        .setTitle(context.getString(R.string.improve_text)))
                .insertItem(factory.create(SettingsItemOneRowViewModel.class)
                        .setEvent(reportEvent)
                        .setTitle(context.getString(R.string.issue_report_label)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemOneRowViewModel.class)
                        .setEvent(feedbackEvent)
                        .setTitle(context.getString(R.string.feedback_label)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemTwoRowViewModel.class)
                        .setEvent(tipsEvent)
                        .setTitle(context.getString(R.string.tips_label))
                        .setSubtitle(context.getString(R.string.tips_message)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemTwoRowViewModel.class)
                        .setEvent(walkthroughEvent)
                        .setTitle(context.getString(R.string.walkthrough_label))
                        .setSubtitle(context.getString(R.string.walkthrough_message)));
        about
                .insertItem(factory.create(SettingsItemCategoryViewModel.class)
                        .setTitle(context.getString(R.string.about_label)))
                .insertItem(factory.create(SettingsItemTwoRowViewModel.class)
                        .setTitle(context.getString(R.string.application_version_label))
                        .setSubtitle(prefs.getVersionName())
                        .setHiddenFunctionality(debugEvent)
                        .setDelayedAction(snackbarMessage))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemOneRowViewModel.class)
                        .setEvent(termsEvent)
                        .setTitle(context.getString(R.string.terms_text)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemOneRowViewModel.class)
                        .setEvent(policyEvent)
                        .setTitle(context.getString(R.string.policy_text)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemOneRowViewModel.class)
                        .setTitle(context.getString(R.string.copyright_text)));
        debug
                .insertItem(factory.create(SettingsItemCategoryViewModel.class)
                        .setTitle(context.getString(R.string.debug_label)))
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getDebugEnabledLive())
                        .setTitle(context.getString(R.string.debug_features_label))
                        .setSubtitle(context.getString(R.string.debug_switch_subtitle)));
        debugHidden
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemTwoRowViewModel.class)
                        .setEvent(serverSettingEvent).setTitle(context.getString(R.string.current_server_label))
                        .setSubtitle(Transformations.map(prefs.getServerTypeLive(), this::getServerForInt)))
                .insertItem(factory.create(SettingsItemDividerViewModel.class))
                .insertItem(factory.create(SettingsItemSwitchViewModel.class)
                        .setPreference(prefs.getSaveAuthLive())
                        .setTitle(context.getString(R.string.remember_login_credentials_label))
                        .setSubtitle(context.getString(R.string.remember_login_credentials_text)));
    }

    private String getServerForInt(Integer i) {
        return URL_ENV[i];
    }

    private int getObdStatusText(int status) {
        switch (status) {
            case STATE_CONNECTED:
                return R.string.connected;
            case STATE_CONNECTING:
                return R.string.connecting_label;
            case STATE_DISCONNECTED:
            default:
                return R.string.disconnect;
        }
    }

    private int getObdActionText(int status) {
        switch (status) {
            case STATE_CONNECTED:
                return R.string.disconnect;
            case STATE_CONNECTING:
                return R.string.connecting;
            case STATE_DISCONNECTED:
            default:
                return R.string.connect;
        }
    }

    private int getObdTypeText(int type) {
        switch (type) {
            case TYPE_WIFI:
                return R.string.wifi_label;
            case TYPE_BLE:
                return R.string.ble_label;
            case TYPE_BT:
            default:
                return R.string.bt_label;
        }
    }
}