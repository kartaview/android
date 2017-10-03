package com.telenav.osv.ui.fragment;

import android.app.Dialog;
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
import com.telenav.osv.data.Preferences;
import com.telenav.osv.di.Injectable;
import javax.inject.Inject;

import static com.telenav.osv.manager.obd.ObdManager.TYPE_BLE;
import static com.telenav.osv.manager.obd.ObdManager.TYPE_BT;
import static com.telenav.osv.manager.obd.ObdManager.TYPE_WIFI;

/**
 * obd type selection dialog
 * Created by Kalman on 21/06/16.
 */
public class OBDDialogFragment extends DialogFragment implements View.OnClickListener, Injectable {

  public static final String TAG = OBDDialogFragment.class.getSimpleName();

  /**
   * shared preferences
   */
  @Inject
  Preferences preferences;

  /**
   * the view of the fragment
   */
  private View root;

  private int obdSelected = -1;

  private OnTypeSelectedListener typeSelectedListener;

  public void setTypeSelectedListener(OnTypeSelectedListener typeSelectedListener) {
    this.typeSelectedListener = typeSelectedListener;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return super.onCreateDialog(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    root = inflater.inflate(R.layout.fragment_obd_list, container, false);

    return root;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    TextView okTextView = view.findViewById(R.id.ok_button_selection_obd);
    okTextView.setOnClickListener(this);
    okTextView.setVisibility(View.VISIBLE);
    initViews();
  }

  /**
   * Initialize the view from the fragment
   */
  private void initViews() {
    RadioGroup mRadioGroup = root.findViewById(R.id.obd_radio_group);
    int type = preferences.getObdType();
    switch (type) {
      case TYPE_WIFI:
        mRadioGroup.check(R.id.obd_radio_wifi);
        break;
      case TYPE_BLE:
        mRadioGroup.check(R.id.obd_radio_ble);
        break;
      case TYPE_BT:
        mRadioGroup.check(R.id.obd_radio_bt);
        break;
    }
    mRadioGroup.setOnCheckedChangeListener((group, checkedId) -> obdSelected = checkedId);
  }

  @Override
  public void onClick(View v) {
    if (obdSelected != -1) {
      switch (obdSelected) {
        case R.id.obd_radio_wifi:
          typeSelectedListener.onTypeSelected(TYPE_WIFI);
          break;
        case R.id.obd_radio_ble:
          typeSelectedListener.onTypeSelected(TYPE_BLE);
          break;
        case R.id.obd_radio_bt:
          typeSelectedListener.onTypeSelected(TYPE_BT);
          break;
      }
    }
    new Handler(Looper.getMainLooper()).postDelayed(this::dismiss, 200);
  }

  public interface OnTypeSelectedListener {

    void onTypeSelected(int type);
  }
}
