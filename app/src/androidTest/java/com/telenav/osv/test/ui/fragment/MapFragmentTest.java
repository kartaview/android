package com.telenav.osv.test.ui.fragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.test.ui.rules.MainActivityTestRule;
import com.telenav.osv.utils.Log;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


/**
 * Test class testing the map fragment and ui
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MapFragmentTest {

    private static final String TAG = "MapFragmentTest";

    @Rule
    public MainActivityTestRule mMainActivityRule = new MainActivityTestRule(MainActivity.class, false, true);

    @Before
    public void setup() {

    }

    @Test
    public void activityOpenedTest() {
        onView(withId(R.id.fragment_map_root)).check(matches(isDisplayed()));
//        Log.d(TAG, "setup: " + mMainActivityRule.getActivity());
    }

}