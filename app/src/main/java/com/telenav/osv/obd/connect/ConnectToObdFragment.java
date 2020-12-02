package com.telenav.osv.obd.connect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.telenav.osv.R;
import com.telenav.osv.common.adapter.GeneralSettingsAdapter;
import com.telenav.osv.common.model.base.KVBaseFragment;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.ObdBaseFragment;
import com.telenav.osv.obd.faq.ObdFaqFragment;
import com.telenav.osv.utils.ActivityUtils;

/**
 * Fragment containing all the OBD connection options.
 * Implements the {@link com.telenav.osv.obd.connect.ConnectToObdContract.ConnectToObdView} with all the available UI operations.
 * @author cameliao
 */

public class ConnectToObdFragment extends ObdBaseFragment implements ConnectToObdContract.ConnectToObdView {

    public static final String TAG = ConnectToObdFragment.class.getSimpleName();

    /**
     * The instance of the presenter for the OBD connection screen.
     */
    private ConnectToObdContract.ConnectToObdPresenter presenter;

    /**
     * Factory method to create a new instance for the current fragment.
     * @return a new instance of the {@code ConnectToObdFragment}.
     */
    public static ConnectToObdFragment newInstance() {
        return new ConnectToObdFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new ConnectToObdPresenterImpl(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_connect_to_obd, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initObdRewardLabel(view);
        initObdConnectionOptionsList(view);
        initObdFaq(view);
    }

    @Override
    public void setPresenter(ConnectToObdContract.ConnectToObdPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public ToolbarSettings.Builder getToolbarSettings() {
        return new ToolbarSettings.Builder()
                .setTitle(R.string.obd_connect_get_started);
    }

    @Override
    public void displayFragment(KVBaseFragment fragment, String tag) {
        ActivityUtils.replaceFragment(getActivity().getSupportFragmentManager(),
                fragment, R.id.layout_activity_obd_fragment_container,
                true, tag);
    }

    /**
     * Initialises the text for the obd reward label.
     * @param view the parent view.
     */
    private void initObdRewardLabel(View view) {
        TextView textView = view.findViewById(R.id.text_view_fragment_connect_to_obd_reward_label);
        textView.setText(R.string.obd_connect_earn_double_the_points);
    }

    /**
     * Initializes the button to the OBD FAQ screen.
     * @param view the parent view.
     */
    private void initObdFaq(View view) {
        TextView textView = view.findViewById(R.id.text_view_connect_to_obd_learn_more);
        textView.setOnClickListener(v -> ActivityUtils.replaceFragment(getActivity().getSupportFragmentManager(),
                ObdFaqFragment.newInstance(),
                R.id.layout_activity_obd_fragment_container,
                true,
                ObdFaqFragment.TAG));
    }

    /**
     * Initializes the obd connection list containing the options for connecting to the OBD.
     * @param view the parent view.
     */
    private void initObdConnectionOptionsList(View view) {
        RecyclerView connectGuideRecyclerView = view.findViewById(R.id.recycler_view_fragment_connect_to_obd);
        connectGuideRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(connectGuideRecyclerView.getContext(), LinearLayoutManager.VERTICAL);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.recycler_view_default_divider));
        connectGuideRecyclerView.addItemDecoration(dividerItemDecoration);
        connectGuideRecyclerView.setAdapter(new GeneralSettingsAdapter(presenter.getObdConnectionOptions()));
    }
}
