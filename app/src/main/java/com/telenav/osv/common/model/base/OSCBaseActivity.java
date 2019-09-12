package com.telenav.osv.common.model.base;

import java.util.List;
import android.os.Bundle;
import com.telenav.osv.R;
import com.telenav.osv.common.toolbar.OSCToolbar;
import com.telenav.osv.ui.fragment.camera.controls.CameraControlsFragment;
import com.telenav.osv.ui.fragment.camera.preview.CameraPreviewFragment;
import com.telenav.osv.utils.Log;
import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;

/**
 * The base activity for all activities within the app.
 * <p>This contains logic for {@link #onBackPressed()} signalling all active fragments that a back action was press. If there
 * is no handling it will automatically call {@link AppCompatActivity#onBackPressed()}.
 * </p>
 * <p>
 * <p>Furthermore actionbar is set on {@link #onCreate(Bundle)} lifecycle.
 * @author horatiuf
 */

public abstract class OSCBaseActivity extends AppCompatActivity {

    private static final String TAG = OSCBaseActivity.class.getSimpleName();

    private OSCToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        toolbar = new OSCToolbar(findViewById(R.id.toolbar_partial), v -> onBackPressed());
    }

    @Override
    public void onBackPressed() {
        boolean onBackHandled = false;
        List<Fragment> activeFragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : activeFragments) {
            if (!(fragment instanceof PreferenceFragmentCompat)) {
                OSCBaseFragment baseFragment = (OSCBaseFragment) fragment;
                onBackHandled |= baseFragment.handleBackPressed();
            }
        }
        if (!onBackHandled && getSupportFragmentManager().getBackStackEntryCount() == 0) {
            this.finish();
        } else if (!onBackHandled) {
            super.onBackPressed();
        }
    }

    public void setStatusBarColor(@ColorInt int statusBarColor) {
        getWindow().setStatusBarColor(statusBarColor);
    }

    public OSCToolbar getToolbar() {
        return toolbar;
    }

    /**
     * Return from the OBD connecting flow to the recording screen.
     */
    public void returnToRecordingScreen() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        for (int i = fragmentManager.getBackStackEntryCount() - 1; i > 0; i--) {
            if (!(fragmentManager.getBackStackEntryAt(i - 1).getName().equalsIgnoreCase(CameraControlsFragment.TAG) ||
                    fragmentManager.getBackStackEntryAt(i - 1).getName().equalsIgnoreCase(CameraPreviewFragment.TAG))) {
                try {
                    fragmentManager.popBackStack();
                } catch (IllegalStateException ignored) {
                    //Sometimes the onSavedInstanceState method is called before popBackStack and there is no way to avoid the error
                    //Therefore this is a workaround for the specific error
                }
            } else {
                try {
                    onBackPressed();
                } catch (IllegalStateException e) {
                    Log.d(TAG, "returnToRecordingScreen. Status:exception. Message:" + e.getMessage());
                    //ignore exception
                }
                return;
            }
        }
    }

    /**
     * @return {@code int} representing the layout id to be inflated by the activity.
     */
    @LayoutRes
    protected abstract int getLayoutId();
}
