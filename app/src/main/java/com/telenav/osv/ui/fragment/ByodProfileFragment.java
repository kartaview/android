package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import org.greenrobot.eventbus.Subscribe;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.ui.loader.LoadingScreen;
import com.telenav.osv.data.sequence.model.Sequence;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.data.user.model.User;
import com.telenav.osv.data.user.model.details.BaseUserDetails;
import com.telenav.osv.data.user.model.details.driver.DriverDetails;
import com.telenav.osv.data.user.model.details.driver.DriverPayment;
import com.telenav.osv.data.user.model.details.driver.DriverToBePaid;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.Payment;
import com.telenav.osv.item.network.DriverData;
import com.telenav.osv.item.network.PaymentCollection;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.network.payrate.PayRateInteractor;
import com.telenav.osv.network.payrate.PayRateInteractorImpl;
import com.telenav.osv.network.payrate.model.PayRateData;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.ui.custom.ProgressImageView;
import com.telenav.osv.ui.list.PaymentAdapter;
import com.telenav.osv.ui.list.SequenceAdapter;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.StringUtils;
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
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * fragment holding the ui for the user's data
 * Created by kalmanbencze on 7/07/17.
 */
public class ByodProfileFragment extends OSVFragment implements AppBarLayout.OnOffsetChangedListener, View.OnClickListener {

    private static final int NUMBER_OF_ITEMS_PER_PAGE = 30;

    private static final int PERCENTAGE_TO_ANIMATE_AVATAR = 5;

    private static final String TAG = "ByodProfileFragment";

    private static final int DATA_SOURCE_TRACKS = 0;

    private static final int DATA_SOURCE_PAYMENTS = 1;

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

    protected Handler mainThreadHandler = new Handler(Looper.getMainLooper());

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

    private ArrayList<Payment> mPaymentsList = new ArrayList<>();

    private PaymentAdapter mPaymentAdapter;

    private View mInfoView;

    private TextView mDistanceTextView;

    private TextView mRateTextView;

    private TextView mValueTextView;

    private ImageView payRateInfoImage;

    private View byodPayRateInfoContainer;

    private int mCurrentDataSource = DATA_SOURCE_TRACKS;

    private int mLastVisibleItem;

    private int mTotalItemCount;

    private Timer mTimer = new Timer();

    private double maxPayRate = 0;

    private Runnable mCancelRunnable = () -> {
        if (mSwipeRefreshLayout != null) {
            activity.runOnUiThread(() -> {
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setRefreshing(false);
                    if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MY_PROFILE) {
                        activity.showSnackBar(R.string.something_wrong_try_again, Snackbar.LENGTH_LONG);
                    }
                }
            });
        }
    };

    private Runnable mNotifyRunnable = () -> {
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (mSwipeRefreshLayout != null) {
                    if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MY_PROFILE) {
                        activity.showSnackBar(R.string.loading_too_long, Snackbar.LENGTH_LONG);
                    }
                }
            });
        }
    };

    private TimerTask mCancelTask = new TimerTask() {

        @Override
        public void run() {
            mCancelRunnable.run();
        }
    };

    private TimerTask mNotifyTask = new TimerTask() {

        @Override
        public void run() {
            mNotifyRunnable.run();
        }
    };

    private LinearLayout mHeaderContentHolder;

    private PayRateData payRateData;

    private SharedPreferences driverProfilePrefs;

    /**
     * Instance to rx disposable for getting the user.
     */
    @NonNull
    private Disposable getUserDisposable;

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

    /**
     * Instance of {@code PayRateInteractor} for performing a pay rate request to the server.
     */
    private PayRateInteractor payRateInteractor;

    /**
     * Disposable object for pay rate request.
     */
    private Disposable payRateDisposable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = Injection.provideUserRepository(getContext().getApplicationContext());
        activity = (MainActivity) getActivity();
        appPrefs = activity.getApp().getAppPrefs();
        payRateInteractor = new PayRateInteractorImpl(appPrefs.getIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE), appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN));
        driverProfilePrefs = activity.getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_byod, null);
        mPortraitLayoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        mLandscapeLayoutManager = new GridLayoutManager(activity, 2);
        mLandscapeLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position != 0 ? 1 : 2;
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
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshContent);
        mMaxScrollSize = appBar.getTotalScrollRange();
        appBar.addOnOffsetChangedListener(this);
        Drawable upArrow = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_back_white, null);
        toolbar.setNavigationIcon(upArrow);
        toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());
        mOnlineSequencesAdapter = new SequenceAdapter(mOnlineSequences, activity, true);
        mOnlineSequencesAdapter.enablePoints(appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true));
        mOnlineSequencesAdapter.enableDriverStats(false);
        mOnlineSequencesAdapter.setTrackIdVisibility(true);
        setupViews(activity.isPortrait());

        collapsingToolbar.setExpandedTitleMarginBottom((int) activity.getResources().getDimension(R.dimen.profile_driver_header_title_margin_bottom));
        mInfoView = view.findViewById(R.id.info_view);
        mDistanceTextView = view.findViewById(R.id.profile_info_distance);
        mRateTextView = view.findViewById(R.id.profile_info_rate);
        mValueTextView = view.findViewById(R.id.profile_info_value);
        mInfoView.setVisibility(View.VISIBLE);
        mTabLayout.setVisibility(View.VISIBLE);
        TabLayout.Tab mTracksTab = mTabLayout.getTabAt(0);
        TabLayout.Tab mPaymentsTab = mTabLayout.getTabAt(1);
        if (mCurrentDataSource == DATA_SOURCE_TRACKS) {
            if (mTracksTab != null) {
                mTracksTab.select();
            }
        } else {
            if (mPaymentsTab != null) {
                mPaymentsTab.select();
            }
        }

        payRateInfoImage = view.findViewById(R.id.image_pay_rate_byod_info);
        byodPayRateInfoContainer = view.findViewById(R.id.container_byod_profile_pay_rate);

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mMaxNumberOfResults = 0;
                mCurrentDataSource = tab.getPosition();
                boolean portrait = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                mOnlineSequencesAdapter.makeHeaderScrollable(portrait);
                if (mCurrentDataSource == DATA_SOURCE_TRACKS) {
                    mSequencesRecyclerView.setLayoutManager(portrait ? mPortraitLayoutManager : mLandscapeLayoutManager);
                } else {
                    mSequencesRecyclerView.setLayoutManager(mPortraitLayoutManager);
                }
                mSequencesRecyclerView.setAdapter(mCurrentDataSource == DATA_SOURCE_TRACKS ? mOnlineSequencesAdapter : mPaymentAdapter);
                if ((mCurrentDataSource == DATA_SOURCE_PAYMENTS && mPaymentCurrentPageToList == 1) ||
                        (mCurrentDataSource == DATA_SOURCE_TRACKS && mCurrentPageToList == 1)) {
                    refreshContent();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        mPaymentAdapter = new PaymentAdapter(mPaymentsList, activity);
        mOnlineSequencesAdapter.enableDriverStats(true);

        loadingIndicatorContainer = view.findViewById(R.id.loading_fragment_profile_byod_container);
        loadingScreen = new LoadingScreen(R.layout.generic_loader, R.id.text_view_generic_loader_message, R.string.profile_fetching_data);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showLoadingIndicator();
        BackgroundThreadPool.post(() -> {

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
        });
        mSequencesRecyclerView.setAdapter(mCurrentDataSource == DATA_SOURCE_TRACKS ? mOnlineSequencesAdapter : mPaymentAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.register(this);
    }

    @Override
    public void onStop() {
        EventBus.unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (payRateDisposable != null && !payRateDisposable.isDisposed()) {
            payRateDisposable.dispose();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupViews(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (mMaxScrollSize == 0) {
            mMaxScrollSize = appBarLayout.getTotalScrollRange();
        }

        if (mMaxScrollSize == 0) {
            return;
        }
        int percentage = (Math.abs(i)) * 100 / mMaxScrollSize;

        if (percentage >= PERCENTAGE_TO_ANIMATE_AVATAR && mIsAvatarShown) {
            mIsAvatarShown = false;

            mProfileImage.animate()
                    .scaleY(0)
                    .scaleX(0)
                    .alpha(0.0f)
                    .setDuration(200)
                    .start();

            mInfoView.animate()
                    .alpha(0.0f)
                    .setDuration(200)
                    .start();
        }

        if (percentage <= PERCENTAGE_TO_ANIMATE_AVATAR && !mIsAvatarShown) {
            mIsAvatarShown = true;

            mProfileImage.animate()
                    .scaleY(1)
                    .scaleX(1)
                    .alpha(1.0f)
                    .setDuration(200)
                    .start();

            mInfoView.animate()
                    .alpha(1.0f)
                    .setDuration(200)
                    .start();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.container_byod_profile_pay_rate:
                showPayRatesDialog();
                break;
            default:
        }
    }

    @Subscribe
    public void onRefreshNeeded(SequencesChangedEvent event) {
        if (event.online) {
            refreshContent();
        }
    }

    private void showPayRatesDialog() {
        ByodPayRatesFragment payRatesFragment = new ByodPayRatesFragment();
        payRatesFragment.setPayRateData(payRateData);
        payRatesFragment.show(getFragmentManager(), ByodPayRatesFragment.TAG);
    }

    private void setCorrectStateOfPayRateView(@Nullable String paymentModel) {
        if (ProfileFragment.PAYMENT_MODEL_VERSION_10.equals(paymentModel)) {
            payRateInfoImage.setVisibility(View.GONE);
            byodPayRateInfoContainer.setOnClickListener(null);
        } else {
            payRateInfoImage.setVisibility(View.VISIBLE);
            byodPayRateInfoContainer.setOnClickListener(this);
        }
    }

    private void setupViews(boolean portrait) {
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
        lp = mTabLayout.getLayoutParams();
        lp.width = width;
        mTabLayout.setLayoutParams(lp);
        mTabLayout.setMinimumWidth(width);
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        mOnlineSequencesAdapter.enableAnimation(false);
        mOnlineSequencesAdapter.makeHeaderScrollable(portrait);
    }

    private void requestDetails() {
        activity.getUserDataManager().getDriverProfileDetails(new NetworkResponseDataListener<DriverData>() {

            @Override
            public void requestFinished(int status, final DriverData driverData) {
                Log.d(TAG, "requestDriverDetails: " + " status - > " + status + " result - > " + driverData);
                if (driverData != null) {
                    if (ProfileFragment.PAYMENT_MODEL_VERSION_20.equals(driverData.getPaymentModelVersion())) {
                        fetchPayRatesData(driverData);
                    } else if (isAdded()) {
                        displayUserInfo();
                    }
                } else {
                    showDataFetchErrorSnackBar();
                }
            }

            @Override
            public void requestFailed(int status, DriverData details) {
                showDataFetchErrorSnackBar();
                Log.d(TAG, "requestDriverDetails: " + details);
            }
        });
    }

    private void fetchPayRatesData(final DriverData freshDriverData) {
        payRateDisposable = payRateInteractor.getDriverPayRateDetails().observeOn(AndroidSchedulers.mainThread()).subscribe(
                payRate -> {
                    Log.d(TAG, "requestDriverDetails: " + " result - > " + freshDriverData);
                    if (payRate != null) {
                        PayRateData payRateData = payRate.getPayRateData();
                        if (payRateData != null) {
                            this.payRateData = payRate.getPayRateData();
                            if (payRateData.getPayRates() != null && !payRateData.getPayRates().isEmpty()) {
                                maxPayRate = Utils.getMaxPayRate(payRateData);
                            }
                        }
                        if (isAdded()) {
                            displayUserInfo();
                        }
                    } else {
                        showDataFetchErrorSnackBar();
                    }
                },
                e -> {
                    showDataFetchErrorSnackBar();
                    Log.e(TAG, String.format("requestDriverDetailsError: %s", e.getMessage()));
                }
        );
    }

    private void showDataFetchErrorSnackBar() {
        activity.showSnackBar("An unexpected error occurred when trying to fetch updated data. Please verify your " +
                "internet connection, then try again.", Snackbar.LENGTH_LONG);
    }

    @NonNull
    private String formatPayRatePerKm(final String currency, double maxPayRate) {
        return currency + FormatUtils.formatMoneyForPreferredDistanceUnit(getContext(), maxPayRate, true)
                + FormatUtils.getDistanceUnitLabel(getContext(), FormatUtils.SEPARATOR_SLASH, false);
    }

    /**
     * Displays the driver information from {@code UserDataSource}.
     */
    private void displayUserInfo() {
        hideLoadingIndicator();
        getUserDisposable = userRepository
                .getUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        //onSuccess
                        user -> {
                            Log.d(TAG, String.format("displayUserInfo. Status: success. ID: %s. Message: User found.", user.getID()));
                            displayDriverInfo(user);
                        },
                        //onError
                        throwable -> Log.d(TAG, String.format("displayUserInfo. Status: error. Message: %s", throwable.getMessage())),
                        //OnComplete
                        () -> Log.d(TAG, "displayUserInfo. Status: complete. Message: User not found.")
                );
    }

    /**
     * Displays the driver information.
     * @param user the driver data which is displayed.
     */
    private void displayDriverInfo(User user) {
        collapsingToolbar.setTitle(user.getDisplayName());
        displayProfileImage(user.getUserName());
        if (user.getDetails() != null && user.getDetails().getType() == BaseUserDetails.UserDetailsTypes.DRIVER) {
            DriverDetails driverDetails = (DriverDetails) user.getDetails();
            displayToBePaidInformation(driverDetails.getDriverPayment());
            displayDriverCompleteInfo(driverDetails);
            displayDriverPaymentInformation(driverDetails.getDriverPayment());
        }
    }

    /**
     * Displays driver complete information meaning it will display total values for distances and tracks with photos information.
     * @param driverDetails the driver details.
     */
    private void displayDriverCompleteInfo(DriverDetails driverDetails) {
        final String[] accepted = FormatUtils.formatDistanceFromKiloMeters(activity, driverDetails.getDistance(), FormatUtils.FORMAT_ONE_DECIMAL);
        final String[] rejected = FormatUtils.formatDistanceFromKiloMeters(activity, driverDetails.getRejectedDistance(), FormatUtils.FORMAT_ONE_DECIMAL);
        final String[] obdDistance = FormatUtils.formatDistanceFromKiloMeters(activity, driverDetails.getObdDistance(), FormatUtils.FORMAT_ONE_DECIMAL);
        final String tracks = FormatUtils.formatNumber(driverDetails.getTracksCount());
        final String photos = FormatUtils.formatNumber(driverDetails.getPhotosCount());

        mOnlineSequencesAdapter.refreshDetails(accepted, rejected, obdDistance, photos, tracks);
    }

    /**
     * Display the driver complete payment information, i.e total paid value with the currency symbol selected.
     * @param driverPayment the information related to driver payment.
     */
    private void displayDriverPaymentInformation(DriverPayment driverPayment) {
        mPaymentAdapter.setTotalPayment(driverPayment.getPaidValue(), driverPayment.getCurrencySymbol());
        setCorrectStateOfPayRateView(driverPayment.getPaymentModel());
    }

    /**
     * Displays the driver profile picture. This is performed using {@link Glide}.
     * @param username the username used in {@code Glide} signature.
     */
    private void displayProfileImage(String username) {
        String photoUrl = appPrefs.getStringPreference(PreferenceTypes.K_USER_PHOTO_URL);
        if (!photoUrl.equals(StringUtils.EMPTY_STRING)) {
            Glide.with(activity).load(photoUrl).centerCrop().dontAnimate().diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false)
                    .signature(new StringSignature("profile " + username + "-" + photoUrl)).priority(Priority.IMMEDIATE)
                    .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                    .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                    .listener(MainActivity.mGlideRequestListener).into(mProfileImage);
        }
        mProfileImage.setInstantProgress(0.01f);
    }

    /**
     * Displays the driver information.
     * @param driverPayment the {@code DriverPayment} information.
     */
    private void displayToBePaidInformation(DriverPayment driverPayment) {
        DriverToBePaid driverToBePaid = driverPayment.getDriverToBePaid();
        String[] currentAccepted = FormatUtils.formatDistanceFromKiloMeters(getContext().getApplicationContext(), driverToBePaid.getAcceptedDistance(), FormatUtils
                .FORMAT_ONE_DECIMAL);
        String currency = driverPayment.getCurrencySymbol();
        String paymentModel = driverPayment.getPaymentModel() == null ? ProfileFragment.PAYMENT_MODEL_VERSION_10 : driverPayment.getPaymentModel();

        mDistanceTextView.setText(String.format("%s %s", currentAccepted[0], currentAccepted[1]));
        mValueTextView.setText(String.format("%s%s", currency, driverToBePaid.getPaidValue()));
        if (ProfileFragment.PAYMENT_MODEL_VERSION_20.equals(paymentModel)) {
            String rate = formatPayRatePerKm(currency, maxPayRate);
            mRateTextView.setText(String.format(getString(R.string.byod_pay_rate_up_to), rate));
        } else {
            mRateTextView.setText(formatPayRatePerKm(currency, driverToBePaid.getPaidValue()));
        }
    }

    private void loadMoreResults() {
        Log.d(TAG, "loadMoreResults: ");
        BackgroundThreadPool.post(() -> {

            if (mCurrentDataSource == DATA_SOURCE_PAYMENTS) {
                activity.getUserDataManager().listDriverPayments(new NetworkResponseDataListener<PaymentCollection>() {

                    @Override
                    public void requestFailed(int status, PaymentCollection details) {
                        mainThreadHandler.post(() -> {
                            if (mPaymentsList.isEmpty()) {
                                mPaymentAdapter.setOnline(Utils.isInternetAvailable(activity));
                            }
                            mPaymentAdapter.notifyDataSetChanged();
                            stopRefreshing();
                        });
                    }

                    @Override
                    public void requestFinished(int status, final PaymentCollection collectionData) {
                        BackgroundThreadPool.post(() -> {

                            if (collectionData != null) {
                                try {
                                    mPaymentCurrentPageToList++;
                                    mPaymentMaxNumberOfResults = collectionData.getTotalFilteredItems();
                                    mPaymentsList.addAll(collectionData.getPaymentList());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            mLoading = false;

                            mainThreadHandler.post(() -> {

                                //change adapter contents
                                if (mPaymentAdapter != null && collectionData != null) {
                                    mPaymentAdapter.setOnline(Utils.isInternetAvailable(activity));
                                    mPaymentAdapter.notifyDataSetChanged();
                                }
                            });
                            stopRefreshing();
                        });
                    }
                }, mPaymentCurrentPageToList, NUMBER_OF_ITEMS_PER_PAGE);
            } else if (mCurrentDataSource == DATA_SOURCE_TRACKS) {
                activity.getUserDataManager().listDriverSequences(new NetworkResponseDataListener<TrackCollection>() {

                    @Override
                    public void requestFailed(int status, TrackCollection details) {
                        mainThreadHandler.post(() -> {
                            if (mOnlineSequences.isEmpty()) {
                                mOnlineSequencesAdapter.setOnline(Utils.isInternetAvailable(activity));
                            }
                            mOnlineSequencesAdapter.notifyDataSetChanged();
                            stopRefreshing();
                        });
                    }

                    @Override
                    public void requestFinished(int status, final TrackCollection collectionData) {
                        BackgroundThreadPool.post(() -> {

                            if (collectionData != null) {
                                try {
                                    mCurrentPageToList++;
                                    mMaxNumberOfResults = collectionData.getTotalFilteredItems();
                                    mOnlineSequences.addAll(collectionData.getTrackList());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            mLoading = false;

                            mainThreadHandler.post(() -> {
                                //change adapter contents
                                if (mOnlineSequencesAdapter != null) {
                                    mOnlineSequencesAdapter.setOnline(Utils.isInternetAvailable(activity));
                                    mOnlineSequencesAdapter.notifyDataSetChanged();
                                }
                            });
                            stopRefreshing();
                        });
                    }
                }, mCurrentPageToList, NUMBER_OF_ITEMS_PER_PAGE);
            }
        });
    }

    private RecyclerView.LayoutManager getLayoutManager(boolean portrait) {
        return portrait ? mPortraitLayoutManager :
                (mCurrentDataSource == DATA_SOURCE_PAYMENTS ? mPortraitLayoutManager : mLandscapeLayoutManager);
    }

    private void refreshContent() {
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

    private void showLoadingIndicator() {
        appBar.setVisibility(View.GONE);
        loadingScreen.show(loadingIndicatorContainer, 0);
    }

    private void hideLoadingIndicator() {
        appBar.setVisibility(View.VISIBLE);
        loadingScreen.hide(loadingIndicatorContainer);
    }

    private void stopRefreshing() {
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

    private void startRefreshing() {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.post(() -> {

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
            });
        }
    }
}

