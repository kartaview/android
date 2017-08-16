package com.telenav.osv.ui.fragment;

import android.support.annotation.AnimRes;
import android.support.annotation.AnimatorRes;
import android.support.v4.app.Fragment;
import android.view.View;
import com.telenav.osv.R;

/**
 * Created by kalmanb on 8/9/17.
 */
public abstract class OSVFragment extends Fragment {

    public void cancelAction(){

    }

    public boolean onBackPressed(){
        return false;
    }

    public View getSharedElement(){
        return null;
    }

    public String getSharedElementTransitionName(){
        return null;
    }

    public @AnimatorRes @AnimRes int getEnterAnimation(){
        return R.anim.slide_up_add;
    }

    public @AnimatorRes @AnimRes int getExitAnimation(){
        return R.anim.slide_down_remove;
    }
}
