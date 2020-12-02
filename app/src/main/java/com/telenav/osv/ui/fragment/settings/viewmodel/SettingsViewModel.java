package com.telenav.osv.ui.fragment.settings.viewmodel;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.SwitchPreferenceCompat;

import com.telenav.osv.R;
import com.telenav.osv.activity.WalkthroughActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.Injection;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.SdkEnabledEvent;
import com.telenav.osv.recorder.camera.Camera;
import com.telenav.osv.recorder.camera.model.CameraDeviceData;
import com.telenav.osv.ui.fragment.HintsFragment;
import com.telenav.osv.ui.fragment.IssueReportFragment;
import com.telenav.osv.ui.fragment.settings.RestartAppDialogFragment;
import com.telenav.osv.ui.fragment.settings.SettingsFragment;
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup;
import com.telenav.osv.ui.fragment.settings.model.SettingsItem;
import com.telenav.osv.ui.fragment.settings.presenter.group.CategoryPresenter;
import com.telenav.osv.ui.fragment.settings.presenter.group.FooterPresenter;
import com.telenav.osv.ui.fragment.settings.presenter.item.PreferencePresenter;
import com.telenav.osv.ui.fragment.settings.presenter.item.SwitchPresenter;
import com.telenav.osv.upload.UploadManager;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.StringUtils;
import com.telenav.osv.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings {@code ViewModel} class responsible to handle the functionality for the Settings menu.
 */
public class SettingsViewModel extends SettingsBaseViewModel {

    /**
     * The tag for the {@code Resolution} submenu screen.
     */
    public static final String SUBMENU_TAG_RESOLUTION = "Resolution";

    /**
     * The tag for the {@code Camera Angle} submenu screen.
     */
    public static final String SUBMENU_TAG_CAMERA_ANGLE = "CameraAngle";

    /**
     * The tag for the {@code Distance unit} submenu screen.
     */
    public static final String SUBMENU_TAG_DISTANCE_UNIT = "DistanceUnit";

    /**
     * The tag for the {@code Debug} submenu screen.
     */
    public static final String SUBMENU_DEBUG = "Debug";

    private static final String TAG = SettingsViewModel.class.getSimpleName();

    /**
     * The {@code URL} for privacy policy defined for this app.
     */
    private static final String URL_PRIVACY_POLICY = "https://kartaview.org/privacy-policy";

    /**
     * The {@code URL} for terms and condition definition.
     */
    private static final String URL_TERMS_AND_CONDITIONS = "https://kartaview.org/terms";

    /**
     * The key for the resolution option.
     * This key is required in order to update the summary for this preference.
     */
    private static final String KEY_RESOLUTION = "KeyResolution";

    /**
     * The key for the distance unit option.
     * This key is required in order to update the summary for this preference.
     */
    private static final String KEY_DISTANCE_UNIT = "KeyDistanceUnit";

    /**
     * The key for the distance unit option.
     * This key is required in order to update the summary for this preference.
     */
    private static final String KEY_CAMERA_ANGLE = "KeyCameraAngle";

    /**
     * The format for the current camera resolution.
     */
    private static final String FORMAT_RESOLUTION = "%s MP";

    /**
     * The format for the version name and version code.
     */
    private static final String FORMAT_VERSIONING = "%s (%s) ";

    /**
     * Observable instance for sending an event when the camera permission need to be granted in order to retrieve the camera resolution.
     * The observable emits an array of {@code String}, containing all the required permissions.
     */
    private MutableLiveData<String[]> cameraPermissionsObservable;

    /**
     * The {@code UploadManager} reference.
     */
    private UploadManager uploadManager;

    /**
     * The list of camera devices supported.
     */
    private List<CameraDeviceData> cameraDeviceDataList;

    /**
     * Default constructor for the current class.
     */
    public SettingsViewModel(@NonNull Application application, @NonNull ApplicationPreferences appPrefs) {
        super(application, appPrefs);
        //ToDo: remove upload manager injection directly in constructor and add it as a paramter.
        uploadManager = Injection.provideUploadManager();
        Camera camera = ((KVApplication) application).getCamera();
        cameraDeviceDataList = camera.getCameraDevices();
        disableDebugMode();
    }

    @Override
    public int getTitle() {
        return R.string.settings_title;
    }

    @Override
    public void start() {
        if (checkPermissionsForCamera()) {
            ((KVApplication) getApplication()).releaseCamera();
            ((KVApplication) getApplication()).reloadCameraBasedOnSettings();
        }
        List<SettingsGroup> settingsCategories = new ArrayList<>();
        settingsCategories.add(createUploadCategory());
        settingsCategories.add(createRecordingCategory());
        settingsCategories.add(createMapCategory());
        settingsCategories.add(createImproveCategory());
        settingsCategories.add(createLegalCategory());
        settingsCategories.add(createFooterCategory());
        if (settingsDataObservable != null) {
            settingsDataObservable.setValue(settingsCategories);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (summaryPreferenceObservable == null) {
            return;
        }
        summaryPreferenceObservable.setValue(new Pair<>(KEY_RESOLUTION, getFormattedResolution()));
        summaryPreferenceObservable.setValue(new Pair<>(KEY_CAMERA_ANGLE, getCameraAngle()));
        summaryPreferenceObservable.setValue(new Pair<>(KEY_DISTANCE_UNIT, isDistanceUnitMetric() ?
                getApplication().getString(R.string.settings_metric_label) :
                getApplication().getString(R.string.settings_imperial_label)));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ((KVApplication) getApplication()).releaseCamera();
    }

    /**
     * @return an observable representing {@link #cameraPermissionsObservable}.
     */
    public LiveData<String[]> getCameraPermissionsObservable() {
        if (cameraPermissionsObservable == null) {
            cameraPermissionsObservable = new MutableLiveData<>();
        }
        return cameraPermissionsObservable;
    }

    /**
     * Creates the {@code Upload} category with it's settings items.
     *
     * @return {@code SettingsGroup} representing the {@code Upload} category.
     */
    private SettingsGroup createUploadCategory() {
        List<SettingsItem> items = new ArrayList<>();
        Resources resources = getApplication().getResources();
        items.add(new SettingsItem(resources.getString(R.string.settings_upload_on_cellular_title),
                resources.getString(R.string.settings_upload_on_cellular_summary),
                PreferenceTypes.K_UPLOAD_DATA_ENABLED,
                R.drawable.ic_settings_cellular,
                new SwitchPresenter()));
        return new SettingsGroup(resources.getString(R.string.settings_category_upload), items, new CategoryPresenter());
    }

    /**
     * Creates the {@code Recording} category with it's settings items.
     *
     * @return {@code SettingsGroup} representing the {@code Recording} category.
     */
    private SettingsGroup createRecordingCategory() {
        List<SettingsItem> items = new ArrayList<>();
        Resources resources = getApplication().getResources();
        if (!cameraDeviceDataList.isEmpty()) {
            items.add(new SettingsItem(resources.getString(R.string.settings_camera_angle_title),
                    getCameraAngle(),
                    KEY_CAMERA_ANGLE,
                    R.drawable.ic_icon_camera_angle,
                    new PreferencePresenter((v) -> {
                        appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, 0);
                        appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, 0);
                        start();
                        openScreenObservable.setValue(new Pair<>(SUBMENU_TAG_CAMERA_ANGLE, SettingsFragment.newInstance(CameraAngleViewModel.class)));
                        return true;
                    })));
        }
        items.add(new SettingsItem(resources.getString(R.string.settings_resolution_title),
                getFormattedResolution(),
                KEY_RESOLUTION,
                R.drawable.ic_settings_resolution,
                new PreferencePresenter((v) -> {
                    openScreenObservable.setValue(new Pair<>(SUBMENU_TAG_RESOLUTION, SettingsFragment.newInstance(ResolutionViewModel.class)));
                    return true;
                })));
        items.add(new SettingsItem(resources.getString(R.string.settings_video_recording_mode),
                resources.getString(R.string.settings_video_recording_mode_summary),
                PreferenceTypes.K_VIDEO_MODE_ENABLED,
                R.drawable.ic_settings_video,
                new SwitchPresenter((preference) -> {
                    appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, 0);
                    appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, 0);
                    start();
                    return true;
                })));
        items.add(new SettingsItem(resources.getString(R.string.settings_mini_map_title),
                resources.getString(R.string.settings_mini_map_summary),
                PreferenceTypes.K_RECORDING_MINI_MAP_ENABLED,
                R.drawable.ic_settings_mini_map,
                new SwitchPresenter()));
        //Display removable storage option only when SD card storage is available
        //This options allows the user to store the recorded sequences on SD card storage.
        if (Utils.checkSDCard(getApplication().getApplicationContext())) {
            items.add(new SettingsItem(resources.getString(R.string.settings_removable_storage),
                    resources.getString(R.string.settings_removable_storage_summary),
                    PreferenceTypes.K_EXTERNAL_STORAGE,
                    R.drawable.ic_settings_sd,
                    new SwitchPresenter((preference, newValue) -> handleRemovableStoragePreferenceChange((boolean) newValue))));
        }
        items.add(new SettingsItem(resources.getString(R.string.settings_distance_unit_title),
                isDistanceUnitMetric() ? resources.getString(R.string.settings_metric_label) : resources.getString(R.string.settings_imperial_label),
                KEY_DISTANCE_UNIT,
                R.drawable.ic_settings_distance,
                new PreferencePresenter((v) -> {
                    openScreenObservable.setValue(new Pair<>(SUBMENU_TAG_DISTANCE_UNIT, SettingsFragment.newInstance(DistanceUnitViewModel.class)));
                    return true;
                })));
        items.add(new SettingsItem(resources.getString(R.string.settings_points_title),
                resources.getString(R.string.settings_points_summary),
                PreferenceTypes.K_GAMIFICATION,
                R.drawable.ic_settings_points,
                new SwitchPresenter()));
        return new SettingsGroup(resources.getString(R.string.settings_category_recording), items, new CategoryPresenter());
    }

    /**
     * Set the path for storing the recorded sequences to SD card storage if the option is enable
     * and saves the new preference value.
     * In case an upload task is started the preference is not allowed to be changed until the task is done.
     *
     * @param isRemovableStorageEnable {@code true} if the removable preference is enable, {@code false} otherwise.
     * @return {@code true} if the storage was successfully set, {@code false} if the storage need to wait for upload task.
     */
    private boolean handleRemovableStoragePreferenceChange(boolean isRemovableStorageEnable) {
        Context context = getApplication().getApplicationContext();
        if (uploadManager.isInProgress()) {
            Log.d(TAG, String.format("handleRemovableStoragePreferenceChange. Status: not allowed. Message: Not allowed to switch while uploading. Value: %s",
                    isRemovableStorageEnable));
            Toast.makeText(context, context.getString(R.string.settings_storage_not_allowed_while_upload), Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
        if (isRemovableStorageEnable) {
            Utils.generateOSVFolder(context);
        }
        Log.d(TAG, String.format("handleRemovableStoragePreferenceChange. Status: success. Message:Set external storage: %s", isRemovableStorageEnable));
        return true;
    }

    /**
     * Creates the {@code Map} category with it's settings items.
     *
     * @return {@code SettingsGroup} representing the {@code Map} category.
     */
    private SettingsGroup createMapCategory() {
        List<SettingsItem> items = new ArrayList<>();
        Resources resources = getApplication().getResources();
        items.add(new SettingsItem(resources.getString(R.string.settings_use_map_title),
                resources.getString(R.string.settings_use_map_summary),
                PreferenceTypes.K_MAP_ENABLED,
                R.drawable.ic_settings_map,
                new SwitchPresenter((preference, newValue) -> {
                    boolean preferenceValue = (boolean) newValue;
                    appPrefs.saveBooleanPreference(PreferenceTypes.K_MAP_ENABLED, preferenceValue);
                    if (!preferenceValue) {
                        appPrefs.saveBooleanPreference(PreferenceTypes.K_RECORDING_MINI_MAP_ENABLED, false);
                    }
                    EventBus.postSticky(new SdkEnabledEvent(preferenceValue));
                    RestartAppDialogFragment dialogFragment = new RestartAppDialogFragment();
                    dialogFragment.setCancelable(false);
                    dialogFragment.setOnDismissListener(() -> {
                        appPrefs.saveBooleanPreference(PreferenceTypes.K_MAP_ENABLED, !preferenceValue);
                        if (!preferenceValue) {
                            appPrefs.saveBooleanPreference(PreferenceTypes.K_RECORDING_MINI_MAP_ENABLED,
                                    !appPrefs.getBooleanPreference(PreferenceTypes.K_RECORDING_MINI_MAP_ENABLED));
                        }
                        ((SwitchPreferenceCompat) preference).setChecked(!preferenceValue);
                    });
                    openScreenObservable.setValue(new Pair<>(RestartAppDialogFragment.TAG, dialogFragment));
                    return true;
                })));
        return new SettingsGroup(resources.getString(R.string.settings_category_map), items, new CategoryPresenter());
    }

    /**
     * Creates the {@code Improve} category with it's settings items.
     *
     * @return {@code SettingsGroup} representing the {@code Improve} category.
     */
    private SettingsGroup createImproveCategory() {
        List<SettingsItem> items = new ArrayList<>();
        Resources resources = getApplication().getResources();
        items.add(new SettingsItem(resources.getString(R.string.settings_report_a_problem_title),
                null, null,
                R.drawable.ic_settings_bug,
                new PreferencePresenter((v) -> {
                    openScreenObservable.setValue(new Pair<>(IssueReportFragment.TAG, IssueReportFragment.newInstance()));
                    return true;
                })));
        items.add(new SettingsItem(resources.getString(R.string.settings_tips_title),
                resources.getString(R.string.settings_tips_summary),
                null,
                R.drawable.ic_settings_tips,
                new PreferencePresenter((v) -> {
                    openScreenObservable.setValue(new Pair<>(HintsFragment.TAG, HintsFragment.newInstance()));
                    return true;
                })));
        items.add(new SettingsItem(resources.getString(R.string.settings_app_guide_title),
                resources.getString(R.string.settings_app_guide_summary),
                null,
                R.drawable.ic_settings_app_guide,
                new PreferencePresenter((v) -> {
                    Intent intent = new Intent(getApplication().getApplicationContext(), WalkthroughActivity.class);
                    openActivityScreenObservable.setValue(intent);
                    return true;
                })));
        return new SettingsGroup(resources.getString(R.string.settings_category_improve), items, new CategoryPresenter());
    }

    /**
     * Creates the {@code Legal} category with it's settings items.
     *
     * @return {@code SettingsGroup} representing the {@code Legal} category.
     */
    private SettingsGroup createLegalCategory() {
        List<SettingsItem> items = new ArrayList<>();
        Resources resources = getApplication().getResources();
        items.add(new SettingsItem(resources.getString(R.string.settings_terms_and_conditions), null, null, 0,
                new PreferencePresenter((v) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_TERMS_AND_CONDITIONS));
                    openActivityScreenObservable.setValue(browserIntent);
                    return true;
                })));
        items.add(new SettingsItem(resources.getString(R.string.settings_privacy_policy_title),
                null, null, 0,
                new PreferencePresenter((v) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_PRIVACY_POLICY));
                    openActivityScreenObservable.setValue(browserIntent);
                    return true;
                })));
        return new SettingsGroup(resources.getString(R.string.settings_category_legal), items, new CategoryPresenter());
    }

    /**
     * Creates the {@code Footer} category with it's settings items.
     *
     * @return a {@code SettingsGroup} representing the {@code Footer} category.
     */
    private SettingsGroup createFooterCategory() {
        String buildVersion;
        try {
            PackageInfo packageInfo = getApplication().getPackageManager().getPackageInfo(getApplication().getPackageName(), 0);
            buildVersion = String.format(FORMAT_VERSIONING, packageInfo.versionName, packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            buildVersion = StringUtils.EMPTY_STRING;
        }
        return new SettingsGroup(new FooterPresenter(getApplication().getResources().getString(R.string.settings_build_version) + buildVersion, null));
    }

    /**
     * @return a formatted {@code String} representing the camera resolution in mega pixels.
     */
    private String getFormattedResolution() {
        Size resolutionSize = new Size(appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH),
                appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT));
        int mp = resolutionSize.getRoundedMegaPixels();
        return String.format(FORMAT_RESOLUTION,
                mp == 0 ? FormatUtils.formatDecimalNumber(resolutionSize.getMegaPixelsWithPrecision(), FormatUtils.FORMAT_SHORT_ONE_DECIMAL) : mp);
    }

    /**
     * @return a {@code String} label for selected camera angle to display in settings.
     */
    private String getCameraAngle() {
        if (cameraDeviceDataList.isEmpty()) {
            return "";
        }
        String cameraId = appPrefs.getStringPreference(PreferenceTypes.K_USED_CAMERA_ID);
        double maxFOV = 0;
        String cameraIdNew = "";
        for (CameraDeviceData data : cameraDeviceDataList) {
            if (data.getHorizontalFOV() > maxFOV) {
                cameraIdNew = data.getCameraId();
                maxFOV = data.getHorizontalFOV();
            }
            if (data.getCameraId().equals(cameraId)) {
                return getLabelForFOV(data.getHorizontalFOV());
            }
        }
        appPrefs.saveStringPreference(PreferenceTypes.K_USED_CAMERA_ID, cameraIdNew);
        return getLabelForFOV(maxFOV);
    }

    /**
     * @param fov horizontal camera FOV
     * @return a {@code String} label for selected camera angle depending on the camera fov.
     */
    private String getLabelForFOV(double fov) {
        if (fov > 100) {
            return getApplication().getString(R.string.settings_camera_angle_ultra_wide);
        } else if ((fov >= 64) && (fov <= 100)) {
            return getApplication().getString(R.string.settings_camera_angle_wide);
        } else {
            return getApplication().getString(R.string.settings_camera_angle_normal);
        }
    }

    /**
     * @return {@code true} if the distance unit is metric, {@code false} if teh distance unit is imperial.
     */
    private boolean isDistanceUnitMetric() {
        return appPrefs.getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
    }

    /**
     * Disable debug mode when entering the settings screen.
     */
    private void disableDebugMode() {
        appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED, false);
    }

    /**
     * @return {@code true} if the camera permissions are granted, {@code false} otherwise.
     */
    private boolean checkPermissionsForCamera() {
        ArrayList<String> needed = new ArrayList<>();
        int cameraPermitted = ContextCompat.checkSelfPermission(getApplication().getApplicationContext(), Manifest.permission.CAMERA);
        if (cameraPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.CAMERA);
        }
        if (needed.size() > 0) {
            String[] array = new String[needed.size()];
            needed.toArray(array);
            if (cameraPermissionsObservable != null) {
                cameraPermissionsObservable.setValue(array);
            }
            return false;
        } else {
            return true;
        }
    }
}