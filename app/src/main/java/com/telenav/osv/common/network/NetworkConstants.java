package com.telenav.osv.common.network;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import androidx.annotation.IntDef;

/**
 * The constants of the network which be used in various app features.
 */
public class NetworkConstants {

    /**
     * Value for when the network will signal a duplicate value for data in server requests.
     * @since api_v1
     */
    public static final int NETWORK_ERROR_API_V1_CODE_DUPLICATE_KEY = 660;

    /**
     * Value for when the network will signal a duplicate value for data in server requests.
     * @since api_v2
     */
    public static final int NETWORK_ERROR_API_V2_CODE_DUPLICATE_KEY = 406;

    /**
     * Value for when the network will signal a success response in server requests.
     * @since api_v1
     */
    public static final int NETWORK_SUCCESS_API_V1_CODE_SUCCESS = 600;

    /**
     * Error api codes returned by the API.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {NETWORK_ERROR_API_V1_CODE_DUPLICATE_KEY, NETWORK_ERROR_API_V2_CODE_DUPLICATE_KEY})
    public @interface NetworkErrorApiCode {
        //empty since we use the value from the current class in the value field
    }

    /**
     * Api codes returned by the API.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {NETWORK_SUCCESS_API_V1_CODE_SUCCESS})
    public @interface NetworkApiCodes {
        //empty since we use the value from the current class in the value field
    }
}
