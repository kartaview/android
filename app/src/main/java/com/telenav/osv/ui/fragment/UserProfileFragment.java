package com.telenav.osv.ui.fragment;

import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
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
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.item.view.profile.UserProfileData;
import com.telenav.osv.item.view.tracklist.StatsData;
import com.telenav.osv.item.view.tracklist.StatsDataFactory;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.ui.Navigator;
import com.telenav.osv.utils.Log;

/**
 * fragment holding the ui for the user's data
 * Created by kalmanbencze on 7/07/17.
 */
public class UserProfileFragment extends SimpleProfileFragment {

    public static final String TAG = "UserProfileFragment";

    @Inject
    UserDataManager mUserDataManager;

    private View mScoreView;

    private View mRankView;

    private TextView mRankText;

    private TextView mScoreText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            mScoreView = view.findViewById(R.id.score_view);
            mScoreView.setVisibility(View.VISIBLE);
            mRankView = view.findViewById(R.id.rank_view);
            mRankView.setVisibility(View.VISIBLE);
            mRankView.setOnClickListener(v -> activity.openScreen(Navigator.SCREEN_LEADERBOARD));
            mRankText = view.findViewById(R.id.rank_text);
            mScoreText = view.findViewById(R.id.score_text);
        }
        mOnlineSequencesAdapter.showValue(appPrefs.isGamificationEnabled());
        return view;
    }

    @Override
    protected void requestDetails() {
        mUserDataManager.getUserProfileDetails(new NetworkResponseDataListener<UserData>() {

            @Override
            public void requestFailed(int status, UserData details) {
                activity.showSnackBar(R.string.warning_server_comunication_failiure, Snackbar.LENGTH_LONG);
                displayCachedStats();
                Log.d(TAG, "requestUserDetails: " + " status - > " + status + " details - > " + details);
            }

            @Override
            public void requestFinished(int status, final UserData userData) {
                Log.d(TAG, "requestUserDetails: " + " status - > " + status + " result - > " + userData);
                if (userData != null) {
                    UserProfileData profileData = new UserProfileData();
                    profileData.setName(userData.getDisplayName());
                    profileData.setUsername(userData.getUserName());
                    profileData.setPhotoUrl(appPrefs.getUserPhotoUrl());
                    profileData.setRank(userData.getOverallRank());
                    profileData.setScore(userData.getTotalPoints());
                    profileData.setLevel(userData.getLevel());
                    profileData.setXpProgress(userData.getLevelProgress());
                    profileData.setXpTarget(userData.getLevelTarget());

                    StatsData stats = StatsDataFactory.create(activity, valueFormatter
                            , userData.getTotalDistance()
                            , 0
                            , userData.getTotalObdDistance()
                            , (int) userData.getTotalTracks()
                            , (int) userData.getTotalPhotos());
                    fillUserInformation(profileData, stats, false);
                }
            }
        });
    }

    protected void displayCachedStats() {
        UserProfileData profileData = new UserProfileData();
        profileData.setUsername(appPrefs.getUserName());
        profileData.setName(appPrefs.getUserDisplayName());
        profileData.setPhotoUrl(appPrefs.getUserPhotoUrl());
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        profileData.setRank(prefs.getInt(K_RANK, 0));
        profileData.setScore(prefs.getInt(K_SCORE, 0));
        profileData.setLevel(prefs.getInt(K_LEVEL, 0));
        profileData.setXpProgress(prefs.getInt(K_XP_PROGRESS, 0));
        profileData.setXpTarget(prefs.getInt(K_XP_TARGET, 1));

        StatsData stats = StatsDataFactory.create(activity, valueFormatter
                , prefs.getFloat(K_TOTAL_DISTANCE, 0.0f)
                , 0
                , prefs.getFloat(K_OBD_DISTANCE, 0.0f)
                , prefs.getInt(K_TOTAL_TRACKS, 0)
                , prefs.getInt(K_TOTAL_PHOTOS, 0));
        fillUserInformation(profileData, stats, true);
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

    @Subscribe
    public void onRefreshNeeded(SequencesChangedEvent event) {
        if (event.online) {
            refreshContent();
        }
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
            mScoreView.animate().alpha(0.0f).setDuration(200).start();
            mRankView.animate().alpha(0.0f).setDuration(200).start();
        }

        if (percentage <= PERCENTAGE_TO_ANIMATE_AVATAR && !mIsAvatarShown) {
            mIsAvatarShown = true;

            mProfileImage.animate().scaleY(1).scaleX(1).alpha(1.0f).setDuration(200).start();
            mScoreView.animate().alpha(1.0f).setDuration(200).start();
            mRankView.animate().alpha(1.0f).setDuration(200).start();
        }
    }

    private void fillUserInformation(UserProfileData profileData, StatsData stats, final boolean fromCache) {
        mHandler.post(() -> {
            collapsingToolbar.setTitle(profileData.getName());
            mScoreText.setText(valueFormatter.formatNumber(profileData.getScore()));
            mRankText.setText("" + profileData.getRank());
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
            mProfileImage.setLevel(profileData.getLevel());
            float levelLenght = profileData.getXpTarget() - (profileData.getScore() - profileData.getXpProgress());
            float progressPercentage = ((int) ((float) profileData.getXpProgress() / levelLenght * 100f)) / 100f;
            if (progressPercentage > 0.94f) {
                progressPercentage = 0.94f;
            } else if (progressPercentage < 0.07f) {
                progressPercentage = 0.07f;
            }
            if (fromCache) {
                mProfileImage.setLinearProgress(true);
                mProfileImage.setInstantProgress(0.01f);
            } else {
                mProfileImage.setLinearProgress(true);
                mProfileImage.setProgress(progressPercentage);
            }
            mOnlineSequencesAdapter.refreshDetails(stats);
        });
    }
}

