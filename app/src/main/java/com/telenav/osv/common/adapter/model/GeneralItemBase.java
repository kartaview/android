package com.telenav.osv.common.adapter.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import android.view.View;
import androidx.annotation.IntDef;

/**
 * Base model class for settings items.
 * @author horatiuf
 */
public abstract class GeneralItemBase {

    /**
     * The type for an item which has an left icon and a title.
     */
    public static final int GENERAL_TYPE_OBD_LEFT_ICON_TITLE = 0;

    /**
     * The type for an item which has a left icon, a title and a subtitle.
     */
    public static final int GENERAL_TYPE_OBD_LEFT_ICON_TITLE_SUBTITLE = 1;

    /**
     * The type for a bluetooth device item containing a name, an address, a left icon
     * and a custom click listener.
     */
    public static final int GENERAL_TYPE_OBD_BT_DEVICE_ITEM = 2;

    /**
     * The type for a header item containing a title and a progress indicator.
     */
    public static final int GENERAL_TYPE_OBD_HEADER = 3;

    /**
     * The type for a header item containing an icon and a subtitle.
     */
    public static final int GENERAL_TYPE_TAGGING = 4;

    /**
     * Consumer code which handles on click operation for the setting item.
     */
    private View.OnClickListener clickListener;

    /**
     * Default constructor for the base item.
     * @param clickListener the clickListener for the click logic.
     */
    protected GeneralItemBase(View.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /**
     * @return {@code Function} representing on click handle logic.
     */
    public View.OnClickListener getClickListener() {
        return clickListener;
    }

    /**
     * @return {@code int} representing the item type.
     */
    @GeneralItemTypes
    public abstract int getType();

    /**
     * The available types for the settings items around the app such as:
     * <ul>
     * <li>{@link #GENERAL_TYPE_OBD_LEFT_ICON_TITLE}</li>
     * <li>{@link #GENERAL_TYPE_OBD_HEADER}</li>
     * <li>{@link #GENERAL_TYPE_OBD_LEFT_ICON_TITLE}</li>
     * <li>{@link #GENERAL_TYPE_OBD_LEFT_ICON_TITLE_SUBTITLE}</li>
     * </ul>
     * @author horatiuf
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {GENERAL_TYPE_OBD_BT_DEVICE_ITEM, GENERAL_TYPE_OBD_HEADER, GENERAL_TYPE_OBD_LEFT_ICON_TITLE, GENERAL_TYPE_OBD_LEFT_ICON_TITLE_SUBTITLE, GENERAL_TYPE_TAGGING})
    public @interface GeneralItemTypes {
        //empty since is not required
    }
}
