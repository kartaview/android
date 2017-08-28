package com.telenav.osv.ui.custom;

import android.widget.ScrollView;

/**
 * scroll change listener
 * Created by Kalman on 1/11/16.
 */
interface OnScrollChangedListener {

  void onScrollChanged(ScrollView view, int x, int y, int oldX, int oldY);
}
