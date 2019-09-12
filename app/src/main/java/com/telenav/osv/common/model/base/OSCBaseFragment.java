package com.telenav.osv.common.model.base;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import com.telenav.osv.R;
import com.telenav.osv.common.OnBackPressed;
import com.telenav.osv.common.toolbar.OSCToolbar;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.common.ui.loader.LoadingScreen;
import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Base fragment class for all fragments in the application. Its main purpose is to:
 * <ul>
 * <li>expose toolbar functionality</li>
 * </ul>
 * @author horatiuf
 */
//ToDo: Use this fragment for new components instead of the OSVFragment
public abstract class OSCBaseFragment extends Fragment implements OnBackPressed {

    /**
     * The value for the status bar which will be used for reset.
     */
    public static final int STATUS_BAR_RESET_VALUE = -1;

    protected OSCToolbar oscToolbar;

    protected ViewGroup loaderContainer;

    /**
     * The {@code LoadingScreen} displayed before the network requests.
     */
    protected LoadingScreen loadingScreen;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getActivity() != null) {
            OSCBaseActivity oscBaseActivity = (OSCBaseActivity) getActivity();
            loaderContainer = oscBaseActivity.findViewById(R.id.frame_layout_activity_obd_loader);
            setupToolbar(oscBaseActivity);
        }
        LoadingScreen loadingScreen = setupLoadingScreen();
        if (loadingScreen != null) {
            this.loadingScreen = loadingScreen;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (oscToolbar != null) {
            oscToolbar.showToolbar();
        }
    }

    public void showLoadingIndicator() {
        if (loadingScreen != null) {
            loadingScreen.show(loaderContainer, 0);
        }
    }

    public void hideLoadingIndicator() {
        if (loadingScreen != null) {
            loadingScreen.hide(loaderContainer);
        }
    }

    /**
     * Setup method for custom {@code LoadingScreen}.
     * <p> Override this method for loader setup and display.
     * @return either null or custom Loading screen for the current fragment.
     */
    @Nullable
    public LoadingScreen setupLoadingScreen() {
        return null;
    }

    @Nullable
    public abstract ToolbarSettings getToolbarSettings(OSCToolbar oscToolbar);

    /**
     * Default method in order to setup the status bar color.
     * <p>This will either reset the status bar if the statusBarColor value will be {@link #STATUS_BAR_RESET_VALUE} otherwise the value will he set.</p>
     * <p> This can be called on lifecycle method such as {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} to change the color.
     * @param statusBarColor
     */
    public void setupStatusBarColor(@ColorRes int statusBarColor) {
        Activity activity = getActivity();
        if (activity != null) {
            Window window = activity.getWindow();
            if (statusBarColor == STATUS_BAR_RESET_VALUE) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                int color = activity.getResources().getColor(statusBarColor);
                window.setStatusBarColor(color);
            }
        }
    }

    /**
     * Setup the toolbar if set based on the given
     * @param oscBaseActivity
     */
    private void setupToolbar(OSCBaseActivity oscBaseActivity) {
        ToolbarSettings settings = getToolbarSettings(oscToolbar);
        oscToolbar = oscBaseActivity.getToolbar();
        if (oscToolbar != null) {
            if (settings != null) {
                oscToolbar.updateToolbar(settings);
            } else {
                oscToolbar.hideToolbar();
            }
        }
    }
}
