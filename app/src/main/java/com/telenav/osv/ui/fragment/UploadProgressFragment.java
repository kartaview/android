package com.telenav.osv.ui.fragment;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.upload.UploadBandwidthEvent;
import com.telenav.osv.event.network.upload.UploadCancelledEvent;
import com.telenav.osv.event.network.upload.UploadFinishedEvent;
import com.telenav.osv.event.network.upload.UploadPausedEvent;
import com.telenav.osv.event.network.upload.UploadProgressEvent;
import com.telenav.osv.event.network.upload.UploadStartedEvent;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * ui for upload progress
 * Created by Kalman on 10/15/2015.
 */
public class UploadProgressFragment extends Fragment {

    public static final String TAG = "UploadProgressFragment";

    private MainActivity activity;

    private TextView uploadSpeedText;

    private TextView timeText;

    private TextView percentText;

//    private LinearLayout closeButton;

    private ProgressWheel progressbar;

    private LinearLayout cancelAllButton;

    private LinearLayout pauseButton;

    private long lastUpdateTime = 0;

    private TextView pauseText;

    private TextView remainingText;

    private TextView totalText;

    private View view;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private LinearLayout mUploadLinearLayout;

    private UploadManager mUploadManager;

    private long mUploadedSize = 0;

    private RelativeLayout progressLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_upload_progress, null);
        Log.d(TAG, "onCreateView: newly creating views");
        activity = (MainActivity) getActivity();
        init(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        mUploadManager = activity.getApp().getUploadManager();
        return view;
    }

    @Override
    public void onDestroyView() {
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (activity == null) {
            activity = (MainActivity) getActivity();
        }
        setupButtons();

        int orientation = activity.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;

        if (mUploadLinearLayout != null) {
            mUploadLinearLayout.setOrientation(portrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        }
        if (progressLayout != null){
            progressLayout.getLayoutParams().height = (int) Utils.dpToPx(activity, portrait ? 400 : 400);
            progressLayout.getLayoutParams().width = (int) Utils.dpToPx(activity, portrait ? 400 : 300);
        }
        if (progressbar != null){
            progressbar.setCircleRadius((int) Utils.dpToPx(activity, portrait ? 140 : 110));
            progressbar.requestLayout();
            progressbar.postInvalidate();
        }
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

    public void init(MainActivity activity) {
        this.activity = activity;
        uploadSpeedText = (TextView) view.findViewById(R.id.upload_speed_text);
        mUploadLinearLayout = (LinearLayout) view.findViewById(R.id.upload_details_linear_layout);
        progressLayout = (RelativeLayout) view.findViewById(R.id.progress_layout);
        timeText = (TextView) view.findViewById(R.id.time_text);
        percentText = (TextView) view.findViewById(R.id.percent_text);
        remainingText = (TextView) view.findViewById(R.id.done_text);
        totalText = (TextView) view.findViewById(R.id.total_text);
        progressbar = (ProgressWheel) view.findViewById(R.id.upload_total_progress);
        cancelAllButton = (LinearLayout) view.findViewById(R.id.cancel_all_button);
        pauseButton = (LinearLayout) view.findViewById(R.id.pause_button);
        pauseText = (TextView) view.findViewById(R.id.pause_text);
    }


    private void setupButtons() {
        if (pauseButton != null && pauseText != null && cancelAllButton != null) {
            if (!mUploadManager.isPaused()) {
                pauseButton.setOnClickListener(activity.pauseOnClickListener);
                pauseText.setText(R.string.pause_caps_label);
                pauseText.setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable(R.drawable.ic_uploading_pause_gray), null, null, null);
            } else {
                pauseButton.setOnClickListener(activity.resumeOnClickListener);
                pauseText.setText(R.string.continue_caps_label);
                pauseText.setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable((R.drawable.ic_uploading_resume_gray)), null, null, null);

            }
            cancelAllButton.setOnClickListener(activity.actionCancelListener);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (activity != null){
            int orientation = newConfig.orientation;
            boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
            if (mUploadLinearLayout != null) {
                mUploadLinearLayout.setOrientation(portrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            }
            if (progressLayout != null){
                progressLayout.getLayoutParams().height = (int) Utils.dpToPx(activity, portrait ? 400 : 400);
                progressLayout.getLayoutParams().width = (int) Utils.dpToPx(activity, portrait ? 400 : 300);
            }
            if (progressbar != null){
                progressbar.setCircleRadius((int) Utils.dpToPx(activity, portrait ? 140 : 110));
                progressbar.requestLayout();
                progressbar.postInvalidate();
            }
        }
    }

    @Subscribe
    public void onUploadStarted(UploadStartedEvent event) {
        Log.d(TAG, "onUploadStarted: totalSize = " + event.totalSize);
        mUploadedSize = 0;
        if (event.totalSize == 0) {
            event.totalSize = 1;
        }
        updateStats(event.totalSize, event.totalSize);
        if (progressbar != null) {
            progressbar.setVisibility(View.VISIBLE);
//            progressbar.setProgress(0);
//            progressbar.setLinearProgress(false);
            progressbar.spin();
//            progressbar.spin();
        }
        if (pauseButton != null) {
            pauseButton.setOnClickListener(activity.pauseOnClickListener);
        }
        if (pauseText != null) {
            pauseText.setText(R.string.pause_caps_label);
            pauseText.setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable(R.drawable.ic_uploading_pause_gray), null, null, null);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUploadCancelled(UploadCancelledEvent event) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressbar != null) {
                    progressbar.setVisibility(View.VISIBLE);
                    progressbar.setProgress(0);
//                    progressbar.setLinearProgress(false);
                }
                if (percentText != null) {
                    percentText.setText("0");
                }
                if (activity != null) {
                    activity.openScreen(ScreenComposer.SCREEN_MAP);
                }
                mUploadedSize = 0;
            }
        }, 1500);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUploadFinished(UploadFinishedEvent event) {
        if (progressbar != null) {
            progressbar.setVisibility(View.VISIBLE);
            progressbar.setProgress(100);
//                    progressbar.setLinearProgress(false);
//                    progressbar.stopSpinning();
        }
        if (percentText != null) {
            percentText.setText("100");
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressbar != null) {
                    progressbar.setVisibility(View.VISIBLE);
                    progressbar.setProgress(0);
//                    progressbar.setLinearProgress(false);
//                    progressbar.stopSpinning();
                }
                if (percentText != null) {
                    percentText.setText("0");
                }
                if (activity != null) {
                    activity.openScreen(ScreenComposer.SCREEN_MAP);
                }
            }
        }, 2500);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onProgressChanged(UploadProgressEvent event) {
        if (event.total == 0) {
            event.total = 1;
        }
        if (progressbar != null) {
            if (progressbar.isSpinning()) {
//                progressbar.stopSpinning();
            }
            progressbar.setLinearProgress(true);
        } else {
            Log.w(TAG, "onProgressChanged: progressbar is null");
        }

        if (mUploadManager != null && mUploadManager.isUploading()) {
            updateStats(event.total, event.remaining);
        }
    }

    @Subscribe
    public void onUploadPaused(UploadPausedEvent event) {
        if (event.paused) {
            if (pauseButton != null) {
                pauseText.setText(R.string.continue_caps_label);
                pauseText.setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable((R.drawable.ic_uploading_resume_gray)), null, null, null);
                pauseButton.setOnClickListener(activity.resumeOnClickListener);
            }
            onBandwidthStateChange(new UploadBandwidthEvent(ConnectionQuality.POOR, 0));
        } else {
            if (pauseButton != null && pauseText != null) {
                pauseText.setText(R.string.pause_caps_label);
                pauseText.setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable((R.drawable.ic_uploading_pause_gray)), null, null, null);
                pauseButton.setOnClickListener(activity.pauseOnClickListener);
            }
        }
    }

    @Subscribe
    public void onBandwidthStateChange(UploadBandwidthEvent event) {
        if (event.bandwidthState != ConnectionQuality.UNKNOWN && event.bandwidth >= 0) {
            if (uploadSpeedText != null) {
                uploadSpeedText.setText(formatBandwidth(event.bandwidth));
            }
        }
        if (System.currentTimeMillis() - lastUpdateTime >= 5000 && mUploadManager != null && mUploadManager.isUploading()) {
            lastUpdateTime = System.currentTimeMillis();
            final double bw = ConnectionClassManager.getInstance().getCurrentBandwidth();
            if (bw >= 0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final long size = mUploadManager.getRemainingSizeValue();
                        final double seconds = (size / 1024d / (bw / 8d));
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    boolean isPaused = UploadManager.sUploadStatus == UploadManager.STATUS_PAUSED;
                                    if (timeText != null) {
                                        timeText.setText(isPaused ? DecimalFormatSymbols.getInstance().getInfinity() : (formatETA(seconds)));
                                    }
                                    if (uploadSpeedText != null) {
                                        uploadSpeedText.setText(isPaused ? formatBandwidth(0) : formatBandwidth(bw));
                                    }
                                }
                            });
                        }
                    }
                }).start();
            }
        }
    }

    public void updateStats(final long mTotalSize, final long remaining) {
        if (remainingText != null) {
            long reported = mTotalSize - remaining;
            String remText = (String) remainingText.getText();
            remText = remText.replace("MB", "");
            remText = remText.replace(" ", "");
            remText = remText.replace("GB", "");
            double textValue = Double.parseDouble(remText);
            String reportedTruncated = Utils.formatSize(mTotalSize - remaining);
            reportedTruncated = reportedTruncated.replace("MB", "");
            reportedTruncated = reportedTruncated.replace(" ", "");
            reportedTruncated = reportedTruncated.replace("GB", "");
            double reportedValue = Double.parseDouble(reportedTruncated);
            if (reported > mUploadedSize || reportedValue > textValue) {
                mUploadedSize = reported;
                remainingText.setText(Utils.formatSize(mTotalSize - remaining));
            }
        }
        if (totalText != null) {
            totalText.setText(Utils.formatSize(mTotalSize));
        }
        if (progressbar != null) {
            final int progress = (int) (((mTotalSize - remaining) * 100) / mTotalSize);
            final int oldProgress = (int) (progressbar.getProgress() * 100f);
            if (progress > oldProgress) {
                if (progress >= 0) {
                    float progressf = ((float) progress / 100f);
//            Log.d(TAG, "updateStats: new progress is " + progressf);
                    progressbar.setProgress(progressf);
                }
            }
            if (percentText != null) {
                int textValue = Integer.parseInt((String) percentText.getText());
                if (textValue <= progress) {
                    percentText.setText(progress + "");
                }
            }
        }
    }

    private String formatETA(double seconds) {
        double value = seconds;
        String q = " s";
        String res;
        if (value > 60) {
            value = value / 60d;
            q = " m";
        }
        if (value > 60) {
            q = " h";
            int min = (int) (value % 60);
            value = value / 60;
            res = "" + (int) value + q + " " + min + " m";
        } else {
            res = "" + (int) value + q;
        }
        return res;
    }

    private String formatBandwidth(double bandwidth) {
        DecimalFormat df2 = new DecimalFormat("#.#");
        double value = bandwidth;
        String q = " KB/s";
        value = value / 8;
        String res;
        if (value > 1024) {
            q = " MB/s";
            value = value / 1024;
            res = "" + df2.format(value) + q;
        } else {
            res = "" + (int) value + q;
        }
        return res;
    }
}