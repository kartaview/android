package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.ui.loader.LoadingScreen;
import com.telenav.osv.data.sequence.model.Sequence;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.data.user.model.User;
import com.telenav.osv.item.Payment;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.ui.custom.ProgressImageView;
import com.telenav.osv.ui.list.PaymentAdapter;
import com.telenav.osv.ui.list.SequenceAdapter;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * fragment holding the ui for the user's data
 * Created by adrianbostan on 11/07/16.
 */
public abstract class ProfileFragment extends DisplayFragment implements AppBarLayout.OnOffsetChangedListener {

    public final static String TAG = "ProfileFragment";

    public static final String K_DRIVER_BYOD20_MAX_PAYRATE = "byod20MaxPayRate";

    /**
     * preference name
     */
    public static final String PREFS_NAME = "osvMyProfileAppPrefs";

    public static final String PAYMENT_MODEL_VERSION_20 = "2.0";

    public static final String PAYMENT_MODEL_VERSION_10 = "1.0";

    public static final String K_DRIVER_PAYMENT_MODEL_VERSION = "driverPaymentModelVersion";

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
                activity.runOnUiThread(() -> {
                    if (mSwipeRefreshLayout != null) {
                        if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MY_PROFILE) {
                            activity.showSnackBar(R.string.loading_too_long, Snackbar.LENGTH_LONG);
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

    /**
     * Container for Rx disposables which will automatically dispose them after execute.
     */
    @NonNull
    private CompositeDisposable compositeDisposable;

    /**
     * Instance for {@code UserDataSource} which represents the user repository.
     */
    private UserDataSource userRepository;

    /**
     * The loading indicator container
     */
    private ViewGroup loadingIndicatorContainer;

    /**
     * The {@code LoadingScreen} displayed before the network requests.
     */
    private LoadingScreen loadingScreen;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = Injection.provideUserRepository(getContext().getApplicationContext());
        compositeDisposable = new CompositeDisposable();
    }

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
        mSwipeRefreshLayout = view.findViewById(R.id.profile_swipe_refresh_layout);
        mHeaderContentHolder = view.findViewById(R.id.header_content_holder);
        appBar = view.findViewById(R.id.profile_appbar);
        mProfileImage = view.findViewById(R.id.profile_image);
        toolbar = view.findViewById(R.id.profile_toolbar);
        collapsingToolbar = view.findViewById(R.id.profile_collapsing_toolbar);
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshContent);
        mMaxScrollSize = appBar.getTotalScrollRange();
        appBar.addOnOffsetChangedListener(this);
        Drawable upArrow = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_back_white, null);
        toolbar.setNavigationIcon(upArrow);
        toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());
        mOnlineSequencesAdapter = new SequenceAdapter(mOnlineSequences, activity, !(this instanceof NearbyFragment));
        mOnlineSequencesAdapter.enablePoints(appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true));
        mOnlineSequencesAdapter.enableDriverStats(false);
        loadingIndicatorContainer = view.findViewById(R.id.loading_fragment_profile_container);
        loadingScreen = new LoadingScreen(R.layout.generic_loader, R.id.text_view_generic_loader_message, R.string.profile_fetching_data);
        setupViews(activity.isPortrait());
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showLoadingIndicator();
        BackgroundThreadPool.post(new Runnable() {

            @Override
            public void run() {
                mSequencesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
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
                refreshContent();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupViews(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void setSource(Object extra) {
        //no source for default profile fragment
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
            RecyclerView.LayoutManager lm = mSequencesRecyclerView.getLayoutManager();
            int position = 0;
            if (lm instanceof LinearLayoutManager) {
                position = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
            }
            mSequencesRecyclerView.setLayoutManager(getLayoutManager(portrait));
            int bottomPadding = (int) resources.getDimension(R.dimen.sequence_list_padding_bottom);
            mSequencesRecyclerView.setPadding(sidePadding, 0, sidePadding, bottomPadding);
            mSequencesRecyclerView.getLayoutManager().scrollToPosition(position);
        }
        int width = (int) Utils.dpToPx(activity, activity.getResources().getConfiguration().smallestScreenWidthDp);
        ViewGroup.LayoutParams lp = mHeaderContentHolder.getLayoutParams();
        lp.width = width;
        mHeaderContentHolder.setLayoutParams(lp);
        mHeaderContentHolder.requestLayout();
        lp.width = width;
        mOnlineSequencesAdapter.enableAnimation(false);
    }

    protected abstract RecyclerView.LayoutManager getLayoutManager(boolean portrait);

    protected void refreshContent() {
        mSequencesRecyclerView.post(() -> {
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
            BackgroundThreadPool.post(() -> {
                Log.d(TAG, "refreshContent ");
                requestDetails();
                loadMoreResults();
                startRefreshing();
            });
        });
    }

    protected abstract void requestDetails();

    protected void stopRefreshing() {
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (mSwipeRefreshLayout != null) {
                    if (mCancelTask != null) {
                        mCancelTask.cancel();
                    }
                    if (mNotifyTask != null) {
                        mNotifyTask.cancel();
                    }
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    protected void displayCachedStats(Consumer<User> onSuccess, Consumer<Throwable> onError, Action onComplete) {
        compositeDisposable.clear();
        Disposable disposable = userRepository
                .getUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onSuccess, onError, onComplete);
        compositeDisposable.add(disposable);
    }

    protected abstract void loadMoreResults();

    protected void showLoadingIndicator() {
        appBar.setVisibility(View.GONE);
        loadingScreen.show(loadingIndicatorContainer, 0);
    }

    protected void hideLoadingIndicator() {
        appBar.setVisibility(View.VISIBLE);
        loadingScreen.hide(loadingIndicatorContainer);
    }

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
}

