package com.telenav.osv.ui.fragment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.skobbler.ngx.SKMaps;
import com.telenav.osv.R;
import com.telenav.osv.activity.LoginActivity;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.activity.WalkthroughActivity;
import com.telenav.osv.command.ObdCommand;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.databinding.FragmentSettingsBinding;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.network.LoginManager;
import com.telenav.osv.manager.obd.ObdManager;
import com.telenav.osv.ui.Navigator;
import com.telenav.osv.ui.binding.viewmodel.DefaultBindingComponent;
import com.telenav.osv.ui.binding.viewmodel.settings.SettingsViewModel;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import javax.inject.Inject;

import static com.telenav.osv.data.Preferences.URL_ENV;

/**
 * the new settings fragment
 * Created by kalmanb on 9/6/17.
 */
public class SettingsFragment extends OSVFragment {

  public static final String TAG = "SettingsFragment";

  @Inject
  LoginManager mLoginManager;

  @Inject
  ViewModelProvider.Factory mViewModelFactory;

  @Inject
  Preferences appPrefs;

  @Inject
  Recorder mRecorder;

  private SettingsViewModel viewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = ViewModelProviders.of(this, mViewModelFactory).get(SettingsViewModel.class);
    viewModel.setOwner(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    FragmentSettingsBinding binding =
        DataBindingUtil.inflate(inflater, R.layout.fragment_settings, null, false, new DefaultBindingComponent());
    binding.setViewModel(viewModel);
    binding.toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    OSVActivity activity = (OSVActivity) getActivity();
    appPrefs.getMapEnabledLive().observe(this, enabled -> {
      if (enabled != null) {
        boolean initialized = SKMaps.getInstance().isSKMapsInitialized();
        if ((enabled && !initialized) || (!enabled && initialized)) {
          activity.showSnackBar(R.string.restart_needed, Snackbar.LENGTH_INDEFINITE, getString(R.string.restart_label), () -> {
            Intent mStartActivity = new Intent(activity, SplashActivity.class);
            int mPendingIntentId = 123456;
            PendingIntent mPendingIntent =
                PendingIntent.getActivity(activity, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            if (mgr != null) {
              mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            }
            appPrefs.setCrashed(false);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
          });
        } else {
          activity.hideSnackBar();
        }
      }
    });
    viewModel.logEvent.observe(this, o -> {
      if (appPrefs.loggedIn()) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
        builder.setMessage(activity.getString(R.string.logout_confirmation_message)).setTitle(activity.getString(R.string.log_out))
            .setNegativeButton(R.string.cancel_label, (dialog, which) -> {/*nothing*/})
            .setPositiveButton(R.string.log_out, (dialog, which) -> mLoginManager.logout()).create().show();
      } else {
        if (Utils.isInternetAvailable(activity)) {
          activity.startActivity(new Intent(activity, LoginActivity.class));
        } else {
          activity.showSnackBar(R.string.check_internet_connection, Snackbar.LENGTH_LONG);
        }
      }
    });
    viewModel.reportEvent.observe(this, o -> activity.openScreen(Navigator.SCREEN_REPORT));
    viewModel.feedbackEvent.observe(this, o -> {
      Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/openstreetview/android/issues"));
      startActivity(browserIntent);
    });
    viewModel.policyEvent.observe(this, o -> {
      Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.skobbler.com/legal#privacy"));
      startActivity(browserIntent);
    });
    viewModel.termsEvent.observe(this, o -> {
      Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://openstreetcam.org/terms/"));
      startActivity(browserIntent);
    });
    viewModel.obdConnectEvent.observe(this, o -> {
      final ObdManager obdManager = mRecorder.getOBDManager();
      if (!ObdManager.isConnected()) {
        if (obdManager.isFunctional(activity)) {
          Log.d(TAG, "obdContainerPressed: isFunctional = true");
        }
      } else {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
        builder.setMessage(activity.getString(R.string.disconnect_obd_message)).setTitle(activity.getString(R.string.obd_label))
            .setNegativeButton(R.string.cancel_label, (dialog, which) -> Log.d(TAG, "onViewCreated: cancelled dialog"))
            .setPositiveButton(R.string.disconnect, (dialog, which) -> EventBus.postSticky(new ObdCommand(false)))
            .create().show();
      }
    });
    viewModel.connectionSettingEvent.observe(this, o -> {
      OBDDialogFragment fragment = new OBDDialogFragment();
      fragment.setTypeSelectedListener(type -> {
        int saved = appPrefs.getObdType();
        if (type != saved) {
          if (ObdManager.isConnected()) {
            EventBus.postSticky(new ObdCommand(false));
          }
          appPrefs.setObdType(type);
        }
      });
      fragment.show(activity.getSupportFragmentManager(), OBDDialogFragment.TAG);
    });
    viewModel.resolutionSettingEvent.observe(this, o -> {
      PictureSizeDialogFragment fragmentPictureSize = new PictureSizeDialogFragment();
      fragmentPictureSize.setPreviewSizes(appPrefs.getSupportedResolutions());
      fragmentPictureSize.show(activity.getSupportFragmentManager(), PictureSizeDialogFragment.TAG);
    });
    viewModel.tipsEvent.observe(this, o -> activity.openScreen(Navigator.SCREEN_RECORDING_HINTS));
    viewModel.walkthroughEvent.observe(this, o -> {
      Intent intent = new Intent(activity, WalkthroughActivity.class);
      startActivity(intent);
    });
    viewModel.serverSettingEvent.observe(this, o -> {
      int currentServer = appPrefs.getServerType();
      currentServer = (currentServer + 1) % URL_ENV.length;
      appPrefs.setServerType(currentServer);
      activity.showSnackBar(R.string.restart_needed, Snackbar.LENGTH_SHORT, getString(R.string.restart_label), () -> {
        Intent mStartActivity = new Intent(activity, SplashActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent =
            PendingIntent.getActivity(activity, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        if (mgr != null) {
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        }
        appPrefs.setCrashed(false);
        System.exit(0);
      });
    });
    viewModel.snackbarMessage.observe(this, i -> {
      if (i != null) {
        if (i > 0) {
          activity.showSnackBar(i + " " + getString(R.string.unlock_debug_message), Snackbar.LENGTH_SHORT);
        } else {
          activity.showSnackBar(getString(R.string.debug_settings_notification), Snackbar.LENGTH_SHORT);
        }
      }
    });
  }
}
