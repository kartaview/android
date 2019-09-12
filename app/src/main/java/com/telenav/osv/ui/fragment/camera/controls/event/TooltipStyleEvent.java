package com.telenav.osv.ui.fragment.camera.controls.event;

import androidx.annotation.StyleRes;

/**
 * Model class for a tooltip event.
 */
public class TooltipStyleEvent {

    /**
     * The tooltip style resource
     */
    @StyleRes
    private int tooltipStyle;

    /**
     * The tooltip message to be displayed.
     */
    private String tooltipMessage;

    public TooltipStyleEvent(String tooltipMessage, @StyleRes int tooltipStyle) {
        this.tooltipMessage = tooltipMessage;
        this.tooltipStyle = tooltipStyle;
    }

    /**
     * @return an {@code String} representing the tooltip message.
     */
    public String getTooltipMessage() {
        return tooltipMessage;
    }

    /**
     * @return an {@code int} representing the {@code StyleRes} of the tooltip style.
     */
    public int getTooltipStyle() {
        return tooltipStyle;
    }
}
