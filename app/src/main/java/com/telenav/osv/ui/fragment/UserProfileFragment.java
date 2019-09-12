package com.telenav.osv.ui.fragment;

import org.greenrobot.eventbus.Subscribe;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.data.user.model.User;
import com.telenav.osv.data.user.model.details.BaseUserDetails;
import com.telenav.osv.data.user.model.details.gamification.GamificationDetails;
import com.telenav.osv.data.user.model.details.gamification.GamificationLevel;
import com.telenav.osv.data.user.model.details.gamification.GamificationRank;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;

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

    @Override
    protected void requestDetails() {
        activity.getUserDataManager().getUserProfileDetails(new NetworkResponseDataListener<UserData>() {

            @Override
            public void requestFailed(int status, UserData details) {
                Log.d(TAG, String.format("requestDetails. Status: error. Status code: %s. User data: %s. Message: Request details failed.", status, details));
                activity.showSnackBar("Failed to communicate with server.", Snackbar.LENGTH_LONG);
            }

            @Override
            public void requestFinished(int status, final UserData userData) {
                Log.d(TAG, String.format("requestDetails. Status: success. Status code: %s. User data: %s. Message: Request details successful.", status, userData));
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

    /**
     * Displays the gamification information.
     * @param user the gamification data which is displayed.
     */
    private void displayGamificationDetails(User user) {
        if (!isAdded()) {
            return;
        }
        hideLoadingIndicator();
        collapsingToolbar.setTitle(user.getDisplayName());
        displayProfileImage(user.getUserName());
        if (user.getDetails() != null && user.getDetails().getType() == BaseUserDetails.UserDetailsTypes.GAMIFICATION) {
            GamificationDetails gamificationDetails = (GamificationDetails) user.getDetails();
            int points = gamificationDetails.getPoints();
            displayRankInformation(points, gamificationDetails.getRank());
            displayLevelInfo(points, gamificationDetails.getLevel());
            displayGamificationInfo(gamificationDetails);
        }
    }

    /**
     * Displays the rank information for the current gamification account.
     * @param score the score of the account.
     * @param gamificationRank the rank info of the account.
     */
    private void displayRankInformation(int score, GamificationRank gamificationRank) {
        mScoreText.setText(FormatUtils.formatNumber(score));
        mRankText.setText(String.valueOf(gamificationRank.getOverall()));
    }

    /**
     * Displays the level information for the curent gamification profile.
     * @param level the gamification information in {@code GamificationLevel} format.
     */
    private void displayLevelInfo(int score, GamificationLevel level) {
        mProfileImage.setLevel(level.getLevel());
        float levelLenght = level.getTarget() - (score - level.getProgress());
        float progressPercentage = ((int) ((float) level.getProgress() / levelLenght * 100f)) / 100f;
        if (progressPercentage > 0.94f) {
            progressPercentage = 0.94f;
        } else if (progressPercentage < 0.07f) {
            progressPercentage = 0.07f;
        }
        mProfileImage.setLinearProgress(true);
        mProfileImage.setProgress(progressPercentage);
    }
}

