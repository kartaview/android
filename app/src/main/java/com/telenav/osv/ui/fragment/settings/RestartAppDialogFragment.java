package com.telenav.osv.ui.fragment.settings;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.telenav.osv.R;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.application.PreferenceTypes;

/**
 * Dialog class which is responsible to request the users agreement for the application restart.
 */
public class RestartAppDialogFragment extends DialogFragment {

    public static final String TAG = RestartAppDialogFragment.class.getSimpleName();

    /**
     * The id for the pending intent.
     */
    private static final int PENDING_INTENT_ID = 123456;

    /**
     * The millis to pass until the app is restarted.
     */
    private static final int MILLIS_TO_TRIGGER_RESTART = 100;

    /**
     * Dismiss listener for which receives dialog dismiss event.
     */
    private OnDismissListener dismissListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity(), R.style.SettingsAlertDialogStyle)
                .setMessage(R.string.settings_restart_dialog_message)
                .setPositiveButton(R.string.ok_label, (dialogInterface, i) -> {
                    Intent mStartActivity = new Intent(getActivity(), SplashActivity.class);
                    PendingIntent mPendingIntent =
                            PendingIntent.getActivity(getActivity(), PENDING_INTENT_ID, mStartActivity,
                                    PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager mgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + MILLIS_TO_TRIGGER_RESTART, mPendingIntent);
                    ApplicationPreferences appPrefs = ((KVApplication) getActivity().getApplication()).getAppPrefs();
                    appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, false);
                    //removes all the activities before restarting the app.
                    getActivity().finishAffinity();
                    System.exit(0);
                })
                .setNegativeButton(R.string.settings_restart_dialog_negative_button, (dialogInterface, i) -> dialogInterface.dismiss())
                .create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        dismissListener.onDismiss();
    }

    /**
     * Setter dismiss listener.
     * @param dismissListener the listener to be notified when the dialog was dismissed.
     */
    public void setOnDismissListener(OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    /**
     * Interface that provides a callback for listening when the dialog was dismissed.
     */
    public interface OnDismissListener {
        /**
         * Callback that notifies the observer when the dialog was dismissed.
         */
        void onDismiss();
    }
}