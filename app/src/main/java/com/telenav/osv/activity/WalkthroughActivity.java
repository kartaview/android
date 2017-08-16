package com.telenav.osv.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Window;
import android.view.WindowManager;
import com.github.paolorotolo.appintro.AppIntro;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.ui.fragment.WalkthroughSlideFragment;

/**
 * Created by alexandra on 7/7/16.
 */

public class WalkthroughActivity extends AppIntro {

    private ApplicationPreferences appPrefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appPrefs = ((OSVApplication) getApplication()).getAppPrefs();

        addSlide(WalkthroughSlideFragment.newInstance(R.layout.fragment_walkthrough_discover));
        addSlide(WalkthroughSlideFragment.newInstance(R.layout.fragment_walkthrough_record));
        addSlide(WalkthroughSlideFragment.newInstance(R.layout.fragment_walkthrough_share));

        setSeparatorColor(getResources().getColor(R.color.transparent));
        showSkipButton(true);
        setProgressButtonEnabled(true);
        showStatusBar(true);

        setFadeAnimation();
        setZoomAnimation();
        setFlowAnimation();
        setSlideOverAnimation();
        setDepthAnimation();

    }

    @Override
    protected void onPageSelected(int position) {
        super.onPageSelected(position);
        int backgroundColor = -1;
        switch (position) {
            case 0:
                backgroundColor = R.color.walkthrought_1;
                break;
            case 1:
                backgroundColor = R.color.walkthrought_2;
                break;
            case 2:
                backgroundColor = R.color.walkthrought_3;
                break;
            case 3:
                backgroundColor = -1;
                break;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            if (backgroundColor == -1) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            } else {
                int clr = getResources().getColor(backgroundColor);
                window.setStatusBarColor(clr);
            }

        }
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        // Do something when users tap on Done button.
        appPrefs.saveBooleanPreference(PreferenceTypes.K_INTRO_SHOWN, true);
        finish();


    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        appPrefs.saveBooleanPreference(PreferenceTypes.K_INTRO_SHOWN, true);
        finish();


    }
}
