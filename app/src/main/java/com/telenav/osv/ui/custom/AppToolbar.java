package com.telenav.osv.ui.custom;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by kalmanb on 8/16/17.
 */
public class AppToolbar extends Toolbar {

  public AppToolbar(Context context) {
    super(context);
  }

  public AppToolbar(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public AppToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    final int action = ev.getActionMasked();
    boolean mEatingTouch = true;
    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      mEatingTouch = false;
    }
    return isClickable() || mEatingTouch;
  }
}
