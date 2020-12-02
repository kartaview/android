package com.telenav.osv.obd.model;

import android.view.View;
import com.telenav.osv.common.adapter.model.GeneralItemBase;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/**
 * Setting item which has a specific icon on the left and on the titleResId.
 * @author horatiuf
 */
public class LeftIconTitleSettingItem extends GeneralItemBase {

    /**
     * The title resource id for the current setting item.
     */
    @StringRes
    private int titleResId;

    /**
     * The icon resource id for the icon.
     */
    @DrawableRes
    private int iconResId;

    /**
     * The default constructor for the current class.
     * @param titleResId the {@link #titleResId}.
     * @param iconResId the {@link #iconResId}.
     * @param clickListener the click listener for the setting.
     */
    public LeftIconTitleSettingItem(@StringRes int titleResId, @DrawableRes int iconResId, View.OnClickListener clickListener) {
        super(clickListener);
        this.titleResId = titleResId;
        this.iconResId = iconResId;
    }

    @Override
    public int getType() {
        return GeneralItemBase.GENERAL_TYPE_OBD_LEFT_ICON_TITLE;
    }

    /**
     * @return {@code String} representing {@link #titleResId}.
     */
    @StringRes
    public int getTitleResId() {
        return titleResId;
    }

    /**
     * @return {@code String} representing {@link #iconResId}.
     */
    @DrawableRes
    public int getIconResId() {
        return iconResId;
    }
}
