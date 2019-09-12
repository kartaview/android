package com.telenav.osv.obd.faq.details;

import androidx.annotation.StringDef;

/**
 * The urls to purchase recommended obd related products. The available values:
 * <ul>
 * <li>{@link #OBD_WIFI}</li>
 * <li>{@link #OBD_BLE}</li>
 * </ul>
 * @author horatiuf
 */
@StringDef
public @interface ObdDetailsRecommendations {
    /**
     * Url for wifi obd2.
     */
    String OBD_WIFI = "https://www.amazon.com/Veepeak-Scanner-Adapter-Android-Diagnostic/dp/B00WPW6BAE";

    /**
     * Url for obd ble.
     */
    String OBD_BLE = "https://www.amazon.com/LELink-Bluetooth-Energy-OBD-II-Diagnostic/dp/B00QJRYMFC";
}
