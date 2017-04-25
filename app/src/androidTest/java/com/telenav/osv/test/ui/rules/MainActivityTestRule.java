package com.telenav.osv.test.ui.rules;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.test.rule.ActivityTestRule;
import com.telenav.osv.activity.MainActivity;

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
    public MainActivity launchActivity(@Nullable Intent startIntent) {
        return super.launchActivity(startIntent);
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
}
