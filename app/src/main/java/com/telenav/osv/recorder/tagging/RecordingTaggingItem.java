package com.telenav.osv.recorder.tagging;

import android.view.View;
import com.telenav.osv.common.adapter.model.GeneralItemBase;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/**
 * Items which will represent a tagging operation.
 */
public class RecordingTaggingItem extends GeneralItemBase {

    /**
     * The subtitles resource id for the current tagging item.
     */
    @StringRes
    private int subtitle;

    /**
     * The icon resource id for the icon.
     */
    @DrawableRes
    private int iconResId;

    /**
     * Default constructor for the base item.
     * @param subtitle the {@link #subtitle}.
     * @param iconResId the {@link #iconResId}.
     * @param clickListener the clickListener for the click logic.
     */
    public RecordingTaggingItem(@StringRes int subtitle, @DrawableRes int iconResId, View.OnClickListener clickListener) {
        super(clickListener);
        this.subtitle = subtitle;
        this.iconResId = iconResId;
    }

    @Override
    public int getType() {
        return GeneralItemBase.GENERAL_TYPE_TAGGING;
    }

    public int getSubtitle() {
        return subtitle;
    }

    public int getIconResId() {
        return iconResId;
    }
}
