package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.item.Payment;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.ui.custom.ProgressImageView;
import com.telenav.osv.ui.list.PaymentAdapter;
import com.telenav.osv.ui.list.SequenceAdapter;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * fragment holding the ui for the user's data
 * Created by adrianbostan on 11/07/16.
 */
public abstract class ProfileFragment extends DisplayFragment
        implements AppBarLayout.OnOffsetChangedListener {

    public final static String TAG = "ProfileFragment";

    /**
     * preference name
     */
    public static final String PREFS_NAME = "osvMyProfileAppPrefs";

    public static final String K_RANK = "rank";

    public static final String K_LEVEL = "level";

    public static final String K_SCORE = "score";

    public static final String K_XP_PROGRESS = "xpProgress";

    public static final String K_XP_TARGET = "xpTarget";

    public static final String K_TOTAL_DISTANCE = "totalDistance";

    public static final String K_OBD_DISTANCE = "obdDistance";

    public static final String K_TOTAL_PHOTOS = "totalPhotos";

    public static final String K_TOTAL_TRACKS = "totalTracks";

    public static final String K_DRIVER_CURRENT_ACCEPTED_DISTANCE = "currentAcceptedDistance";

    public static final String K_DRIVER_CURRENT_PAYRATE = "currentPayRate";

    public static final String K_DRIVER_CURRENT_VALUE = "currentPaymentValue";

    public static final String K_DRIVER_TOTAL_VALUE = "totalPaymentValue";

    public static final String K_DRIVER_TOTAL_ACCEPTED_DISTANCE = "driverAcceptedDistance";

    public static final String K_DRIVER_TOTAL_REJECTED_DISTANCE = "driverRejectedDistance";

    public static final String K_DRIVER_TOTAL_OBD_DISTANCE = "driverObdDistance";

    public static final String K_DRIVER_TRACKS_COUNT = "driverTracksCount";

    public static final String K_DRIVER_PHOTOS_COUNT = "driverPhotosCount";

    public static final String K_DRIVER_CURRENCY = "driverCurrency";

    protected static final int NUMBER_OF_ITEMS_PER_PAGE = 30;

    protected static final int PERCENTAGE_TO_ANIMATE_AVATAR = 5;

    protected boolean mIsAvatarShown = true;

    protected MainActivity activity;

    protected RecyclerView mSequencesRecyclerView;

    protected SequenceAdapter mOnlineSequencesAdapter;

    protected SwipeRefreshLayout mSwipeRefreshLayout;

    protected ArrayList<Sequence> mOnlineSequences = new ArrayList<>();

    protected int mCurrentPageToList = 1;

    protected int mPaymentCurrentPageToList = 1;

    protected int mPaymentMaxNumberOfResults = 10000;

    protected boolean mLoading;

    protected Handler mHandler = new Handler(Looper.getMainLooper());

    protected int mMaxNumberOfResults = 0;

    protected LinearLayoutManager mPortraitLayoutManager;

    protected GridLayoutManager mLandscapeLayoutManager;

    protected ApplicationPreferences appPrefs;

    protected ProgressImageView mProfileImage;

    protected int mMaxScrollSize;

    protected CollapsingToolbarLayout collapsingToolbar;

    protected Toolbar toolbar;

    protected AppBarLayout appBar;

    protected TabLayout mTabLayout;

    ArrayList<Payment> mPaymentsList = new ArrayList<>();

    PaymentAdapter mPaymentAdapter;

    private int mLastVisibleItem, mTotalItemCount;

    private Timer mTimer = new Timer();

    private Runnable mCancelRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSwipeRefreshLayout != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSwipeRefreshLayout != null) {
                            mSwipeRefreshLayout.setRefreshing(false);
                            if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MY_PROFILE) {
                                activity.showSnackBar(R.string.something_wrong_try_again, Snackbar.LENGTH_LONG);
                            }
                        }
                    }
                });
            }
        }
    };

    private TimerTask mCancelTask = new TimerTask() {
        @Override
        public void run() {
            mCancelRunnable.run();
        }
    };

    private Runnable mNotifyRunnable = new Runnable() {
        @Override
        public void run() {
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSwipeRefreshLayout != null) {
                            if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MY_PROFILE) {
                                activity.showSnackBar(R.string.loading_too_long, Snackbar.LENGTH_LONG);
                            }
                        }
                    }
                });
            }
        }
    };

    private TimerTask mNotifyTask = new TimerTask() {
        @Override
        public void run() {
            mNotifyRunnable.run();
        }
    };

    private LinearLayout mHeaderContentHolder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, null);

        activity = (MainActivity) getActivity();
        appPrefs = activity.getApp().getAppPrefs();
        mPortraitLayoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        mLandscapeLayoutManager = new GridLayoutManager(activity, 2);
        mLandscapeLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position != 0 || ProfileFragment.this instanceof NearbyFragment ? 1 : 2;
            }
        });
        mSequencesRecyclerView = view.findViewById(R.id.profile_sequences_recycle_view);
        mTabLayout = view.findViewById(R.id.profile_tabs);
        mSwipeRefreshLayout = view.findViewById(R.id.profile_swipe_refresh_layout);
        mHeaderContentHolder = view.findViewById(R.id.header_content_holder);
        appBar = view.findViewById(R.id.profile_appbar);
        mProfileImage = view.findViewById(R.id.profile_image);
        toolbar = view.findViewById(R.id.profile_toolbar);
        collapsingToolbar = view.findViewById(R.id.profile_collapsing_toolbar);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshContent();
            }
        });
        mMaxScrollSize = appBar.getTotalScrollRange();
        appBar.addOnOffsetChangedListener(this);
        Drawable upArrow = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_back_white, null);
        toolbar.setNavigationIcon(upArrow);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.onBackPressed();
            }
        });
        mOnlineSequencesAdapter = new SequenceAdapter(mOnlineSequences, activity, !(this instanceof NearbyFragment));
        mOnlineSequencesAdapter.enablePoints(appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true));
        mOnlineSequencesAdapter.enableDriverStats(false);
        setupViews(activity.isPortrait());
        return view;
    }

    protected void setupViews(boolean portrait) {
        Resources resources = activity.getResources();
        int sidePadding;
        if (portrait) {
            sidePadding = (int) resources.getDimension(R.dimen.sequence_list_padding_side_portrait);
        } else {
            sidePadding = (int) resources.getDimension(R.dimen.sequence_list_padding_side_landscape);
        }
        if (mSequencesRecyclerView != null) {
            mSequencesRecyclerView.setLayoutManager(getLayoutManager(portrait));
            int bottomPadding = (int) resources.getDimension(R.dimen.sequence_list_padding_bottom);
            mSequencesRecyclerView.setPadding(sidePadding, 0, sidePadding, bottomPadding);
        }
        int width = (int) Utils.dpToPx(activity, activity.getResources().getConfiguration().smallestScreenWidthDp);
        ViewGroup.LayoutParams lp = mHeaderContentHolder.getLayoutParams();
        lp.width = width;
        mHeaderContentHolder.setLayoutParams(lp);
        mHeaderContentHolder.requestLayout();
        lp = mTabLayout.getLayoutParams();
        lp.width = width;
        mTabLayout.setLayoutParams(lp);
        mTabLayout.setMinimumWidth(width);
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        mOnlineSequencesAdapter.enableAnimation(false);
    }

    protected abstract RecyclerView.LayoutManager getLayoutManager(boolean portrait);

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    protected void refreshContent() {
        mSequencesRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mCurrentPageToList = 1;
                mPaymentCurrentPageToList = 1;
                mMaxNumberOfResults = 0;
                mOnlineSequences.clear();
                mPaymentsList.clear();
                if (mPaymentAdapter != null) {
                    mPaymentAdapter.notifyDataSetChanged();
                }
                if (mOnlineSequencesAdapter != null) {
                    mOnlineSequencesAdapter.notifyDataSetChanged();
                    mOnlineSequencesAdapter.resetLastAnimatedItem();
                }
                BackgroundThreadPool.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "refreshContent ");
                        requestDetails();
                        loadMoreResults();
                        startRefreshing();
                    }
                });
            }
        });
    }

    protected abstract void requestDetails();

    private void startRefreshing() {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setRefreshing(true);
                        if (mCancelTask != null) {
                            mCancelTask.cancel();
                        }
                        if (mNotifyTask != null) {
                            mNotifyTask.cancel();
                        }

                        mCancelTask = new TimerTask() {
                            @Override
                            public void run() {
                                mCancelRunnable.run();
                            }
                        };
                        mNotifyTask = new TimerTask() {
                            @Override
                            public void run() {
                                mNotifyRunnable.run();
                            }
                        };
                        mTimer.schedule(mNotifyTask, 10000);
                        mTimer.schedule(mCancelTask, 15000);
                    }
                }
            });
        }
    }

    protected void stopRefreshing() {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mSwipeRefreshLayout != null) {
                        if (mCancelTask != null) {
                            mCancelTask.cancel();
                        }
                        if (mNotifyTask != null) {
                            mNotifyTask.cancel();
                        }
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }
            });
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupViews(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BackgroundThreadPool.post(new Runnable() {
            @Override
            public void run() {
                mSequencesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView,
                                           int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);

                        RecyclerView.LayoutManager lm = mSequencesRecyclerView.getLayoutManager();
                        if (lm instanceof LinearLayoutManager) {
                            mTotalItemCount = lm.getItemCount() - 1;
                            mLastVisibleItem = ((LinearLayoutManager) lm).findLastCompletelyVisibleItemPosition();
                            if (!mLoading && mTotalItemCount == mLastVisibleItem && mTotalItemCount < mMaxNumberOfResults) {
                                // End has been reached
                                mSwipeRefreshLayout.setRefreshing(true);
                                loadMoreResults();
                                mLoading = true;
                            }
                        }
                    }
                });
                displayCachedStats();
                refreshContent();
            }
        });
    }

    protected abstract void displayCachedStats();

    protected abstract void loadMoreResults();

    @Override
    public void setSource(Object extra) {
        //no source for default profile fragment
    }
}

