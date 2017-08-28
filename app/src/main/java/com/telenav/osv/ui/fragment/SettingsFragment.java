package com.telenav.osv.ui.fragment;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.crashlytics.android.Crashlytics;
import com.telenav.osv.R;
import com.telenav.osv.activity.LoginActivity;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.activity.WalkthroughActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.CameraConfigChangedCommand;
import com.telenav.osv.command.LogoutCommand;
import com.telenav.osv.command.ObdCommand;
import com.telenav.osv.command.SignDetectInitCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.SdkEnabledEvent;
import com.telenav.osv.event.hardware.obd.ObdStatusEvent;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.event.ui.GamificationSettingEvent;
import com.telenav.osv.event.ui.UserTypeChangedEvent;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.manager.obd.ObdManager;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;
import io.fabric.sdk.android.Fabric;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Fragment holding the ui for the settings
 * Created by Kalman on 10/2/2015.
 */
public class SettingsFragment extends FunctionalFragment implements View.OnClickListener {

  public static final String TAG = "SettingsFragment";

  private TextView logInButton;

  private View view;

  private SwitchCompat autoSwitch;

  private SwitchCompat chargingSwitch;

  private SwitchCompat dataSwitch;

  private SwitchCompat metricSwitch;

  private SwitchCompat mapSwitch;

  private SwitchCompat sdkSwitch;

  private SwitchCompat gameSwitch;

  private SwitchCompat safeModeSwitch;

  private SwitchCompat focusModeSwitch;

  private SwitchCompat apiModeSwitch;

  private SwitchCompat signDetectionSwitch;

  private ApplicationPreferences appPrefs;

  private MainActivity activity;

  private View.OnClickListener loginListener;

  private View.OnClickListener logoutListener;

  private SwitchCompat debugSwitch;

  //    private SwitchCompat hdrSwitch;

  private SwitchCompat authSwitch;

  private SwitchCompat speedSwitch;

  private TextView serverText;

  private LinearLayout mFeedbackButton;

  private LinearLayout mTipsButton;

  private SwitchCompat storageSwitch;

  private TextView mObdTitle;

  private TextView mObdButton;

  private View mOBDProgressBar;

  private TextView obdTypeText;

  private Recorder mRecorder;

  private LinearLayout mWalkthroughButton;

  private LinearLayout mReportButton;

  @Override
  public void setRecorder(Recorder recorder) {
    mRecorder = recorder;
  }

  private void setupDebugSettings() {
    debugSwitch = view.findViewById(R.id.debug_switch);
    authSwitch = view.findViewById(R.id.debug_auth_switch);
    speedSwitch = view.findViewById(R.id.debug_speed_switch);
    serverText = view.findViewById(R.id.server_text);
    boolean isDebug = appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED, false);
    if (isDebug) {
      view.findViewById(R.id.debug_setting_container).setVisibility(View.VISIBLE);
      view.findViewById(R.id.debugText).setVisibility(View.VISIBLE);
    }
    debugSwitch.setChecked(isDebug);
    int currentServer = appPrefs.getIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE);
    serverText.setText(UploadManager.URL_ENV[currentServer]);
    view.findViewById(R.id.debug_options_container).setVisibility(isDebug ? View.VISIBLE : View.GONE);

    view.findViewById(R.id.debug_setting_container).setOnClickListener(this);
    view.findViewById(R.id.debug_auth_container).setOnClickListener(this);
    view.findViewById(R.id.debug_server_container).setOnClickListener(this);
    view.findViewById(R.id.debug_speed_container).setOnClickListener(this);

    debugSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Utils.DEBUG = isChecked;
      appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED, isChecked);
      view.findViewById(R.id.debug_options_container).setVisibility(isChecked ? View.VISIBLE : View.GONE);
      if (!isChecked) {
        CookieSyncManager.createInstance(activity);
        CookieManager.getInstance().removeAllCookie();
      }
    });

    //switches
    authSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_SAVE_AUTH, isChecked);
      if (!isChecked) {
        //clear auth cache
        CookieSyncManager.createInstance(activity);
        CookieManager.getInstance().removeAllCookie();
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(activity).edit();
        editor.clear();
        editor.apply();
      }
    });

    speedSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_SPEED_DIST, isChecked));
    authSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SAVE_AUTH, false));
    speedSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SPEED_DIST, false));
  }

  private void setListeners() {
    //login button
    loginListener = v -> {
      if (Utils.isInternetAvailable(activity)) {
        activity.startActivity(new Intent(activity, LoginActivity.class));
      } else {
        activity.showSnackBar(R.string.check_internet_connection, Snackbar.LENGTH_LONG);
      }
    };
    logoutListener = v -> {
      final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
      builder.setMessage(activity.getString(R.string.logout_confirmation_message)).setTitle(activity.getString(R.string.log_out))
          .setNegativeButton(R.string.cancel_label, (dialog, which) -> {

          }).setPositiveButton(R.string.log_out, (dialog, which) -> EventBus.post(new LogoutCommand())).create().show();
    };
    if (!"".equals(appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME))) {
      logInButton.setText(activity.getString(R.string.log_out) + " (" + appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME) + ")");
      logInButton.setOnClickListener(logoutListener);
    } else {
      logInButton.setOnClickListener(loginListener);
    }

    //switches
    autoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      appPrefs.saveBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO, isChecked);
      view.findViewById(R.id.charging_setting_separator).setVisibility(!isChecked ? View.GONE : View.VISIBLE);
      view.findViewById(R.id.charging_setting_container).setVisibility(!isChecked ? View.GONE : View.VISIBLE);
      if (!isChecked) {
        ((OSVApplication) activity.getApplication()).getUploadManager().pauseUpload();
        UploadManager.cancelAutoUpload(activity);
      } else {
        UploadManager.scheduleAutoUpload(activity);
      }
    });
    chargingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      appPrefs.saveBooleanPreference(PreferenceTypes.K_UPLOAD_CHARGING, isChecked);
      if (appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO)) {
        UploadManager.scheduleAutoUpload(activity);
      }
    });
    dataSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      appPrefs.saveBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED, isChecked);
      if (!isChecked) {
        UploadManager uploadManager = ((OSVApplication) activity.getApplication()).getUploadManager();
        if (uploadManager.isUploading() && !uploadManager.isPaused() && !NetworkUtils.isWifiInternetAvailable(activity) &&
            NetworkUtils.isInternetAvailable(activity)) {
          uploadManager.pauseUpload();
        }
      }
    });
    storageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (UploadManager.sUploadStatus != UploadManager.STATUS_IDLE) {
        activity.showSnackBar(getString(R.string.not_allowed_while_upload), Snackbar.LENGTH_LONG);
        return;
      }
      appPrefs.saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE, isChecked);
      Utils.generateOSVFolder(activity);
      Utils.getSelectedStorage(activity);
    });
    metricSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> appPrefs.saveBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC, isChecked));
    signDetectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      appPrefs.saveBooleanPreference(PreferenceTypes.K_SIGN_DETECTION_ENABLED, isChecked);
      if (isChecked) {
        EventBus.post(new SignDetectInitCommand(true));
      } else {
        EventBus.post(new SignDetectInitCommand(false));
      }
    });
    sdkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      EventBus.postSticky(new SdkEnabledEvent(isChecked));
      appPrefs.saveBooleanPreference(PreferenceTypes.K_MAP_DISABLED, !isChecked);
      view.findViewById(R.id.map_visibility_container).setVisibility(!isChecked ? View.GONE : View.VISIBLE);
      view.findViewById(R.id.map_visibility_separator).setVisibility(!isChecked ? View.GONE : View.VISIBLE);
      activity.showSnackBar(R.string.restart_needed, Snackbar.LENGTH_INDEFINITE, getString(R.string.restart_label), () -> {
        Intent mStartActivity = new Intent(activity, SplashActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent =
            PendingIntent.getActivity(activity, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, false);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
      });
    });
    mapSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> appPrefs.saveBooleanPreference(PreferenceTypes.K_RECORDING_MAP_ENABLED, isChecked));
    gameSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Log.d(TAG, "onCheckedChanged: gameSwitch isPressed:" + buttonView.isPressed());
      if (buttonView.isPressed()) {
        buttonView.setPressed(false);
        appPrefs.saveBooleanPreference(PreferenceTypes.K_GAMIFICATION, isChecked);
        if (Fabric.isInitialized()) {
          Crashlytics.setBool(Log.POINTS_ENABLED, appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION));
        }
        EventBus.post(new GamificationSettingEvent(isChecked));
      }
    });
    safeModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      appPrefs.saveBooleanPreference(PreferenceTypes.K_SAFE_MODE_ENABLED, isChecked);
      if (Fabric.isInitialized()) {
        Crashlytics.setBool(Log.SAFE_RECORDING, appPrefs.getBooleanPreference(PreferenceTypes.K_SAFE_MODE_ENABLED));
      }
    });
    focusModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      appPrefs.saveBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC, isChecked);
      if (Fabric.isInitialized()) {
        Crashlytics.setBool(Log.STATIC_FOCUS, appPrefs.getBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC));
      }
      EventBus.post(new CameraConfigChangedCommand());
    });
    apiModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      appPrefs.saveBooleanPreference(PreferenceTypes.K_USE_CAMERA_API_NEW, isChecked);
      if (Fabric.isInitialized()) {
        Crashlytics.setBool(Log.CAMERA_API_NEW, appPrefs.getBooleanPreference(PreferenceTypes.K_USE_CAMERA_API_NEW));
      }
      activity.showSnackBar(R.string.restart_needed, Snackbar.LENGTH_INDEFINITE, getString(R.string.restart_label), () -> {
        Intent mStartActivity = new Intent(activity, SplashActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent =
            PendingIntent.getActivity(activity, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, false);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
      });
    });

    mFeedbackButton.setOnClickListener(view -> {
      Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/openstreetview/android/issues"));
      startActivity(browserIntent);
    });

    mReportButton.setOnClickListener(view -> activity.openScreen(ScreenComposer.SCREEN_REPORT));

    mTipsButton.setOnClickListener(v -> {
      activity.openScreen(ScreenComposer.SCREEN_RECORDING_HINTS);
    });

    mWalkthroughButton.setOnClickListener(v -> {
      Intent intent = new Intent(activity, WalkthroughActivity.class);
      startActivity(intent);
    });
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onUserTypeChanged(UserTypeChangedEvent event) {
    if (view != null) {
      switch (event.type) {
        default:
        case PreferenceTypes.USER_TYPE_CONTRIBUTOR:
        case PreferenceTypes.USER_TYPE_QA:
          view.findViewById(R.id.gamification_separator).setVisibility(View.VISIBLE);
          view.findViewById(R.id.gamification_container).setVisibility(View.VISIBLE);
          break;
        case PreferenceTypes.USER_TYPE_BYOD:
        case PreferenceTypes.USER_TYPE_DEDICATED:
        case PreferenceTypes.USER_TYPE_BAU:
          view.findViewById(R.id.gamification_separator).setVisibility(View.GONE);
          view.findViewById(R.id.gamification_container).setVisibility(View.GONE);
          break;
      }
    }
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onLoginChanged(LoginChangedEvent event) {
    if (activity != null && logInButton != null) {
      if (event.logged) {
        logInButton
            .setText(activity.getString(R.string.log_out) + " (" + appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME) + ")");
        logInButton.setOnClickListener(logoutListener);
      } else {
        logInButton.setText(activity.getString(R.string.log_in));
        logInButton.setOnClickListener(loginListener);
      }
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == Utils.REQUEST_ENABLE_BT) {
      if (resultCode == Activity.RESULT_CANCELED) {
        return;
      } else if (resultCode == Activity.RESULT_OK) {
        BTDialogFragment blefr = new BTDialogFragment();
        blefr.show(activity.getSupportFragmentManager(), BTDialogFragment.TAG);
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    view = inflater.inflate(R.layout.fragment_settings, null);
    activity = (MainActivity) getActivity();
    appPrefs = ((OSVApplication) activity.getApplication()).getAppPrefs();
    logInButton = view.findViewById(R.id.login_button);
    mFeedbackButton = view.findViewById(R.id.feedback_setting_container);
    mReportButton = view.findViewById(R.id.issue_report_setting_container);
    mTipsButton = view.findViewById(R.id.tips_setting_container);
    mWalkthroughButton = view.findViewById(R.id.walkthrough_container);
    autoSwitch = view.findViewById(R.id.auto_upload_switch);
    chargingSwitch = view.findViewById(R.id.charging_upload_switch);
    dataSwitch = view.findViewById(R.id.data_switch);
    storageSwitch = view.findViewById(R.id.storage_switch);
    metricSwitch = view.findViewById(R.id.metric_switch);
    mapSwitch = view.findViewById(R.id.map_visibility_switch);
    sdkSwitch = view.findViewById(R.id.map_sdk_switch);
    gameSwitch = view.findViewById(R.id.gamification_switch);
    safeModeSwitch = view.findViewById(R.id.safe_mode_switch);
    focusModeSwitch = view.findViewById(R.id.focus_mode_switch);
    apiModeSwitch = view.findViewById(R.id.api_mode_switch);
    signDetectionSwitch = view.findViewById(R.id.sensor_switch);

    obdTypeText = view.findViewById(R.id.obd_selector_button);
    mObdTitle = view.findViewById(R.id.obd_title);
    mObdButton = view.findViewById(R.id.obd_button);
    mOBDProgressBar = view.findViewById(R.id.obd_progressbar);
    if (Utils.checkSDCard(activity)) {
      view.findViewById(R.id.storage_container).setVisibility(View.VISIBLE);
      view.findViewById(R.id.storage_separator).setVisibility(View.VISIBLE);
    }
    if (appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO)) {
      view.findViewById(R.id.charging_setting_separator).setVisibility(View.VISIBLE);
      view.findViewById(R.id.charging_setting_container).setVisibility(View.VISIBLE);
    }
    PackageInfo pInfo;
    String version = null;
    try {
      pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
      version = pInfo.versionName;
    } catch (Exception e) {
      e.printStackTrace();
    }
    int type = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, -1);
    switch (type) {
      default:
      case PreferenceTypes.USER_TYPE_CONTRIBUTOR:
      case PreferenceTypes.USER_TYPE_QA:
        view.findViewById(R.id.gamification_separator).setVisibility(View.VISIBLE);
        view.findViewById(R.id.gamification_container).setVisibility(View.VISIBLE);
        break;
      case PreferenceTypes.USER_TYPE_BYOD:
      case PreferenceTypes.USER_TYPE_DEDICATED:
      case PreferenceTypes.USER_TYPE_BAU:
        view.findViewById(R.id.gamification_separator).setVisibility(View.GONE);
        view.findViewById(R.id.gamification_container).setVisibility(View.GONE);
        break;
    }

    ((TextView) view.findViewById(R.id.version_text)).setText(version);
    view.findViewById(R.id.auto_setting_container).setOnClickListener(this);
    view.findViewById(R.id.charging_setting_container).setOnClickListener(this);
    view.findViewById(R.id.data_setting_container).setOnClickListener(this);
    view.findViewById(R.id.metric_container).setOnClickListener(this);
    view.findViewById(R.id.map_visibility_container).setOnClickListener(this);
    view.findViewById(R.id.map_sdk_container).setOnClickListener(this);
    view.findViewById(R.id.gamification_container).setOnClickListener(this);
    view.findViewById(R.id.safe_mode_container).setOnClickListener(this);
    view.findViewById(R.id.focus_mode_container).setOnClickListener(this);
    view.findViewById(R.id.api_mode_container).setOnClickListener(this);
    view.findViewById(R.id.sensor_lib_container).setOnClickListener(this);
    view.findViewById(R.id.picture_size_container).setOnClickListener(this);
    view.findViewById(R.id.storage_container).setOnClickListener(this);
    view.findViewById(R.id.obd_container).setOnClickListener(this);
    view.findViewById(R.id.obd_selector_container).setOnClickListener(this);
    view.findViewById(R.id.terms_holder).setOnClickListener(this);
    view.findViewById(R.id.policy_holder).setOnClickListener(this);
    //        if (Utils.isDebuggableFlag(activity)) {
    view.findViewById(R.id.aboutText).setOnLongClickListener(v -> {
      view.findViewById(R.id.debug_setting_container).setVisibility(View.VISIBLE);
      view.findViewById(R.id.debugText).setVisibility(View.VISIBLE);
      activity.showSnackBar(getString(R.string.debug_settings_notification), Snackbar.LENGTH_LONG);
      return true;
    });
    //        } else {
    //            appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED, false);
    //        }
    setupDebugSettings();

    if (appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED)) {
      view.findViewById(R.id.map_visibility_container).setVisibility(View.GONE);
      view.findViewById(R.id.map_visibility_separator).setVisibility(View.GONE);
    }

    storageSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE));
    autoSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO));
    chargingSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_CHARGING));
    dataSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED));
    metricSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC));
    sdkSwitch.setChecked(!appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED, false));
    mapSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_RECORDING_MAP_ENABLED, true));
    gameSwitch.setOnCheckedChangeListener(null);
    gameSwitch.setPressed(false);
    gameSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true));
    safeModeSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_SAFE_MODE_ENABLED, false));
    focusModeSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC, false));
    apiModeSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_USE_CAMERA_API_NEW, false));
    signDetectionSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_SIGN_DETECTION_ENABLED));

    switch (appPrefs.getIntPreference(PreferenceTypes.K_OBD_TYPE)) {
      case PreferenceTypes.V_OBD_WIFI:
        obdTypeText.setText(R.string.wifi_label);
        break;
      case PreferenceTypes.V_OBD_BLE:
        obdTypeText.setText(R.string.ble_label);
        break;
      case PreferenceTypes.V_OBD_BT:
        obdTypeText.setText(R.string.bt_label);
        break;
    }

    setListeners();
    return view;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onStart() {
    super.onStart();
    EventBus.register(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    appPrefs = ((OSVApplication) activity.getApplication()).getAppPrefs();
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
  public void onDetach() {
    super.onDetach();
  }

  private void obdContainerPressed() {
    final ObdManager obdManager = mRecorder.getOBDManager();
    if (!ObdManager.isConnected()) {
      if (obdManager.isFunctional(activity)) {
        Log.d(TAG, "obdContainerPressed: isFunctional = true");
      }
    } else {
      final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
      builder.setMessage(activity.getString(R.string.disconnect_obd_message)).setTitle(activity.getString(R.string.obd_label))
          .setNegativeButton(R.string.cancel_label, (dialog, which) -> {
          }).setPositiveButton(R.string.disconnect, (dialog, which) -> {
        EventBus.postSticky(new ObdCommand(false));
        appPrefs.saveBooleanPreference(PreferenceTypes.K_OBD_CONNECTED, false);
      }).create().show();
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
  public void onObdStatusEvent(ObdStatusEvent event) {
    switch (event.type) {
      case ObdStatusEvent.TYPE_CONNECTED:
        Log.d(TAG, "onObdStatusEvent: obd connected");
        mObdTitle.setText(R.string.connected);
        mObdButton.setText(R.string.disconnect);
        mObdButton.setVisibility(View.VISIBLE);
        view.findViewById(R.id.obd_container).setEnabled(true);
        mOBDProgressBar.setVisibility(View.GONE);
        appPrefs.saveBooleanPreference(PreferenceTypes.K_OBD_CONNECTED, false);
        break;
      case ObdStatusEvent.TYPE_CONNECTING:
        Log.d(TAG, "onObdStatusEvent: obd connecting");
        if (mOBDProgressBar != null && view != null && mObdButton != null && mObdTitle != null) {
          mOBDProgressBar.setVisibility(View.VISIBLE);
          view.findViewById(R.id.obd_container).setEnabled(false);
          mObdButton.setVisibility(View.GONE);
          mObdTitle.setText(R.string.connecting_label);
        }
        break;
      case ObdStatusEvent.TYPE_DISCONNECTED:
        Log.d(TAG, "onObdStatusEvent: obd disconnected");
        mObdTitle.setText(R.string.not_connected_label);
        mObdButton.setText(R.string.connect);
        mObdButton.setVisibility(View.VISIBLE);
        view.findViewById(R.id.obd_container).setEnabled(true);
        mOBDProgressBar.setVisibility(View.GONE);
        break;
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.picture_size_container:
        PictureSizeDialogFragment fragmentPictureSize = new PictureSizeDialogFragment();
        fragmentPictureSize.setPreviewSizes(mRecorder.getSupportedPicturesSizes());
        fragmentPictureSize.show(activity.getSupportFragmentManager(), PictureSizeDialogFragment.TAG);
        break;
      case R.id.obd_selector_container:
        OBDDialogFragment fragment = new OBDDialogFragment();
        fragment.setTypeSelectedListener(type -> {
          int saved = appPrefs.getIntPreference(PreferenceTypes.K_OBD_TYPE);
          if (type != saved) {
            if (ObdManager.isConnected()) {
              EventBus.postSticky(new ObdCommand(false));
            }
            appPrefs.saveIntPreference(PreferenceTypes.K_OBD_TYPE, type);
            mRecorder.createObdManager(type);
            switch (appPrefs.getIntPreference(PreferenceTypes.K_OBD_TYPE)) {
              case PreferenceTypes.V_OBD_WIFI:
                obdTypeText.setText(R.string.wifi_label);
                break;
              case PreferenceTypes.V_OBD_BLE:
                obdTypeText.setText(R.string.ble_label);
                break;
              case PreferenceTypes.V_OBD_BT:
                obdTypeText.setText(R.string.bt_label);
                break;
            }
          }
        });
        fragment.show(activity.getSupportFragmentManager(), OBDDialogFragment.TAG);
        break;
      case R.id.obd_container:
        obdContainerPressed();
        break;
      case R.id.storage_container:
        storageSwitch.setChecked(!storageSwitch.isChecked());
        break;
      case R.id.auto_setting_container:
        autoSwitch.setChecked(!autoSwitch.isChecked());
        break;
      case R.id.charging_setting_container:
        chargingSwitch.setChecked(!chargingSwitch.isChecked());
        break;
      case R.id.data_setting_container:
        dataSwitch.setChecked(!dataSwitch.isChecked());
        break;
      case R.id.metric_container:
        metricSwitch.setChecked(!metricSwitch.isChecked());
        break;
      case R.id.map_sdk_container:
        sdkSwitch.setChecked(!sdkSwitch.isChecked());
        break;
      case R.id.map_visibility_container:
        mapSwitch.setChecked(!mapSwitch.isChecked());
        break;
      case R.id.gamification_container:
        gameSwitch.setPressed(true);
        gameSwitch.setChecked(!gameSwitch.isChecked());
        break;
      case R.id.safe_mode_container:
        safeModeSwitch.setChecked(!safeModeSwitch.isChecked());
        break;
      case R.id.focus_mode_container:
        focusModeSwitch.setChecked(!focusModeSwitch.isChecked());
        break;
      case R.id.api_mode_container:
        apiModeSwitch.setChecked(!apiModeSwitch.isChecked());
        break;
      case R.id.terms_holder:
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://openstreetcam.org/terms/"));
        startActivity(browserIntent);
        break;
      case R.id.policy_holder:
        browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.skobbler.com/legal#privacy"));
        startActivity(browserIntent);
        break;
      case R.id.sensor_lib_container:
        signDetectionSwitch.setChecked(!signDetectionSwitch.isChecked());
        break;
      case R.id.debug_setting_container:
        debugSwitch.setChecked(!debugSwitch.isChecked());
        break;
      case R.id.debug_auth_container:
        authSwitch.setChecked(!authSwitch.isChecked());
        break;
      //            case R.id.debug_hdr_container:
      //                hdrSwitch.setChecked(!hdrSwitch.isChecked());
      //                break;
      case R.id.debug_server_container:
        if (activity.mUploadManager != null) {
          activity.mUploadManager.mCurrentServer = (activity.mUploadManager.mCurrentServer + 1) % UploadManager.URL_ENV.length;
          appPrefs.saveIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE, activity.mUploadManager.mCurrentServer);
          serverText.setText(UploadManager.URL_ENV[activity.mUploadManager.mCurrentServer]);
          activity.showSnackBar(R.string.restart_needed, Snackbar.LENGTH_SHORT, getString(R.string.restart_label), () -> {
            Intent mStartActivity = new Intent(activity, SplashActivity.class);
            int mPendingIntentId = 123456;
            PendingIntent mPendingIntent =
                PendingIntent.getActivity(activity, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, false);
            System.exit(0);
          });
        }
        break;
      case R.id.debug_speed_container:
        speedSwitch.setChecked(!speedSwitch.isChecked());
        break;
    }
  }
}
