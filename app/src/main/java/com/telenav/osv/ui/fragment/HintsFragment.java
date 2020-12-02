package com.telenav.osv.ui.fragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.matthewtamlin.dotindicator.DotIndicator;
import com.telenav.osv.R;
import com.telenav.osv.common.model.base.KVBaseFragment;
import com.telenav.osv.common.toolbar.KVToolbar;
import com.telenav.osv.common.toolbar.ToolbarSettings;

import java.util.ArrayList;

/**
 * Created by Kalman on 18/07/16.
 */
public class HintsFragment extends KVBaseFragment {

    public static final String TAG = "HintsFragment";

    public static final String SHOW_POINTS_ARG = "POINTS_ARGUMENT";

    private ViewPager hintPager;

    private DotIndicator hintIndicator;

    private HintPagerAdapter hintPagerAdapter;

    private Runnable hintPagerAutoRunnable;

    private View view;

    /**
     * Method used to create an instance of {@link HintsFragment}.
     * @return a new instance of {@code HintsFragment}.
     */
    public static HintsFragment newInstance() {
        return new HintsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_recording_hints, container, false);
        populatePager(view);
        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        populatePager(view);
    }

    @Override
    public ToolbarSettings getToolbarSettings(KVToolbar toolbar) {
        return null;
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void populatePager(View view) {
        hintPager = view.findViewById(R.id.hint_pager);
        hintIndicator = view.findViewById(R.id.hint_indicator);
        hintPager.removeCallbacks(hintPagerAutoRunnable);
        hintIndicator.setSelectedItem(0, false);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        hintPagerAdapter = new HintPagerAdapter(inflater, getPointsArgument());
        int orientation = getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        hintPagerAdapter.populate(portrait);
        hintPager.setAdapter(hintPagerAdapter);
        hintPagerAutoRunnable = () -> hintPager.setCurrentItem((hintPager.getCurrentItem() + 1) % hintPagerAdapter.getCount(), true);
        hintPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                hintIndicator.setSelectedItem(Math.min(position, hintIndicator.getNumberOfItems() - 1), true);
                hintPager.removeCallbacks(hintPagerAutoRunnable);
                hintPager.postDelayed(hintPagerAutoRunnable, 8000);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        hintPager.postDelayed(hintPagerAutoRunnable, 8000);
        ImageView mCloseButton = view.findViewById(R.id.close_button);
        mCloseButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private boolean getPointsArgument() {
        if (getArguments() != null) {
            return getArguments().getBoolean(SHOW_POINTS_ARG, false);
        }
        return false;
    }

    public class HintPagerAdapter extends PagerAdapter {

        private final LayoutInflater mInflater;

        /**
         * The fragments used in the pager
         */
        ArrayList<FrameLayout> views = new ArrayList<>();

        ArrayList<Integer> colors = new ArrayList<>();

        ArrayList<String[]> hints = new ArrayList<>();

        HintPagerAdapter(LayoutInflater inflater, boolean showPoints) {
            super();
            mInflater = inflater;
            hints.clear();

            String[] secondHint = new String[2];
            String[] thirdHint = new String[2];
            String[] fourthHint = new String[2];
            String[] fifthHint = new String[2];

            secondHint[0] = getString(R.string.hint_mount_label);
            secondHint[1] = getString(R.string.hint_mount_message);
            thirdHint[0] = getString(R.string.hint_windshield_label);
            thirdHint[1] = getString(R.string.hint_windshield_message);
            fourthHint[0] = getString(R.string.hint_focus_label);
            fourthHint[1] = getString(R.string.hint_focus_message);
            fifthHint[0] = getString(R.string.hint_points);
            fifthHint[1] = getString(R.string.hint_points_message);

            if (showPoints) {
                hints.add(fifthHint);
            }
            hints.add(secondHint);
            hints.add(thirdHint);
            hints.add(fourthHint);
            colors.clear();
            colors.add(R.color.default_purple);
            colors.add(R.color.default_green);
            colors.add(R.color.default_yellow);
            colors.add(R.color.default_red);
        }

        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = views.get(Math.min(position, views.size() - 1));
            container.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        void populate(boolean portrait) {
            views.clear();
            //            long seed = System.nanoTime();
            //            Collections.shuffle(hints, new Random(seed));
            TextView viewTitleHint;
            TextView viewHintDescription;
            FrameLayout frameLayout;
            FrameLayout landscape = null;
            int numberOfItems;
            if (portrait) {
                landscape = (FrameLayout) mInflater.inflate(R.layout.item_hint_text, null);
                landscape.setBackgroundColor(getResources().getColor(colors.get((views.size() + 1) % colors.size())));
                viewTitleHint = landscape.findViewById(R.id.title_hint_text_vertical);
                viewTitleHint.setText(R.string.hint_landscape_label);
                viewHintDescription = landscape.findViewById(R.id.hint_text_vertical);
                viewHintDescription.setText(R.string.hint_landscape_message);
                numberOfItems = hints.size() + 1;
            } else {
                numberOfItems = hints.size();
            }
            int i = 0;
            for (String[] hint : hints) {
                if (i == 1 && landscape != null) {
                    views.add(landscape);
                }
                frameLayout = (FrameLayout) mInflater.inflate(R.layout.item_hint_text, null);
                frameLayout.setBackgroundColor(getResources().getColor(colors.get(views.size() % colors.size())));
                viewTitleHint = frameLayout.findViewById(R.id.title_hint_text_vertical);
                viewHintDescription = frameLayout.findViewById(R.id.hint_text_vertical);
                viewTitleHint.setText(hint[0]);
                viewHintDescription.setText(hint[1]);
                views.add(frameLayout);
                i++;
            }
            hintIndicator.setNumberOfItems(numberOfItems);
            notifyDataSetChanged();
        }
    }
}
