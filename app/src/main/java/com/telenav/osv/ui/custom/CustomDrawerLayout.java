package com.telenav.osv.ui.custom;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * quick fix for drawer layout error
 * Created by Kalman on 29/07/16.
 */
public class CustomDrawerLayout extends DrawerLayout {

  public CustomDrawerLayout(Context context) {
    super(context);
  }

  public CustomDrawerLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CustomDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    try {
      return super.onInterceptTouchEvent(ev);
    } catch (Exception e) {
      //uncomment if you really want to see these errors
      //Log.d(TAG, Log.getStackTraceString(e));
      return false;
    }
  }
}
