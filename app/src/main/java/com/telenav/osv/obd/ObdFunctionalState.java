package com.telenav.osv.obd;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import androidx.annotation.IntDef;

/**
 * Interface containing the functional state of the OBD.
 * @author cameliao
 */

@Retention(RetentionPolicy.SOURCE)
@IntDef({ObdFunctionalState.OBD_FUNCTIONAL, ObdFunctionalState.OBD_NOT_SUPPORTED, ObdFunctionalState.OBD_NEED_PERMISSIONS})
public @interface ObdFunctionalState {
    /**
     * OBD is functional and ready to connect.
     */
    int OBD_FUNCTIONAL = 0;

    /**
     * OBD is not supported on this device.
     */
    int OBD_NOT_SUPPORTED = 1;

    /**
     * Device needs permission to access the bluetooth or the wi-fi.
     */
    int OBD_NEED_PERMISSIONS = 2;
}
