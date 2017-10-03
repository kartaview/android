package com.telenav.osv.ui.binding.viewmodel;

import android.databinding.DataBindingComponent;
import com.telenav.osv.ui.binding.DefaultBindingAdapter;

/**
 * Created by kalmanb on 8/29/17.
 */
public class DefaultBindingComponent implements DataBindingComponent {

  private static final String TAG = "DefaultBindingComponent";

  private DefaultBindingAdapter mDefaultBindingAdapter = new DefaultBindingAdapter();

  @Override
  public DefaultBindingAdapter getDefaultBindingAdapter() {
    return mDefaultBindingAdapter;
  }
}
