package com.telenav.osv.ui.fragment;

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
import com.telenav.osv.data.Preferences;
import com.telenav.osv.di.Injectable;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * fragment for picture resolution selection
 * Created by Kalman on 11/11/2016.
 */
public class PictureSizeDialogFragment extends DialogFragment implements View.OnClickListener, Injectable {

  public static final String TAG = PictureSizeDialogFragment.class.getSimpleName();

  /**
   * shared preferences
   */
  @Inject
  Preferences preferences;

  /**
   * the view of the fragment
   */
  private View root;

  private RadioGroup mRadioGroup;

  private Size resolution;

  private List<Size> availablePictureSizesList;

  private LayoutInflater mInflater;

  private Size newResolution;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    root = inflater.inflate(R.layout.fragment_picture_size, container, false);
    mInflater = inflater;
    return root;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    TextView okTextView = view.findViewById(R.id.ok_button_selection);
    okTextView.setOnClickListener(this);
    okTextView.setVisibility(View.VISIBLE);
    initViews(mInflater);
  }

  public void setPreviewSizes(List<Size> sizes) {
    availablePictureSizesList = sizes;
  }

  /**
   * Initialize the view from the fragment
   */
  private void initViews(LayoutInflater inflater) {
    mRadioGroup = root.findViewById(R.id.picture_size_radio_group);
    List<Integer> radioButtonsIdsList = new ArrayList<>();
    resolution = preferences.getResolution();
    newResolution = resolution;
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
      radioButton.setText(
          availablePictureSizesList.get(i).width + " x " + availablePictureSizesList.get(i).height + " (" + Math.round(pictureSizeInMp) +
              " MP)");

      if ((availablePictureSizesList.get(i).width == resolution.width) && availablePictureSizesList.get(i).height == resolution.height) {
        radioButton.setChecked(true);
      }

      mRadioGroup.addView(radioButton);
      radioButtonsIdsList.add(radioButton.getId());
      Log.d(TAG, "radio button id list: " + radioButtonsIdsList.get(i));
    }
    mRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
      View radioButton = mRadioGroup.findViewById(checkedId);
      if (radioButton.getTag() != null) {
        Size size = (Size) radioButton.getTag();
        newResolution = size;
        resolution = size;
        Log.d(TAG, "The resolution is " +
            availablePictureSizesList.get(checkedId).width + " x " + availablePictureSizesList.get(checkedId).height);
      }
    });
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.ok_button_selection:
        if (newResolution != resolution) {
          preferences.setResolution(newResolution);

          Log.d(TAG, "Resolution when press OK: " + resolution);
        }
        dismissDialog();
        break;
    }
  }

  private void dismissDialog() {
    new Handler(Looper.getMainLooper()).postDelayed(this::dismiss, 200);
  }
}
