package com.telenav.osv.obd.faq.details.what;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.telenav.osv.R;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.ObdBaseFragment;
import com.telenav.osv.obd.faq.details.ObdDetailsContract;
import com.telenav.osv.obd.faq.details.ObdDetailsPresenterImpl;

/**
 * Obd details what is concrete fragment implementation. It holds information about what is obd.
 * Use the {@link ObdDetailsWhatFragment#newInstance} factory method to
 * create an instance of this fragment.
 * @author horatiuf
 */
public class ObdDetailsWhatFragment extends ObdBaseFragment implements ObdDetailsContract.ObdDetailsView {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ObdDetailsWhatFragment.class.getSimpleName();

    /**
     * Instance of the {@code ObdDetailsPresenter} associated with the view.
     */
    private ObdDetailsContract.ObdDetailsPresenter presenter;

    /**
     * Default constructor for the current class.
     */
    public ObdDetailsWhatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment ObdDetailsAdapterFragment.
     */
    public static ObdDetailsWhatFragment newInstance() {
        return new ObdDetailsWhatFragment();
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
        ((TextView) view.findViewById(R.id.text_view_partial_obd_details_header)).setText(R.string.what_is_obd);
        ((TextView) view.findViewById(R.id.text_view_partial_obd_details_subtext)).setText(R.string.what_is_obd_explication);
    }
}
