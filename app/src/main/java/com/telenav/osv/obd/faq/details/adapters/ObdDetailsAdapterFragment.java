package com.telenav.osv.obd.faq.details.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.telenav.osv.R;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.ObdBaseFragment;
import com.telenav.osv.obd.faq.details.ObdDetailsContract;
import com.telenav.osv.obd.faq.details.ObdDetailsPresenterImpl;
import com.telenav.osv.obd.faq.details.ObdDetailsRecommendations;

/**
 * Obd details adapters concrete fragment implementation. It holds recommendation for adapter purchases and info related to them.
 * Use the {@link ObdDetailsAdapterFragment#newInstance} factory method to
 * create an instance of this fragment.
 * @author horatiuf
 */
public class ObdDetailsAdapterFragment extends ObdBaseFragment implements ObdDetailsContract.ObdDetailsView {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ObdDetailsAdapterFragment.class.getSimpleName();

    /**
     * Instance of the {@code ObdDetailsPresenter} associated with the view.
     */
    private ObdDetailsContract.ObdDetailsPresenter presenter;

    /**
     * Default constructor for the current class.
     */
    public ObdDetailsAdapterFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment ObdDetailsAdapterFragment.
     */
    public static ObdDetailsAdapterFragment newInstance() {
        return new ObdDetailsAdapterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new ObdDetailsPresenterImpl(this, ((KVApplication) getActivity().getApplication()).getAppPrefs());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_obd_details_adapters, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initExplication(view);
        initWifiViews(view);
        initBleViews(view);
    }

    @Override
    public ToolbarSettings.Builder getToolbarSettings() {
        return new ToolbarSettings.Builder().setTitle(R.string.obd_faq);
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public void setPresenter(ObdDetailsContract.ObdDetailsPresenter presenter) {
        this.presenter = presenter;
    }

    /**
     * Initialise the views.
     * @param view the parent view.
     */
    private void initExplication(View view) {
        View explicationView = view.findViewById(R.id.layout_fragment_obd_details_adapter_explication);
        ((TextView) explicationView.findViewById(R.id.text_view_partial_obd_details_header)).setText(R.string.obd_adapters);
        ((TextView) explicationView.findViewById(R.id.text_view_partial_obd_details_subtext)).setText(R.string.obd_adapters_explication);
    }

    /**
     * Initialise the wifi views.
     * @param view the parent view.
     */
    private void initWifiViews(View view) {
        View wifiView = view.findViewById(R.id.layout_fragment_obd_details_adapter_wifi);
        ((TextView) wifiView.findViewById(R.id.text_view_partial_obd_details_adapter_sub_header)).setText(R.string.wifi_adapters);
        ((TextView) wifiView.findViewById(R.id.text_view_partial_obd_details_adapter_explication)).setText(R.string.wifi_adapters_explication);
        ((ImageView) wifiView.findViewById(R.id.image_view_partial_obd_details_adapter_icon)).setImageResource(R.drawable.obd_wifi);
        ((TextView) wifiView.findViewById(R.id.text_view_partial_obd_details_adapter_recommended)).setText(R.string.veepeak_obd2);
        TextView button = wifiView.findViewById(R.id.text_view_partial_obd_details_adapter_button);
        button.setText(R.string.get_on_amazon);
        button.setOnClickListener(click -> {
            presenter.openRecommendation(ObdDetailsRecommendations.OBD_WIFI, getContext());
        });
    }

    /**
     * Initialise the ble views.
     * @param view the parent view.
     */
    private void initBleViews(View view) {
        View bleView = view.findViewById(R.id.layout_fragment_obd_details_adapter_ble);
        ((TextView) bleView.findViewById(R.id.text_view_partial_obd_details_adapter_sub_header)).setText(R.string.ble_adapters);
        ((TextView) bleView.findViewById(R.id.text_view_partial_obd_details_adapter_explication)).setText(R.string.ble_adapters_explication);
        ((ImageView) bleView.findViewById(R.id.image_view_partial_obd_details_adapter_icon)).setImageResource(R.drawable.obd_ble);
        ((TextView) bleView.findViewById(R.id.text_view_partial_obd_details_adapter_recommended)).setText(R.string.lelink_ble);
        TextView button = bleView.findViewById(R.id.text_view_partial_obd_details_adapter_button);
        button.setText(R.string.get_on_amazon);
        button.setOnClickListener(click -> {
            presenter.openRecommendation(ObdDetailsRecommendations.OBD_BLE, getContext());
        });
    }
}
