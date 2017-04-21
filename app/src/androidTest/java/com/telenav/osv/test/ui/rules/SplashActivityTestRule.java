package com.telenav.osv.test.ui.rules;

import android.support.test.rule.ActivityTestRule;
import com.telenav.osv.activity.SplashActivity;

/**
 * Created by Kalman on 03/04/2017.
 */

public class SplashActivityTestRule extends ActivityTestRule<SplashActivity> {


    public SplashActivityTestRule(Class<SplashActivity> activityClass) {
        super(activityClass);
    }

    public SplashActivityTestRule(Class<SplashActivity> activityClass, boolean initialTouchMode) {
        super(activityClass, initialTouchMode);
    }

    public SplashActivityTestRule(Class<SplashActivity> activityClass, boolean initialTouchMode, boolean launchActivity) {
        super(activityClass, initialTouchMode, launchActivity);
    }

    @Override
    protected void beforeActivityLaunched() {
        super.beforeActivityLaunched();
    }

    @Override
    protected void afterActivityLaunched() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.afterActivityLaunched();
    }

    @Override
    protected void afterActivityFinished() {
        super.afterActivityFinished();
    }
}
