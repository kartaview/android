package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.matthewtamlin.dotindicator.DotIndicator;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;

/**
 * Created by Kalman on 18/07/16.
 */

public class HintsFragment extends Fragment {

    public static final String TAG = "HintsFragment";

    private View view;

    private ViewPager hintPager;

    private DotIndicator hintIndicator;

    private HintPagerAdapter hintPagerAdapter;

    private Runnable hintPagerAutoRunnable;

    private MainActivity activity;

    private ImageView mCloseButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_recording_hints, null);
        activity = (MainActivity) getActivity();
        hintPager = (ViewPager) view.findViewById(R.id.hint_pager);
        hintIndicator = (DotIndicator) view.findViewById(R.id.hint_indicator);
        hintPagerAdapter = new HintPagerAdapter(inflater);
        int orientation = activity.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        hintPagerAdapter.populate(portrait);
        hintPager.setAdapter(hintPagerAdapter);
        hintPagerAutoRunnable = new Runnable() {
            @Override
            public void run() {
                hintPager.setCurrentItem((hintPager.getCurrentItem() + 1) % hintPagerAdapter.getCount(), true);
            }
        };
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
        mCloseButton = (ImageView) view.findViewById(R.id.close_button);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.onBackPressed();
            }
        });
        return view;
    }

    public class HintPagerAdapter extends PagerAdapter {

        private final LayoutInflater mInflater;

        /**
         * The fragments used in the pager
         */
        public ArrayList<FrameLayout> views = new ArrayList<>();

        public ArrayList<Integer> colors = new ArrayList<>();

        public ArrayList<String[]> hints = new ArrayList<>();

        public HintPagerAdapter(LayoutInflater inflater) {
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


            hints.add(fifthHint);
            hints.add(secondHint);
            hints.add(thirdHint);
            hints.add(fourthHint);
            colors.clear();
            colors.add(R.color.hint_blue);
            colors.add(R.color.hint_purple);
            colors.add(R.color.hint_green);
            colors.add(R.color.hint_yellow);
            colors.add(R.color.hint_red);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        public void populate(boolean portrait) {
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
                landscape.setBackgroundColor(activity.getResources().getColor(colors.get((views.size()+1) % colors.size())));
                viewTitleHint = (TextView) landscape.findViewById(R.id.title_hint_text_vertical);
                viewTitleHint.setText(R.string.hint_landscape_label);
                viewHintDescription = (TextView) landscape.findViewById(R.id.hint_text_vertical);
                viewHintDescription.setText(R.string.hint_landscape_message);
                numberOfItems = hints.size() + 1;
            } else {
                numberOfItems = hints.size();
            }
            int i = 0;
            for (String[] hint : hints) {
                if (i == 1 && landscape != null){
                    views.add(landscape);
                }
                frameLayout = (FrameLayout) mInflater.inflate(R.layout.item_hint_text, null);
                frameLayout.setBackgroundColor(activity.getResources().getColor(colors.get(views.size() % colors.size())));
                viewTitleHint = (TextView) frameLayout.findViewById(R.id.title_hint_text_vertical);
                viewHintDescription = (TextView) frameLayout.findViewById(R.id.hint_text_vertical);
                viewTitleHint.setText(hint[0]);
                viewHintDescription.setText(hint[1]);
                views.add(frameLayout);
                i++;
            }
            hintIndicator.setNumberOfItems(numberOfItems);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = views.get(Math.min(position, views.size() - 1));
            container.addView(v);
            return v;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

}
