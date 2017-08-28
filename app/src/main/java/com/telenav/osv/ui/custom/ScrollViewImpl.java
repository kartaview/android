package com.telenav.osv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by Kalman on 1/11/16.
 */
public class ScrollViewImpl extends ScrollView {

  private OnScrollChangedListener scrollListener = null;

  public ScrollViewImpl(Context context) {
    super(context);
  }

  public ScrollViewImpl(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ScrollViewImpl(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    super.onScrollChanged(l, t, oldl, oldt);
    if (scrollListener != null) {
      scrollListener.onScrollChanged(this, l, t, oldl, oldt);
    }
  }

  public void setScrollListener(OnScrollChangedListener scrollListener) {
    this.scrollListener = scrollListener;
  }
}
