package com.telenav.osv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

/**
 * Created by Dmitri on 13/07/2015.
 */
public class ScrollDisabledListView extends ListView {

  private int mPosition;

  private boolean canScroll;

  public ScrollDisabledListView(Context context) {
    super(context);
  }

  public ScrollDisabledListView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ScrollDisabledListView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (!canScroll) {
      final int actionMasked = ev.getActionMasked() & MotionEvent.ACTION_MASK;

      if (actionMasked == MotionEvent.ACTION_DOWN) {
        // Record the position the list the touch landed on
        mPosition = pointToPosition((int) ev.getX(), (int) ev.getY());
        return super.dispatchTouchEvent(ev);
      }

      if (actionMasked == MotionEvent.ACTION_MOVE) {
        // Ignore move events
        return true;
      }

      if (actionMasked == MotionEvent.ACTION_UP) {
        // Check if we are still within the same view
        if (pointToPosition((int) ev.getX(), (int) ev.getY()) == mPosition) {
          super.dispatchTouchEvent(ev);
        } else {
          // Clear pressed state, cancel the action
          setPressed(false);
          invalidate();
          return true;
        }
      }
    }
    return super.dispatchTouchEvent(ev);
  }

  public void canScroll(boolean enable) {
    canScroll = enable;
  }
}