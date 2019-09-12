package com.telenav.osv.common.toolbar;

import android.graphics.Color;
import android.util.SparseArray;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.StringRes;

/**
 * Settings class for {@code Toolbar} customisation.
 */
public class ToolbarSettings {

    /**
     * The icon for the navigation which will be displayed the in left side.
     */
    private int navigationIcon;

    /**
     * The custom navigation click listener.
     */
    private View.OnClickListener navigationClickListener;

    /**
     * The {@code String} resource id for title.
     */
    private int titleResource;

    /**
     * The {@code String} containing the title.
     */
    private String title;

    /**
     * The resource id for the title color.
     */
    private int titleColor;

    /**
     * The resource id containing the menu items which will be displayed in the right side.
     */
    private int menuResource;

    /**
     * The actions for each menu item defined as (key, value),
     * where the key represents the id of the menu item and
     * the value represents the action to execute for that specific item.
     */
    private SparseArray<MenuAction> menuActions;

    /**
     * The resource id for the toolbar background color.
     */
    private int backgroundColor;

    /**
     * Private constructor for preventing instantiation without using the {@link Builder}.
     * @param builder the builder instance which contains the toolbar settings;
     */
    private ToolbarSettings(Builder builder) {
        navigationIcon = builder.navigationIcon;
        titleResource = builder.titleResource;
        title = builder.title;
        titleColor = builder.titleColor;
        menuResource = builder.menuResource;
        menuActions = builder.menuActions;
        backgroundColor = builder.backgroundColor;
        navigationClickListener = builder.navigationOnClickListener;
    }

    /**
     * @return a drawable resource id representing the {@link ToolbarSettings#navigationIcon}.
     */
    public int getNavigationIcon() {
        return navigationIcon;
    }

    /**
     * @return the navigation click listener representing the {@link ToolbarSettings#navigationClickListener}.
     */
    public View.OnClickListener getNavigationClickListener() {
        return navigationClickListener;
    }


    /**
     * @return a string resource id representing the {@link ToolbarSettings#titleResource}.
     */
    public int getTitleResource() {
        return titleResource;
    }

    /**
     * @return a {@code String} object representing the {@link ToolbarSettings#title}.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return a color resource id representing the {@link ToolbarSettings#titleColor}.
     */
    public int getTitleColor() {
        return titleColor;
    }

    /**
     * @return a menu resource id representing the {@link ToolbarSettings#menuResource}.
     */
    public int getMenuResource() {
        return menuResource;
    }

    /**
     * @return a (key, value) structure representing the {@link ToolbarSettings#menuActions}.
     */
    public SparseArray<MenuAction> getMenuActions() {
        return menuActions;
    }

    /**
     * @return a color resource id representing the {@link ToolbarSettings#backgroundColor}.
     */
    public int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Builder class for creating a {@link ToolbarSettings} instance.
     */
    public static class Builder {

        /**
         * Default value for the resources.
         */
        static final int DEFAULT_VALUE = -1;

        /**
         * @see ToolbarSettings#navigationIcon
         */
        int navigationIcon = DEFAULT_VALUE;

        /**
         * @see ToolbarSettings#navigationClickListener
         */
        View.OnClickListener navigationOnClickListener;

        /**
         * @see ToolbarSettings#titleResource
         */
        int titleResource = DEFAULT_VALUE;

        /**
         * @see ToolbarSettings#title
         */
        String title;

        /**
         * @see ToolbarSettings#titleColor
         */
        int titleColor = Color.BLACK;

        /**
         * @see ToolbarSettings#menuResource
         */
        int menuResource = DEFAULT_VALUE;

        /**
         * @see ToolbarSettings#menuActions
         */
        SparseArray<MenuAction> menuActions;

        /**
         * @see ToolbarSettings#backgroundColor
         */
        int backgroundColor = Color.WHITE;

        /**
         * Sets the navigation icon with a custom action.
         * @param toolbarNavigationIcon the resource id for the icon.
         * @param clickListener the action to perform when is clicked.
         * @return the {@code Builder} instance.
         * @see ToolbarSettings#navigationIcon
         * @see ToolbarSettings#navigationClickListener
         */
        public Builder setNavigationIcon(@DrawableRes int toolbarNavigationIcon, View.OnClickListener clickListener) {
            this.navigationIcon = toolbarNavigationIcon;
            navigationOnClickListener = clickListener;
            return this;
        }

        /**
         * Sets the navigation icon.
         * @param toolbarNavigationIcon the resource id for the icon.
         * @return the {@code Builder} instance.
         * @see ToolbarSettings#navigationIcon
         */
        public Builder setNavigationIcon(@DrawableRes int toolbarNavigationIcon) {
            this.navigationIcon = toolbarNavigationIcon;
            return this;
        }

        /**
         * Sets the title from resources.
         * @param toolbarTitleResource the resource id for the title.
         * @return the {@code Builder} instance.
         * @see ToolbarSettings#titleResource
         */
        public Builder setTitle(@StringRes int toolbarTitleResource) {
            this.titleResource = toolbarTitleResource;
            return this;
        }

        /**
         * Sets the title using {@code String} object.
         * @param toolbarTitle the title to be set.
         * @return the {@code Builder} instance.
         * @see ToolbarSettings#title
         */
        public Builder setTitle(String toolbarTitle) {
            this.title = toolbarTitle;
            return this;
        }

        /**
         * Sets the color title.
         * @param titleColor the color for the title text.
         * @return the {@code Builder} instance.
         * @see ToolbarSettings#titleColor
         */
        public Builder setTextColor(@ColorInt int titleColor) {
            this.titleColor = titleColor;
            return this;
        }

        /**
         * Sets the menu resource and the actions for each item.
         * @param menuRes the resource id for the menu.
         * @param menuActions a (key, value) pair representing the action for each menu item.
         * @return the {@code Builder} instance.
         * @see ToolbarSettings#menuResource
         * @see ToolbarSettings#menuActions
         */
        public Builder setMenuResources(@MenuRes int menuRes, SparseArray<MenuAction> menuActions) {
            menuResource = menuRes;
            this.menuActions = menuActions;
            return this;
        }

        /**
         * Sets the toolbar background color.
         * @param color the color for the toolbar background.
         * @return the {@code Builder} instance.
         * @see ToolbarSettings#backgroundColor
         */
        public Builder setBackgroundColor(@ColorInt int color) {
            backgroundColor = color;
            return this;
        }

        /**
         * Creates a {@code ToolbarSettings} instance.
         * @return an instance of {@code ToolbarSettings}.
         */
        public ToolbarSettings build() {
            return new ToolbarSettings(this);
        }
    }
}
