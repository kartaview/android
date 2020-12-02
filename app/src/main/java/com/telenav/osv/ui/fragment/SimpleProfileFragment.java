package com.telenav.osv.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.data.user.model.User;
import com.telenav.osv.data.user.model.details.BaseUserDetails;
import com.telenav.osv.data.user.model.details.gamification.GamificationDetails;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.StringUtils;
import com.telenav.osv.utils.Utils;

import org.greenrobot.eventbus.Subscribe;

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
            collapsingToolbar
                    .setExpandedTitleMarginBottom((int) activity.getResources().getDimension(R.dimen.profile_user_header_title_margin_bottom));
        }
        mOnlineSequencesAdapter.setTrackIdVisibility(true);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSequencesRecyclerView.setAdapter(mOnlineSequencesAdapter);
    }

    @Override
    protected RecyclerView.LayoutManager getLayoutManager(boolean portrait) {
        return portrait ? mPortraitLayoutManager : mLandscapeLayoutManager;
    }

    @Override
    protected void requestDetails() {
        activity.getUserDataManager().getUserProfileDetails(new NetworkResponseDataListener<UserData>() {

            @Override
            public void requestFailed(int status, UserData details) {
                Log.d(TAG, String.format("requestUserDetails. Status: error. Status code: %s. User data: %s. Message: Request details failed.", status, details));
                activity.showSnackBar(getString(R.string.failed_server_login), Snackbar.LENGTH_LONG);
                Log.d(TAG, "requestUserDetails: " + " status - > " + status + " details - > " + details);
            }

            @Override
            public void requestFinished(int status, final UserData userData) {
                Log.d(TAG, String.format("requestUserDetails. Status: success. Status code: %s. User data: %s. Message: Request details successful.", status, userData));
                displayCachedStats(user -> {
                            Log.d(TAG, "requestDetails. Status: complete. Message: User found.");
                            displayGamificationDetails(user);
                        },
                        throwable -> Log.d(TAG, String.format("requestDetails. Status: error. Message: %s", throwable.getMessage())),
                        () -> Log.d(TAG, "requestDetails. Status: complete. Message: User not found.")
                );
            }
        });
    }

    protected void loadMoreResults() {
        BackgroundThreadPool.post(() -> activity.getUserDataManager().listSequences(new NetworkResponseDataListener<TrackCollection>() {

            @Override
            public void requestFailed(int status, TrackCollection details) {
                mHandler.post(() -> {
                    if (mOnlineSequences.isEmpty()) {
                        mOnlineSequencesAdapter.setOnline(Utils.isInternetAvailable(activity));
                    }
                    mOnlineSequencesAdapter.notifyDataSetChanged();
                    stopRefreshing();
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

                        mHandler.post(() -> {
                            //change adapter contents
                            if (mOnlineSequencesAdapter != null) {
                                mOnlineSequencesAdapter.setOnline(Utils.isInternetAvailable(activity));
                                mOnlineSequencesAdapter.notifyDataSetChanged();
                            }
                        });
                        stopRefreshing();
                    }
                });
            }
        }, mCurrentPageToList, NUMBER_OF_ITEMS_PER_PAGE));
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

            mProfileImage.animate().scaleY(0).scaleX(0).alpha(0.0f).setDuration(200).start();
        }

        if (percentage <= PERCENTAGE_TO_ANIMATE_AVATAR && !mIsAvatarShown) {
            mIsAvatarShown = true;

            mProfileImage.animate().scaleY(1).scaleX(1).alpha(1.0f).setDuration(200).start();
        }
    }

    @Subscribe
    public void onRefreshNeeded(SequencesChangedEvent event) {
        if (event.online) {
            refreshContent();
        }
    }

    /**
     * Displays the gamification profile picture. This is performed using {@link Glide}.
     * @param username the username used in {@code Glide} signature.
     */
    public void displayProfileImage(String username) {
        Log.d(TAG, "displayProfileImage");
        String photoUrl = appPrefs.getStringPreference(PreferenceTypes.K_USER_PHOTO_URL);
        if (!photoUrl.equals(StringUtils.EMPTY_STRING)) {
            Glide.with(activity).load(NetworkUtils.provideGlideUrlWithAuthorizationIfRequired(appPrefs, photoUrl)).centerCrop().dontAnimate().diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false)
                    .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                    .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                    .listener(MainActivity.mGlideRequestListener).into(mProfileImage);
        }
        mProfileImage.setInstantProgress(0.01f);
    }

    /**
     * Displays driver complete information meaning it will display total values for distances and tracks with photos information.
     * @param gamificationDetails the gamification details.
     */
    public void displayGamificationInfo(GamificationDetails gamificationDetails) {
        final String tracks = FormatUtils.formatNumber(gamificationDetails.getTracksCount());
        final String photos = FormatUtils.formatNumber(gamificationDetails.getPhotosCount());
        String[] totalDistanceFormatted = FormatUtils.formatDistanceFromKiloMeters(activity, gamificationDetails.getDistance(), FormatUtils.FORMAT_ONE_DECIMAL);
        String[] obdDistanceFormatted = FormatUtils.formatDistanceFromKiloMeters(activity, gamificationDetails.getObdDistance(), FormatUtils.FORMAT_ONE_DECIMAL);

        mOnlineSequencesAdapter.refreshDetails(totalDistanceFormatted, new String[]{}, obdDistanceFormatted, photos, tracks);
    }

    /**
     * Displays the gamification information.
     * @param user the gamification data which is displayed.
     */
    private void displayGamificationDetails(User user) {
        hideLoadingIndicator();
        collapsingToolbar.setTitle(user.getDisplayName());
        displayProfileImage(user.getUserName());
        if (user.getDetails() != null && user.getDetails().getType() == BaseUserDetails.UserDetailsTypes.GAMIFICATION) {
            displayGamificationInfo((GamificationDetails) user.getDetails());
        }
    }
}

