package com.telenav.osv.ui.fragment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.http.RequestResponseListener;
import com.telenav.osv.item.UserData;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.ui.list.LeaderboardAdapter;
import com.telenav.osv.utils.Log;

/**
 *
 * Created by Kalman on 22/11/2016.
 */
public class LeaderboardFragment extends Fragment {

    public final static String TAG = "LeaderboardFragment";

    private MainActivity activity;

    private UploadManager mUploadManager;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private TabLayout mTabLayout;

    private ViewPager mTimeViewPager;

    private ArrayList<UserData> mUserList = new ArrayList<>();

    private LeaderboardAdapter mLeaderboardAdapter;

    private int mTimePeriod = 0;

    private int mRegionType = 0;

    private String mCountryCode = "us";

    private int mUserPosition = 0;

    private RecyclerView mRecyclerView;

    private View view;

    private String savedUsername;

    private Handler mBackgroundHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_leaderboard, null);

        activity = (MainActivity) getActivity();
        mUploadManager = ((OSVApplication) activity.getApplication()).getUploadManager();
        HandlerThread thread = new HandlerThread("leaderboardLoader", Process.THREAD_PRIORITY_LOWEST);
        thread.start();
        mBackgroundHandler = new Handler(thread.getLooper());
        savedUsername = activity.getApp().getAppPrefs().getStringPreference(PreferenceTypes.K_USER_NAME);

        mTabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        refreshRegionTab();
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mRegionType = tab.getPosition();
                requestLeaderboardData(mRegionType, mTimePeriod);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        mTimeViewPager = (ViewPager) view.findViewById(R.id.time_view_pager);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.rank_recycler_view);
        ImageView mTimePagerLeft = (ImageView) view.findViewById(R.id.button_time_pager_left);
        ImageView mTimePagerRight = (ImageView) view.findViewById(R.id.button_time_pager_right);
        mTimePagerLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimeViewPager.setCurrentItem(Math.max(mTimeViewPager.getCurrentItem()-1,0), true);
            }
        });
        mTimePagerRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimeViewPager.setCurrentItem(Math.min(mTimeViewPager.getCurrentItem()+1,3), true);
            }
        });
        mTimeViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mTimePeriod = position;
                requestLeaderboardData(mRegionType, mTimePeriod);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mLeaderboardAdapter = new LeaderboardAdapter(mUserList, activity);
        mRecyclerView.setAdapter(mLeaderboardAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        return view;
    }

    @Override
    public void onDestroyView() {
        try {
            mBackgroundHandler.getLooper().getThread().interrupt();
        } catch (Exception ignored){}
        super.onDestroyView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
//        if (activity != null && mSequencesRecyclerView != null) {
//            if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//                mSequencesRecyclerView.setLayoutManager(mPortraitLayoutManager);
//            } else {
//                mSequencesRecyclerView.setLayoutManager(mLandscapeLayoutManager);
//            }
//        }
//        if (mTabLayout != null){
//            mTabLayout.requestLayout();
//        }
        if (mLeaderboardAdapter != null && mRecyclerView != null) {
//            mRecyclerView.post(new Runnable() {
//                @Override
//                public void run() {
//                    mLeaderboardAdapter.setUserPosition(mUserPosition);
//                    mLeaderboardAdapter.notifyDataSetChanged();
//                    int viewsize = view.getMeasuredHeight();
//                    int bottom = mRecyclerView.getTop();
////                                    ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(mUserPosition, (viewsize - bottom)/2);
//                    mRecyclerView.smoothScrollToPosition(mUserPosition + (int)((viewsize - bottom)/Utils.dpToPx(activity, 40))/2);
////                                    ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPosition(mUserPosition);
//
//                }
//            });
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLeaderboardAdapter.setUserPosition(mUserPosition);
                    mLeaderboardAdapter.notifyDataSetChanged();
                }
            });
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    int viewsize = view.getMeasuredHeight();
                    int bottom = mRecyclerView.getTop();
//                                                    mRecyclerView.smoothScrollToPosition(mUserPosition + (int) ((viewsize - bottom) / Utils.dpToPx(activity, 40)) / 2);
                    mRecyclerView.scrollToPosition(mUserPosition + mRecyclerView.getChildCount() / 2);
                }
            }, 500);
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//
//        mOnlineSequencesAdapter = new SequenceAdapter(mOnlineSequences, activity);
//        mSequencesRecyclerView.setAdapter(mOnlineSequencesAdapter);
//
//        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//            mSequencesRecyclerView.setLayoutManager(mPortraitLayoutManager);
//        } else {
//            mSequencesRecyclerView.setLayoutManager(mLandscapeLayoutManager);
//        }
        mTimeViewPager.setOffscreenPageLimit(4);
        mTimeViewPager.setAdapter(new PagerAdapter() {

            @Override
            public int getCount() {
                return 4;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                TextView tv = new TextView(activity);
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                tv.setLayoutParams(lp);
                tv.setGravity(Gravity.CENTER);
                switch (position){
                    case 0:
                        tv.setText("All Time");
                        break;
                    case 1:
                        tv.setText("This Month");
                        break;
                    case 2:
                        tv.setText("This Week");
                        break;
                    case 3:
                        tv.setText("This Day");
                        break;
                }
                tv.setTextColor(activity.getResources().getColor(R.color.leaderboard_text_grey));
                container.addView(tv);
                tv.setTextSize(18);
                return tv;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {

            }
        });
        mTimeViewPager.setCurrentItem(mTimePeriod);
        TabLayout.Tab tab = mTabLayout.getTabAt(mRegionType);
        if (tab != null) {
            tab.select();
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                requestLeaderboardData(mRegionType, mTimePeriod);
            }
        }, 500);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void requestLeaderboardData(final int regionType, final int periodType) {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                String date;
                Calendar calendar = Calendar.getInstance();
                @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                switch (periodType) {
                    default:
                    case 0://ALL TIME
                        date = null;
                        break;
                    case 1://MONTHLY
                        calendar.set(Calendar.DAY_OF_MONTH, 1);
                        date = df.format(calendar.getTime());
                        break;
                    case 2://WEEKLY
                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                        date = df.format(calendar.getTime());
                        break;
                    case 3://DAILY
                        date = df.format(calendar.getTime());
                        break;
                }
                final String region;
                switch (regionType) {
                    default:
                    case 0://ALL WORLD
                        region = null;
                        break;
                    case 1://USER'S COUNTRY
                        region = mCountryCode;
                        break;
                }
                Log.d(TAG, "requestLeaderboardData: date " + date);
                mUserPosition = 0;
                mUploadManager.getLeaderboardData(new RequestResponseListener() {
                    @Override
                    public void requestFinished(final int status, final String result) {
                        mBackgroundHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                Log.d(TAG, "requestLeaderboardData: " + " status - > " + status + " result - > " + result);
                                if (result != null && !result.isEmpty() && status == RequestResponseListener.STATUS_SUCCESS_LEADERBOARD) {
                                    final ArrayList<UserData> userList = new ArrayList<>();
                                    try {
                                        JSONObject obj = new JSONObject(result);
                                        JSONObject osv = obj.getJSONObject("osv");

                                        JSONArray users = osv.getJSONArray("users");
                                        for (int i = 0; i < users.length(); i++) {
                                            JSONObject user = users.getJSONObject(i);
                                            int rank = i + 1;
                                            String userName = user.getString("username");
                                            int points = Integer.parseInt(user.getString("total_user_points"));
                                            userList.add(new UserData(userName, rank, points));
                                            if (userName.equals(savedUsername)) {
                                                try {
                                                    mCountryCode = user.getString("country_code");
                                                    mUserPosition = rank;
                                                    refreshRegionTab();
                                                } catch (Exception ignored) {}
                                            }
                                        }
                                        if (savedUsername.equals("")) {
                                            mUserPosition = 0;
                                            mCountryCode = "";
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    if (mTimePeriod == periodType && mRegionType == regionType) {
                                        mUserList.clear();
                                        mUserList.addAll(userList);
                                        if (mLeaderboardAdapter != null) {
                                            mHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mLeaderboardAdapter.setUserPosition(mUserPosition);
                                                    mLeaderboardAdapter.notifyDataSetChanged();
                                                }
                                            });
                                            mHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    int viewsize = view.getMeasuredHeight();
                                                    int bottom = mRecyclerView.getTop();
//                                                    mRecyclerView.smoothScrollToPosition(mUserPosition + (int) ((viewsize - bottom) / Utils.dpToPx(activity, 40)) / 2);
                                                    mRecyclerView.scrollToPosition(mUserPosition + mRecyclerView.getChildCount() / 2);
                                                }
                                            }, 500);
                                        }
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void requestFinished(int status) {
                        activity.showSnackBar("No Internet connection detected.", Snackbar.LENGTH_LONG);
                        Log.d(TAG, "getProfileDetails: " + " status - > " + status);
                    }
                }, date, region);
            }
        });
    }

    public void refreshRegionTab(){
        if (activity != null && mTabLayout != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (savedUsername.equals("")){
                        mTabLayout.removeTabAt(1);
                    } else {
                        TabLayout.Tab tab = mTabLayout.getTabAt(1);
                        if (tab != null) {
                            tab.setText(new Locale("", mCountryCode, "").getDisplayCountry());
                        }
                    }
                }
            });
        }
    }
}
