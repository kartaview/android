package com.telenav.osv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

/**
 * frame layout with fixed physical position on children (for orientation change)
 * Created by Kalman on 26/10/16.
 */

public class FixedFrameLayout extends FrameLayout {

  public FixedFrameLayout(Context context) {
    super(context);
  }

  public FixedFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public FixedFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void refreshChildren(boolean portrait) {
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child != null) {
        if (child.getTag() instanceof String) {
          String tag = (String) child.getTag();
          LayoutParams lp = (LayoutParams) child.getLayoutParams();
          switch (tag) {
            case "top|start":
              lp.gravity = portrait ? (Gravity.TOP | Gravity.START) : (Gravity.BOTTOM | Gravity.START);
              break;
            case "top|end":
              lp.gravity = portrait ? (Gravity.TOP | Gravity.END) : (Gravity.TOP | Gravity.START);
              break;
            case "bottom|end":
              lp.gravity = portrait ? (Gravity.BOTTOM | Gravity.END) : (Gravity.BOTTOM | Gravity.END);
              break;
            case "bottom|start":
              lp.gravity = portrait ? (Gravity.BOTTOM | Gravity.START) : (Gravity.TOP | Gravity.END);
              break;
          }
        }
      }
    }
  }
}
