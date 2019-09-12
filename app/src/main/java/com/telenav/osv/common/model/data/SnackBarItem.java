package com.telenav.osv.common.model.data;

import android.view.View;

/**
 * Model class for a snack bar item.
 */
public class SnackBarItem {

    /**
     * The message that will be displayed in the snack bar.
     */
    private String message;

    /**
     * The duration of the snack bar message.
     * {@link android.support.design.widget.Snackbar#LENGTH_LONG},
     * {@link android.support.design.widget.Snackbar#LENGTH_SHORT},
     * {@link android.support.design.widget.Snackbar#LENGTH_INDEFINITE}.
     */
    private int duration;

    /**
     * The click listener for performing an action when the {@link #actionLabel} is clicked.
     */
    private View.OnClickListener actionListener;

    /**
     * The label for taking an action when is pressed.
     */
    private String actionLabel;

    /**
     * Default constructor for the current class.
     * @param message the message that will be displayed in the snack bar.
     * @param duration the duration of a snack bar message.
     * @param actionLabel the label representing the trigger for an action.
     * @param actionListener the click listener for the {@link #actionLabel} for defining the action to perform.
     */
    public SnackBarItem(String message, int duration, String actionLabel, View.OnClickListener actionListener) {
        this.message = message;
        this.duration = duration;
        this.actionLabel = actionLabel;
        this.actionListener = actionListener;
    }

    /**
     * @return a {@code String} representing {@link #message}.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return an {@code int} representing {@link #duration}.
     */
    public int getDuration() {
        return duration;
    }

    /**
     * @return a {@code ClickListener} representing {@link #actionListener}.
     */
    public View.OnClickListener getActionListener() {
        return actionListener;
    }

    /**
     * @return a {@code String} representing {@link #actionLabel}.
     */
    public String getActionLabel() {
        return actionLabel;
    }
}
