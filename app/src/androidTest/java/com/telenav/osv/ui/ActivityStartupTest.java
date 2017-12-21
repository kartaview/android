package com.telenav.osv.ui;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import com.telenav.osv.R;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.ui.rules.SplashActivityTestRule;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;

/**
 * Test class testing main activity ui, without any fragments
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ActivityStartupTest {

    @Rule
    public SplashActivityTestRule mActivityRule = new SplashActivityTestRule(SplashActivity.class, false, true);

    @Before
    public void setup() {

    }

    @Test
    public void mapOpenedTest() {
        onView(ViewMatchers.withId(R.id.activity_main_root)).check(matches(isDisplayed()));
    }

    //test menu drawer, logged in user name, picture, button
    //test actionbar for startup screen (which is map)
}