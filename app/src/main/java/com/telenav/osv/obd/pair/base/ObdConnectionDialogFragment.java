package com.telenav.osv.obd.pair.base;

import android.app.Activity;
import android.os.Bundle;
import com.telenav.osv.R;
import com.telenav.osv.common.dialog.OscDialog;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.ObdActivity;
import com.telenav.osv.obd.ObdBaseFragment;
import com.telenav.osv.utils.Log;
import androidx.annotation.Nullable;

/**
 * The base OBD connection dialog fragment displaying the connecting and retry dialog.
 * The class implements {@link com.telenav.osv.obd.pair.base.ObdConnectionDialogContract.ObdConnectionDialogView}.
 * @author cameliao
 */

public class ObdConnectionDialogFragment extends ObdBaseFragment implements ObdConnectionDialogContract.ObdConnectionDialogView {

    private static final String TAG = ObdConnectionDialogFragment.class.getSimpleName();

    /**
     * The key for saving the state of the connecting dialog when the device is rotated.
     */
    private static final String KEY_CONNECTING_DIALOG = "connectingDialog";

    /**
     * The key for saving the state of the retry dialog when the device is rotated.
     */
    private static final String KEY_RETRY_DIALOG = "retryDialog";

    /**
     * Instance of the presenter.
     */
    protected ObdConnectionDialogContract.ObdConnectionDialogPresenter presenter;

    /**
     * The displayed dialog while connecting to the OBD.
     */
    private OscDialog connectingDialog;

    /**
     * The displayed dialog when the obd connection is timed out.
     */
    private OscDialog retryDialog;

    /**
     * A flag to determine if the connecting dialog was visible or not before device rotation.
     */
    private boolean wasConnectingDialogVisible;

    /**
     * A flag to determine if the retry dialog was visible or not before device rotation.
     */
    private boolean wasRetryDialogVisible;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.wasConnectingDialogVisible = savedInstanceState.getBoolean(KEY_CONNECTING_DIALOG);
            this.wasRetryDialogVisible = savedInstanceState.getBoolean(KEY_RETRY_DIALOG);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.start();
        Log.d(TAG, "Dialog onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Dialog onPause");
        dismissDialogs();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Dialog onDestroyView");
        dismissDialogs();
        presenter.release();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CONNECTING_DIALOG, wasConnectingDialogVisible);
        outState.putBoolean(KEY_RETRY_DIALOG, wasRetryDialogVisible);
    }


    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public ToolbarSettings.Builder getToolbarSettings() {
        return null;
    }

    @Override
    public void displayConnectingDialog() {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            if (connectingDialog != null && connectingDialog.isShowing()) {
                return;
            }
            if (retryDialog != null && retryDialog.isShowing()) {
                Log.d(TAG, "Dialog retry dismissed.");
                retryDialog.dismiss();
            }
            if (connectingDialog == null) {
                connectingDialog = new OscDialog.Builder(getContext())
                        .setTitleResId(R.string.obd_wifi_dialog_connecting_title)
                        .setInfoResId(R.string.obd_wifi_dialog_connecting_info)
                        .setProgressEnable()
                        .setNegativeButton(R.string.obd_wifi_dialog_cancel, v -> {
                            Log.d(TAG, "Dialog connecting dismissed.");
                            connectingDialog.dismiss();
                            presenter.disconnect();
                        })
                        .setCancelable()
                        .build();
            }
            Log.d(TAG, "Dialog connecting visible.");
            connectingDialog.show();
        });
    }

    @Override
    public void displayRetryDialog() {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            if (retryDialog != null && retryDialog.isShowing()) {
                return;
            }
            if (connectingDialog != null && connectingDialog.isShowing()) {
                Log.d(TAG, "Dialog connecting dismissed.");
                connectingDialog.dismiss();
            } else {
                return;
            }
            showRetryDialog();
        });
    }

    @Override
    public void dismissDialogs() {
        if (retryDialog != null && retryDialog.isShowing()) {
            Log.d(TAG, "Dialog retry dismissed.");
            retryDialog.dismiss();
            wasRetryDialogVisible = true;
        }
        if (connectingDialog != null && connectingDialog.isShowing()) {
            Log.d(TAG, "Dialog connecting dismissed.");
            connectingDialog.dismiss();
            wasConnectingDialogVisible = true;
        }
    }

    @Override
    public void setPresenter(ObdConnectionDialogContract.ObdConnectionDialogPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateUiForObdConnected() {
        presenter.release();
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                ObdActivity obdActivity = (ObdActivity) getActivity();
                if (obdActivity != null) {
                    obdActivity.returnToRecordingScreen();
                }
            });
        }
    }

    /**
     * Shows the retry dialog. If is necessary the dialog will be recreated.
     */
    private void showRetryDialog() {
        if (retryDialog == null) {
            retryDialog = new OscDialog.Builder(getContext())
                    .setTitleResId(R.string.obd_wifi_dialog_retry_title)
                    .setInfoResId(R.string.obd_wifi_dialog_retry_info)
                    .setIconResId(R.drawable.vector_obd_attention)
                    .setPositiveButton(R.string.obd_wifi_dialog_try_again, v -> {
                        presenter.retryConnecting();
                        Log.d(TAG, "Dialog retry dismissed.");
                        retryDialog.dismiss();
                    })
                    .setNegativeButton(R.string.obd_wifi_dialog_cancel, v -> {
                        Log.d(TAG, "Dialog retry dismissed.");
                        retryDialog.dismiss();
                    })
                    .setCancelable()
                    .build();
        }
        Log.d(TAG, "Dialog retry visible.");
        retryDialog.show();
    }
}
