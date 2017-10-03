package com.telenav.osv.ui.list;

import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * Created by kalmanb on 9/6/17.
 */
public class BindingRecyclerAdapter<T> extends me.tatarka.bindingcollectionadapter2.BindingRecyclerViewAdapter<T> {

  public static final String TAG = "BindingRecyclerAdapter";

  @Override
  public ViewDataBinding onCreateBinding(LayoutInflater inflater, @LayoutRes int layoutId, ViewGroup viewGroup) {
    ViewDataBinding binding = super.onCreateBinding(inflater, layoutId, viewGroup);
    return binding;
  }

  @Override
  public void onBindBinding(ViewDataBinding binding, int variableId, @LayoutRes int layoutRes, int position, T item) {
    super.onBindBinding(binding, variableId, layoutRes, position, item);
  }
}
