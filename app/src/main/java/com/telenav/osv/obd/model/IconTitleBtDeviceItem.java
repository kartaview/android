package com.telenav.osv.obd.model;

import com.telenav.osv.common.adapter.model.GeneralItemBase;
import com.telenav.osv.listener.OnDeviceSelectedListener;
import androidx.annotation.DrawableRes;

/**
 * Setting item specifically for bluetooth devices containing the name and the address of device.
 * @author cameliao
 */

public class IconTitleBtDeviceItem extends GeneralItemBase {

    /**
     * The icon resource id for the current item.
     */
    @DrawableRes
    private int iconResId;

    /**
     * The name of the item.
     */
    private String name;

    /**
     * The address of the item.
     */
    private String address;

    /**
     * The visibility of the divider between the items.
     */
    private boolean itemDividerVisible;

    /**
     * The item custom click listener.
     */
    private OnDeviceSelectedListener clickListener;

    /**
     * Default constructor for the current class.
     * @param name the name of the item.
     * @param address the address of the item.
     * @param iconResId the icon resource id.
     * @param itemDividerVisible the visibility of the divider between the items.
     * @param clickListener the click listener for the current item.
     */
    public IconTitleBtDeviceItem(String name, String address, @DrawableRes int iconResId, boolean itemDividerVisible, OnDeviceSelectedListener clickListener) {
        super(null);
        this.iconResId = iconResId;
        this.name = name;
        this.address = address;
        this.clickListener = clickListener;
        this.itemDividerVisible = itemDividerVisible;
    }

    @Override
    public int getType() {
        return GeneralItemBase.GENERAL_TYPE_OBD_BT_DEVICE_ITEM;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IconTitleBtDeviceItem && ((IconTitleBtDeviceItem) obj).getAddress().equals(this.getAddress());
    }

    /**
     * @return an {@code int} representing {@link #iconResId}.
     */
    public int getIconResId() {
        return iconResId;
    }

    /**
     * @return a {@code String} representing the {@link #name}.
     */
    public String getName() {
        return name;
    }

    /**
     * @return a {@code String} representing the {@link #address}.
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return a {@code boolean} value which represents the visibility of an item divider.
     */
    public boolean isItemDividerVisible() {
        return itemDividerVisible;
    }

    /**
     * @return a {@code OnDeviceSelectedListener} representing {@link #clickListener}.
     */
    public OnDeviceSelectedListener getOnDeviceClickListener() {
        return clickListener;
    }
}
