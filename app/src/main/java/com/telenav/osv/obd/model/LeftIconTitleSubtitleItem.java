package com.telenav.osv.obd.model;

import android.view.View;
import com.telenav.osv.common.adapter.model.GeneralItemBase;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/**
 * Setting item which has a specific icon on the lef, a title id and a subtitle id.
 * @author cameliao
 */

public class LeftIconTitleSubtitleItem extends GeneralItemBase {

    /**
     * The title resource id for the current item.
     */
    @StringRes
    private int titleResId;

    /**
     * The subtitle resource id for the current item.
     */
    @StringRes
    private int subtitleResId;

    /**
     * The icon resource id for the left icon.
     */
    @DrawableRes
    private int iconResId;

    /**
     * The default constructor for the current class.
     * @param titleResId the {@link #titleResId}.
     * @param subtitleResId the {@link #subtitleResId}.
     * @param iconResId the {@link #iconResId}.
     * @param clickListener the click listener for the current item.
     */
    public LeftIconTitleSubtitleItem(int titleResId, int subtitleResId, int iconResId, View.OnClickListener clickListener) {
        super(clickListener);
        this.titleResId = titleResId;
        this.subtitleResId = subtitleResId;
        this.iconResId = iconResId;
    }

    @Override
    public int getType() {
        return GeneralItemBase.GENERAL_TYPE_OBD_LEFT_ICON_TITLE_SUBTITLE;
    }

    /**
     * @return id of a {@code String} representing {@link #titleResId}.
     */
    public int getTitleResId() {
        return titleResId;
    }

    /**
     * @return id of a {@code String} representing {@link #subtitleResId}.
     */
    public int getSubtitleResId() {
        return subtitleResId;
    }

    /**
     * @return id of a {@code String} representing {@link #iconResId}.
     */
    public int getIconResId() {
        return iconResId;
    }
}
