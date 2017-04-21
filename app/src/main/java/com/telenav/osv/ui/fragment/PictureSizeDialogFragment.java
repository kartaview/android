package com.telenav.osv.ui.fragment;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.command.CameraResetCommand;
import com.telenav.osv.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by adrianbostan on 06/07/16.
 */

public class PictureSizeDialogFragment extends DialogFragment implements View.OnClickListener {

    public final static String TAG = PictureSizeDialogFragment.class.getSimpleName();

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

    private List<Integer> radioButtonsIdsList;

    private TextView okTextView;

    private int widthSize;

    private int heightSize;

    private List<android.hardware.Camera.Size> availablePictureSizesList;

    private LayoutInflater mInflater;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_picture_size, container, false);
        mInflater = inflater;
        mActivity = (MainActivity) getActivity();
        preferences = mActivity.getApp().getAppPrefs();
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        okTextView = (TextView) view.findViewById(R.id.ok_button_selection);
        okTextView.setOnClickListener(this);
        okTextView.setVisibility(View.VISIBLE);
        initViews(mInflater);
    }

    public void setPreviewSizes(List<android.hardware.Camera.Size> sizes){
        availablePictureSizesList = sizes;
    }

    /**
     * Initialize the view from the fragment
     */
    private void initViews(LayoutInflater inflater) {
        mRadioGroup = (RadioGroup) root.findViewById(R.id.picture_size_radio_group);
        radioButtonsIdsList = new ArrayList<>();
        widthSize = preferences.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH);
        heightSize = preferences.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT);
        if (availablePictureSizesList == null) {
            Log.d(TAG, "initViews: availablePictureSizesList is null");
            return;
        }
        for (int i = 0; i < availablePictureSizesList.size(); i++) {

            RadioButton radioButton = (RadioButton) inflater.inflate(R.layout.item_picture_size_radio_button, null);

            radioButton.setId(i);
            radioButton.setTag(availablePictureSizesList.get(i));
            int widthSizeInMp = availablePictureSizesList.get(i).width;
            int heightSizeInMp = availablePictureSizesList.get(i).height;
            float pictureSizeInMp = (widthSizeInMp * heightSizeInMp) / 1000000f;

            radioButton.setTextColor(getResources().getColor(R.color.text_colour_default_light));
            radioButton.setText(availablePictureSizesList.get(i).width + " x " + availablePictureSizesList.get(i).height + " (" + Math.round(pictureSizeInMp) + " MP)");

            if ((availablePictureSizesList.get(i).width == widthSize) && availablePictureSizesList.get(i).height == heightSize) {
                radioButton.setChecked(true);
            }

            mRadioGroup.addView(radioButton);
            radioButtonsIdsList.add(radioButton.getId());
            Log.d(TAG, "radio button id list: " + radioButtonsIdsList.get(i));
        }
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                View radioButton = mRadioGroup.findViewById(checkedId);
                if (radioButton.getTag() != null) {
                    Camera.Size size = (Camera.Size) radioButton.getTag();
                    widthSize = size.width;
                    heightSize = size.height;
                    Log.d(TAG, "The resolution is " + availablePictureSizesList.get(checkedId).width + " x " + availablePictureSizesList.get(checkedId).height);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ok_button_selection:
                if (widthSize != preferences.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH) || heightSize != preferences.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT)) {
                    preferences.saveIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, widthSize);
                    preferences.saveIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, heightSize);
                    EventBus.post(new CameraResetCommand());

                    Log.d(TAG, "Resolution when press OK: " + preferences.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH) + " x " + preferences.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT));
                }
                dismissDialog();
                break;
        }

    }

    private void dismissDialog() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, 200);
    }
}
