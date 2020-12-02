package com.telenav.osv.obd.faq;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.telenav.osv.R;
import com.telenav.osv.common.adapter.GeneralSettingsAdapter;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.ObdBaseFragment;
import com.telenav.osv.ui.fragment.OSVFragment;


/**
 * A simple {@link OSVFragment} subclass.
 * Use the {@link ObdFaqFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ObdFaqFragment extends ObdBaseFragment implements ObdFaqContract.ObdFaqView {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = "ObdFaqFragment";

    /**
     * Instance to the presenter of the Obd faq contract.
     */
    private ObdFaqContract.ObdFaqPresenter presenter;

    /**
     * Instance for the {@code GeneralSettingsAdapter}.
     */
    private GeneralSettingsAdapter generalSettingsAdapter;

    /**
     * The obd faq settings recycler view instance.
     */
    private RecyclerView settingsRecyclerView;

    public ObdFaqFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment ObdFaqFragment.
     */
    public static ObdFaqFragment newInstance() {
        return new ObdFaqFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = new ObdFaqPresenter(this, getActivity().getSupportFragmentManager());
        generalSettingsAdapter = new GeneralSettingsAdapter(presenter.getObdFaqSettings());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_obd_faq, container, false);
    }

    @Override
    public ToolbarSettings.Builder getToolbarSettings() {
        return new ToolbarSettings.Builder()
                .setTitle(R.string.obd_faq);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initSettingsView(view);
        initSupportView(view);
    }

    @Override
    public void setPresenter(ObdFaqContract.ObdFaqPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    /**
     * Sets the view related to the faq settings view.
     * @param view the parent view.
     */
    private void initSettingsView(View view) {
        settingsRecyclerView = view.findViewById(R.id.recycler_view_fragment_obd_faq_settings);
        settingsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(settingsRecyclerView.getContext(), LinearLayoutManager.VERTICAL);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.recycler_view_default_divider));
        settingsRecyclerView.addItemDecoration(dividerItemDecoration);
        settingsRecyclerView.setAdapter(generalSettingsAdapter);
    }

    /**
     * Initialise the support view;
     * @param view the parent view.
     */
    private void initSupportView(View view) {
        view.findViewById(R.id.text_view_fragment_obd_faq_contact).setOnClickListener(click -> presenter.contactKvSupport(getContext()));
    }
}
