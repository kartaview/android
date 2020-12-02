package com.telenav.osv.ui.fragment.settings.viewmodel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.recorder.camera.Camera;
import com.telenav.osv.recorder.camera.model.CameraDeviceData;
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup;
import com.telenav.osv.ui.fragment.settings.presenter.group.RadioGroupPresenter;

import java.util.ArrayList;
import java.util.List;

public class CameraAngleViewModel extends SettingsBaseViewModel {

    /**
     * {@code List} with the supported cameras.
     */
    private List<CameraDeviceData> cameraDeviceDataList;

    public CameraAngleViewModel(@NonNull Application application, @NonNull ApplicationPreferences appPrefs) {
        super(application, appPrefs);
        Camera camera = ((KVApplication) application).getCamera();
        cameraDeviceDataList = camera.getCameraDevices();
    }

    @Override
    public int getTitle() {
        return R.string.settings_camera_angle_title;
    }

    @Override
    public void start() {
        List<SettingsGroup> settingsCategories = new ArrayList<>();
        settingsCategories.add(createCameraAngleCategory());
        settingsDataObservable.setValue(settingsCategories);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ((KVApplication) getApplication()).releaseCamera();
    }

    /**
     * @return a group model for the camera angle options.
     */
    private SettingsGroup createCameraAngleCategory() {
        return new SettingsGroup(new RadioGroupPresenter(getCameraAngleOptions(), getCameraAngleGroupListener()));
    }

    /**
     * @return a list of {@code RadioButtons} representing the camera angle options.
     */
    @SuppressLint("DefaultLocale")
    private List<RadioButton> getCameraAngleOptions() {
        List<RadioButton> radioButtons = new ArrayList<>();
        String currentCameraId = appPrefs.getStringPreference(PreferenceTypes.K_USED_CAMERA_ID);
        for (CameraDeviceData cameraDeviceData : cameraDeviceDataList) {
            RadioButton radioButton = (RadioButton) LayoutInflater.from(getApplication().getApplicationContext()).inflate(R.layout.settings_item_radio, null);
            radioButton.setText(String.format("%s (%d\u00B0)", getLabelForFOV(cameraDeviceData.getHorizontalFOV()), (int) cameraDeviceData.getHorizontalFOV()));
            radioButton.setTag(cameraDeviceData.getCameraId());
            radioButton.setId(View.generateViewId());
            if (cameraDeviceData.getCameraId().equals(currentCameraId)) {
                radioButton.setChecked(true);
            }
            radioButtons.add(radioButton);
        }
        return radioButtons;
    }

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
     * @return a {@code listener} which is called when a camera angle is selected.
     */
    private RadioGroup.OnCheckedChangeListener getCameraAngleGroupListener() {
        return (radioGroup, i) -> {
            RadioButton radioButton = radioGroup.findViewById(i);
            if (radioButton != null) {
                String cameraId = (String) radioButton.getTag();
                ApplicationPreferences applicationPreferences = ((KVApplication) getApplication()).getAppPrefs();
                applicationPreferences.saveStringPreference(PreferenceTypes.K_USED_CAMERA_ID, cameraId);
            }
        };
    }
}
