package com.telenav.osv.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.common.model.data.SnackBarItem;

/**
 * Util class to display default UI elements.
 * @author cameliao
 */

public class UiUtils {

    /**
     * Displays a snack bar on the screen.
     * @param context the app context.
     * @param view the view to find a parent from.
     * @param message the message to be displayed.
     * @param duration the duration for displaying the message.
     * @param button the text used for an action.
     * @param runnable the action that should start when {@code button} is pressed.
     */
    public static void showSnackBar(Context context, View view, String message, final int duration, final CharSequence button, final Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            boolean shouldGoUp;

            @Override
            public void run() {
                Snackbar mSnackBar = Snackbar.make(view, message, duration);
                mSnackBar.setActionTextColor(context.getResources().getColor(R.color.default_purple));
                View snackBarView = mSnackBar.getView();
                snackBarView.setBackgroundColor(context.getResources().getColor(R.color.darker_grey));
                TextView snackBarTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
                snackBarTextView.setTextColor(context.getResources().getColor(R.color.default_white));
                snackBarTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.snack_bar_default_text_size));
                if (button != null && runnable != null) {
                    mSnackBar.setAction(button, v -> runnable.run());
                }
                shouldGoUp = true;
                mSnackBar.show();
            }
        });
    }

    /**
     * Displays a snack bar on the screen.
     * @param context the context to access the app resources.
     * @param view the view to find a parent from.
     * @param snackBarItem the object containing all the information that will be set on the snack bar view.
     */
    public static void showSnackBar(Context context, View view, SnackBarItem snackBarItem) {
        if (snackBarItem == null || view == null) {
            return;
        }
        Snackbar snackBar = Snackbar.make(view, snackBarItem.getMessage(), snackBarItem.getDuration());
        View snackBarView = snackBar.getView();
        snackBarView.setBackgroundColor(context.getResources().getColor(R.color.darker_grey));
        //set message
        TextView snackBarMessageTextView = snackBarView.findViewById(R.id.snackbar_text);
        snackBarMessageTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.snack_bar_default_text_size));
        snackBarMessageTextView.setTextColor(context.getResources().getColor(R.color.default_white));
        //set action
        snackBar.setAction(snackBarItem.getActionLabel(), snackBarItem.getActionListener());
        TextView snackBarActionTextView = snackBarView.findViewById(R.id.snackbar_action);
        snackBarActionTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.snack_bar_default_text_size));
        snackBar.show();
    }

    public static void setData(String value, TextView textView) {
        if (TextUtils.isEmpty(value)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(value);
            textView.setVisibility(View.VISIBLE);
        }
    }

    public static void setData(SpannableString value, TextView textView) {
        if (TextUtils.isEmpty(value)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(value);
            textView.setVisibility(View.VISIBLE);
        }
    }
}
