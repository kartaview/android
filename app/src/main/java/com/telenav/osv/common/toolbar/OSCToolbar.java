package com.telenav.osv.common.toolbar;

import android.graphics.Color;
import android.util.SparseArray;
import android.view.View;
import com.telenav.osv.utils.StringUtils;
import androidx.annotation.MenuRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import static com.telenav.osv.common.toolbar.ToolbarSettings.Builder.DEFAULT_VALUE;

/**
 * Class responsible with {@code Toolbar} customisation.
 * The available operations are:
 * <ul>
 * <li>{@link OSCToolbar#updateToolbar(ToolbarSettings)}</li>
 * <li>{@link OSCToolbar#setTitle(String)}</li>
 * <li>{@link OSCToolbar#setTitle(int)}</li>
 * <li>{@link OSCToolbar#showToolbar()}</li>
 * <li>{@link OSCToolbar#hideToolbar()}</li>
 * <li>{@link OSCToolbar#addMenu(int, SparseArray)}</li>
 * </ul>
 * @see ToolbarSettings
 */
public class OSCToolbar {

    /**
     * Default navigation click listener which is set when the toolbar is created.
     * This is not affected when the toolbar is updated using
     * {@link ToolbarSettings.Builder#setNavigationIcon(int, View.OnClickListener)}.
     * If the click listener is set through update then will have priority over this default implementation.
     */
    private View.OnClickListener navigationClickListener;

    /**
     * Default value for the title color.
     * This value can be modified by updating the toolbar using {@link OSCToolbar#updateToolbar(ToolbarSettings)} method
     * and setting {@link ToolbarSettings.Builder#setTextColor(int)}
     */
    private int titleColor = Color.BLACK;

    /**
     * Default value for the toolbar background.
     * This value can be modified by updating the toolbar using {@link OSCToolbar#updateToolbar(ToolbarSettings)} method
     * and setting {@link ToolbarSettings.Builder#setBackgroundColor(int)}
     */
    private int backgroundColor = Color.WHITE;

    /**
     * A (key, value) structure which is contains as key the resource id of a menu item and
     * as value the desired click action.
     */
    private SparseArray<MenuAction> menuActions;

    /**
     * The view instance of the toolbar on which the setting will be applied.
     */
    private Toolbar toolbar;

    /**
     * Default constructor for the class. which defines a default implementation for the toolbar.
     * @param toolbar the view instance of the toolbar which should be customised.
     * @param navigationClickListener the default listener for navigation click.
     */
    public OSCToolbar(Toolbar toolbar, View.OnClickListener navigationClickListener) {
        this.toolbar = toolbar;
        this.navigationClickListener = navigationClickListener;
        toolbar.setBackgroundColor(backgroundColor);
        toolbar.setTitleTextColor(titleColor);
        toolbar.setNavigationOnClickListener(navigationClickListener);
    }

    /**
     * Updates the toolbar with the given settings.
     * The old toolbar settings will be invalidated.
     * @param settings the new toolbar settings which should be applied.
     */
    public void updateToolbar(ToolbarSettings settings) {
        invalidateToolbar();
        menuActions = settings.getMenuActions();
        customiseToolbar(settings);
    }

    /**
     * Displays the toolbar view.
     */
    public void showToolbar() {
        toolbar.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the toolbar view.
     */
    public void hideToolbar() {
        toolbar.setVisibility(View.GONE);
    }

    /**
     * Sets the toolbar title from a {@code String} resource.
     * @param titleResource the id of the {@code String} resource.
     */
    public void setTitle(@StringRes int titleResource) {
        toolbar.setTitle(titleResource);
    }

    /**
     * Sets the toolbar title from a {@code String}.
     * @param title the {@code String} title.
     */
    public void setTitle(String title) {
        toolbar.setTitle(title);
    }

    /**
     * Adds a new menu resource to the current menu.
     * If the toolbar doesn't contain a menu than will contain only the current added elements.
     * @param menuId the menu resource id.
     * @param menuActions the actions for the menu elements.
     */
    public void addMenu(@MenuRes int menuId, SparseArray<MenuAction> menuActions) {
        addActions(menuActions);
        toolbar.inflateMenu(menuId);
        toolbar.setOnMenuItemClickListener(item -> {
            menuActions.get(item.getItemId()).onClick();
            return true;
        });
    }

    /**
     * Sets the settings to the {@code Toolbar} view.
     * @param settings the settings which should be added.
     */
    private void customiseToolbar(ToolbarSettings settings) {
        if (toolbar == null) {
            return;
        }
        if (settings.getTitle() != null) {
            toolbar.setTitle(settings.getTitle());
        }
        if (settings.getTitleResource() != DEFAULT_VALUE) {
            toolbar.setTitle(settings.getTitleResource());
        }

        if (settings.getNavigationIcon() != DEFAULT_VALUE) {
            toolbar.setNavigationIcon(settings.getNavigationIcon());
        }
        if (settings.getNavigationClickListener() != null) {
            toolbar.setNavigationOnClickListener(settings.getNavigationClickListener());
        }
        if (settings.getMenuResource() != DEFAULT_VALUE) {
            toolbar.inflateMenu(settings.getMenuResource());
        }
        if (menuActions != null) {
            toolbar.setOnMenuItemClickListener(item -> {
                menuActions.get(item.getItemId()).onClick();
                return true;
            });
        }
        if (settings.getBackgroundColor() != 0) {
            backgroundColor = settings.getBackgroundColor();
            toolbar.setBackgroundColor(backgroundColor);
        }
        if (settings.getTitleColor() != 0) {
            titleColor = settings.getTitleColor();
            toolbar.setTitleTextColor(titleColor);
        }
    }

    /**
     * Restores to default the toolbar settings.
     */
    private void invalidateToolbar() {
        if (toolbar == null) {
            return;
        }
        toolbar.getMenu().clear();
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(navigationClickListener);
        toolbar.setTitle(StringUtils.EMPTY_STRING);
        backgroundColor = Color.WHITE;
        titleColor = Color.BLACK;
        toolbar.setTitleTextColor(titleColor);
        toolbar.setBackgroundColor(backgroundColor);
        toolbar.setVisibility(View.VISIBLE);
    }

    /**
     * Adds new menu actions to the existing menu.
     * @param actions the new actions to be added.
     */
    private void addActions(SparseArray<MenuAction> actions) {
        if (menuActions == null) {
            menuActions = new SparseArray<>();
        }
        for (int i = 0; i < actions.size(); i++) {
            int key = actions.keyAt(i);
            menuActions.put(key, actions.get(key));
        }
    }
}