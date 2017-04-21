package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
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
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.FullscreenEvent;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.http.RequestListener;
import com.telenav.osv.item.ScoreHistory;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.manager.playback.SafePlaybackManager;
import com.telenav.osv.ui.custom.RevealRelativeLayout;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import com.telenav.osv.ui.list.ScoreHistoryAdapter;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import com.telenav.streetview.scalablevideoview.ScalableVideoView;
import io.fabric.sdk.android.Fabric;

public class TrackPreviewFragment extends Fragment implements View.OnClickListener, PlaybackManager.PlaybackListener {

    public static final String TAG = "TrackPreviewFragment";

    private PlaybackManager mPlayer;

    private OSVActivity activity;

    private boolean mOnline;

    private ImageView mPlayButton;

    private Sequence mSequence;

    private ImageView mDeleteButton;

    private ImageView mMaximizeButton;


    private Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean mMaximized = false;

    private TextView mCurrentImageText;

    private TextView mImageDateText;

    private boolean shouldHideDelete = false;

    private FrameLayout mFrameHolder;

    private boolean mPrepared;

    private TextView mPointsText;

    private RevealRelativeLayout mScoreLayout;

    private boolean mScoreVisible = false;

    private ImageView mPointsBackground;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track_details, null);
        activity = (OSVActivity) getActivity();
        mFrameHolder = (FrameLayout) view.findViewById(R.id.image_holder);
        SeekBar mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar_for_preview);
        mSeekBar.setProgress(0);
        if (!mPrepared){
            activity.enableProgressBar(true);
        }
        if (mPlayer != null) {
            mPlayer.addPlaybackListener(this);
            mPlayer.setSeekBar(mSeekBar);
        }
        ImageView mPreviousButton = (ImageView) view.findViewById(R.id.previous_button);
        ImageView mFastBackwardButton = (ImageView) view.findViewById(R.id.fast_backward_button);
        mPlayButton = (ImageView) view.findViewById(R.id.play_button);
        ImageView mFastForwardButton = (ImageView) view.findViewById(R.id.fast_forward_button);
        ImageView mNextButton = (ImageView) view.findViewById(R.id.next_button);
        mDeleteButton = (ImageView) view.findViewById(R.id.delete_button);
        mMaximizeButton = (ImageView) view.findViewById(R.id.maximize_button);
        mPointsText = (TextView) view.findViewById(R.id.points_text);
        mPointsBackground = (ImageView) view.findViewById(R.id.points_background);
        TextView mTotalPointsText = (TextView) view.findViewById(R.id.total_points_text);
        mScoreLayout = (RevealRelativeLayout) view.findViewById(R.id.score_reveal_layout);
        RecyclerView mPointsDetails = (RecyclerView) view.findViewById(R.id.points_details);
        ImageView mScoreClose = (ImageView) view.findViewById(R.id.score_close);

        mCurrentImageText = (TextView) view.findViewById(R.id.current_image_text);
        mImageDateText = (TextView) view.findViewById(R.id.image_date_text);
        if (mSequence == null) {
            activity.onBackPressed();
            return view;
        }
        mCurrentImageText.setText(getSpannable("0", "/0 IMG"));
        mImageDateText.setText(getSpannable("January 1st", " | 02:00 AM"));
        if (mSequence.score > 0) {
//            List<ScoreHistory> results = new ArrayList<>();

//            if (!mSequence.online) {
//                Cursor scores = SequenceDB.instance.getScores(mSequence.sequenceId);
//                while (scores != null && !scores.isAfterLast()) {
//                    int coverage = scores.getInt(scores.getColumnIndex(SequenceDB.SCORE_COVERAGE));
//                    int photoCount = scores.getInt(scores.getColumnIndex(SequenceDB.SCORE_COUNT));
//                    int obdCount = scores.getInt(scores.getColumnIndex(SequenceDB.SCORE_OBD_COUNT));
//                    ScoreHistory score = new ScoreHistory(coverage, photoCount, obdCount);
//                    results.add(score);
//                    scores.moveToNext();
//                }
//                if (scores != null) {
//                    scores.close();
//                }
//
//            } else {
////                activity.getApp().getUploadManager(). //todo online point details
//            }
            ArrayList<ScoreHistory> array = new ArrayList<>(mSequence.scoreHistories.values());
            Iterator<ScoreHistory> iter = array.iterator();
            while (iter.hasNext()){
                ScoreHistory sch = iter.next();
                if (sch.coverage == -1){
                    iter.remove();
                }
            }
            @SuppressLint("UseSparseArrays") HashMap<Integer, ScoreHistory> res = new HashMap<>();
            for (ScoreHistory hist : array){
                int value = Utils.getValueOnSegment(hist.coverage);
                if (res.containsKey(value)){
                    res.get(value).photoCount += hist.photoCount;
                    res.get(value).obdPhotoCount += hist.obdPhotoCount;
                    res.get(value).detectedSigns += hist.detectedSigns;
                } else {
                    res.put(value, hist);
                }
            }
            mPointsDetails.setAdapter(new ScoreHistoryAdapter(new ArrayList<>(res.values()), activity));
            String first;
            mPointsDetails.setLayoutManager(new LinearLayoutManager(activity));
            if (mSequence.score > 10000){
                first = mSequence.score / 1000 + "K\n";
            } else {
                first = mSequence.score + "\n";
            }
            String second = "pts";
            SpannableString styledString = new SpannableString(first + second);
            styledString.setSpan(new StyleSpan(Typeface.BOLD), 0, first.length(), 0);
            styledString.setSpan(new StyleSpan(Typeface.NORMAL), first.length(), second.length() + first.length(), 0);
            styledString.setSpan(new AbsoluteSizeSpan(16, true), 0, first.length(), 0);
            styledString.setSpan(new AbsoluteSizeSpan(12, true), first.length(), second.length() + first.length(), 0);
            mTotalPointsText.setText("Total Points: " + mSequence.score);
            mPointsText.setText(styledString);
            mPointsText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlayer != null) {
                        mPlayer.pause();
                    }
                    mScoreVisible = true;
                    mScoreLayout.reveal(new Point((int) (mScoreLayout.getWidth()-(mPointsText.getWidth()/2 + Utils.dpToPx(activity, 9))),(int) (mPointsText.getWidth()/2 + Utils.dpToPx(activity, 7))),500);
                }
            });
            mScoreClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mScoreVisible = false;
                    mScoreLayout.reveal(new Point((int) (mScoreLayout.getWidth()-(mPointsText.getWidth()/2 + Utils.dpToPx(activity, 9))),(int) (mPointsText.getWidth()/2 + Utils.dpToPx(activity, 7))),500);
                }
            });
//            mSequence.scoreHistories;todo
        } else {
            mPointsBackground.setVisibility(View.GONE);
            mPointsText.setVisibility(View.GONE);
        }

        mPreviousButton.setOnClickListener(this);
        mFastBackwardButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mFastForwardButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);
        mMaximizeButton.setOnClickListener(this);
        mMaximized = false;

        if (activity.getApp().getAppPrefs().getBooleanPreference(PreferenceTypes.K_MAP_DISABLED)) {
            mMaximizeButton.setVisibility(View.GONE);
        }

        if (shouldHideDelete) {
            mDeleteButton.setVisibility(View.GONE);
        } else {
            mDeleteButton.setVisibility(View.VISIBLE);
        }
        if (!mOnline) {
            ScalableVideoView mSurfaceView = new ScalableVideoView(activity);
            mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            mSurfaceView.setScalableType(ScalableVideoView.ScalableType.FIT_CENTER);
            if (mPlayer != null) {
                mPlayer.setSurface(mSurfaceView);
            }
            mFrameHolder.addView(mSurfaceView);
        } else {
            ScrollDisabledViewPager mPager = new ScrollDisabledViewPager(activity);

            if (mPlayer != null) {
                mPlayer.setSurface(mPager);
            }
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            int fiveDp = (int) Utils.dpToPx(activity, 5);
            lp.bottomMargin = -fiveDp;
            lp.topMargin = -fiveDp;
            lp.leftMargin = -fiveDp;
            lp.rightMargin = -fiveDp;
            mPager.setLayoutParams(lp);
            mFrameHolder.addView(mPager);
        }
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activity.enableProgressBar(true);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (mPlayer != null && mPrepared){
//            mPlayer.play();
//        }
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
                    mPointsBackground.animate().scaleXBy(-0.3f).scaleYBy(-0.3f).setDuration(500).setInterpolator(new BounceInterpolator()).setListener(new Animator.AnimatorListener() {


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

    private SpannableString getSpannable(String first, String second) {
        SpannableString spannable = new SpannableString(first + second);
        spannable.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.text_colour_default)), 0, first.length(), 0);
        spannable.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.md_grey_600)), first.length(), second.length() + first.length(), 0);
        return spannable;
    }

    public void setSource(PlaybackManager playbackManager) {
        mPlayer = playbackManager;
        mSequence = playbackManager.getSequence();
        mOnline = playbackManager.isSafe();
    }

    @Override
    public void onDestroyView() {
        if (mPlayer != null) {
            mPlayer.destroy();
        }
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroyView();
    }

    @Override
    public void onClick(View v) {
        if (mScoreVisible){
            mScoreVisible = false;
            mScoreLayout.reveal(new Point((int) (mScoreLayout.getWidth()-(mPointsText.getWidth()/2 + Utils.dpToPx(activity, 9))),(int) (mPointsText.getWidth()/2 + Utils.dpToPx(activity, 7))),500);
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
                    if (mSequence.online) {
                        builder.setMessage(activity.getString(R.string.delete_online_track));
                    } else {
                        builder.setMessage(activity.getString(R.string.delete_local_track));
                    }
                    builder.setTitle(activity.getString(R.string.delete_track_title)).setNegativeButton(R.string.no,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mSequence.online) {
                                deleteOnlineTrack();
                            } else {
                                deleteLocalTrack();
                            }
                        }
                    }).create().show();
                }
                break;
            case R.id.maximize_button:
                toggleMaximize();
                break;
        }
    }

    private void toggleMaximize() {
        if (mMaximized) {
            mMaximized = false;
            mMaximizeButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_maximize));
            mDeleteButton.setVisibility(View.VISIBLE);
            mFrameHolder.setBackground(activity.getResources().getDrawable(R.drawable.custom_pattern_preview_background));
            EventBus.post(new FullscreenEvent(false));
        } else {
            mMaximized = true;
            mMaximizeButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_minimize));
            mDeleteButton.setVisibility(View.GONE);
            mFrameHolder.setBackground(null);
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

    public boolean isOnline() {
        return mSequence != null && mSequence.online;
    }


    private void deleteOnlineTrack() {
        UploadManager uploadManager = ((OSVApplication) activity.getApplication()).getUploadManager();
        activity.enableProgressBar(true);
        uploadManager.deleteSequence(mSequence.sequenceId, new RequestListener() {
            @Override
            public void requestFinished(final int status) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.enableProgressBar(false);
                        if (status == STATUS_FAILED) {
                            activity.showSnackBar(R.string.something_wrong_try_again, Snackbar.LENGTH_SHORT);
                        } else {
                            if (mScoreLayout != null && mScoreVisible) {
                                mScoreVisible = false;
                                mScoreLayout.reveal();
                            }
                            if (mMaximized && mDeleteButton != null && mMaximizeButton != null) {
                                toggleMaximize();
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
                    }
                });
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteLocalTrack() {
        if (mSequence != null) {
            final int sequenceId = mSequence.sequenceId;
            activity.enableProgressBar(true);
            Sequence.deleteSequence(mSequence.sequenceId);
            if (mSequence.folder.exists()) {
                mSequence.folder.delete();
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
                        if (mMaximized && mDeleteButton != null && mMaximizeButton != null) {
                            toggleMaximize();
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


    @Override
    public void onPlaying() {
        if (mPlayButton != null) {
            mPlayButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_pause_white));
        }
    }

    @Override
    public void onPaused() {
        if (mPlayButton != null) {
            mPlayButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_play_white));
        }
    }

    @Override
    public void onStopped() {
        if (mPlayButton != null) {
            mPlayButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_play_white));
        }

    }


    @Override
    public void onPrepared() {
        mPrepared = true;
        activity.enableProgressBar(false);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCurrentImageText != null && mPlayer != null) {
                    mCurrentImageText.setText(getSpannable("0", "/" + mPlayer.getLength() + " IMG"));
                }
                if (mImageDateText != null) {
                    if (mSequence != null) {
                        if (mSequence.title != null) {
                            String date = "";
                            try {
                                date = Utils.niceDateFormat.format(Utils.numericDateFormat.parse(mSequence.title));
                            } catch (Exception e) {
                                Log.d(TAG, "onPrepared: " + e.getLocalizedMessage());
                            }
                            String[] parts = date.split("\\|");
                            if (parts.length > 1) {
                                mImageDateText.setText(getSpannable(parts[0], "|" + parts[1]));
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onProgressChanged(int index) {
        if (mPlayer != null) {
            if (mScoreVisible){
                mScoreVisible = false;
                mScoreLayout.reveal(new Point((int) (mScoreLayout.getWidth()-(mPointsText.getWidth()/2 + Utils.dpToPx(activity, 9))),(int) (mPointsText.getWidth()/2 + Utils.dpToPx(activity, 7))),500);
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
    public void onDetach() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.destroy();
        }
        super.onDetach();
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

    public boolean onBackPressed() {
        activity.enableProgressBar(false);
        if (mScoreLayout != null && mScoreVisible && !fromNearby()) {
            mScoreVisible = false;
            mScoreLayout.reveal();
            return true;
        } else if (mMaximized && mDeleteButton != null && mMaximizeButton != null && activity != null) {
            toggleMaximize();
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

    public boolean fromNearby() {
        return mSequence != null && mSequence.isPublic;
    }
}
