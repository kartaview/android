package com.telenav.osv.ui.fragment;

import android.view.View;

import androidx.annotation.AnimRes;
import androidx.annotation.AnimatorRes;
import androidx.fragment.app.Fragment;

import com.telenav.osv.R;

/**
 * Created by kalmanb on 8/9/17.
 * This should not be used for new implementation, over time is intended to be replaced and deleted.
 */
@Deprecated
public abstract class OSVFragment extends Fragment {

    public void cancelAction() {

    }

    public boolean onBackPressed() {
        return false;
    }

    public View getSharedElement() {
        return null;
    }

    public String getSharedElementTransitionName() {
        return null;
    }

    public @AnimatorRes
    @AnimRes
    int getEnterAnimation() {
        return R.anim.slide_up_add;
    }

    public @AnimatorRes
    @AnimRes
    int getExitAnimation() {
        return R.anim.slide_down_remove;
    }
}
