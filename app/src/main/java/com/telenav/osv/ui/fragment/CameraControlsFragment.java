package com.telenav.osv.ui.fragment;

import java.text.DecimalFormat;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.animation.Animator;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.ImageSavedEvent;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.ui.RecordingVisibleEvent;
import com.telenav.osv.event.ui.ShutterPressEvent;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * camera control buttons ui
 * Created by Kalman on 14/07/16.
 */

public class CameraControlsFragment extends FunctionalFragment implements View.OnClickListener {

    public static final String TAG = "CameraControlsFragment";

    private View view;

    private ImageView mShutterButton;

    private MainActivity activity;

    private RelativeLayout mInfoButton;

    private View mCancelAndHomeText;

    private TextView mDistanceText;

    private TextView mPicturesText;

    private TextView mSizeText;

    private TextView mDistanceUnitText;

    private TextView mSizeUnitText;

    private LinearLayout mRecordingDetailsLayout;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private ApplicationPreferences appPrefs;

    private View mControlsView;

    private View mControlsViewH;

    private Recorder mRecorder;

    private LinearLayout mHintLayout;

    private ProgressWheel mShutterProgress;

    private View.OnClickListener shutterListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            pressShutterButton(null);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_camera_controls, null);
        activity = (MainActivity) getActivity();
        mControlsView = inflater.inflate(R.layout.partial_camera_controls, null);
        mControlsViewH = inflater.inflate(R.layout.partial_camera_controls_landscape, null);

        appPrefs = activity.getApp().getAppPrefs();
        boolean portrait = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        initLayouts(portrait);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int orientation = newConfig.orientation;
        final boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        initLayouts(portrait);

        Log.d(TAG, "onConfigurationChanged: ");
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        EventBus.unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case (R.id.cancel_text_camera_preview):
                activity.openScreen(ScreenComposer.SCREEN_MAP);
                if (mRecorder != null && mRecorder.isRecording()) {
                    mRecorder.stopRecording();
                }
                break;
        }
    }

    @Override
    public void setRecorder(Recorder recorder) {
        this.mRecorder = recorder;
    }

    private static String[] formatSize(double value) {
        String[] sizeText = new String[]{"0", " MB"};
        if (value > 0) {
            double size = value / (double) 1024 / (double) 1024;
            String type = " MB";
            DecimalFormat df2 = new DecimalFormat("#.#");
            if (size > 1024) {
                size = (size / (double) 1024);
                type = " GB";
                sizeText = new String[]{"" + df2.format(size), type};
            } else {
                sizeText = new String[]{"" + (int) size, type};
            }
        }
        return sizeText;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onImageSaved(ImageSavedEvent event) {
        Log.d(TAG, "onImageSaved: called");
        float space = 0;
        if (event.sequence != null) {
            if (event.sequence.getFolder() != null) {
                space = (float) Utils.folderSize(event.sequence.getFolder());
            }
            refreshDetails(space, event.sequence.getDistance(), event.sequence.getFrameCount());
        }
    }

    @Subscribe
    public void onScreenVisible(RecordingVisibleEvent event) {
        if (mInfoButton != null) {
            mInfoButton.animate().scaleXBy(0.3f).scaleYBy(0.3f).setDuration(300).setListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mInfoButton.animate().scaleXBy(-0.3f).scaleYBy(-0.3f).setDuration(500).setInterpolator(new BounceInterpolator())
                            .setListener(new Animator.AnimatorListener() {

                                @Override
                                public void onAnimationStart(Animator animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mInfoButton.clearAnimation();
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    mInfoButton.clearAnimation();
                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {
                                }
                            }).start();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mInfoButton.clearAnimation();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            }).start();
        }
    }

    public void refreshDetails(final float spaceOccupied, final int distance, final int imageCount) {
        if (view != null && mRecorder != null) {

            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mDistanceText != null && mDistanceUnitText != null && mSizeText != null && mPicturesText != null && mSizeUnitText != null) {
                        String[] dist = Utils.formatDistanceFromMeters(activity, distance);
                        String[] size = formatSize(spaceOccupied);
                        mDistanceText.setText(dist[0]);
                        mDistanceUnitText.setText(dist[1]);
                        mSizeText.setText(size[0]);
                        mSizeUnitText.setText(size[1]);
                        mPicturesText.setText(imageCount + "");
                    }

                    if (imageCount == 5 && !appPrefs.getBooleanPreference(PreferenceTypes.K_HINT_TAP_TO_SHOOT, false)) {
                        Snackbar snack = Snackbar.make(view, R.string.tap_to_shoot_hint, Snackbar.LENGTH_LONG);
                        snack.setAction(R.string.got_it_label, new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {
                                appPrefs.saveBooleanPreference(PreferenceTypes.K_HINT_TAP_TO_SHOOT, true);
                            }
                        });
                        snack.show();
                    }
                }
            });
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onRecordingStatusChanged(RecordingEvent event) {
        int orientation = activity.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        if (event.started) {
            showDetails(portrait);
            if (!appPrefs.getBooleanPreference(PreferenceTypes.K_HINT_BACKGROUND, false)) {
                final Snackbar snack = Snackbar.make(view, R.string.turn_off_screen_hint, Snackbar.LENGTH_INDEFINITE);

                snack.setAction(R.string.got_it_label, new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        appPrefs.saveBooleanPreference(PreferenceTypes.K_HINT_BACKGROUND, true);
                    }
                });
                snack.show();
                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        snack.dismiss();
                    }
                }, 5000);
            }
            float space = 0;
            if (event.sequence != null) {
                if (event.sequence.getFolder() != null) {
                    space = (float) Utils.folderSize(event.sequence.getFolder());
                }
                refreshDetails(space, event.sequence.getDistance(), event.sequence.getFrameCount());
            }
        } else {
            displayProgress(false);
            hideDetails(portrait);
            refreshDetails(0, 0, 0);
        }
    }

    @Subscribe
    public void pressShutterButton(ShutterPressEvent event) {
        if (!activity.checkPermissionsForRecording()) {
            return;
        }
        if (mRecorder == null) {
            return;
        }

        if (!mRecorder.isRecording()) {
            if (!Utils.isGPSEnabled(activity)) {
                if (activity instanceof MainActivity) {
                    activity.resolveLocationProblem(true);
                }
                return;
            }
            started();
            mRecorder.startRecording();
        } else {
            stopped();
            displayProgress(true);
            mRecorder.stopRecording();
        }
    }

    private void initLayouts(final boolean portrait) {
        Log.d(TAG, "InitLayouts");
        boolean finishing = false;
        ProgressWheel oldProgress = mShutterProgress;
        if (oldProgress != null) {
            if (oldProgress.getVisibility() == View.VISIBLE) {
                Log.d(TAG, "progress: finishing ");
                finishing = true;
            }
        }
        final View current;
        if (portrait) {
            current = mControlsView;
        } else {
            current = mControlsViewH;
        }
        ((ViewGroup) view).removeAllViews();
        final boolean finalFinishing = finishing;
        view.post(new Runnable() {

            @Override
            public void run() {
                try {
                    ((ViewGroup) current.getParent()).removeAllViews();
                } catch (Exception ignored) {
                }
                ((ViewGroup) view).addView(current);
                // Setup shutter button

                mRecordingDetailsLayout = current.findViewById(R.id.track_details);
                if (portrait) {
                    mHintLayout = current.findViewById(R.id.record_hint_layout);
                }
                mPicturesText = current.findViewById(R.id.images_recorded_value);
                mDistanceText = current.findViewById(R.id.distance_covered_value);
                mDistanceUnitText = current.findViewById(R.id.distance_covered_text);
                mSizeText = current.findViewById(R.id.size_recorder_images_value);
                mSizeUnitText = current.findViewById(R.id.size_recorder_images_text);
                mInfoButton = current.findViewById(R.id.info_button_layout);
                mInfoButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        activity.openScreen(ScreenComposer.SCREEN_RECORDING_HINTS);
                    }
                });
                mInfoButton.clearAnimation();
                mShutterButton = current.findViewById(R.id.btn_shutter);
                mShutterProgress = current.findViewById(R.id.shutter_progress);
                mShutterButton.setOnClickListener(shutterListener);
                mCancelAndHomeText = current.findViewById(R.id.cancel_text_camera_preview);

                mCancelAndHomeText.setOnClickListener(CameraControlsFragment.this);
                mShutterButton.setOnClickListener(shutterListener);

                displayProgress(finalFinishing);

                if (mRecordingDetailsLayout != null && mCancelAndHomeText != null) {
                    if (mRecorder != null && (mRecorder.isRecording() || finalFinishing)) {
                        showDetails(portrait);
                        if (!appPrefs.getBooleanPreference(PreferenceTypes.K_HINT_BACKGROUND, false)) {
                            final Snackbar snack = Snackbar.make(view, R.string.turn_off_screen_hint, Snackbar.LENGTH_INDEFINITE);

                            snack.setAction(R.string.got_it_label, new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    appPrefs.saveBooleanPreference(PreferenceTypes.K_HINT_BACKGROUND, true);
                                }
                            });
                            snack.show();
                            mHandler.postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    snack.dismiss();
                                }
                            }, 5000);
                        }
                        if (mCancelAndHomeText instanceof TextView) {
                            ((TextView) mCancelAndHomeText).setText(R.string.home_label);
                        }
                    } else {
                        hideDetails(portrait);
                        if (mCancelAndHomeText instanceof TextView) {
                            ((TextView) mCancelAndHomeText).setText(R.string.cancel_label);
                        }
                        refreshDetails(0, 0, 0);
                    }
                }
            }
        });
    }

    private void showDetails(final boolean portrait) {
        if (mHintLayout != null) {
            mHintLayout.setVisibility(View.INVISIBLE);
        }
        mRecordingDetailsLayout.setVisibility(View.VISIBLE);
        started();
        if (portrait) {
            if (mCancelAndHomeText instanceof TextView) {
                ((TextView) mCancelAndHomeText).setText(R.string.home_label);
            }
        }
    }

    private void hideDetails(final boolean portrait) {
        mRecordingDetailsLayout.setVisibility(View.INVISIBLE);
        if (mHintLayout != null) {
            mHintLayout.setVisibility(View.VISIBLE);
        }
        stopped();
        if (portrait) {
            if (mCancelAndHomeText instanceof TextView) {
                ((TextView) mCancelAndHomeText).setText(R.string.cancel_label);
            }
        }
    }

    private void displayProgress(boolean show) {
        if (mShutterButton != null && mShutterProgress != null) {
            Log.d(TAG, "displayProgress: " + show + " on " + mShutterProgress);
            mShutterProgress.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            mShutterButton.setEnabled(!show);
        }
    }

    private void started() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mShutterButton.setImageDrawable(activity.getDrawable(R.drawable.vector_stop_recording));
        } else {
            mShutterButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_stop_recording));
        }
    }

    private void stopped() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mShutterButton.setImageDrawable(activity.getDrawable(R.drawable.vector_button_record_inactive_2));
        } else {
            mShutterButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.vector_button_record_inactive_2));
        }
    }


}