package com.telenav.osv.ui.fragment.transition;

import android.support.transition.ChangeBounds;
import android.support.transition.ChangeImageTransform;
import android.support.transition.ChangeTransform;
import android.support.transition.TransitionSet;

/**
 * Created by kalmanb on 8/9/17.
 */
public class ScaleFragmentTransition extends TransitionSet {

  public ScaleFragmentTransition() {
    setOrdering(ORDERING_TOGETHER);
    addTransition(new ChangeBounds()).addTransition(new ChangeTransform()).addTransition(new ChangeImageTransform());
  }
}
