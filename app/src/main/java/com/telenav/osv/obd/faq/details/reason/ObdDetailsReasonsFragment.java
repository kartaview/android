package com.telenav.osv.obd.faq.details.reason;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.ObdBaseFragment;
import com.telenav.osv.obd.faq.details.ObdDetailsContract;
import com.telenav.osv.obd.faq.details.ObdDetailsPresenterImpl;
import com.telenav.osv.utils.FormatUtils;
import androidx.annotation.Nullable;

/**
 * Obd details adapters concrete fragment implementation. It holds information related to reasons for user interest if it uses obd for track creation.
 * Use the {@link ObdDetailsReasonsFragment#newInstance} factory method to
 * create an instance of this fragment.
 * @author horatiuf
 */
public class ObdDetailsReasonsFragment extends ObdBaseFragment implements ObdDetailsContract.ObdDetailsView {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ObdDetailsReasonsFragment.class.getSimpleName();

    /**
     * Instance of the {@code ObdDetailsPresenter} associated with the view.
     */
    private ObdDetailsContract.ObdDetailsPresenter presenter;

    /**
     * Default constructor for the current class.
     */
    public ObdDetailsReasonsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment ObdDetailsAdapterFragment.
     */
    public static ObdDetailsReasonsFragment newInstance() {
        return new ObdDetailsReasonsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new ObdDetailsPresenterImpl(this, ((OSVApplication) getActivity().getApplication()).getAppPrefs());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.partial_obd_details, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
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
    private void initViews(View view) {
        ((TextView) view.findViewById(R.id.text_view_partial_obd_details_header)).setText(R.string.obd_reasons);
        TextView subText = view.findViewById(R.id.text_view_partial_obd_details_subtext);
        if (presenter.isByod()) {
            subText.setText(String.format(getContext().getString(R.string.obd_reason_byod),
                    presenter.isImperial() ? FormatUtils.FORMAT_UNIT_DISTANCE_IMPERIAL_MILES_LABEL : FormatUtils.FORMAT_UNIT_DISTANCE_METRIC_KM));
        } else {
            subText.setText(R.string.obd_reason_community);
        }
    }
}
