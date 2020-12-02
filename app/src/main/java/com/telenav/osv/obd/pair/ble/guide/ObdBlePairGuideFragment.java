package com.telenav.osv.obd.pair.ble.guide;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.telenav.osv.R;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.ObdBaseFragment;
import com.telenav.osv.obd.pair.adapter.ObdPairGuideAdapter;
import com.telenav.osv.obd.pair.ble.devices.ObdBleDevicesFragment;
import com.telenav.osv.utils.ActivityUtils;
import com.telenav.osv.utils.UiUtils;
import com.telenav.osv.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * The OBD BLE pair guide fragment containing the step by step details for connecting to the OBD through bluetooth.
 * The class implements {@link com.telenav.osv.obd.pair.ble.guide.ObdBlePairGuideContract.ObdBlePairGuideView}.
 * @author cameliao
 */
public class ObdBlePairGuideFragment extends ObdBaseFragment implements ObdBlePairGuideContract.ObdBlePairGuideView {

    public static final String TAG = ObdBlePairGuideFragment.class.getSimpleName();

    /**
     * The presenter for the OBD BLE pair guide business logic.
     */
    private ObdBlePairGuideContract.ObdBlePairGuidePresenter presenter;

    /**
     * The button for displaying a list with the paired and available devices.
     */
    private Button chooseDeviceButton;

    /**
     * Factory method to create a new instance for the current fragment.
     * @return a new instance of the {@code ObdBlePairGuideFragment}.
     */
    public static ObdBlePairGuideFragment newInstance() {
        return new ObdBlePairGuideFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KVApplication kvApplication = (KVApplication) getActivity().getApplication();
        new ObdBlePairGuidePresenterImpl(this,
                Injection.provideObdManager(kvApplication.getApplicationContext(), kvApplication.getAppPrefs()));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_obd_pair_guide, container, false);
        presenter.start();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initObdBlePairGuideLayout(view);
        initPairGuideList(view);
    }

    @Override
    public void setPresenter(ObdBlePairGuideContract.ObdBlePairGuidePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void displayChooseDevicesScreen() {
        ActivityUtils.replaceFragment(getActivity().getSupportFragmentManager(), ObdBleDevicesFragment.newInstance(),
                R.id.layout_activity_obd_fragment_container, true, ObdBleDevicesFragment.TAG);
    }

    @Override
    public void showSnackBar(int messageId, int duration) {
        UiUtils.showSnackBar(getContext(), chooseDeviceButton, getResources().getString(messageId), duration, null, null);
    }

    @Override
    public void requestBluetoothPermissions() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        getActivity().startActivityForResult(enableBtIntent, Utils.REQUEST_ENABLE_BT);
    }

    @Override
    public ToolbarSettings.Builder getToolbarSettings() {
        return new ToolbarSettings.Builder().setTitle(R.string.obd_ble_pair_guide_screen_title);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    /**
     * Initializes the layout elements of the OBD BLE Pair guide screen.
     * @param view the parent layout.
     */
    private void initObdBlePairGuideLayout(View view) {
        ((ImageView) view.findViewById(R.id.image_view_obd_pair_guide)).setImageDrawable(getResources().getDrawable(R.drawable.vector_bluetooth_big));
        ((TextView) view.findViewById(R.id.text_view_obd_pair_guide_title)).setText(R.string.obd_ble_pair_guide_title);
        chooseDeviceButton = view.findViewById(R.id.button_obd_pair_guide_connect);
        chooseDeviceButton.setText(R.string.obd_ble_pair_guide_choose_devices);
        chooseDeviceButton.setOnClickListener(v -> {
            if (checkGPSPermission(R.string.permission_bluetooth_rationale)) {
                presenter.checkObdBleState(getContext());
            }
        });
    }

    /**
     * Initializes the list with the pair guide tips.
     * @param view the parent layout.
     */
    private void initPairGuideList(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_obd_pair_guide);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(new ObdPairGuideAdapter(Arrays.asList(getResources().getTextArray(R.array.obd_ble_pair_guide))));
    }

    /**
     * Checks the permission for GPS if the permission are not available requests the permissions.
     * @param message the message to be displayed while requesting permissions.
     * @return true if the permission was granted, false otherwise.
     */
    private boolean checkGPSPermission(@StringRes int message) {
        final ArrayList<String> needed = new ArrayList<>();
        int locationPermitted = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        if (locationPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (needed.size() > 0) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), needed.get(0))) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialog);
                AlertDialog dialog = builder.setMessage(message).setTitle(R.string.permission_request)
                        .setNeutralButton(R.string.ok_label, (dialog1, which) -> {
                            String[] array = new String[needed.size()];
                            needed.toArray(array);
                            ActivityCompat.requestPermissions(getActivity(), array, KVApplication.LOCATION_PERMISSION_BT);
                        }).create();
                dialog.show();
                return false;
            } else {
                String[] array = new String[needed.size()];
                needed.toArray(array);
                ActivityCompat.requestPermissions(getActivity(), array, KVApplication.LOCATION_PERMISSION_BT);
                return false;
            }
        }
        return true;
    }
}
