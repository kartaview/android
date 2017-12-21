package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.crashlytics.android.Crashlytics;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.FullscreenEvent;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.DriverOnlineSequence;
import com.telenav.osv.item.DriverPayRateBreakdownCoverageItem;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.NearbySequence;
import com.telenav.osv.item.ScoreHistory;
import com.telenav.osv.item.ScoreItem;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.network.ApiResponse;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.manager.playback.SafePlaybackManager;
import com.telenav.osv.ui.custom.RevealRelativeLayout;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import com.telenav.osv.ui.fragment.transition.ScaleFragmentTransition;
import com.telenav.osv.ui.list.PayRateBreakdownAdapter;
import com.telenav.osv.ui.list.ScoreHistoryAdapter;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import com.telenav.streetview.scalablevideoview.ScalableVideoView;
import io.fabric.sdk.android.Fabric;

public class TrackPreviewFragment extends DisplayFragment implements View.OnClickListener, PlaybackManager.PlaybackListener {

    public static final String TAG = "TrackPreviewFragment";

    private PlaybackManager mPlayer;

    private OSVActivity activity;

    private boolean mOnline;

    private ImageView mPlayButton;

    private Sequence mSequence;

    private ImageView mDeleteButton;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean mMaximized = false;

    private TextView mCurrentImageText;

    private boolean shouldHideDelete = false;

    private FrameLayout mFrameHolder;

    private boolean mPrepared;

    private TextView mPointsText;

    private RevealRelativeLayout mScoreLayout;

    private boolean mScoreVisible = false;

    private ImageView mPointsBackground;

    private SeekBar mSeekBar;

    private ImageView mMaximizeButton;

    private SharedPreferences profilePrefs;

    @Override
    public void setSource(Object extra) {
        mPlayer = (PlaybackManager) extra;
        mSequence = mPlayer.getSequence();
        mOnline = mPlayer.isSafe();
        hideDelete(mSequence instanceof NearbySequence);
    }

    @Override
    public void onPlaying() {
        if (mPlayButton != null) {
            mPlayButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_pause));
        }
    }

    @Override
    public void onPaused() {
        if (mPlayButton != null) {
            mPlayButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_play));
        }
    }

    @Override
    public void onStopped() {
        if (mPlayButton != null) {
            mPlayButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_play));
        }
    }

    @Override
    public void onPrepared() {
        mPrepared = true;
        activity.enableProgressBar(false);
    }

    @Override
    public void onProgressChanged(int index) {
        if (mPlayer != null) {
            if (mScoreVisible && mScoreLayout != null && mPointsText != null) {
                mScoreVisible = false;
                float tenDp = activity.getResources().getDimension(R.dimen.track_preview_score_button_margin);
                mScoreLayout.reveal(
                        new Point((int) (mScoreLayout.getWidth() - (mPointsText.getWidth() / 2 + tenDp)), (int) (mPointsText.getWidth() / 2 + tenDp)),
                        500);
            }
            if (mCurrentImageText != null) {
                mCurrentImageText.setText(getSpannable("" + index, "/" + mPlayer.getLength() + " IMG"));
            }
        }
    }

    @Override
    public void onExit() {

    }

    @Override
    public void onClick(View v) {
        if (mScoreVisible && mScoreLayout != null && mPointsText != null) {
            mScoreVisible = false;
            mScoreLayout.reveal(new Point((int) (mScoreLayout.getWidth() - (mPointsText.getWidth() / 2 + Utils.dpToPx(activity, 9))),
                    (int) (mPointsText.getWidth() / 2 + Utils.dpToPx(activity, 7))), 500);
        }
        switch (v.getId()) {
            case R.id.previous_button:
                if (mPlayer != null) {
                    mPlayer.previous();
                }
                break;
            case R.id.fast_backward_button:
                if (mPlayer != null) {
                    mPlayer.fastBackward();
                }
                break;
            case R.id.play_button:
                if (mPlayer != null) {
                    if (mPlayer.isPlaying()) {
                        mPlayer.pause();
                    } else {
                        mPlayer.play();
                    }
                }
                break;
            case R.id.fast_forward_button:
                if (mPlayer != null) {
                    mPlayer.fastForward();
                }
                break;
            case R.id.next_button:
                if (mPlayer != null) {
                    mPlayer.next();
                }
                break;
            case R.id.delete_button:
                if (mPlayer != null) {
                    if (mPlayer.isPlaying()) {
                        mPlayer.pause();
                    }
                    final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
                    if (mSequence.isOnline()) {
                        builder.setMessage(activity.getString(R.string.delete_online_track));
                    } else {
                        builder.setMessage(activity.getString(R.string.delete_local_track));
                    }
                    builder.setTitle(activity.getString(R.string.delete_track_title))
                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mSequence.isOnline()) {
                                deleteOnlineTrack();
                            } else {
                                deleteLocalTrack((LocalSequence) mSequence);
                            }
                        }
                    }).create().show();
                }
                break;
            case R.id.maximize_button:
                if (mPrepared) {
                    toggleMaximize();
                }
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (OSVActivity) context;
        if (mPlayer != null) {
            mPlayer.addPlaybackListener(this);
        }
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementEnterTransition(new ScaleFragmentTransition());
        setSharedElementReturnTransition(new ScaleFragmentTransition());
        this.profilePrefs = getContext().getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track_details, null);
        activity = (OSVActivity) getActivity();
        mFrameHolder = view.findViewById(R.id.image_holder);
        mSeekBar = view.findViewById(R.id.seek_bar_for_preview);
        mSeekBar.setProgress(0);
        if (!mPrepared) {
            activity.enableProgressBar(true);
        }
        if (mPlayer != null) {
            mPlayer.setSeekBar(mSeekBar);
        }
        ImageView mPreviousButton = view.findViewById(R.id.previous_button);
        ImageView mFastBackwardButton = view.findViewById(R.id.fast_backward_button);
        mPlayButton = view.findViewById(R.id.play_button);
        ImageView mFastForwardButton = view.findViewById(R.id.fast_forward_button);
        ImageView mNextButton = view.findViewById(R.id.next_button);
        mDeleteButton = view.findViewById(R.id.delete_button);
        mMaximizeButton = view.findViewById(R.id.maximize_button);
        mPointsText = view.findViewById(R.id.points_text);
        mPointsBackground = view.findViewById(R.id.points_background);
        TextView mTotalPointsText = view.findViewById(R.id.total_points_text);
        mScoreLayout = view.findViewById(R.id.score_reveal_layout);
        RecyclerView mPointsDetails = view.findViewById(R.id.points_details);
        ImageView mScoreClose = view.findViewById(R.id.score_close);

        mCurrentImageText = view.findViewById(R.id.current_image_text);
        mCurrentImageText
                .setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable(R.drawable.vector_camera_gray), null, null, null);
        TextView mImageDateText = view.findViewById(R.id.image_date_text);
        if (mSequence == null) {
            activity.onBackPressed();
            return view;
        }
        mCurrentImageText.setText(getSpannable("0", "/0 IMG"));
        mCurrentImageText.setText(getSpannable("0", "/" + mSequence.getFrameCount() + " IMG"));

        if (mSequence.getDate() != null) {
            String date = "";
            try {
                date = Utils.niceDateFormat.format(mSequence.getDate());
            } catch (Exception e) {
                Log.d(TAG, "onPrepared: " + e.getLocalizedMessage());
            }
            String[] parts = date.split("\\| ");
            if (parts.length > 1) {
                mImageDateText.setText(getSpannable(parts[0], parts[1]));
            }
        }
        if (mSequence instanceof DriverOnlineSequence) {
            boolean byod20 = profilePrefs.getString(ProfileFragment.K_DRIVER_PAYMENT_MODEL_VERSION, ProfileFragment.PAYMENT_MODEL_VERSION_10)
                    .equals(ProfileFragment.PAYMENT_MODEL_VERSION_20);
            if (byod20) {
                List<DriverPayRateBreakdownCoverageItem> payRateItems = ((DriverOnlineSequence) mSequence).getDriverPayRateBreakdownCoverage();
                filterRedundantValues(payRateItems);
                Collections.sort(payRateItems, (rhs, lhs) -> rhs.passes - lhs.passes);
                mPointsDetails.setLayoutManager(new LinearLayoutManager(activity));
                mPointsDetails.setAdapter(new PayRateBreakdownAdapter(payRateItems, mSequence.getCurrency(), activity));

                String monetaryValue;
                if (mSequence.getValue() > 1000) {
                    monetaryValue = Utils.formatMoneyConstrained(mSequence.getValue() / 1000) + "K\n";
                } else {
                    monetaryValue = Utils.formatMoneyConstrained(mSequence.getValue()) + "\n";
                }
                String currency = mSequence.getCurrency();

                setupBreakdownTextsAndClickListeners(mTotalPointsText, mPointsText, mScoreClose, getString(R.string.total_value) + currency + Utils.formatMoneyConstrained(mSequence
                        .getValue()), monetaryValue, currency);
            } else {
                //byod 1.0, do not show any score indicator
                hideTrackValueIndicator();
            }
        } else if (mSequence.getScore() > 0 && activity.getApp().getAppPrefs().getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true)) {
            ArrayList<ScoreHistory> array = new ArrayList<>(mSequence.getScoreHistories().values());
            Iterator<ScoreHistory> iter = array.iterator();
            while (iter.hasNext()) {
                ScoreHistory sch = iter.next();
                if (sch.coverage == -1) {
                    iter.remove();
                }
            }
            @SuppressLint("UseSparseArrays") HashMap<Integer, ScoreItem> res = new HashMap<>();
            for (ScoreHistory hist : array) {
                ScoreItem normal = new ScoreItem(Utils.getValueOnSegment(hist.coverage), hist.photoCount, false);
                ScoreItem obd = new ScoreItem(Utils.getValueOnSegment(hist.coverage) * 2, hist.obdPhotoCount, true);
                if (normal.photoCount > 0) {
                    if (res.containsKey(normal.value)) {
                        res.get(normal.value).photoCount += normal.photoCount;
                        res.get(normal.value).detectedSigns += normal.detectedSigns;
                    } else {
                        res.put(normal.value, normal);
                    }
                }
                if (obd.photoCount > 0) {
                    if (res.containsKey(obd.value)) {
                        res.get(obd.value).photoCount += obd.photoCount;
                        res.get(obd.value).detectedSigns += obd.detectedSigns;
                    } else {
                        res.put(obd.value, obd);
                    }
                }
            }
            ArrayList<ScoreItem> scores = new ArrayList<>(res.values());
            Collections.sort(scores, (rhs, lhs) -> rhs.value - lhs.value);
            mPointsDetails.setLayoutManager(new LinearLayoutManager(activity));
            mPointsDetails.setAdapter(new ScoreHistoryAdapter(scores, activity));

            String first;
            if (mSequence.getScore() > 10000) {
                first = mSequence.getScore() / 1000 + "K\n";
            } else {
                first = mSequence.getScore() + "\n";
            }
            String second = "pts";

            setupBreakdownTextsAndClickListeners(mTotalPointsText, mPointsText, mScoreClose, "Total Points: " + mSequence.getScore(), first, second);
        } else {
            hideTrackValueIndicator();
        }

        mPreviousButton.setOnClickListener(this);
        mFastBackwardButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mFastForwardButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);
        mMaximizeButton.setOnClickListener(this);

        if (activity.getApp().getAppPrefs().getBooleanPreference(PreferenceTypes.K_MAP_DISABLED)) {
            mMaximizeButton.setVisibility(View.GONE);
        }

        if (shouldHideDelete) {
            mDeleteButton.setVisibility(View.GONE);
        } else {
            mDeleteButton.setVisibility(View.VISIBLE);
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                onPlaying();
            } else {
                onPaused();
            }
        }
        if (mFrameHolder != null) {
            mFrameHolder.removeAllViews();
            if (!mOnline) {
                ScalableVideoView mSurfaceView = new ScalableVideoView(activity);
                mSurfaceView
                        .setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                mSurfaceView.setScalableType(ScalableVideoView.ScalableType.FIT_CENTER);
                mFrameHolder.addView(mSurfaceView);
                if (mPlayer != null) {
                    mPlayer.setSurface(mSurfaceView);
                }
            } else {
                ScrollDisabledViewPager mPager = new ScrollDisabledViewPager(activity);

                FrameLayout.LayoutParams lp =
                        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                int fiveDp = (int) Utils.dpToPx(activity, 5);
                lp.bottomMargin = -fiveDp;
                lp.topMargin = -fiveDp;
                lp.leftMargin = -fiveDp;
                lp.rightMargin = -fiveDp;
                mPager.setLayoutParams(lp);
                mFrameHolder.addView(mPager);
                if (mPlayer != null) {
                    mPlayer.setSurface(mPager);
                }
            }
        }
        if (Fabric.isInitialized() && mPlayer != null) {
            Crashlytics.setString(Log.PLAYBACK, mPlayer.isSafe() ? "safe" : "local-mp4");
        }
        if (mPointsBackground != null) {
            mPointsBackground.animate().scaleXBy(0.3f).scaleYBy(0.3f).setDuration(300).setListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mPointsBackground.clearAnimation();
                    mPointsBackground.animate().scaleXBy(-0.3f).scaleYBy(-0.3f).setDuration(500).setInterpolator(new BounceInterpolator())
                            .setListener(new Animator.AnimatorListener() {

                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mPointsBackground.clearAnimation();
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            }).start();
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            }).start();
        }
    }

    @Override
    public void onPause() {
        //        if (mPlayer != null){
        //            mPlayer.pause();
        //        }
        if (Fabric.isInitialized()) {
            Crashlytics.setString(Log.PLAYBACK, "none");
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.destroy();
            mPlayer = null;
        }
        if (activity != null) {
            if (!activity.getApp().isMainProcess()) {
                activity.finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    }

    @Override
    public void onDetach() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.destroy();
            mPlayer.removePlaybackListener(this);
        }
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDetach();
    }

    @Override
    public boolean onBackPressed() {
        activity.enableProgressBar(false);
        if (mScoreLayout != null && mScoreVisible && !fromNearby()) {
            mScoreVisible = false;
            mScoreLayout.reveal();
            return true;
        } else {
            if (mPlayer != null) {
                mPlayer.stop();
                mPlayer.destroy();
                mPlayer = null;
            }
            if (activity != null) {
                if (!activity.getApp().isMainProcess()) {
                    activity.finish();
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }
            return false;
        }
    }

    @Override
    public View getSharedElement() {
        return mPlayer == null ? null : mPlayer.getSurface();
    }

    @Override
    public String getSharedElementTransitionName() {
        return activity == null ? "" : activity.getString(R.string.transition_name_fullscreen_preview);
    }

    public boolean isOnline() {
        return mSequence != null && mSequence.isOnline();
    }

    public void hideDelete(boolean hide) {
        shouldHideDelete = hide;
        if (mDeleteButton != null) {
            if (shouldHideDelete) {
                mDeleteButton.setVisibility(View.GONE);
            } else {
                mDeleteButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideTrackValueIndicator() {
        mPointsBackground.setVisibility(View.GONE);
        mPointsText.setVisibility(View.GONE);
        mPointsBackground = null;
        mPointsText = null;
        mScoreLayout = null;
    }

    private void setupBreakdownTextsAndClickListeners(TextView mTotalPointsTextView, TextView pointsTextView, ImageView mScoreCloseImageView, String totalValueText, String
            first, String second) {
        SpannableString styledString = new SpannableString(first + second);
        styledString.setSpan(new StyleSpan(Typeface.BOLD), 0, first.length(), 0);
        styledString.setSpan(new StyleSpan(Typeface.NORMAL), first.length(), second.length() + first.length(), 0);
        styledString.setSpan(new AbsoluteSizeSpan(16, true), 0, first.length(), 0);
        styledString.setSpan(new AbsoluteSizeSpan(12, true), first.length(), second.length() + first.length(), 0);

        mTotalPointsTextView.setText(totalValueText);
        pointsTextView.setText(styledString);
        pointsTextView.setOnClickListener((View v) -> {
            if (mPlayer != null) {
                mPlayer.pause();
            }
            mScoreVisible = true;
            float tenDp = activity.getResources().getDimension(R.dimen.track_preview_score_button_margin);
            mScoreLayout.reveal(
                    new Point((int) (mScoreLayout.getWidth() - (pointsTextView.getWidth() / 2 + tenDp)), (int) (pointsTextView.getWidth() / 2 + tenDp)),
                    500);
        });
        mScoreCloseImageView.setOnClickListener((View v) -> {
            mScoreVisible = false;
            float tenDp = activity.getResources().getDimension(R.dimen.track_preview_score_button_margin);
            mScoreLayout.reveal(
                    new Point((int) (mScoreLayout.getWidth() - (pointsTextView.getWidth() / 2 + tenDp)), (int) (pointsTextView.getWidth() / 2 + tenDp)),
                    500);
        });
    }

    private void filterRedundantValues(List<DriverPayRateBreakdownCoverageItem> payrateItems) {
        Iterator<DriverPayRateBreakdownCoverageItem> iter = payrateItems.iterator();
        while (iter.hasNext()) {
            DriverPayRateBreakdownCoverageItem item = iter.next();
            if (item == null || item.receivedMoney <= 0) {
                iter.remove();
            }
        }
    }

    private SpannableString getSpannable(String first, String second) {
        SpannableString spannable = new SpannableString(first + second);
        spannable.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.text_colour_default_light)), 0, first.length(), 0);
        spannable.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.text_colour_default_light_faded)), first.length(),
                second.length() + first.length(), 0);
        return spannable;
    }

    private void toggleMaximize() {
        //        ViewCompat.setTransitionName(mPlayer.getSurface(), activity.getString(R.string.transition_name_fullscreen_preview));
        ////        Bitmap bitmap = ((GlideBitmapDrawable)((ImageView)((ScrollDisabledViewPager)mPlayer.getSurface()).getChildAt(0))
        /// .getDrawable()).getBitmap();
        //        mPlayer.pause();
        //        activity.openScreen(ScreenComposer.SCREEN_PREVIEW_FULLSCREEN, mSequence);
        //        mFrameHolder.post(new Runnable() {
        //            @Override
        //            public void run() {
        //                if (mPlayer != null) {
        //                    mPlayer.onSizeChanged();
        //                }
        //            }
        //        });

        if (mMaximized) {
            mMaximized = false;
            mMaximizeButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_maximize));
            mDeleteButton.setVisibility(View.VISIBLE);
            if (mPointsBackground != null) {
                mPointsBackground.setVisibility(View.VISIBLE);
            }
            if (mPointsText != null) {
                mPointsText.setVisibility(View.VISIBLE);
            }
            EventBus.post(new FullscreenEvent(false));
        } else {
            mMaximized = true;
            mMaximizeButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_minimize));
            mDeleteButton.setVisibility(View.GONE);
            if (mPointsBackground != null) {
                mPointsBackground.setVisibility(View.GONE);
            }
            if (mPointsText != null) {
                mPointsText.setVisibility(View.GONE);
            }
            EventBus.post(new FullscreenEvent(true));
        }
        mFrameHolder.post(new Runnable() {

            @Override
            public void run() {
                if (mPlayer != null) {
                    mPlayer.onSizeChanged();
                }
            }
        });
    }

    private void deleteOnlineTrack() {
        activity.enableProgressBar(true);
        activity.getUserDataManager().deleteSequence(mSequence.getId(), new NetworkResponseDataListener<ApiResponse>() {

            @Override
            public void requestFailed(int status, ApiResponse details) {
                activity.showSnackBar(R.string.something_wrong_try_again, Snackbar.LENGTH_SHORT);
            }

            @Override
            public void requestFinished(int status, final ApiResponse details) {
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        activity.enableProgressBar(false);
                        if (mScoreLayout != null && mScoreVisible) {
                            mScoreVisible = false;
                            mScoreLayout.reveal();
                        }
                        activity.onBackPressed();
                        EventBus.post(new SequencesChangedEvent(true));
                        mHandler.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                activity.showSnackBar(R.string.recording_deleted, Snackbar.LENGTH_SHORT);
                            }
                        }, 250);
                    }
                });
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteLocalTrack(LocalSequence mSequence) {
        if (mSequence != null) {
            final int sequenceId = mSequence.getId();
            activity.enableProgressBar(true);
            LocalSequence.deleteSequence(mSequence.getId());
            if (mSequence.getFolder().exists()) {
                mSequence.getFolder().delete();
            }
            activity.enableProgressBar(false);
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    activity.showSnackBar(R.string.recording_deleted, Snackbar.LENGTH_SHORT);
                    if (mPlayer instanceof SafePlaybackManager) {
                        if (mScoreLayout != null && mScoreVisible) {
                            mScoreVisible = false;
                            mScoreLayout.reveal();
                        }
                        activity.onBackPressed();
                        EventBus.postSticky(new SequencesChangedEvent(false, sequenceId));
                    } else {
                        EventBus.post(new SequencesChangedEvent(false, sequenceId));
                        activity.finish();
                    }
                }
            }, 250);
        }
    }

    private boolean fromNearby() {
        return mSequence instanceof NearbySequence;
    }
}
