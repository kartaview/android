package com.telenav.osv.ui.fragment.camera.controls.event;

/**
 * Model class for the recording details having a value and a label.
 */
public class RecordingDetails {

    /**
     * The value for the current recording information.
     */
    private String value;

    /**
     * The label for the current recording information.
     */
    private String label;

    public RecordingDetails(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}