package com.telenav.osv.ui.fragment;

import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.manager.CameraManager;
import com.telenav.osv.manager.ObdManager;
import com.telenav.osv.manager.UploadManager;
import com.telenav.osv.obd.BLEConnection;
import com.telenav.osv.service.UploadHandlerService;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;
import com.telenav.osv.obd.Constants;

/**
 * Created by Kalman on 10/2/2015.
 */
public class SettingsFragment extends Fragment implements View.OnClickListener, ObdManager.ConnectionListener, BLEDialogFragment.OnDeviceSelectedListener, BTDialogFragment.OnDeviceSelectedListener {

    public static final String TAG = "SettingsFragment";

    public static final int MIN_FREE_SPACE = 500;

    public boolean paused;

    private TextView logInButton;

    private View view;

    private SwitchCompat autoSwitch;

    private SwitchCompat dataSwitch;

    private SwitchCompat metricSwitch;

    private SwitchCompat mapSwitch;

    private SwitchCompat signDetectionSwitch;

    private ApplicationPreferences appPrefs;

    private MainActivity activity;

    private View.OnClickListener loginListener;

    private View.OnClickListener logoutListener;

    private UploadHandlerService mUploadHandlerService;

    private SwitchCompat debugSwitch;

    private SwitchCompat authSwitch;

//    private SwitchCompat hdrSwitch;

    private SwitchCompat shutterSwitch;

    private SwitchCompat speedSwitch;

    private TextView serverText;

    private LinearLayout mFeedbackButton;

    private LinearLayout mWebsiteButton;

    private SwitchCompat storageSwitch;

    private TextView mObdTitle;

    private TextView mObdButton;

    private View mOBDProgressBar;

    private TextView obdTypeText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_settings, null);
        activity = (MainActivity) getActivity();
        appPrefs = ((OSVApplication) activity.getApplication()).getAppPrefs();
        logInButton = (TextView) view.findViewById(R.id.login_button);
        mFeedbackButton = (LinearLayout) view.findViewById(R.id.feedback_setting_container);
        mWebsiteButton = (LinearLayout) view.findViewById(R.id.about_setting_container);
        autoSwitch = (SwitchCompat) view.findViewById(R.id.auto_upload_switch);
        dataSwitch = (SwitchCompat) view.findViewById(R.id.data_switch);
        storageSwitch = (SwitchCompat) view.findViewById(R.id.storage_switch);
        metricSwitch = (SwitchCompat) view.findViewById(R.id.metric_switch);
        mapSwitch = (SwitchCompat) view.findViewById(R.id.map_visibility_switch);
        signDetectionSwitch = (SwitchCompat) view.findViewById(R.id.sensor_switch);

        shutterSwitch = (SwitchCompat) view.findViewById(R.id.debug_shutter_switch);
        obdTypeText = (TextView) view.findViewById(R.id.obd_selector_button);
        mObdTitle = (TextView) view.findViewById(R.id.obd_title);
        mObdButton = (TextView) view.findViewById(R.id.obd_button);
        mOBDProgressBar = view.findViewById(R.id.obd_progressbar);
        if (Utils.checkSDCard(activity)) {
            view.findViewById(R.id.storage_container).setVisibility(View.VISIBLE);
            view.findViewById(R.id.storage_separator).setVisibility(View.VISIBLE);

        }
        PackageInfo pInfo;
        String version = null;
        try {
            pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        ((TextView) view.findViewById(R.id.version_text)).setText(version);
        view.findViewById(R.id.auto_setting_container).setOnClickListener(this);
        view.findViewById(R.id.data_setting_container).setOnClickListener(this);
        view.findViewById(R.id.metric_container).setOnClickListener(this);
        view.findViewById(R.id.map_visibility_container).setOnClickListener(this);
        view.findViewById(R.id.sensor_lib_container).setOnClickListener(this);
        view.findViewById(R.id.debug_shutter_container).setOnClickListener(this);
        view.findViewById(R.id.picture_size_container).setOnClickListener(this);
        view.findViewById(R.id.storage_container).setOnClickListener(this);
        view.findViewById(R.id.obd_container).setOnClickListener(this);
        view.findViewById(R.id.obd_selector_container).setOnClickListener(this);
        if (Utils.isDebuggableFlag(activity)) {
            view.findViewById(R.id.aboutText).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    view.findViewById(R.id.debug_setting_container).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.debugText).setVisibility(View.VISIBLE);
                    activity.showSnackBar(getString(R.string.debug_settings_notification), Snackbar.LENGTH_LONG);
                    return true;
                }
            });
        } else {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED, false);
        }
        setupDebugSettings();

        storageSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE));
        autoSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO));
        dataSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED));
        metricSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC));
        mapSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_RECORDING_MAP_ENABLED, Build.VERSION.SDK_INT < 24));
        signDetectionSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_SIGN_DETECTION));

        shutterSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER));
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
        onUploadServiceConnected(activity.mUploadHandlerService);
        return view;
    }

    private void setupDebugSettings() {
        debugSwitch = (SwitchCompat) view.findViewById(R.id.debug_switch);
        authSwitch = (SwitchCompat) view.findViewById(R.id.debug_auth_switch);
//        hdrSwitch = (SwitchCompat) view.findViewById(R.id.debug_hdr_switch);
        speedSwitch = (SwitchCompat) view.findViewById(R.id.debug_speed_switch);
        serverText = (TextView) view.findViewById(R.id.server_text);
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
//        view.findViewById(R.id.debug_hdr_container).setOnClickListener(this);
        view.findViewById(R.id.debug_server_container).setOnClickListener(this);
        view.findViewById(R.id.debug_speed_container).setOnClickListener(this);

        debugSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Utils.DEBUG = isChecked;
                appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED, isChecked);
                view.findViewById(R.id.debug_options_container).setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if (!isChecked) {
                    CookieSyncManager.createInstance(activity);
                    CookieManager.getInstance().removeAllCookie();
                }
            }
        });

        //switches
        authSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_SAVE_AUTH, isChecked);
                if (!isChecked) {
                    //clear auth cache
                    CookieSyncManager.createInstance(activity);
                    CookieManager.getInstance().removeAllCookie();
                    final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(activity).edit();
                    editor.clear();
                    editor.commit();
                }
            }
        });

//        hdrSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_HDR, isChecked);
//                if (activity.mCameraHandlerService != null && activity.mCameraHandlerService.mCameraReady) {
//                    boolean enabled = CameraManager.instance.enableHDR(isChecked);
//                    if (enabled != isChecked) {
//                        hdrSwitch.setChecked(enabled);
//                        appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_HDR, enabled);
//                        activity.showSnackBar(R.string.hdr_not_supported, Snackbar.LENGTH_SHORT);
//                    }
//                }
//            }
//        });

        speedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_SPEED_DIST, isChecked);
            }
        });

        authSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SAVE_AUTH, false));
//        hdrSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_HDR, false));
//        shutterSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER, false));
//        hdrSwitch.setChecked(false);
        speedSwitch.setChecked(appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SPEED_DIST, false));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        paused = false;
        appPrefs = ((OSVApplication) activity.getApplication()).getAppPrefs();
        activity.getApp().getOBDManager().addConnectionListener(this);
    }

    @Override
    public void onPause() {
        activity.getApp().getOBDManager().removeConnectionListener(this);
        paused = true;
        super.onPause();
    }

    public void setListeners() {
        //login button
        loginListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showLogInScreen();
            }
        };
        logoutListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
                builder.setMessage(activity.getString(R.string.logout_confirmation_message)).setTitle(activity.getString(R.string.log_out)).setNegativeButton(R.string.cancel_label,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).setPositiveButton(R.string.log_out, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.logout();
                    }
                }).create().show();
            }
        };
        if (!appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME).equals("")) {
            logInButton.setText(activity.getString(R.string.log_out) + " (" + appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME) + ")");
            logInButton.setOnClickListener(logoutListener);
        } else {
            logInButton.setOnClickListener(loginListener);
        }

        //switches
        autoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                appPrefs.saveBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO, isChecked);
                if (!isChecked) {
                    ((OSVApplication) activity.getApplication()).getUploadManager().pauseUpload();
                }
            }
        });
        dataSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                appPrefs.saveBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED, isChecked);
                if (!isChecked) {
                    UploadManager uploadManager = ((OSVApplication) activity.getApplication()).getUploadManager();
                    if (uploadManager.isUploading() && !uploadManager.isPaused() && !NetworkUtils.isWifiInternetAvailable(activity) && NetworkUtils.isInternetAvailable(activity)) {
                        uploadManager.pauseUpload();
                    }
                }
            }
        });
        storageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (UploadManager.sUploadStatus != UploadManager.STATUS_IDLE) {
                    activity.showSnackBar(getString(R.string.not_allowed_while_upload), Snackbar.LENGTH_LONG);
                    return;
                }
                appPrefs.saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE, isChecked);

//                final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
//                builder.setMessage("The app will restart now.").setTitle("Restart needed").setCancelable(false).setNeutralButton("No", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Intent mStartActivity = new Intent(activity, SplashActivity.class);
//
//                        int mPendingIntentId = 123456;
//                        PendingIntent mPendingIntent = PendingIntent.getActivity(activity, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
//                        AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
//                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
//                    }
//                }).create().show();
                Utils.generateOSVFolder(activity);
                Utils.getSelectedStorage(activity);
//                activity.mapFragment.refreshDisplayedSequences();
                long totalFreeSpace = Math.min(Utils.getAvailableSpace(activity), 1024);

            }
        });
        metricSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                appPrefs.saveBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC, isChecked);
            }
        });
        signDetectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                appPrefs.saveBooleanPreference(PreferenceTypes.K_SIGN_DETECTION, isChecked);
                if(isChecked){
                    CameraManager.instance.initSensorLib();
                } else {
                    CameraManager.instance.destroySensorLib();
                }
            }
        });
        mapSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                appPrefs.saveBooleanPreference(PreferenceTypes.K_RECORDING_MAP_ENABLED, isChecked);
            }
        });
        signDetectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                appPrefs.saveBooleanPreference(PreferenceTypes.K_SIGN_DETECTION, isChecked);
                if (isChecked) {
                    CameraManager.instance.initSensorLib();
                } else {
                    CameraManager.instance.destroySensorLib();
                }
            }
        });
        shutterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER, isChecked);
                CameraManager.instance.forceCloseCamera();
                CameraManager.instance.open();
                CameraManager.instance.restartPreviewIfNeeded();
            }
        });

        mFeedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/openstreetview/android/issues"));
                startActivity(browserIntent);

            }
        });

        mWebsiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mWebsiteText.callOnClick();
                activity.openScreen(MainActivity.SCREEN_RECORDING_HINTS);
            }
        });
    }

    public void onUploadServiceConnected(UploadHandlerService uploadHandlerService) {
        this.mUploadHandlerService = uploadHandlerService;
    }

    public void onLoginChanged(boolean logged) {
        if (activity != null && logInButton != null) {
            if (logged) {
                logInButton.setText(activity.getString(R.string.log_out) + " (" + appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME) + ")");
                logInButton.setOnClickListener(logoutListener);
            } else {
                activity.showSnackBar(R.string.logged_out_confirmation, Snackbar.LENGTH_SHORT);
                logInButton.setText(activity.getString(R.string.log_in));
                logInButton.setOnClickListener(loginListener);
            }
        }
    }

    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                return;
            } else if (resultCode == Activity.RESULT_OK) {
                BLEDialogFragment blefr = new BLEDialogFragment();
                blefr.show(activity.getSupportFragmentManager(), BLEDialogFragment.TAG);
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.picture_size_container:
                if (appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
                    activity.showSnackBar(getString(R.string.resolution_selection_not_available), Snackbar.LENGTH_LONG);
                } else {
                    PictureSizeDialogFragment fragmentPictureSize = new PictureSizeDialogFragment();
                    fragmentPictureSize.show(activity.getSupportFragmentManager(), PictureSizeDialogFragment.TAG);
                }
                break;
            case R.id.obd_selector_container:
                OBDDialogFragment fragment = new OBDDialogFragment();
                fragment.setTypeSelectedListener(new OBDDialogFragment.OnTypeSelectedListener() {
                    @Override
                    public void onTypeSelected(int type) {
                        int saved = appPrefs.getIntPreference(PreferenceTypes.K_OBD_TYPE);
                        if (type != saved) {
                            ConcurrentLinkedQueue<ObdManager.ConnectionListener> list = activity.getApp().getOBDManager().getConnectionListeners();
                            if (activity.getApp().getOBDManager().isConnected()) {
                                activity.getApp().getOBDManager().disconnect();
                            }
                            appPrefs.saveIntPreference(PreferenceTypes.K_OBD_TYPE, type);
                            activity.getApp().setObdManager(type);
                            activity.getApp().getOBDManager().setConnectionListeners(list);
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
                    }
                });
                fragment.show(activity.getSupportFragmentManager(), OBDDialogFragment.TAG);
                break;
            case R.id.obd_container:
                final ObdManager obdManager = activity.getApp().getOBDManager();
                if (!obdManager.isConnected()) {
                    ObdManager.sDisconnected = false;
                    boolean ret = false;
                    if (obdManager.isBluetooth()) {
                        if (obdManager.isBle()) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                activity.showSnackBar(getString(R.string.ble_android_not_supported), Snackbar.LENGTH_LONG);
                                break;
                            }

                            // Use this check to determine whether BLE is supported on the device. Then
                            // you can selectively disable BLE-related features.
                            if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                                activity.showSnackBar(R.string.ble_not_supported, Snackbar.LENGTH_SHORT);
                                return;
                            }

                            BluetoothAdapter bluetoothAdapter = BLEConnection.getInstance().initConnection(getActivity());

                            // Checks if Bluetooth is supported on the device.
                            if (bluetoothAdapter == null) {
                                activity.showSnackBar(R.string.error_bluetooth_not_supported, Snackbar.LENGTH_SHORT);
                                return;
                            }

                            if (!activity.checkPermissionsForGPSWithRationale(R.string.permission_bluetooth_rationale)){
                                return;
                            }

                            // Ensures Bluetooth is available on the device and it is enabled. If not,
                            // displays a dialog requesting user permission to enable Bluetooth.
                            if (!bluetoothAdapter.isEnabled()) {
                                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                                return;
                            }
                            SharedPreferences preferences = activity.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
                            if (preferences.getBoolean(Constants.BLE_SERVICE_STARTED, false) && preferences.getString(Constants.EXTRAS_BLE_DEVICE_ADDRESS, null) != null) {
                                ret = obdManager.connect();
                            } else {
                                ret = true;
                                BLEDialogFragment blefr = new BLEDialogFragment();
                                blefr.setDeviceSelectedListener(this);
                                blefr.show(activity.getSupportFragmentManager(), BLEDialogFragment.TAG);
                            }
                        } else {
                            // Use this check to determine whether BT is supported on the device. Then
                            // you can selectively disable BT-related features.
                            if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                                activity.showSnackBar(R.string.bl_not_supported, Snackbar.LENGTH_SHORT);
                                return;
                            }

                            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                            // Checks if Bluetooth is supported on the device.
                            if (bluetoothAdapter == null) {
                                activity.showSnackBar(R.string.error_bluetooth_not_supported, Snackbar.LENGTH_SHORT);
                                return;
                            }

                            if (!activity.checkPermissionsForGPSWithRationale(R.string.permission_bluetooth_rationale)){
                                return;
                            }

                            // Ensures Bluetooth is available on the device and it is enabled. If not,
                            // displays a dialog requesting user permission to enable Bluetooth.
                            if (!bluetoothAdapter.isEnabled()) {
                                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                                return;
                            }
                            SharedPreferences preferences = activity.getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
                            if (preferences.getBoolean(Constants.BT_SERVICE_STARTED, false) && preferences.getString(Constants.EXTRAS_BT_DEVICE_ADDRESS, null) != null) {
                                ret = obdManager.connect();
                            } else {
                                ret = true;
                                BTDialogFragment btFragment = new BTDialogFragment();
                                btFragment.setListener(this);
                                btFragment.show(activity.getSupportFragmentManager(), BTDialogFragment.TAG);
                            }

                        }
                    } else {
                        ret = obdManager.connect();
                        if (ret) {
                            mOBDProgressBar.setVisibility(View.VISIBLE);
                            view.findViewById(R.id.obd_container).setEnabled(false);
                            mObdButton.setVisibility(View.GONE);
                            mObdTitle.setText(R.string.connecting_label);
                        } else {
                            activity.showSnackBar(R.string.no_obd_detected_message, Snackbar.LENGTH_LONG, R.string.wifi_label, new Runnable() {
                                @Override
                                public void run() {
                                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                }
                            });
                        }
                    }
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
                    builder.setMessage(activity.getString(R.string.disconnect_obd_message)).setTitle(activity.getString(R.string.obd_label)).setNegativeButton(R.string.cancel_label,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setPositiveButton(R.string.disconnect, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ObdManager.sDisconnected = true;
                            obdManager.disconnect();
                            appPrefs.saveBooleanPreference(PreferenceTypes.K_OBD_CONNECTED, false);
                        }
                    }).create().show();
                }
                break;
            case R.id.storage_container:
                storageSwitch.setChecked(!storageSwitch.isChecked());
                break;
            case R.id.auto_setting_container:
                autoSwitch.setChecked(!autoSwitch.isChecked());
                break;
            case R.id.data_setting_container:
                dataSwitch.setChecked(!dataSwitch.isChecked());
                break;
            case R.id.metric_container:
                metricSwitch.setChecked(!metricSwitch.isChecked());
                break;
            case R.id.map_visibility_container:
                mapSwitch.setChecked(!mapSwitch.isChecked());
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
                if (mUploadHandlerService != null && mUploadHandlerService.mUploadManager != null) {
                    mUploadHandlerService.mUploadManager.mCurrentServer = (mUploadHandlerService.mUploadManager.mCurrentServer + 1) % 3;
                    appPrefs.saveIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE, mUploadHandlerService.mUploadManager.mCurrentServer);
                    serverText.setText(UploadManager.URL_ENV[mUploadHandlerService.mUploadManager.mCurrentServer]);
                    activity.showSnackBar(R.string.restart_needed, Snackbar.LENGTH_SHORT, getString(R.string.restart_label), new Runnable() {
                        @Override
                        public void run() {
                            Intent mStartActivity = new Intent(activity, SplashActivity.class);
                            int mPendingIntentId = 123456;
                            PendingIntent mPendingIntent = PendingIntent.getActivity(activity, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                            AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
                            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                            System.exit(0);
                        }
                    });
                }
                break;
            case R.id.debug_shutter_container:
                shutterSwitch.setChecked(!shutterSwitch.isChecked());
                break;
            case R.id.debug_speed_container:
                speedSwitch.setChecked(!speedSwitch.isChecked());
                break;
        }

    }

    @Override
    public void onConnected() {
        mObdTitle.setText(R.string.connected);
        mObdButton.setText(R.string.disconnect);
        mObdButton.setVisibility(View.VISIBLE);
        view.findViewById(R.id.obd_container).setEnabled(true);
        mOBDProgressBar.setVisibility(View.GONE);
        appPrefs.saveBooleanPreference(PreferenceTypes.K_OBD_CONNECTED, false);
    }

    @Override
    public void onDisconnected() {
        mObdTitle.setText(R.string.not_connected_label);
        mObdButton.setText(R.string.connect);
        mObdButton.setVisibility(View.VISIBLE);
        view.findViewById(R.id.obd_container).setEnabled(true);
        mOBDProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onSpeedObtained(ObdManager.SpeedData speed) {

    }

    @Override
    public void onConnecting() {
        if (mOBDProgressBar != null && view != null && mObdButton != null && mObdTitle != null) {
            mOBDProgressBar.setVisibility(View.VISIBLE);
            view.findViewById(R.id.obd_container).setEnabled(false);
            mObdButton.setVisibility(View.GONE);
            mObdTitle.setText(R.string.connecting_label);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        try {
            if (mOBDProgressBar != null) {
                mOBDProgressBar.setVisibility(View.VISIBLE);
                view.findViewById(R.id.obd_container).setEnabled(false);
                mObdButton.setVisibility(View.GONE);
                mObdTitle.setText(R.string.connecting_label);
            }
        } catch (Exception e) {
            Log.d(TAG, "onTypeSelected: exception " + Log.getStackTraceString(e));
        }
    }

    @Override
    public void onDeviceSelected(String device) {
        try {
            if (mOBDProgressBar != null) {
                mOBDProgressBar.setVisibility(View.VISIBLE);
                view.findViewById(R.id.obd_container).setEnabled(false);
                mObdButton.setVisibility(View.GONE);
                mObdTitle.setText(R.string.connecting_label);
            }
        } catch (Exception e) {
            Log.d(TAG, "onTypeSelected: exception " + Log.getStackTraceString(e));
        }
    }
}
