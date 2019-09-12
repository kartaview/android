package com.telenav.osv.network;

import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.utils.Log;

/**
 * Factory used in order to fetch server endpoint url. This can be done by calling {@link #getServerEndpoint()}.
 * <p> If any modifications will be done to the persistence where the server url is kept the {@link #invalidate()} can be called to set the correct url.
 * <p> The method will be called once in the constructor for the current class in order to set the correct url at initialise time.
 * @author horatiuf
 */
public class FactoryServerEndpointUrl {

    /**
     * Url separator which will be in turn used in order to be replaced based on the current persistence, {@link #applicationPreferences}, set value.
     */
    public static final String SERVER_PLACEHOLDER = "&&";

    /**
     * Array containing all the endpoints for the server in the app.
     * <p> The persistence behind the app (currently {@link #applicationPreferences}).
     */
    private static final String[] SERVER_ENDPOINTS =
            {"api.openstreetcam.org/", "staging-api.openstreetcam.org/", "testing-api.openstreetcam.org/", "beta-api.openstreetcam.org/", "api.private.openstreetcam.org/"};

    /**
     * The identifier for the current class used in logs.
     */
    private static final String TAG = FactoryServerEndpointUrl.class.getSimpleName();

    /**
     * Create sequence request url.
     */
    private static String URL = "https://" + SERVER_PLACEHOLDER;

    /**
     * Reference to application preferences which holds the current setup for the environment value.
     */
    private ApplicationPreferences applicationPreferences;

    /**
     * Default constructor for the current class.
     */
    public FactoryServerEndpointUrl(ApplicationPreferences applicationPreferences) {
        this.applicationPreferences = applicationPreferences;
        invalidate();
    }

    /**
     * @return {@code String} which will have the current server endpoint based on the settings value.
     * <p>Note that this will contain the protocol with which it will be used. If is not required use {@link #getServerEndpointWithoutProtocol()}.</p>
     */
    public String getServerEndpoint() {
        if (URL.contains(SERVER_PLACEHOLDER)) {
            //checks if the url was set or not based on the existence of the placeholder in it.
            invalidate();
        }
        return URL;
    }

    /**
     * @return {@code String} which will have the current server endpoint without the procol as a prefix based on the settings value.
     */
    public String getServerEndpointWithoutProtocol() {
        String environmentWithoutProtocol = SERVER_ENDPOINTS[applicationPreferences.getIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE)];
        Log.d(TAG, String.format("getServerEndpointWithoutProtocol. Status: get environment without protocol. Environment: %s.", environmentWithoutProtocol));
        return environmentWithoutProtocol;
    }

    /**
     * @return {@code Array} of {@code Strings} represents all the available endpoints for the app.
     */
    public String[] getServerEndpoints() {
        return SERVER_ENDPOINTS;
    }

    /**
     * Invalidates the current url based on the settings saved in app persistence.
     */
    public void invalidate() {
        String environment = SERVER_ENDPOINTS[applicationPreferences.getIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE)];
        Log.d(TAG, String.format("invalidate. Status: set environment. Environment: %s.", environment));
        URL = URL.replace(SERVER_PLACEHOLDER, environment);
    }
}
