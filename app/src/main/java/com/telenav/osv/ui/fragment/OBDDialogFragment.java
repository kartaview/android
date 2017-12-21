package com.telenav.osv.ui.fragment;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;

/**
 * Created by Kalman on 21/06/16.
 */

public class OBDDialogFragment extends DialogFragment implements View.OnClickListener {

    public final static String TAG = OBDDialogFragment.class.getSimpleName();

    /**
     * the view of the fragment
     */
    private View root;

    /**
     * shared preferences
     */
    private ApplicationPreferences preferences;

    private MainActivity mActivity;

    private RadioGroup mRadioGroup;

    private TextView okTextView;

    private int obdSelected = -1;

    private OnTypeSelectedListener typeSelectedListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_obd_list, container, false);
        mActivity = (MainActivity) getActivity();
        preferences = mActivity.getApp().getAppPrefs();

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        okTextView = view.findViewById(R.id.ok_button_selection_obd);
        okTextView.setOnClickListener(this);
        okTextView.setVisibility(View.VISIBLE);
        initViews();
    }

    @Override
    public void onClick(View v) {
        if (obdSelected != -1) {
            switch (obdSelected) {
                case R.id.obd_radio_wifi:
                    typeSelectedListener.onTypeSelected(PreferenceTypes.V_OBD_WIFI);
                    break;
                case R.id.obd_radio_ble:
                    typeSelectedListener.onTypeSelected(PreferenceTypes.V_OBD_BLE);
                    break;
                case R.id.obd_radio_bt:
                    typeSelectedListener.onTypeSelected(PreferenceTypes.V_OBD_BT);
                    break;
            }
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

            @Override
            public void run() {
                dismiss();
            }
        }, 200);
    }

    public void setTypeSelectedListener(OnTypeSelectedListener typeSelectedListener) {
        this.typeSelectedListener = typeSelectedListener;
    }

    /**
     * Initialize the view from the fragment
     */
    private void initViews() {
        mRadioGroup = root.findViewById(R.id.obd_radio_group);
        int type = preferences.getIntPreference(PreferenceTypes.K_OBD_TYPE);
        switch (type) {
            case PreferenceTypes.V_OBD_WIFI:
                mRadioGroup.check(R.id.obd_radio_wifi);
                break;
            case PreferenceTypes.V_OBD_BLE:
                mRadioGroup.check(R.id.obd_radio_ble);
                break;
            case PreferenceTypes.V_OBD_BT:
                mRadioGroup.check(R.id.obd_radio_bt);
                break;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mRadioGroup.findViewById(R.id.obd_radio_ble).setVisibility(View.GONE);
        }
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                obdSelected = checkedId;
            }
        });
    }

    public interface OnTypeSelectedListener {

        void onTypeSelected(int type);
    }
}
