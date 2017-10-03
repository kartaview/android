package com.telenav.osv.ui.fragment;

import java.util.Currency;
import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.network.DriverData;
import com.telenav.osv.item.network.PaymentCollection;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.item.view.profile.DriverProfileData;
import com.telenav.osv.item.view.tracklist.StatsData;
import com.telenav.osv.item.view.tracklist.StatsDataFactory;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.ui.list.PaymentAdapter;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * fragment holding the ui for the user's data
 * Created by kalmanbencze on 7/07/17.
 */
public class ByodProfileFragment extends ProfileFragment implements TabLayout.OnTabSelectedListener {

    public static final String TAG = "ByodProfileFragment";

    private static final int DATA_SOURCE_TRACKS = 0;

    private static final int DATA_SOURCE_PAYMENTS = 1;

    @Inject
    UserDataManager mUserDataManager;

    private View mInfoView;

    private TextView mDistanceTextView;

    private TextView mRateTextView;

    private TextView mValueTextView;

    private int mCurrentDataSource = DATA_SOURCE_TRACKS;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            collapsingToolbar
                    .setExpandedTitleMarginBottom((int) activity.getResources().getDimension(R.dimen.profile_driver_header_title_margin_bottom));
            mTabLayout.addOnTabSelectedListener(this);
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
            mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    mMaxNumberOfResults = 0;
                    mCurrentDataSource = tab.getPosition();
                    boolean portrait = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                    mOnlineSequencesAdapter.makeHeaderScrollable(portrait);
                    mSequencesRecyclerView.setLayoutManager(portrait ? mPortraitLayoutManager :
                            (mCurrentDataSource == DATA_SOURCE_TRACKS ? mLandscapeLayoutManager :
                                    mPortraitLayoutManager));
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
            mPaymentAdapter = new PaymentAdapter(mPaymentsList, activity, valueFormatter);
        }
        mOnlineSequencesAdapter.enableDriverStats(true);
        mOnlineSequencesAdapter.showValue(true);
        displayCachedStats();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSequencesRecyclerView.setAdapter(mCurrentDataSource == DATA_SOURCE_TRACKS ? mOnlineSequencesAdapter : mPaymentAdapter);
    }

    @Override
    protected void setupViews(boolean portrait) {
        super.setupViews(portrait);
        mOnlineSequencesAdapter.makeHeaderScrollable(portrait);
    }

    @Override
    protected RecyclerView.LayoutManager getLayoutManager(boolean portrait) {
        return portrait ? mPortraitLayoutManager :
                (mCurrentDataSource == DATA_SOURCE_PAYMENTS ? mPortraitLayoutManager : mLandscapeLayoutManager);
    }

    @Override
    protected void requestDetails() {
        mUserDataManager.getDriverProfileDetails(new NetworkResponseDataListener<DriverData>() {

            @Override
            public void requestFailed(int status, DriverData details) {
                activity.showSnackBar(getString(R.string.no_internet_connection_detected), Snackbar.LENGTH_LONG);
                Log.d(TAG, "requestDriverDetails: " + details);
            }

            @Override
            public void requestFinished(int status, final DriverData driverData) {
                Log.d(TAG, "requestDriverDetails: " + " status - > " + status + " result - > " + driverData);
                if (driverData != null) {
                    mHandler.post(() -> {
                        DriverProfileData profileData = new DriverProfileData();
                        profileData.setName(driverData.getDisplayName());
                        profileData.setUsername(appPrefs.getUserName());
                        profileData.setPhotoUrl(appPrefs.getUserPhotoUrl());
                        profileData.setCurrency(driverData.getCurrency());
                        profileData.setCurrentAccepted(driverData.getCurrentAcceptedDistance());
                        profileData.setValue(driverData.getCurrentPaymentValue());
                        profileData.setRate(driverData.getCurrentPayRate());
                        profileData.setPaymentValue(driverData.getTotalPaidValue());
                        StatsData stats = StatsDataFactory.create(activity, valueFormatter
                                , driverData.getTotalAcceptedDistance()
                                , driverData.getTotalRejectedDistance()
                                , driverData.getTotalObdDistance()
                                , (int) driverData.getTotalTracks()
                                , (int) driverData.getTotalPhotos());
                        fillUserInformation(profileData, stats);
                    });
                }
            }
        });
    }

    protected void displayCachedStats() {

        mHandler.post(
                () -> {
                    DriverProfileData profileData = new DriverProfileData();
                    profileData.setName(appPrefs.getUserDisplayName());
                    profileData.setUsername(appPrefs.getUserName());
                    profileData.setPhotoUrl(appPrefs.getUserPhotoUrl());

                    SharedPreferences prefs = activity.getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
                    profileData.setCurrency(prefs.getString(ProfileFragment.K_DRIVER_CURRENCY, "USD"));
                    profileData.setCurrentAccepted(prefs.getFloat(ProfileFragment.K_DRIVER_CURRENT_ACCEPTED_DISTANCE, 0));
                    profileData.setRate(prefs.getFloat(ProfileFragment.K_DRIVER_CURRENT_PAYRATE, 0));
                    profileData.setValue(prefs.getFloat(ProfileFragment.K_DRIVER_CURRENT_VALUE, 0));
                    profileData.setPaymentValue(prefs.getFloat(ProfileFragment.K_DRIVER_TOTAL_VALUE, 0));

                    StatsData stats = StatsDataFactory.create(activity, valueFormatter
                            , prefs.getFloat(ProfileFragment.K_DRIVER_TOTAL_ACCEPTED_DISTANCE, 0)
                            , prefs.getFloat(ProfileFragment.K_DRIVER_TOTAL_REJECTED_DISTANCE, 0)
                            , prefs.getFloat(ProfileFragment.K_DRIVER_TOTAL_OBD_DISTANCE, 0)
                            , (int) prefs.getFloat(ProfileFragment.K_DRIVER_TRACKS_COUNT, 0)
                            , (int) prefs.getFloat(ProfileFragment.K_DRIVER_PHOTOS_COUNT, 0));
                    fillUserInformation(profileData, stats);
                });
    }

    protected void loadMoreResults() {
        Log.d(TAG, "loadMoreResults: ");
        BackgroundThreadPool.post(() -> {
            if (mCurrentDataSource == DATA_SOURCE_PAYMENTS) {
                mUserDataManager.listDriverPayments(new NetworkResponseDataListener<PaymentCollection>() {

                    @Override
                    public void requestFailed(int status, PaymentCollection details) {
                        mHandler.post(() -> {
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
                                    Log.d(TAG, Log.getStackTraceString(e));
                                }
                            }
                            mLoading = false;

                            mHandler.post(() -> {
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
                mUserDataManager.listDriverSequences(new NetworkResponseDataListener<TrackCollection>() {

                    @Override
                    public void requestFailed(int status, TrackCollection details) {
                        mHandler.post(() -> {
                            //                                mCurrentPageToList--;
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
                                    Log.d(TAG, Log.getStackTraceString(e));
                                }
                            }
                            mLoading = false;

                            mHandler.post(() -> {
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

            mProfileImage.animate().scaleY(0).scaleX(0).alpha(0.0f)
                    //                    .translationYBy(-200)
                    .setDuration(200).start();
            mInfoView.animate().alpha(0.0f)
                    //                    .translationYBy(-200)
                    .setDuration(200).start();
        }

        if (percentage <= PERCENTAGE_TO_ANIMATE_AVATAR && !mIsAvatarShown) {
            mIsAvatarShown = true;

            mProfileImage.animate().scaleY(1).scaleX(1).alpha(1.0f).setDuration(200)
                    //                    .translationYBy(200)
                    .start();
            mInfoView.animate().alpha(1.0f).setDuration(200)
                    //                    .translationYBy(200)
                    .start();
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    @Subscribe
    public void onRefreshNeeded(SequencesChangedEvent event) {
        if (event.online) {
            refreshContent();
        }
    }

    private void fillUserInformation(DriverProfileData profileData, StatsData stats) {
        collapsingToolbar.setTitle(profileData.getName());
        if (!"".equals(profileData.getPhotoUrl())) {
            Glide.with(activity).load(profileData.getPhotoUrl())
                    .centerCrop()
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .signature(new StringSignature("profile " + profileData.getUsername() + "-" + profileData.getPhotoUrl()))
                    .priority(Priority.IMMEDIATE)
                    .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                    .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                    .listener(MainActivity.mGlideRequestListener)
                    .into(mProfileImage);
        }
        mProfileImage.setInstantProgress(0.01f);
        String[] currentAccepted = valueFormatter.formatDistanceFromKiloMeters(profileData.getCurrentAccepted());
        mDistanceTextView.setText(currentAccepted[0] + " " + currentAccepted[1]);
        mValueTextView.setText(valueFormatter.formatMoney(profileData.getValue()));
        String currency = profileData.getCurrency();
        try {
            currency = Currency.getInstance(currency).getSymbol();
        } catch (IllegalArgumentException ignored) {
            Log.d(TAG, Log.getStackTraceString(ignored));
        }
        mRateTextView.setText(valueFormatter.formatMoney(profileData.getRate()) + " " + currency + getString(R.string.partial_rate_km_label));
        mOnlineSequencesAdapter.refreshDetails(stats);
        mPaymentAdapter.setTotalPayment(profileData.getPaymentValue(), profileData.getCurrency());
    }
}

