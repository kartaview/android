package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.http.RequestListener;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.VideoFile;
import com.telenav.osv.manager.PlaybackManager;
import com.telenav.osv.manager.UploadManager;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import wseemann.media.FFmpegMediaPlayer;

public class TrackPreviewFragment extends Fragment implements View.OnClickListener, PlaybackManager.PlaybackListener {

    public static final String TAG = "TrackPreviewFragment";

    private SurfaceView mSurfaceView;

    private SurfaceHolder mSurfaceHolder;

    private Surface mFinalSurface;

    private PlaybackManager mPlayer;

    private ArrayList<VideoFile> mVideos = new ArrayList<>();

    private View view;

    private MainActivity activity;

    private FFmpegMediaPlayer.OnErrorListener mOnErrorListener = new FFmpegMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(FFmpegMediaPlayer mp, int what, int extra) {
            Log.d(TrackPreviewFragment.class.getName(), "Error: " + what);
            return true;
        }
    };

    private FFmpegMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new FFmpegMediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(FFmpegMediaPlayer mp) {
            Log.d(TAG, "onSeekComplete: to " + mp.getCurrentPosition());
        }
    };

    private SeekBar mSeekBar;

    private FFmpegMediaPlayer.OnPreparedListener mOnPreparedListener = new FFmpegMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(FFmpegMediaPlayer mp) {
            Log.d(TAG, "onPrepared: duration = " + mp.getDuration() / 1000f);
            mSeekBar.setMax(mPlayer.getLength());
            mSeekBar.setProgress(0);
            mp.start();
        }
    };

    private boolean mOnline;

    private ImageView mPreviousButton;

    private ImageView mFastBackwardButton;

    private ImageView mPlayButton;

    private ImageView mFastForwardButton;

    private ImageView mNextButton;

    private ScrollDisabledViewPager mPager;

    private Sequence mSequence;

    private ImageView mDeleteButton;

    private ImageView mMaximizeButton;


    private Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean mMaximized = false;

    private TextView mCurrentImageText;

    private TextView mImageDateText;

    private boolean shouldHideDelete = false;

    private FrameLayout mFrameHolder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_track_details, null);
        activity = (MainActivity) getActivity();
        mFrameHolder = (FrameLayout) view.findViewById(R.id.image_holder);
        mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar_for_preview);
        mSeekBar.setProgress(0);
        if (mPlayer != null) {
            mPlayer.addPlaybackListener(this);
            mPlayer.setSeekBar(mSeekBar);
        }
        mPreviousButton = (ImageView) view.findViewById(R.id.previous_button);
        mFastBackwardButton = (ImageView) view.findViewById(R.id.fast_backward_button);
        mPlayButton = (ImageView) view.findViewById(R.id.play_button);
        mFastForwardButton = (ImageView) view.findViewById(R.id.fast_forward_button);
        mNextButton = (ImageView) view.findViewById(R.id.next_button);
        mDeleteButton = (ImageView) view.findViewById(R.id.delete_button);
        mMaximizeButton = (ImageView) view.findViewById(R.id.maximize_button);

        mCurrentImageText = (TextView) view.findViewById(R.id.current_image_text);
        mImageDateText = (TextView) view.findViewById(R.id.image_date_text);

        mCurrentImageText.setText(getSpannable("0", "/0 IMG"));
        mImageDateText.setText(getSpannable("January 1st", " | 02:00 AM"));

        mPreviousButton.setOnClickListener(this);
        mFastBackwardButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mFastForwardButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);
        mMaximizeButton.setOnClickListener(this);
        mMaximized = false;

        if (shouldHideDelete) {
            mDeleteButton.setVisibility(View.GONE);
        } else {
            mDeleteButton.setVisibility(View.VISIBLE);
        }
        if (!mOnline) {
            mSurfaceView = new SurfaceView(activity);
            mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            mFrameHolder.addView(mSurfaceView);
            mSurfaceHolder = mSurfaceView.getHolder();

            mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {

                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    Log.v(TAG, "surfaceChanged format=" + format + ", width=" + width + ", height="
                            + height);
                }

                public void surfaceCreated(SurfaceHolder holder) {
                    mFinalSurface = holder.getSurface();
                    Log.d(TAG, "surfaceCreated: setting surface to player");
                    if (mPlayer != null) {
                        mPlayer.setSurface(mFinalSurface);
                    }
                }

                public void surfaceDestroyed(SurfaceHolder holder) {
                    Log.v(TAG, "surfaceDestroyed");
                }

            });
        } else {
            mPager = new ScrollDisabledViewPager(activity);

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
        return view;
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
        mOnline = playbackManager.isOnline();
    }

    @Override
    public void onDestroyView() {
        if (mPlayer != null) {
            mPlayer.removePlaybackListener(this);
            mPlayer.destroy();
        }
        super.onDestroyView();
    }

    @Override
    public void onClick(View v) {
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
                        //TODO for local
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
                                //TODO for local
                            }
                        }
                    }).create().show();
                }
                break;
            case R.id.maximize_button:
                if (mMaximized) {
                    mMaximized = false;
                    mMaximizeButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.maximize));
                    mDeleteButton.setVisibility(View.VISIBLE);
                    mFrameHolder.setBackground(activity.getResources().getDrawable(R.drawable.preview_background));
                    activity.resizeHolderStatic(0.6f, true);
                } else {
                    mMaximized = true;
                    mMaximizeButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.minimize));
                    mDeleteButton.setVisibility(View.GONE);
                    mFrameHolder.setBackground(null);
                    activity.resizeHolderStatic(1.0f, true);
                }
                break;
        }
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
                            activity.onBackPressed();
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


    @Override
    public void onPlaying() {
        if (mPlayButton != null) {
            mPlayButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.pause));
        }
    }

    @Override
    public void onPaused() {
        if (mPlayButton != null) {
            mPlayButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.play));
        }
    }

    @Override
    public void onStopped() {
        if (mPlayButton != null) {
            mPlayButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.play));
        }

    }


    @Override
    public void onPrepared() {
        if (mCurrentImageText != null) {
            mCurrentImageText.setText(getSpannable("0", "/" + mPlayer.getImages().size() + " IMG"));
            if (mSequence != null && mSequence.title != null) {
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

    @Override
    public void onProgressChanged(int index) {
        if (mPlayer != null) {
            if (mCurrentImageText != null) {
                mCurrentImageText.setText(getSpannable("" + index, "/" + mPlayer.getImages().size() + " IMG"));
            }
        }
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
}
