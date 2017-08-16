package com.telenav.spherical.network;

/**
 * Device information class
 */
public class CameraInfo {
    private String mModel = "";

    private String mDeviceVersion = "";

    private String mSerialNumber = "";

    /**
     * Constructor
     */
    public CameraInfo() {
    }

    /**
     * Acquire model name
     * @return Model name
     */
    public String getModel() {
        return mModel;
    }

    /**
     * Set model name
     * @param model Model name
     */
    public void setModel(String model) {
        mModel = model;
    }

    /**
     * Acquire serial number
     * @return Serial number
     */
    public String getSerialNumber() {
        return mSerialNumber;
    }

    /**
     * Set serial number
     * @param serialNumber Serial number
     */
    public void setSerialNumber(String serialNumber) {
        mSerialNumber = serialNumber;
    }

    /**
     * Acquire firmware version
     * @return Firmware version
     */
    public String getDeviceVersion() {
        return mDeviceVersion;
    }

    /**
     * Set firmware version
     * @param version Firmware version
     */
    public void setDeviceVersion(String version) {
        mDeviceVersion = version;
    }
}
