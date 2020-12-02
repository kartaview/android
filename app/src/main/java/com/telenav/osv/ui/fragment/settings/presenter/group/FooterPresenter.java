package com.telenav.osv.ui.fragment.settings.presenter.group;

import android.content.Context;
import android.view.View;
import com.telenav.osv.ui.fragment.settings.custom.FooterPreference;
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceGroup;

/**
 * Presenter which is responsible to create a {@link FooterPreference} view.
 */
public class FooterPresenter extends CategoryPresenter {

    /**
     * The application build version.
     */
    private String buildVersion;

    /**
     * The touch listener for teh hidden debug menu.
     */
    private View.OnTouchListener onTouchListener;

    public FooterPresenter(@NonNull String buildVersion, @Nullable View.OnTouchListener onTouchListener) {
        this.buildVersion = buildVersion;
        this.onTouchListener = onTouchListener;
    }

    @Override
    public PreferenceGroup getPreference(@NonNull Context context, @NonNull SettingsGroup group) {
        return new FooterPreference(context, onTouchListener, buildVersion);
    }
}