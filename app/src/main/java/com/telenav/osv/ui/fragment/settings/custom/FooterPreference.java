package com.telenav.osv.ui.fragment.settings.custom;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.telenav.osv.R;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

/**
 * Defines a {@code PreferenceGroup} for the custom settings footer.
 */
public class FooterPreference extends PreferenceGroup {
    /**
     * Touch listener for the hidden debug feature.
     */
    private View.OnTouchListener touchListener;

    /**
     * The app build version.
     */
    private String buildVersion;

    /**
     * Constructor for the footer preference.
     * @param context the context to create the view.
     * @param touchListener the touch listener for the hidden debug feature.
     * @param buildVersion the app build version.
     */
    public FooterPreference(@NonNull Context context, @Nullable View.OnTouchListener touchListener, @NonNull String buildVersion) {
        super(context, null);
        setLayoutResource(R.layout.settings_item_footer);
        this.touchListener = touchListener;
        this.buildVersion = buildVersion;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        holder.itemView.setOnTouchListener(touchListener);
        TextView buildVersionTextView = (TextView) holder.findViewById(R.id.build_version);
        if (buildVersionTextView != null) {
            buildVersionTextView.setText(buildVersion);
        }
    }
}