package com.telenav.osv.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by Kalman on 21/02/2017.
 */
public class DimenUtils {

    public final static String TAG = "DimenUtils";

    public static Point getContentSize(Context context, boolean portrait, Point size) {
        Point realSize = getRealScreenSize(context);
        Point navbarSize = portrait ? getNavigationBarSizeIfExistAtTheBottom(context) : getNavigationBarSizeIfExistOnTheRight(context);
        Point statusbarSize = new Point(realSize.x, getStatusBarHeight(context));

        if (portrait) {
            size.x = realSize.x;
            size.y = realSize.y - statusbarSize.y - navbarSize.y;
        } else {
            size.x = realSize.x - navbarSize.x;
            size.y = realSize.y - statusbarSize.y;
        }
        return size;
    }

    private static int getStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        Resources resources = context.getResources();
        int id = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) {
            statusBarHeight = resources.getDimensionPixelSize(id);
        }
        return statusBarHeight;
    }

    private static Point getNavigationBarSizeIfExistAtTheBottom(Context context) {
        Point appUsableSize = getAppUsableScreenSize(context);
        Point realScreenSize = getRealScreenSize(context);
        Point navigationPoint;
        boolean navigationBarIsPresent = appUsableSize.y < realScreenSize.y;
        if (navigationBarIsPresent) {
            navigationPoint = new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
        } else {
            navigationPoint = new Point();
        }
        return navigationPoint;
    }

    private static Point getNavigationBarSizeIfExistOnTheRight(Context context) {
        Point appUsableSize = getAppUsableScreenSize(context);
        Point realScreenSize = getRealScreenSize(context);
        Point navigationPoint;
        boolean navigationBarIsPresent = appUsableSize.x < realScreenSize.x;
        if (navigationBarIsPresent) {
            navigationPoint = new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
        } else {
            navigationPoint = new Point();
        }
        return navigationPoint;
    }

    private static Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    private static Point getRealScreenSize(Context context) {
        Point size = new Point();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            display.getRealSize(size);
        } else {
            display.getSize(size);
            //            View decorView = null;
            //            if (context instanceof Activity) {
            //                decorView = ((Activity) context).getWindow().getDecorView();
            //            } else if (context instanceof ContextThemeWrapper) {
            //                Context baseContext = ((ContextThemeWrapper) context).getBaseContext();
            //                if (baseContext instanceof Activity) {
            //                    decorView = ((Activity) baseContext).getWindow().getDecorView();
            //                }
            //            }
            //            if (decorView != null) {
            //                size.x = decorView.getWidth();
            //                size.y = decorView.getHeight();
            //            }
        }
        return size;
    }
}