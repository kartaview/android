package com.telenav.osv.ui.fragment;

import org.greenrobot.eventbus.Subscribe;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * fragment holding the ui for the user's data
 * Created by kalmanbencze on 7/07/17.
 */
public class SimpleProfileFragment extends ProfileFragment {

    public final static String TAG = "SimpleProfileFragment";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            collapsingToolbar.setExpandedTitleMarginBottom((int) activity.getResources().getDimension(R.dimen.profile_user_header_title_margin_bottom));
        }
        return view;
    }

    @Override
    protected RecyclerView.LayoutManager getLayoutManager(boolean portrait) {
        return portrait ? mPortraitLayoutManager : mLandscapeLayoutManager;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSequencesRecyclerView.setAdapter(mOnlineSequencesAdapter);
    }

    protected void displayCachedStats() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final String photos = prefs.getString(K_TOTAL_PHOTOS, "");
        final String tracks = prefs.getString(K_TOTAL_TRACKS, "");
        final String distance = prefs.getString(K_TOTAL_DISTANCE, "");
        final String obdDistance = prefs.getString(K_OBD_DISTANCE, "");
        final String[] totalDistanceFormatted, obdDistanceFormatted;
        if (distance.equals("")) {
            return;
        }
        double totalDistanceNum = 0;
        double obdDistanceNum = 0;
        try {
            totalDistanceNum = Double.parseDouble(distance);
        } catch (NumberFormatException e) {
            Log.d(TAG, "displayCachedStats: " + Log.getStackTraceString(e));
        }
        try {
            obdDistanceNum = Double.parseDouble(obdDistance);
        } catch (NumberFormatException e) {
            Log.d(TAG, "displayCachedStats: " + Log.getStackTraceString(e));
        }
        totalDistanceFormatted = Utils.formatDistanceFromKiloMeters(activity, totalDistanceNum);
        obdDistanceFormatted = Utils.formatDistanceFromKiloMeters(activity, obdDistanceNum);
        final String photoUrl = appPrefs.getStringPreference(PreferenceTypes.K_USER_PHOTO_URL);
        final String username = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
        final String name = appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME);
        fillUserInformation(name, username, photoUrl, totalDistanceFormatted, obdDistanceFormatted, photos, tracks);
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

    protected void loadMoreResults() {
        BackgroundThreadPool.post(new Runnable() {
            @Override
            public void run() {
                activity.getUserDataManager().listSequences(new NetworkResponseDataListener<TrackCollection>() {

                    @Override
                    public void requestFailed(int status, TrackCollection details) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
//                                mCurrentPageToList--;
                                if (mOnlineSequences.isEmpty()) {
                                    mOnlineSequencesAdapter.setOnline(Utils.isInternetAvailable(activity));
                                }
                                mOnlineSequencesAdapter.notifyDataSetChanged();
                                stopRefreshing();
                            }
                        });
                    }

                    @Override
                    public void requestFinished(int status, final TrackCollection collectionData) {
                        BackgroundThreadPool.post(new Runnable() {
                            @Override
                            public void run() {
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

                                mHandler.post(new Runnable() {
                                    public void run() {
                                        //change adapter contents
                                        if (mOnlineSequencesAdapter != null) {
                                            mOnlineSequencesAdapter.setOnline(Utils.isInternetAvailable(activity));
                                            mOnlineSequencesAdapter.notifyDataSetChanged();
                                        }
                                    }
                                });
                                stopRefreshing();
                            }
                        });

                    }
                }, mCurrentPageToList, NUMBER_OF_ITEMS_PER_PAGE);
            }
        });
    }

    @Override
    protected void requestDetails() {
        activity.getUserDataManager().getUserProfileDetails(new NetworkResponseDataListener<UserData>() {
            @Override
            public void requestFailed(int status, UserData details) {
                activity.showSnackBar("Failed to communicate with server.", Snackbar.LENGTH_LONG);
                displayCachedStats();
                Log.d(TAG, "requestUserDetails: " + " status - > " + status + " details - > " + details);
            }

            @Override
            public void requestFinished(int status, final UserData userData) {
                Log.d(TAG, "requestUserDetails: " + " status - > " + status + " result - > " + userData);
                if (userData != null) {

                    final String[] totalDistanceFormatted = Utils.formatDistanceFromKiloMeters(activity, userData.getTotalDistance());
                    final String[] obdDistanceFormatted = Utils.formatDistanceFromKiloMeters(activity, userData.getObdDistance());
                    String displayName = userData.getDisplayName();
                    String username = userData.getUserName();
                    String totalPhotos = Utils.formatNumber((int) userData.getTotalPhotos());
                    String totalTracks = Utils.formatNumber((int) userData.getTotalTracks());
                    final String photoUrl = appPrefs.getStringPreference(PreferenceTypes.K_USER_PHOTO_URL);
                    fillUserInformation(displayName, username, photoUrl, totalDistanceFormatted, obdDistanceFormatted, totalPhotos, totalTracks);
                }
            }
        });
    }

    private void fillUserInformation(final String name, final String username, final String photoUrl, final String[] totalDistanceFormatted, final String[] obdDistanceFormatted,
                                     final String photos, final String tracks) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                collapsingToolbar.setTitle(name);
                if (!photoUrl.equals("")) {
                    Glide.with(activity)
                            .load(photoUrl)
                            .centerCrop()
                            .dontAnimate()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .skipMemoryCache(false)
                            .signature(new StringSignature("profile " + username + "-" + photoUrl))
                            .priority(Priority.IMMEDIATE)
                            .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                            .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                            .listener(MainActivity.mGlideRequestListener)
                            .into(mProfileImage);
                }
                mProfileImage.setInstantProgress(0.01f);
//                mOnlineSequencesAdapter.notifyDataSetChanged();
                mOnlineSequencesAdapter.refreshDetails(totalDistanceFormatted, new String[]{}, obdDistanceFormatted, photos, tracks);
            }
        });
    }

    @Subscribe
    public void onRefreshNeeded(SequencesChangedEvent event) {
        if (event.online) {
            refreshContent();
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (mMaxScrollSize == 0)
            mMaxScrollSize = appBarLayout.getTotalScrollRange();

        if (mMaxScrollSize == 0)
            return;
        int percentage = (Math.abs(i)) * 100 / mMaxScrollSize;

        if (percentage >= PERCENTAGE_TO_ANIMATE_AVATAR && mIsAvatarShown) {
            mIsAvatarShown = false;

            mProfileImage.animate()
                    .scaleY(0).scaleX(0).alpha(0.0f)
                    .setDuration(200)
                    .start();
        }

        if (percentage <= PERCENTAGE_TO_ANIMATE_AVATAR && !mIsAvatarShown) {
            mIsAvatarShown = true;

            mProfileImage.animate()
                    .scaleY(1).scaleX(1).alpha(1.0f)
                    .setDuration(200)
                    .start();
        }
    }
}

