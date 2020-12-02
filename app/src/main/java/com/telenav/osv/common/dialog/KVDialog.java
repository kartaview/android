package com.telenav.osv.common.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.telenav.osv.R;
import com.telenav.osv.utils.LogUtils;

/**
 * Dialog class that should be used for displaying the dialog applications.
 * The class uses the Builder pattern in order to customize the dialog.
 *
 * @author cameliao
 */

public class KVDialog extends AlertDialog {

    public static final String TAG = KVDialog.class.getSimpleName();

    /**
     * Default constructor of the class.
     *
     * @param context the application context
     */
    KVDialog(Context context) {
        super(context);
    }

    public static class Builder {
        private View dialogView;

        private Context context;

        private KVDialog kvDialog;

        public Builder(Context context) {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_kv_layout, null);
            this.dialogView = dialogView;
            this.context = context;
            this.kvDialog = new KVDialog(context);
            this.kvDialog.setView(dialogView);
        }

        public Builder setTitleResId(@StringRes int titleResId) {
            TextView titleTextView = dialogView.findViewById(R.id.text_view_custom_dialog_title);
            titleTextView.setText(titleResId);
            titleTextView.setVisibility(View.VISIBLE);
            return this;
        }

        public Builder setInfoResId(@StringRes int infoResId) {
            TextView infoTextView = dialogView.findViewById(R.id.text_view_custom_dialog_info);
            infoTextView.setText(infoResId);
            infoTextView.setVisibility(View.VISIBLE);
            return this;
        }

        public Builder setIconResId(@DrawableRes int iconResId) {
            ImageView imageView = dialogView.findViewById(R.id.image_view_custom_dialog);
            imageView.setImageDrawable(context.getResources().getDrawable(iconResId));
            imageView.setVisibility(View.VISIBLE);
            return this;
        }

        public Builder setPositiveButton(@StringRes int positiveButtonResId, View.OnClickListener positiveButtonClickListener) {
            Button positiveButton = dialogView.findViewById(R.id.button_dialog_obd_connecting_positive);
            positiveButton.setText(positiveButtonResId);
            positiveButton.setVisibility(View.VISIBLE);
            positiveButton.setOnClickListener(positiveButtonClickListener);
            return this;
        }

        public Builder setNegativeButton(@StringRes int negativeButtonResId, View.OnClickListener negativeButtonClickListener) {
            Button negativeButton = dialogView.findViewById(R.id.button_dialog_obd_connecting_negative);
            negativeButton.setText(negativeButtonResId);
            negativeButton.setVisibility(View.VISIBLE);
            negativeButton.setOnClickListener(negativeButtonClickListener);
            return this;
        }

        public Builder setNegativeButtonTextColor(@ColorRes int colorResId) {
            Button negativeButton = dialogView.findViewById(R.id.button_dialog_obd_connecting_negative);
            try {
                negativeButton.setTextColor(ContextCompat.getColor(context, colorResId));
            } catch (Resources.NotFoundException exception) {
                LogUtils.logDebug(TAG, "Color resource not found");
            }
            return this;
        }

        public Builder setTitleTextColor(@ColorRes int colorResId) {
            TextView titleTextView = dialogView.findViewById(R.id.text_view_custom_dialog_title);
            try {
                titleTextView.setTextColor(ContextCompat.getColor(context, colorResId));
            } catch (Resources.NotFoundException exception) {
                LogUtils.logDebug(TAG, "Color resource not found");
            }
            return this;
        }

        public Builder setIconLayoutVisibility(boolean isVisible) {
            View iconLayout = dialogView.findViewById(R.id.layout_custom_dialog_icon_container);
            if (isVisible) {
                iconLayout.setVisibility(View.VISIBLE);
            } else {
                iconLayout.setVisibility(View.GONE);
            }
            return this;
        }

        public Builder setCancelable() {
            kvDialog.setCancelable(false);
            return this;
        }

        public Builder setCancelableOnOutsideClick(boolean cancel) {
            kvDialog.setCanceledOnTouchOutside(cancel);
            return this;
        }

        public Builder setProgressEnable() {
            ProgressBar progressBar = dialogView.findViewById(R.id.progress_bar_custom_dialog);
            progressBar.setVisibility(View.VISIBLE);
            return this;
        }

        public KVDialog build() {
            return kvDialog;
        }
    }
}
