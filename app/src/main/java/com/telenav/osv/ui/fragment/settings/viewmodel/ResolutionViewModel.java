package com.telenav.osv.ui.fragment.settings.viewmodel;

import java.util.ArrayList;
import java.util.List;
import android.app.Application;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.recorder.camera.Camera;
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup;
import com.telenav.osv.ui.fragment.settings.presenter.group.RadioGroupPresenter;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Size;
import androidx.annotation.NonNull;

/**
 * {@code ViewModel} class responsible to handle the functionality for the {@code Resolution} submenu.
 */
public class ResolutionViewModel extends SettingsBaseViewModel {

    /**
     * The format for the camera resolution.
     */
    private static final String FORMAT_RESOLUTION = "%d x %d (%s MP)";

    /**
     * {@code List} with the camera supported resolutions.
     */
    private List<Size> supportedResolutionsList;

    public ResolutionViewModel(@NonNull Application application, @NonNull ApplicationPreferences appPrefs) {
        super(application, appPrefs);
        Camera camera = ((OSVApplication) application).getCamera();
        supportedResolutionsList = camera.getSupportedPictureResolutions();
    }

    @Override
    public int getTitle() {
        return R.string.settings_resolution_title;
    }

    @Override
    public void start() {
        List<SettingsGroup> settingsCategories = new ArrayList<>();
        settingsCategories.add(createResolutionCategory());
        settingsDataObservable.setValue(settingsCategories);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ((OSVApplication) getApplication()).releaseCamera();
    }

    /**
     * @return a group model for the resolution options.
     */
    private SettingsGroup createResolutionCategory() {
        return new SettingsGroup(new RadioGroupPresenter(getResolutionOptions(), getResolutionGroupListener()));
    }

    /**
     * @return a list of {@code RadioButtons} representing the resolution options.
     */
    private List<RadioButton> getResolutionOptions() {
        List<RadioButton> radioButtons = new ArrayList<>();
        Size currentResolution = getStoredResolution();
        for (Size size : supportedResolutionsList) {
            RadioButton radioButton = (RadioButton) LayoutInflater.from(getApplication().getApplicationContext()).inflate(R.layout.settings_item_radio, null);
            int mp = size.getRoundedMegaPixels();
            radioButton.setText(String.format(FORMAT_RESOLUTION, size.getWidth(), size.getHeight(),
                    mp == 0 ? FormatUtils.formatDecimalNumber(size.getMegaPixelsWithPrecision(), FormatUtils.FORMAT_SHORT_ONE_DECIMAL) : mp));
            radioButton.setTag(size);
            radioButton.setId(View.generateViewId());
            if (size.equals(currentResolution)) {
                radioButton.setChecked(true);
            }
            radioButtons.add(radioButton);
        }
        return radioButtons;
    }

    /**
     * @return a {@code Size} object representing the stored preference for camera resolution.
     */
    private Size getStoredResolution() {
        ApplicationPreferences applicationPreferences = ((OSVApplication) getApplication()).getAppPrefs();
        return new Size(applicationPreferences.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH),
                applicationPreferences.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT));
    }

    /**
     * @return a {@code listener} which is called when a resolution option is selected.
     */
    private RadioGroup.OnCheckedChangeListener getResolutionGroupListener() {
        return (radioGroup, i) -> {
            RadioButton radioButton = radioGroup.findViewById(i);
            if (radioButton != null) {
                Size size = (Size) radioButton.getTag();
                ApplicationPreferences applicationPreferences = ((OSVApplication) getApplication()).getAppPrefs();
                applicationPreferences.saveIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, size.getWidth());
                applicationPreferences.saveIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, size.getHeight());
            }
        };
    }
}