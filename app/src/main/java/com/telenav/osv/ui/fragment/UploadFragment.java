package com.telenav.osv.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pnikosis.materialishprogress.ProgressWheel;
import com.telenav.osv.R;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.model.base.KVBaseFragment;
import com.telenav.osv.common.toolbar.KVToolbar;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.upload.UploadFinishedEvent;
import com.telenav.osv.upload.UploadManager;
import com.telenav.osv.upload.settings.SettingsUploadManual;
import com.telenav.osv.upload.status.UploadStatus;
import com.telenav.osv.upload.status.UploadUpdate;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

import io.reactivex.disposables.Disposable;

/**
 * @author horatiuf
 */
public class UploadFragment extends KVBaseFragment implements UploadStatus {

    public static final String TAG = "UploadFragment";

    private TextView textViewBandwidth;

    private TextView textViewEta;

    private TextView textViewProgress;

    private ProgressWheel progressbar;

    private LinearLayout stopButton;

    private TextView textViewRemaining;

    private TextView totalText;

    private View view;

    private LinearLayout mUploadLinearLayout;

    private RelativeLayout progressLayout;

    private UploadManager uploadManager;

    private Disposable sequenceIdsDisposable;

    private boolean uploadStarted;

    public static UploadFragment newInstance() {
        Bundle args = new Bundle();
        UploadFragment fragment = new UploadFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uploadManager = Injection.provideUploadManager();
        if (!uploadManager.isInProgress()) {
            setupUpload();
        }
        setupStatusBarColor(R.color.default_black);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_upload_progress, null);
        Log.d(TAG, "onCreateView: newly creating views");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (uploadManager.isInProgress()) {
            uploadManager.onViewCreated();
            uploadManager.addUploadStatusListener(this);
        }
        initUi();
        showLoadingIndicator();
    }

    @Override
    public void onUploadStarted() {
        initUploadUi();
        getActivity().runOnUiThread(this::hideLoadingIndicator);
    }

    @Override
    public void onUploadPaused() {

    }

    @Override
    public void onUploadStoped() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    @Override
    public void onUploadResume() {

    }

    @Override
    public void onUploadComplete() {
        if (progressbar != null) {
            progressbar.setVisibility(View.VISIBLE);
            progressbar.setProgress(100);
        }
        if (textViewProgress != null) {
            textViewProgress.setText("100");
        }
        uploadManager.stop();
        EventBus.post(new UploadFinishedEvent());
    }

    @Override
    public void onResume() {
        super.onResume();

        Context context = getContext();
        if (getActivity() == null || context == null) {
            return;
        }

        int orientation = getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;

        if (mUploadLinearLayout != null) {
            mUploadLinearLayout.setOrientation(portrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        }
        if (progressLayout != null) {
            progressLayout.getLayoutParams().height = (int) Utils.dpToPx(context, portrait ? 400 : 400);
            progressLayout.getLayoutParams().width = (int) Utils.dpToPx(context, portrait ? 400 : 300);
        }
        if (progressbar != null) {
            progressbar.setCircleRadius((int) Utils.dpToPx(context, portrait ? 140 : 110));
            progressbar.requestLayout();
            progressbar.postInvalidate();
        }

        setupButtons();
    }

    @Override
    public void onUploadProgressUpdate(UploadUpdate uploadUpdate) {
        if (!uploadStarted) {
            onUploadStarted();
        }
        double percentage = uploadUpdate.getPercentage();
        if (textViewProgress != null) {
            textViewProgress.setText(String.valueOf((int) (percentage * 100)));
        }
        if (progressbar != null) {
            progressbar.setProgress((float) percentage);
        }
        textViewBandwidth.setText(FormatUtils.formatBandwidth(uploadUpdate.getBandwidth()));
        textViewEta.setText(FormatUtils.formatETA(uploadUpdate.getEta()));
        textViewRemaining.setText(FormatUtils.formatSize(uploadUpdate.getCurrentUnit()));
        totalText.setText(FormatUtils.formatSize(uploadUpdate.getTotalUnit()));
    }

    @Override
    public void onUploadError() {
        Log.d(TAG, "onUploadError. Status: upload failed. Message: Restarting upload.");
    }

    @Override
    public void onNoInternetDetected() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> Toast.makeText(activity, activity.getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Context context = getContext();
        if (getActivity() != null && context != null) {
            int orientation = newConfig.orientation;
            boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
            if (mUploadLinearLayout != null) {
                mUploadLinearLayout.setOrientation(portrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            }
            if (progressLayout != null) {
                progressLayout.getLayoutParams().height = (int) Utils.dpToPx(context, portrait ? 400 : 400);
                progressLayout.getLayoutParams().width = (int) Utils.dpToPx(context, portrait ? 400 : 300);
            }
            if (progressbar != null) {
                progressbar.setCircleRadius((int) Utils.dpToPx(context, portrait ? 140 : 110));
                progressbar.requestLayout();
                progressbar.postInvalidate();
            }
        }
    }

    @Override
    public void onStop() {
        EventBus.unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Activity activity = getActivity();
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            if (uploadManager.isInProgress()) {
                uploadManager.removeUploadStatusListener();
                uploadManager.onViewDestroy();
            }
        }
        super.onDestroyView();
    }

    @Override
    public ToolbarSettings getToolbarSettings(KVToolbar kvToolbar) {
        //noinspection ConstantConditions since the method is called if the getActivity is not null in the base class
        return new ToolbarSettings.Builder()
                .setTitle(R.string.uploading_track_label)
                .setTextColor(getResources().getColor(R.color.default_gray_darker))
                .setNavigationIcon(R.drawable.vector_back_white, (v) -> getActivity().onBackPressed())
                .setBackgroundColor(getResources().getColor(R.color.default_black))
                .build();
    }

    @Override
    public boolean handleBackPressed() {
        if (getActivity() != null) {
            getActivity().finish();
        }
        return true;
    }

    private void setupUpload() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        Context context = activity.getApplicationContext();
        //initialize the upload manager and set all required listeners and initialize all the required listeners. This will be done for the first time upload
        // and not for the ones that are already in progress.
        uploadManager.setup(new SettingsUploadManual(context));
        uploadManager.start();
        uploadManager.onViewCreated();
        uploadManager.addUploadStatusListener(this);
    }

    private void initUploadUi() {
        uploadStarted = true;
        if (progressbar != null) {
            progressbar.setVisibility(View.VISIBLE);
        }
    }

    private void initUi() {
        textViewBandwidth = view.findViewById(R.id.upload_speed_text);
        mUploadLinearLayout = view.findViewById(R.id.upload_details_linear_layout);
        progressLayout = view.findViewById(R.id.progress_layout);
        textViewEta = view.findViewById(R.id.time_text);
        textViewProgress = view.findViewById(R.id.percent_text);
        textViewRemaining = view.findViewById(R.id.done_text);
        totalText = view.findViewById(R.id.total_text);
        progressbar = view.findViewById(R.id.upload_total_progress);
        stopButton = view.findViewById(R.id.cancel_all_button);
    }

    private void setupButtons() {
        if (stopButton != null) {
            stopButton.setOnClickListener(click -> uploadManager.stop());
        }
    }
}