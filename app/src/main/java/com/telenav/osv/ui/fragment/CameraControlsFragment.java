package com.telenav.osv.ui.fragment;

import android.animation.Animator;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.ImageSavedEvent;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.ui.RecordingVisibleEvent;
import com.telenav.osv.event.ui.ShutterPressEvent;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.ui.Navigator;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import java.text.DecimalFormat;
import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NonNls;

/**
 * camera control buttons ui
 * Created by Kalman on 14/07/16.
 */

public class CameraControlsFragment extends OSVFragment implements View.OnClickListener {

  public static final String TAG = "CameraControlsFragment";

  @Inject
  Preferences appPrefs;

  @Inject
  Recorder mRecorder;

  @Inject
  ValueFormatter valueFormatter;

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

  private View mControlsView;

  private View mControlsViewH;

  private LinearLayout mHintLayout;

  private ProgressWheel mShutterProgress;

  private View.OnClickListener shutterListener = v -> pressShutterButton(null);

  private static String[] formatSize(double value) {
    @NonNls String[] sizeText = new String[] {"0", " MB"};
    if (value > 0) {
      double size = value / (double) 1024 / (double) 1024;
      @NonNls String type = " MB";
      DecimalFormat df2 = new DecimalFormat("#.#");
      if (size > 1024) {
        size = (size / (double) 1024);
        type = " GB";
        sizeText = new String[] {"" + df2.format(size), type};
      } else {
        sizeText = new String[] {"" + (int) size, type};
      }
    }
    return sizeText;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    view = inflater.inflate(R.layout.fragment_camera_controls, null);
    activity = (MainActivity) getActivity();
    mControlsView = inflater.inflate(R.layout.partial_camera_controls, null);
    mControlsViewH = inflater.inflate(R.layout.partial_camera_controls_landscape, null);

    //the scope of this fragment is activity scope (it is attached all the time just like preview and map)
    // and we need to observe changes happening in lower scopes like settings fragment
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
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    int orientation = newConfig.orientation;
    final boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
    initLayouts(portrait);

    Log.d(TAG, "onConfigurationChanged: ");
  }


  @Override
  public void onStop() {
    EventBus.unregister(this);
    super.onStop();
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
    view.post(() -> {
      ViewParent viewGroup = current.getParent();
      if (viewGroup != null) {
        ((ViewGroup) viewGroup).removeAllViews();
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
      mInfoButton.setOnClickListener(v -> activity.openScreen(Navigator.SCREEN_RECORDING_HINTS));
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
          if (appPrefs.shouldShowBackgroundHint()) {
            final Snackbar snack = Snackbar.make(view, R.string.turn_off_screen_hint, Snackbar.LENGTH_INDEFINITE);

            snack.setAction(R.string.got_it_label, v -> appPrefs.setShouldShowBackgroundHint(false));
            snack.show();
            mHandler.postDelayed(snack::dismiss, 5000);
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
    });
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

      activity.runOnUiThread(() -> {
        if (mDistanceText != null && mDistanceUnitText != null && mSizeText != null && mPicturesText != null && mSizeUnitText != null) {
          String[] dist = valueFormatter.formatDistanceFromMeters(distance);
          String[] size = formatSize(spaceOccupied);
          mDistanceText.setText(dist[0]);
          mDistanceUnitText.setText(dist[1]);
          mSizeText.setText(size[0]);
          mSizeUnitText.setText(size[1]);
          mPicturesText.setText(imageCount + "");
        }

        if (imageCount == 5 && appPrefs.shouldShowTapToShoot()) {
          Snackbar snack = Snackbar.make(view, R.string.tap_to_shoot_hint, Snackbar.LENGTH_LONG);
          snack.setAction(R.string.got_it_label, view -> appPrefs.setShouldShowTapToShoot(false));
          snack.show();
        }
      });
    }
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

  @Override
  public void onClick(View v) {

    switch (v.getId()) {
      case (R.id.cancel_text_camera_preview):
        activity.openScreen(Navigator.SCREEN_MAP);
        if (mRecorder != null && mRecorder.isRecording()) {
          mRecorder.stopRecording();
        }
        break;
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

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onRecordingStatusChanged(RecordingEvent event) {
    int orientation = activity.getResources().getConfiguration().orientation;
    boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
    if (event.started) {
      showDetails(portrait);
      if (appPrefs.shouldShowBackgroundHint()) {
        final Snackbar snack = Snackbar.make(view, R.string.turn_off_screen_hint, Snackbar.LENGTH_INDEFINITE);

        snack.setAction(R.string.got_it_label, v -> appPrefs.setShouldShowBackgroundHint(false));
        snack.show();
        mHandler.postDelayed(snack::dismiss, 5000);
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

  private void displayProgress(boolean show) {
    if (mShutterButton != null && mShutterProgress != null) {
      Log.d(TAG, "displayProgress: " + show + " on " + mShutterProgress);
      mShutterProgress.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
      mShutterButton.setEnabled(!show);
    }
  }

  private void started() {
    mShutterButton.setImageDrawable(activity.getDrawable(R.drawable.vector_stop_recording));
  }

  private void stopped() {
    mShutterButton.setImageDrawable(activity.getDrawable(R.drawable.vector_button_record_inactive_2));
  }
}