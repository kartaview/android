package com.telenav.osv.common.ui.loader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.LayoutRes;

/**
 * Popup base class which holds all functionality for adding/removing a popup layout to/from a container.
 * @author horatiuf
 */

abstract class PopupBase {

    /**
     * Inflated view that will represent the popup.
     */
    ViewGroup panel;

    /**
     * Returns a boolean indicating if the dialog is visible or not.
     * @return if true, layout is visible
     */
    public boolean isVisible() {
        return panel != null && panel.getVisibility() == View.VISIBLE;
    }

    /**
     * Inflates the layout in the wanted parentView.
     */
    void addViewToPopupFrame(ViewGroup container) {
        if (panel != null) {
            return;
        }
        if (container != null) {
            panel = (ViewGroup) ((LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(getLayoutId(), null);
            container.addView(panel, container.getChildCount());
            panel.setVisibility(View.GONE);
        }
    }

    /**
     * Removes the dialog from the popup frame layout
     */
    void removeViewFromPopupFrame(ViewGroup container) {
        if (container != null && panel != null) {
            container.removeView(panel);
            panel = null;
        }
    }

    /**
     * @return the layout's id that will be inflated
     */
    @LayoutRes
    abstract int getLayoutId();

    /**
     * Shows the popup
     */
    abstract void show(ViewGroup container);

    /**
     * Shows the popup
     */
    abstract void show(ViewGroup container, int fadeInTimeMs);

    /**
     * Hides the popup
     */
    abstract void hide(ViewGroup container);
}
