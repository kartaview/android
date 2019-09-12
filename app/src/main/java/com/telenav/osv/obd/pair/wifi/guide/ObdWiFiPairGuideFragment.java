package com.telenav.osv.obd.pair.wifi.guide;

import java.util.Arrays;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.obd.pair.adapter.ObdPairGuideAdapter;
import com.telenav.osv.obd.pair.base.ObdConnectionDialogFragment;
import com.telenav.osv.obd.pair.base.ObdConnectionDialogPresenterImpl;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * The OBD Wi-Fi pair guide fragment containing a step by step guide for connecting to the OBD through Wi-Fi.
 * @author cameliao
 */

public class ObdWiFiPairGuideFragment extends ObdConnectionDialogFragment {

    public static final String TAG = ObdWiFiPairGuideFragment.class.getSimpleName();

    private boolean created = false;

    /**
     * Factory method to create a new instance for the current fragment.
     * @return a new instance of the {@code ObdWiFiPairGuideFragment}.
     */
    public static ObdWiFiPairGuideFragment newInstance() {
        return new ObdWiFiPairGuideFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OSVApplication osvApplication = (OSVApplication) getActivity().getApplication();
        presenter = new ObdConnectionDialogPresenterImpl(this,
                Injection.provideObdManager(osvApplication.getApplicationContext(), osvApplication.getAppPrefs()));
        created = true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_obd_pair_guide, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initObdWiFiPairGuideLayout(view);
        initPairGuideList(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.start();
        if (!created) {
            presenter.setupObdStateDialog();
        }
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public ToolbarSettings.Builder getToolbarSettings() {
        return new ToolbarSettings.Builder().setTitle(R.string.obd_wifi_pair_guide_screen_title);
    }

    /**
     * Initializes the list with the pair guide tips.
     * @param view the parent layout.
     */
    private void initPairGuideList(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_obd_pair_guide);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(new ObdPairGuideAdapter(Arrays.asList(getResources().getTextArray(R.array.obd_wifi_pair_guide))));
    }

    /**
     * Initializes the layout elements of the OBD Wi-Fi pair guide screen.
     * @param view the parent layout.
     */
    private void initObdWiFiPairGuideLayout(View view) {
        ((ImageView) view.findViewById(R.id.image_view_obd_pair_guide)).setImageDrawable(getResources().getDrawable(R.drawable.vector_wi_fi_big));
        ((TextView) view.findViewById(R.id.text_view_obd_pair_guide_title)).setText(R.string.obd_wifi_pair_guide_title);
        Button connectButton = view.findViewById(R.id.button_obd_pair_guide_connect);
        connectButton.setText(R.string.obd_wifi_pair_guide_connect);
        connectButton.setOnClickListener(v ->
        {
            created = false;
            presenter.connect(ObdManager.ObdTypes.WIFI, null);
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        });
    }
}
