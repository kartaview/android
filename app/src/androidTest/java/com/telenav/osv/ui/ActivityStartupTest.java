package com.telenav.osv.ui;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.ui.rules.SplashActivityTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

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

    /*@Test
    public void mapOpenedTest() {
        onView(ViewMatchers.withId(R.id.activity_main_root)).check(matches(isDisplayed()));
    }*/

    //test menu drawer, logged in user name, picture, button
    //test actionbar for startup screen (which is map)
}