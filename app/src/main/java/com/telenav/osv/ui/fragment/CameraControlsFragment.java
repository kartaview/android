package com.telenav.osv.ui.fragment;

import java.text.DecimalFormat;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.listener.AccuracyListener;
import com.telenav.osv.listener.RecordingStateChangeListener;
import com.telenav.osv.manager.LocationManager;
import com.telenav.osv.manager.ShutterManager;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.ui.ShutterButton;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * camera control buttons ui
 * Created by Kalman on 14/07/16.
 */

public class CameraControlsFragment extends Fragment implements AccuracyListener, View.OnClickListener, RecordingStateChangeListener {

    public static final String TAG = "CameraControlsFragment";

    private View view;

    private ShutterButton mShutterButton;

    private MainActivity activity;

    private CameraHandlerService mCameraHandlerService;

    private RelativeLayout mInfoButton;

    private View mCancelAndHomeText;

    private TextView mDistanceText;

    private TextView mPicturesText;

    private TextView mSizeText;

    private TextView mDistanceUnitText;

    private TextView mSizeUnitText;


    private LinearLayout mRecordingDetailsLayout;

    private LocationManager mLocationManager;

    private ShutterManager mShutterManager;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private ApplicationPreferences appPrefs;

    private View mControlsView;

    private View mControlsViewH;

    public static String[] formatSize(double value) {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_camera_controls, null);
        activity = (MainActivity) getActivity();
        mControlsView = inflater.inflate(R.layout.camera_controls, null);
        mControlsViewH = inflater.inflate(R.layout.camera_controls_landscape, null);

        appPrefs = activity.getApp().getAppPrefs();

        mLocationManager = ((OSVApplication) getActivity().getApplication()).getLocationManager();

        mLocationManager.setAccuracyListener(this);
        initLayouts();
        if (activity.mCameraHandlerService != null) {
            onCameraServiceConnected(activity.mCameraHandlerService);
        }
        return view;
    }

    public void onCameraServiceConnected(CameraHandlerService service) {
        mCameraHandlerService = service;
        mShutterManager = mCameraHandlerService.mShutterManager;
        if (mShutterManager != null) {
            Log.d(TAG, "Create mShutterButton");
            if (mShutterButton != null) {
                mShutterButton.setShutterManager(mShutterManager);
            }
            if (mRecordingDetailsLayout != null && mCancelAndHomeText != null) {
                if (mShutterManager.isRecording()) {
                    mRecordingDetailsLayout.setVisibility(View.VISIBLE);
                    if (mCancelAndHomeText instanceof TextView) {
                        ((TextView) mCancelAndHomeText).setText(R.string.home_label);
                    }
                } else {
                    mRecordingDetailsLayout.setVisibility(View.INVISIBLE);
                    if (mCancelAndHomeText instanceof TextView) {
                        ((TextView) mCancelAndHomeText).setText(R.string.cancel_label);
                    }
                }
                refreshDetails();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initLayouts();

        if (mCameraHandlerService != null && mCameraHandlerService.mShutterManager != null) {
            int orientation = activity.getResources().getConfiguration().orientation;
            boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
            if (mShutterManager.isRecording()) {
                showDetails(portrait);
                if (!appPrefs.getBooleanPreference(PreferenceTypes.K_HINT_BACKGROUND, false)) {
                    final Snackbar snack = Snackbar.make(view, R.string.turn_off_screen_hint, Snackbar
                            .LENGTH_INDEFINITE);

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
            } else {
                hideDetails(portrait);
            }
            refreshDetails();
        }
        Log.d(TAG, "onConfigurationChanged: ");
    }

    public void initLayouts() {
        Log.d(TAG, "InitLayouts");

        View current;
        int orientation = activity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            current = mControlsView;
        } else {
            current = mControlsViewH;
        }
        ((ViewGroup) view).removeAllViews();
        ((ViewGroup) view).addView(current);
        // Setup shutter button

        mRecordingDetailsLayout = (LinearLayout) current.findViewById(R.id.track_details);
        mPicturesText = (TextView) current.findViewById(R.id.images_recorded_value);
        mDistanceText = (TextView) current.findViewById(R.id.distance_covered_value);
        mDistanceUnitText = (TextView) current.findViewById(R.id.distance_covered_text);
        mSizeText = (TextView) current.findViewById(R.id.size_recorder_images_value);
        mSizeUnitText = (TextView) current.findViewById(R.id.size_recorder_images_text);
        mInfoButton = (RelativeLayout) current.findViewById(R.id.info_button_layout);
        mInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.openScreen(MainActivity.SCREEN_RECORDING_HINTS);
            }
        });
        mShutterButton = (ShutterButton) current.findViewById(R.id.btn_shutter);

        mCancelAndHomeText = current.findViewById(R.id.cancel_text_camera_preview);

        mCancelAndHomeText.setOnClickListener(this);
        if (mCameraHandlerService != null && mCameraHandlerService.mShutterManager != null) {
            mShutterButton.setShutterManager(mCameraHandlerService.mShutterManager);
            if (mShutterManager.isRecording()) {
                mRecordingDetailsLayout.setVisibility(View.VISIBLE);
                if (mCancelAndHomeText instanceof TextView) {
                    ((TextView) mCancelAndHomeText).setText(R.string.home_label);
                }
            } else {
                mRecordingDetailsLayout.setVisibility(View.INVISIBLE);
                if (mCancelAndHomeText instanceof TextView) {
                    ((TextView) mCancelAndHomeText).setText(R.string.cancel_label);
                }
            }
            refreshDetails();
        }
    }

    @Override
    public void onResume() {
        mLocationManager.setAccuracyListener(this);
        refreshDetails();
        super.onResume();
    }

    @Override
    public void onPause() {
        mLocationManager.setAccuracyListener(null);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void refreshAccuracyDetails(final float accuracy) {
        if (activity != null && mInfoButton != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    mInfoButton.setText(getAccuracyStatus(accuracy));
                    Log.d(TAG, "Accuracy refreshGPSDetails is: " + getAccuracyStatus(accuracy));
                }
            });
        }
    }

    private String getAccuracyStatus(float accuracy) {
        String textAccuracy;
        if (accuracy <= LocationManager.ACCURACY_GOOD) {
            textAccuracy = getString(R.string.gps_ok_label);
        } else if (accuracy <= LocationManager.ACCURACY_MEDIUM) {
            textAccuracy = getString(R.string.gps_medium_label);
        } else {
            textAccuracy = getString(R.string.gps_bad_label);
        }
        Log.d(TAG, "Accuracy refreshGPSDetails is: " + textAccuracy);
        return textAccuracy;
    }

    @Override
    public void onAccuracyChanged(float accuracy) {
        Log.d(TAG, "Accuracy is: " + accuracy + " " + accuracy);
        refreshAccuracyDetails(accuracy);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case (R.id.cancel_text_camera_preview):
                if (mShutterManager != null && mShutterManager.isRecording()) {
                    activity.mNeedsToExit = true;
                    mShutterManager.stopSequence();
                } else {
                    activity.onBackPressed();
                }
                break;
        }


    }

    public void refreshDetails() {
        if (mCameraHandlerService != null && view != null && mCameraHandlerService.mShutterManager != null) {
            Sequence sequence = mCameraHandlerService.mShutterManager.getSequence();
            float space = 0;
            if (sequence != null && sequence.folder != null) {
                space = (float) Utils.folderSize(sequence.folder);
            }
//            float reserved = appPrefs.getFloatPreference(PreferenceTypes.K_RESERVED_FREE_SPACE, 300);
//            float pictureSize = Utils.getPictureSize(activity);
//            final int possibleImages = (int) ((space - reserved) / pictureSize);
            final float finalSpace = space;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String[] distance = Utils.formatDistanceFromMeters(activity, (int) mCameraHandlerService.mShutterManager.getAproximateDistance());
                    String[] size = formatSize(finalSpace);
                    mDistanceText.setText(distance[0]);
                    mDistanceUnitText.setText(distance[1]);
                    mSizeText.setText(size[0]);
                    mSizeUnitText.setText(size[1]);
                    mPicturesText.setText(mCameraHandlerService.mShutterManager.getNumberOfPictures() + "");

                    if (mCameraHandlerService.mShutterManager.getPictureIndex() == 5 && !appPrefs.getBooleanPreference(PreferenceTypes.K_HINT_TAP_TO_SHOOT, false)) {
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

    public void showDetails(final boolean portrait) {
        mRecordingDetailsLayout.setVisibility(View.VISIBLE);
        mShutterButton.started();
        if (portrait) {
            if (mCancelAndHomeText instanceof TextView) {
                ((TextView) mCancelAndHomeText).setText(R.string.home_label);
            }
        }
    }

    public void hideDetails(final boolean portrait) {
        mRecordingDetailsLayout.setVisibility(View.INVISIBLE);
        mShutterButton.stopped();
        if (portrait) {
            if (mCancelAndHomeText instanceof TextView) {
                ((TextView) mCancelAndHomeText).setText(R.string.cancel_label);
            }
        }
    }

    @Override
    public void onRecordingStatusChanged(boolean started) {
        int orientation = activity.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        if (started) {
            showDetails(portrait);
            if (!appPrefs.getBooleanPreference(PreferenceTypes.K_HINT_BACKGROUND, false)) {
                final Snackbar snack = Snackbar.make(view, R.string.turn_off_screen_hint, Snackbar
                        .LENGTH_INDEFINITE);

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
        } else {
            hideDetails(portrait);
        }
        refreshDetails();
    }

    public void pressShutterButton() {
        boolean ok = activity.checkPermissionsForRecording();
        if (!ok) {
            return;
        }
        if (mShutterManager == null) return;

        if (!mShutterManager.isRecording()) {
            if (!mLocationManager.isGPSEnabled()) {
                if (getContext() instanceof MainActivity) {
                    ((MainActivity) getContext()).resolveLocationProblem(true);
                }
                return;
            }
            if (!mLocationManager.hasPosition()) {
                if (getContext() instanceof MainActivity) {
                    ((MainActivity) getContext()).showSnackBar(R.string.no_gps_message, Snackbar.LENGTH_SHORT);
                }
                return;
            }
            mShutterButton.started();
            mShutterManager.startSequence();
        } else {
            mShutterManager.stopSequence();
            mShutterButton.stopped();
        }
    }
}


