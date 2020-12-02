package com.telenav.osv.ui.rules;

import android.content.Intent;
import com.telenav.osv.activity.MainActivity;
import androidx.annotation.Nullable;
import androidx.test.rule.ActivityTestRule;

/**
 * Created by Kalman on 04/04/2017.
 */

public class MainActivityTestRule extends ActivityTestRule<MainActivity> {

    public MainActivityTestRule(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    public MainActivityTestRule(Class<MainActivity> activityClass, boolean initialTouchMode) {
        super(activityClass, initialTouchMode);
    }

    public MainActivityTestRule(Class<MainActivity> activityClass, boolean initialTouchMode, boolean launchActivity) {
        super(activityClass, initialTouchMode, launchActivity);
    }

    @Override
    protected void beforeActivityLaunched() {
        super.beforeActivityLaunched();
    }

    @Override
    protected void afterActivityLaunched() {
        super.afterActivityLaunched();
    }

    @Override
    protected void afterActivityFinished() {
        super.afterActivityFinished();
    }

    @Override
    public MainActivity launchActivity(@Nullable Intent startIntent) {
        return super.launchActivity(startIntent);
    }
}
