package com.telenav.osv.ui.binding.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LifecycleOwner;
import com.telenav.osv.utils.Log;

/**
 * Created by kalmanb on 9/13/17.
 */
public abstract class AbstractViewModel extends AndroidViewModel {

  protected LifecycleOwner owner;

  public AbstractViewModel(Application application) {
    super(application);
  }

  @Override
  protected void onCleared() {
    String tag = this.getClass().getCanonicalName();
    Log.d(tag, "onCleared: cleared viewmodel for " + tag);
    super.onCleared();
  }

  public abstract void setOwner(LifecycleOwner lifecycleOwner);
}
