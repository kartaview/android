package com.telenav.osv.ui.fragment;

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
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * fragment holding the ui for the user's data
 * Created by kalmanbencze on 7/07/17.
 */
public class UserProfileFragment extends SimpleProfileFragment {

    public final static String TAG = "UserProfileFragment";

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
            mRankView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    activity.openScreen(ScreenComposer.SCREEN_LEADERBOARD);
                }
            });
            mRankText = view.findViewById(R.id.rank_text);
            mScoreText = view.findViewById(R.id.score_text);
        }
        return view;
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
                    final int finalRank = userData.getOverallRank();
                    final int finalScore = userData.getTotalPoints();
                    final int finalLevel = userData.getLevel();
                    final int finalXpProgress = userData.getLevelProgress();
                    final int finalXpTarget = userData.getLevelTarget();
                    final String photoUrl = appPrefs.getStringPreference(PreferenceTypes.K_USER_PHOTO_URL);
                    fillUserInformation(displayName, username, photoUrl, finalScore, finalRank, finalLevel, finalXpTarget, finalXpProgress,
                            totalDistanceFormatted, obdDistanceFormatted, totalPhotos, totalTracks, false);
                }
            }
        });
    }

    protected void displayCachedStats() {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final int rank = prefs.getInt(K_RANK, 0);
        final int score = prefs.getInt(K_SCORE, 0);
        final int level = prefs.getInt(K_LEVEL, 0);
        final int xpProgress = prefs.getInt(K_XP_PROGRESS, 0);
        final int xpTarget = prefs.getInt(K_XP_TARGET, 1);
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
        fillUserInformation(name, username, photoUrl, score, rank, level, xpTarget, xpProgress, totalDistanceFormatted, obdDistanceFormatted,
                photos, tracks, true);
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

    private void fillUserInformation(final String name, final String username, final String photoUrl, final int score, final int rank,
                                     final int level, final int xpTarget, final int xpProgress, final String[] totalDistanceFormatted,
                                     final String[] obdDistanceFormatted, final String photos, final String tracks, final boolean fromCache) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (!isAdded()) {
                    return;
                }
                collapsingToolbar.setTitle(name);
                mScoreText.setText("" + Utils.formatNumber(score));
                mRankText.setText("" + rank);
                if (!photoUrl.equals("")) {
                    Glide.with(activity).load(photoUrl).centerCrop().dontAnimate().diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false)
                            .signature(new StringSignature("profile " + username + "-" + photoUrl)).priority(Priority.IMMEDIATE)
                            .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                            .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                            .listener(MainActivity.mGlideRequestListener).into(mProfileImage);
                }
                mProfileImage.setLevel(level);
                float levelLenght = xpTarget - (score - xpProgress);
                float progressPercentage = ((int) ((float) xpProgress / levelLenght * 100f)) / 100f;
                if (progressPercentage > 0.94f) {
                    progressPercentage = 0.94f;
                } else if (progressPercentage < 0.07f) {
                    progressPercentage = 0.07f;
                }
                if (fromCache) {
                    //                    mProfileImage.setLinearProgress(false);
                    //                    mProfileImage.spin();
                    mProfileImage.setLinearProgress(true);
                    mProfileImage.setInstantProgress(0.01f);
                } else {
                    mProfileImage.setLinearProgress(true);
                    mProfileImage.setProgress(progressPercentage);
                }
                //                mOnlineSequencesAdapter.notifyDataSetChanged();
                mOnlineSequencesAdapter.refreshDetails(totalDistanceFormatted, new String[]{}, obdDistanceFormatted, photos, tracks);
            }
        });
    }
}

