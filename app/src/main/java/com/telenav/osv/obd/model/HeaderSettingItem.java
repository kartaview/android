package com.telenav.osv.obd.model;

import com.telenav.osv.common.adapter.model.GeneralItemBase;
import androidx.annotation.StringRes;

/**
 * Setting item representing a header item with a title and a progress indicator.
 * @author cameliao
 */

public class HeaderSettingItem extends GeneralItemBase {

    /**
     * The title resource id for the current item.
     */
    @StringRes
    private int titleResId;

    /**
     * The visibility of the progress indicator.
     */
    private boolean isProgressVisible;

    /**
     * Default constructor for the current class.
     * @param titleResId the title resource id.
     * @param isProgressVisible the progress indicator visibility.
     */
    public HeaderSettingItem(@StringRes int titleResId, boolean isProgressVisible) {
        super(null);
        this.titleResId = titleResId;
        this.isProgressVisible = isProgressVisible;
    }

    @Override
    public int getType() {
        return GeneralItemBase.GENERAL_TYPE_OBD_HEADER;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HeaderSettingItem && ((HeaderSettingItem) obj).getTitleResId() == this.getTitleResId();
    }

    public int getTitleResId() {
        return titleResId;
    }

    public boolean isProgressVisible() {
        return isProgressVisible;
    }

    public void setProgressVisible(boolean progressVisible) {
        isProgressVisible = progressVisible;
    }
}
