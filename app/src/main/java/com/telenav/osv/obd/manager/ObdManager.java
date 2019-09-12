package com.telenav.osv.obd.manager;

import java.util.concurrent.CopyOnWriteArraySet;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import com.telenav.datacollectormodule.config.Config;
import com.telenav.datacollectormodule.datacollectorstarter.DataCollectorManager;
import com.telenav.datacollectormodule.datatype.EventDataListener;
import com.telenav.datacollectormodule.datatype.datatypes.BaseObject;
import com.telenav.datacollectormodule.datatype.datatypes.SpeedObject;
import com.telenav.datacollectormodule.datatype.util.LibraryUtil;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.obd.BLEConnection;
import com.telenav.osv.obd.ObdFunctionalState;
import com.telenav.osv.utils.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Obd connection manager which is used for both {@link ObdTypes#WIFI} and {@link ObdTypes#BLE}.
 * <p>Provides public methods for setup:
 * <ul>
 * <li>{@link #setupObd(int)}</li>
 * <li>{@link #setupObd(String)} - specific to Obd {@code BLE}</li>
 * </ul></p>
 * <p>
 * For starting the start for obtaining obd speed for either obd types there are start ({@link #startCollecting()} and stop ({@link #stopCollecting()} methods.
 * </p>
 * <p>Furthermore there are multiple helpers methods that provide various misc information such as:
 * <ul>
 * <li>{@link #getType()}</li>
 * <li>{@link #isConnected()}</li>
 * <li>{@link #isObdBleFunctional(Context)}</li>
 * </ul></p>
 * <p>In order to obtain {@link ObdConnectionListener} callbacks a listener must be registered/unregistered via {@link #addObdConnectionListener(ObdConnectionListener)},
 * respectively {@link #removeObdConnectionListener(ObdConnectionListener)}.</p>
 * @author horatiuf
 */
public class ObdManager implements com.telenav.datacollectormodule.datatype.ObdConnectionListener, EventDataListener {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ObdManager.class.getSimpleName();

    /**
     * The instance of the {@code ObdManager} used for {@code Singleton} pattern impl.
     */
    private static ObdManager INSTANCE;

    /**
     * Instance to the {@code ApplicationContext} used for data collector initialisation.
     */
    private final Context context;

    /**
     * {@code DataCollectorManager} instance to the {@code DataCollector} library.
     */
    private DataCollectorManager dataCollectorManager;

    /**
     * The app preferences instance.
     */
    private ApplicationPreferences applicationPreferences;

    /**
     * Collection for obd listeners.
     */
    private CopyOnWriteArraySet<ObdConnectionListener> mObdConnectionListener;

    /**
     * Configuration class for setup data collector for obd wifi speed collecting.
     */
    private Config configObdWifi;

    /**
     * Configuration class for setup data collector for obd ble speed collecting.
     */
    private Config configObdBle;

    /**
     * The type of the obd. It is a value from the {@link ObdTypes} interface.
     */
    @ObdTypes
    private int type;

    /**
     * Flag that represents the state of the OBD.
     */
    @ObdState
    private int obdState;

    /**
     * Default constructor for the current class.
     */
    private ObdManager(Context context, ApplicationPreferences applicationPreferences) {
        this.context = context;
        mObdConnectionListener = new CopyOnWriteArraySet<>();
        this.applicationPreferences = applicationPreferences;
        obdState = ObdState.OBD_NOT_CONNECTED;
    }

    /**
     * Singleton pattern specific implementation getter.
     * @param context the context required for {@code DataCollector} setup.
     * @param applicationPreferences the application preferences required to persist the last connection type for obd auto-connect type identification.
     * @return {@code ObdManager} instance, in case is not set it will set it internally once.
     */
    public static ObdManager getInstance(Context context, ApplicationPreferences applicationPreferences) {
        if (INSTANCE == null) {
            INSTANCE = new ObdManager(context, applicationPreferences);
        }
        return INSTANCE;
    }

    @Override
    public void onConnectionStateChanged(Context context, String source, int statusCode) {
        Log.d(TAG, String.format("OBD connection state changed. Source: %s. Status code: %s,", source, statusCode));
        if (statusCode != LibraryUtil.OBD_REATTEMPT_CONNECTION) {
            notifyObdDisconnected();
        }
    }

    @Override
    public void onConnectionStopped(String source) {
        Log.d(TAG, String.format("OBD connection stopped. Source: %s,", source));
        notifyObdDisconnected();
    }

    @Override
    public void onDeviceConnected(Context context, String source) {
        Log.d(TAG, String.format("OBD device connected. Source: %s.", source));
        notifyObdConnecting();
    }

    @Override
    public void onNewEvent(BaseObject baseObject) {
        if (baseObject.getSensorType().equals(LibraryUtil.SPEED) && !mObdConnectionListener.isEmpty()) {
            SpeedObject speedObject = (SpeedObject) baseObject;
            SpeedData speedData;
            if (speedObject.getStatusCode() != LibraryUtil.OBD_READ_SUCCESS) {
                speedData = new SpeedData(speedObject.getErrorCodeDescription(), baseObject.getTimestamp());
            } else {
                if (obdState != ObdState.OBD_CONNECTED) {
                    notifyObdConnected();
                }
                speedData = new SpeedData((int) (speedObject.getSpeed()), baseObject.getTimestamp());
            }
            Log.d(TAG, String.format("Obd speed sensor. Speed: %s. Error: %s", speedObject.getSpeed(), speedObject.getErrorCodeDescription()));
            for (ObdConnectionListener listener : mObdConnectionListener) {
                listener.onSpeedObtained(speedData);
            }
        } else {
            Log.d(TAG, String.format("Unsupported sensor type: %s", baseObject.getSensorType()));
        }
    }

    /**
     * @return {@code int} representing the type of the obd. If the value {@link ObdTypes#NOT_SET} it will receive the value from shared preferences.
     */
    public int getType() {
        if (type == ObdTypes.NOT_SET) {
            type = applicationPreferences.getIntPreference(PreferenceTypes.K_OBD_TYPE);
        }
        return type;
    }

    /**
     * @param type persists the type in shared preferences for obd type.
     */
    private void setType(@ObdTypes int type) {
        applicationPreferences.saveIntPreference(PreferenceTypes.K_OBD_TYPE, type);
        this.type = type;
    }

    /**
     * Helper method to show the connection state of the obd.
     * @return {@code true} if the obd is connected, {@code false} otherwise.
     */
    public boolean isConnected() {
        return obdState == ObdState.OBD_CONNECTED;
    }

    /**
     * @return {@code true} if the obd is connected, {@code false} otherwise.
     */
    public int getObdState() {
        return obdState;
    }

    /**
     * @return {@code true} if the data collector start was started, {@code false} otherwise.
     */
    public boolean startCollecting() {
        if (dataCollectorManager != null) {
            notifyObdInitialised();
            Log.d(TAG, "startCollecting");
            dataCollectorManager.startCollecting();
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return {@code true} if the data collector start was stopped, {@code false} otherwise.
     */
    public boolean stopCollecting() {
        if (dataCollectorManager != null) {
            obdState = ObdState.OBD_DISCONNECTED;
            Log.d(TAG, "stopCollecting");
            dataCollectorManager.stopCollectingObdData();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes a listener for obd connection.
     * @param listener the listener which was persisted and will be remove if exists.
     */
    public void removeObdConnectionListener(ObdConnectionListener listener) {
        if (mObdConnectionListener.contains(listener)) {
            mObdConnectionListener.remove(listener);
        }
    }

    /**
     * Add a listener for obd connection.
     * @param listener the listener which will be persisted and notified with obd related callbacks.
     */
    public void addObdConnectionListener(ObdConnectionListener listener) {
        if (!mObdConnectionListener.contains(listener)) {
            mObdConnectionListener.add(listener);
        }
    }

    @ObdFunctionalState
    public int isObdBleFunctional(Context context) {

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return ObdFunctionalState.OBD_NOT_SUPPORTED;
        }

        BluetoothAdapter bluetoothAdapter = BLEConnection.getInstance().initConnection(context);
        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            return ObdFunctionalState.OBD_NOT_SUPPORTED;

        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (!bluetoothAdapter.isEnabled()) {
            return ObdFunctionalState.OBD_NEED_PERMISSIONS;
        }
        return ObdFunctionalState.OBD_FUNCTIONAL;
    }

    /**
     * Setup the obd based only on the type. For the {@link ObdTypes#BLE} it is required to be persisted in the shared prefs an valid device mac address.
     * <p>
     * <p>The method will call internally {@link #setupDataCollectorForObd(int, String)} with null {@code bleDeviceMacAddress}.
     * @param type the type of the obd for which the setup will be called. It must be a value from {@link ObdTypes}.
     * @return {@code true} if the setup has been successful, {@code false} otherwise.
     */
    public boolean setupObd(@ObdTypes int type) {
        return setupDataCollectorForObd(type, null);
    }

    /**
     * Setup the obd ble type, it requires valid device mac address.
     * <p>
     * <p>The method will call internally {@link #setupDataCollectorForObd(int, String)}.
     * @return {@code true} if the setup has been successful, {@code false} otherwise.
     */
    public boolean setupObd(@NonNull String bleDeviceMacAddress) {
        configObdBle = null;
        return setupDataCollectorForObd(ObdTypes.BLE, bleDeviceMacAddress);
    }

    /**
     * Setups data collector for obd wifi connection and adds current class as a listener for speed sensor.
     * @param bleDeviceMacAddress the device mac address to which the obd ble is required to connect.
     */
    private void setupObdBle(@NonNull String bleDeviceMacAddress) {
        if (configObdBle == null) {
            Config.Builder configBuilder = getDefaultObdConfigBuilder();
            configBuilder
                    .addSource(LibraryUtil.OBD_BLE_SOURCE)
                    .setBleMacAddress(bleDeviceMacAddress);
            configObdBle = configBuilder.build();
        }
        setType(ObdTypes.BLE);
        dataCollectorManager = new DataCollectorManager(context, configObdBle);
    }

    /**
     * Setups the obd based on the {@code type}. Based on the type:
     * <ul>
     * <li>{@link ObdTypes#WIFI} - call internally {@link #setupObdWifi()}.</li>
     * <li>{@link ObdTypes#BLE} - if the {@code bleDeviceMacAddress} provided is not null it will persist said address in the shared prefs, otherwise it will check for an
     * already persisted device mac address. If either provided or existent the mac address is found internally {@link #setupObdBle(String)} will be called.
     * <p>If none found the setup will not be performed.</p></li>
     * </ul>
     * @param type the type of the obd.
     * @param bleDeviceMacAddress the address for the ble device mac address. Can be null if the type is {@link ObdTypes#WIFI}.
     * @return {@code true} if the setup has been successful, {@code false} otherwise.
     */
    private boolean setupDataCollectorForObd(@ObdTypes int type, @Nullable String bleDeviceMacAddress) {
        Log.d(TAG, String.format("Setup obd for type: %s.", type));
        if (type == ObdTypes.NOT_SET) {
            return false;
        }
        if (type == ObdTypes.WIFI) {
            setupObdWifi();
        } else {
            if (bleDeviceMacAddress != null) {
                applicationPreferences.saveStringPreference(PreferenceTypes.K_BLE_DEVICE_ADDRESS, bleDeviceMacAddress);
            } else {
                String persistedBleMacAddress = applicationPreferences.getStringPreference(PreferenceTypes.K_BLE_DEVICE_ADDRESS);
                if (persistedBleMacAddress == null || persistedBleMacAddress.equals("")) {
                    Log.d(TAG, "Ble Device Mac Address not persisted. The value cannot be null.");
                    return false;
                } else {
                    bleDeviceMacAddress = persistedBleMacAddress;
                }
            }
            setupObdBle(bleDeviceMacAddress);
        }

        return true;
    }

    /**
     * Setups data collector for obd wifi connection and adds current class as a listener for speed sensor.
     */
    private void setupObdWifi() {
        if (configObdWifi == null) {
            Config.Builder configBuilder = getDefaultObdConfigBuilder();
            configBuilder.addSource(LibraryUtil.OBD_WIFI_SOURCE);
            configObdWifi = configBuilder.build();
        }

        setType(ObdTypes.WIFI);
        dataCollectorManager = new DataCollectorManager(context, configObdWifi);
    }

    /**
     * @return {@code Config.Builder} representing the default configuration which has:
     * <p>
     * <ul>
     * <li>obd connection listener - which by default is set to this class</li>
     * <li>obd {@link LibraryUtil#SPEED} listener - which by default is set to this class</li>
     * </ul>
     */
    private Config.Builder getDefaultObdConfigBuilder() {
        Config.Builder newConfig = new Config.Builder();
        newConfig
                .setObdConnectionListener(this)
                .addDataListener(this, LibraryUtil.SPEED);
        return newConfig;
    }

    /**
     * Notifies obd disconnected state.
     */
    private void notifyObdDisconnected() {
        obdState = ObdState.OBD_DISCONNECTED;
        for (ObdConnectionListener listener : mObdConnectionListener) {
            listener.onObdDisconnected();
        }
    }


    /**
     * Notifies obd connecting state.
     */
    private void notifyObdConnecting() {
        obdState = ObdState.OBD_CONNECTING;
        for (ObdConnectionListener listener : mObdConnectionListener) {
            listener.onObdConnecting();
        }
    }

    /**
     * Notifies obd connected state.
     */
    private void notifyObdConnected() {
        obdState = ObdState.OBD_CONNECTED;
        for (ObdConnectionListener listener : mObdConnectionListener) {
            listener.onObdConnected();
        }
    }

    /**
     * Notifies obd initialised state.
     */
    private void notifyObdInitialised() {
        obdState = ObdState.OBD_INITIALISING;
        for (ObdConnectionListener listener : mObdConnectionListener) {
            listener.onObdInitialised();
        }
    }

    /**
     * Interface representing connection listener response with methods such as:
     * <ul>
     * <li>{@link #onSpeedObtained(SpeedData)}</li>
     * <li>{@link #onObdConnected()}</li>
     * <li>{@link #onObdDisconnected()}</li>
     * <li>{@link #onObdConnecting()}</li>
     * <li>{@link #onObdInitialised()}</li>
     * </ul>
     */
    public interface ObdConnectionListener {

        void onSpeedObtained(SpeedData speed);

        void onObdConnected();

        void onObdDisconnected();

        void onObdConnecting();

        void onObdInitialised();
    }

    /**
     * Interface which represents the types of obd available, such as:
     * <ul>
     * <li>{@link #WIFI}</li>
     * <li>{@link #BLE}</li>
     * </ul>
     */
    @IntDef
    public @interface ObdTypes {

        /**
         * The value for obd wifi.
         */
        int WIFI = 0;

        /**
         * The value for obd ble.
         */
        int BLE = 1;

        /**
         * The value for obd not set.
         */
        int NOT_SET = -1;
    }

    /**
     * Interface which represents the states of the OBD, such as:
     * <ul>
     * <li>{@link #OBD_NOT_CONNECTED}</li>
     * <li>{@link #OBD_CONNECTING}</li>
     * <li>{@link #OBD_CONNECTED}</li>
     * <li>{@link #OBD_DISCONNECTED}</li>
     * <li>{@link #OBD_INITIALISING}</li>
     * </ul>
     * The normal lifecycle flow is:
     * <p>
     * {@link #OBD_INITIALISING} --> {@link #OBD_CONNECTING} --> {@link #OBD_CONNECTED}.
     * </p>
     * {@link #OBD_DISCONNECTED} can happen at every lifecycle step. Once the {@link #OBD_INITIALISING} has been passed the manager is configured to continuously re-attempt
     * connecting. This can be stoped only by the user if the manually disconnect from the OBD.
     */
    @IntDef
    public @interface ObdState {

        /**
         * The OBD state when the device has been disconnected from the OBD.
         */
        int OBD_DISCONNECTED = 0;

        /**
         * The OBD state when the device has been connected for the OBD.
         */
        int OBD_CONNECTED = 1;

        /**
         * The OBD state when the device is connecting to the OBD.
         */
        int OBD_CONNECTING = 2;

        /**
         * The OBD state when the OBD is being initialised for data collecting based on specified settings. This is called only if the setup was successful.
         */
        int OBD_INITIALISING = 3;

        /**
         * The OBD state when the device is not yet connected to the OBD. The state represents FTUE state for the app.
         */
        int OBD_NOT_CONNECTED = -1;
    }
}
