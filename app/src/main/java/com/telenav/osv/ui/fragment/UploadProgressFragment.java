package com.telenav.osv.ui.fragment;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.listener.UploadProgressListener;
import com.telenav.osv.manager.UploadManager;
import com.telenav.osv.service.UploadHandlerService;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by Kalman on 10/15/2015.
 */
public class UploadProgressFragment extends Fragment implements UploadProgressListener {

    public static final String TAG = "UploadProgressFragment";

    private MainActivity activity;

    private UploadHandlerService mUploadHandlerService;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_upload_progress, null);
        Log.d(TAG, "onCreateView: newly creating views");
        activity = (MainActivity) getActivity();
        init(activity);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (activity == null) {
            activity = (MainActivity) getActivity();
        }
        if (activity.mUploadHandlerService != null) {
            onUploadServiceConnected(activity.mUploadHandlerService);
            mUploadHandlerService.addUploadProgressListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (activity == null) {
            activity = (MainActivity) getActivity();
        }
        if (activity.mUploadHandlerService != null) {
            onUploadServiceConnected(activity.mUploadHandlerService);
            mUploadHandlerService.addUploadProgressListener(this);
        }
        if ((activity.mUploadHandlerService == null || !activity.mUploadHandlerService.mUploadManager.isUploading()) && isAdded()) {
            activity.onBackPressed();
        }
    }

    @Override
    public void onDetach() {
        if (mUploadHandlerService != null) {
            mUploadHandlerService.removeUploadProgressListener(this);
        }
        super.onDetach();
    }

    public void init(MainActivity activity) {
        this.activity = activity;
        uploadSpeedText = (TextView) view.findViewById(R.id.upload_speed_text);
        timeText = (TextView) view.findViewById(R.id.time_text);
        percentText = (TextView) view.findViewById(R.id.percent_text);
        remainingText = (TextView) view.findViewById(R.id.done_text);
        totalText = (TextView) view.findViewById(R.id.total_text);
        progressbar = (ProgressWheel) view.findViewById(R.id.upload_total_progress);
        cancelAllButton = (LinearLayout) view.findViewById(R.id.cancel_all_button);
        pauseButton = (LinearLayout) view.findViewById(R.id.pause_button);
        pauseText = (TextView) view.findViewById(R.id.pause_text);
    }


    public void onUploadServiceConnected(UploadHandlerService service) {
        mUploadHandlerService = service;
        if (pauseButton != null && pauseText != null && cancelAllButton != null) {
            int size = Sequence.getLocalSequencesSize();
            int remainingRecordings = mUploadHandlerService.mUploadManager.getRemainingSequences();
            activity.refreshSignatureValue((size - remainingRecordings) + "/" + size);
            if (!mUploadHandlerService.mUploadManager.isPaused()) {
                pauseButton.setOnClickListener(activity.pauseOnClickListener);
                pauseText.setText("PAUSE");
                pauseText.setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable(R.drawable.uploading_pause), null, null, null);
            } else {
//            ((ImageView) pauseButton.findViewById(R.id.pause_icon)).setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_play_arrow_black_36dp));
                pauseButton.setOnClickListener(activity.resumeOnClickListener);
                pauseText.setText("CONTINUE");
                pauseText.setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable((R.drawable.uploading_resume)), null, null, null);

            }
            cancelAllButton.setOnClickListener(activity.actionCancelListener);
        }
    }

    @Override
    public void onUploadStarted(long mTotalSize) {
        Log.d(TAG, "onUploadStarted: totalSize = " + mTotalSize);
        if (mTotalSize == 0) {
            mTotalSize = 1;
        }
        updateStats(mTotalSize, mTotalSize);
        if (progressbar != null) {
            progressbar.setVisibility(View.VISIBLE);
//            progressbar.setProgress(0);
            progressbar.setLinearProgress(false);
            progressbar.spin();
//            progressbar.spin();
        }
        if (pauseButton != null) {
            pauseButton.setOnClickListener(activity.pauseOnClickListener);
        }
        if (pauseText != null) {
            pauseText.setText("PAUSE");
            pauseText.setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable(R.drawable.uploading_pause), null, null, null);
        }
    }

    @Override
    public void onUploadingMetadata() {
    }

    @Override
    public void onPreparing(int nrOfFrames) {
    }

    @Override
    public void onIndexingFinished() {
    }

    @Override
    public void onUploadCancelled(int total, int remaining) {
        if (progressbar != null) {
            progressbar.setVisibility(View.VISIBLE);
            progressbar.setProgress(0);
            progressbar.setLinearProgress(false);
        }
        if (percentText != null) {
            percentText.setText("0");
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                activity.onBackPressed();
            }
        });
    }

    @Override
    public void onUploadFinished(int successful, int unsuccessful) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressbar != null) {
                    progressbar.setVisibility(View.VISIBLE);
                    progressbar.setProgress(0);
                    progressbar.setLinearProgress(false);
//                    progressbar.stopSpinning();
                }
                if (percentText != null) {
                    percentText.setText("0");
                }
                if (isAdded() && activity != null) {
                    activity.onBackPressed();
                }
            }
        }, 1500);
    }

    @Override
    public void onProgressChanged(long total, long remaining) {
        if (total == 0) {
            total = 1;
        }
        if (progressbar != null) {
            if (progressbar.isSpinning()) {
//                progressbar.stopSpinning();
                progressbar.setLinearProgress(true);
            }
        } else {
            Log.d(TAG, "onProgressChanged: progressbar is null");
        }

        if (mUploadHandlerService != null && mUploadHandlerService.isUploading()) {
            updateStats(total, remaining);
        }
    }

    @Override
    public void onImageUploaded(Sequence sequence, boolean success) {
    }

    @Override
    public void onSequenceUploaded(Sequence sequence) {
    }

    @Override
    public void onIndexingSequence(Sequence sequence, int remainingRecordings) {
        int size = Sequence.getLocalSequencesSize();
        activity.refreshSignatureValue((size - remainingRecordings) + "/" + size);
    }

    @Override
    public void onUploadPaused() {
        if (pauseButton != null) {
            pauseText.setText("CONTINUE");
            pauseText.setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable((R.drawable.uploading_resume)), null, null, null);
            pauseButton.setOnClickListener(activity.resumeOnClickListener);
        }
        onBandwidthStateChange(ConnectionQuality.POOR, 0);
    }

    @Override
    public void onUploadResumed() {
        if (pauseButton != null && pauseText != null) {
            pauseText.setText("PAUSE");
            pauseText.setCompoundDrawablesWithIntrinsicBounds(activity.getResources().getDrawable((R.drawable.uploading_pause)), null, null, null);
            pauseButton.setOnClickListener(activity.pauseOnClickListener);
        }
    }

    @Override
    public void onBandwidthStateChange(ConnectionQuality bandwidthState, final double bandwidth) {
        if (bandwidthState != ConnectionQuality.UNKNOWN && bandwidth >= 0) {
            if (uploadSpeedText != null) {
                uploadSpeedText.setText(formatBandwidth(bandwidth));
            }
        }
        if (System.currentTimeMillis() - lastUpdateTime >= 5000 && mUploadHandlerService != null && mUploadHandlerService.isUploading()) {
            lastUpdateTime = System.currentTimeMillis();
            final double bw = ConnectionClassManager.getInstance().getCurrentBandwidth();
            if (bw >= 0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final long size = mUploadHandlerService.mUploadManager.getTotalSizeValue();
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int progress = (int) (((mTotalSize - remaining) * 100) / mTotalSize);
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (remainingText != null) {
                                remainingText.setText(Utils.formatSize(mTotalSize - remaining));
                            }
                            if (totalText != null) {
                                totalText.setText(Utils.formatSize(mTotalSize));
                            }
                            if (progressbar != null && progress > 0) {
                                float progressf = ((float) progress / 100f);
                                Log.d(TAG, "updateStats: progressbar progress is " + progressf);
                                progressbar.setProgress(progressf);
                            }
                            if (percentText != null) {
                                percentText.setText(progress + "");
                            }
                            }
                    });
                }

            }
        }).start();
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