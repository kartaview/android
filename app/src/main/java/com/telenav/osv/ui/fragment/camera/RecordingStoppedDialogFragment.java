package com.telenav.osv.ui.fragment.camera;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.telenav.osv.R;
import com.telenav.osv.common.dialog.KVDialog;

/**
 * Fragment which displays an error dialog for recording screen.
 */
public class RecordingStoppedDialogFragment extends DialogFragment {

    public static final String TAG = RecordingStoppedDialogFragment.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new KVDialog.Builder(getActivity())
                .setTitleResId(R.string.recording_something_went_wrong)
                .setInfoResId(R.string.recording_start_new_recording)
                .setIconResId(R.drawable.vector_obd_attention)
                .setPositiveButton(R.string.ok_label, v -> dismiss())
                .setCancelableOnOutsideClick(false)
                .build();
    }
}