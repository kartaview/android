package com.telenav.osv.ui.fragment.transition;

import androidx.transition.ChangeBounds;
import androidx.transition.ChangeImageTransform;
import androidx.transition.ChangeTransform;
import androidx.transition.TransitionSet;

/**
 * Created by kalmanb on 8/9/17.
 */
public class ScaleFragmentTransition extends TransitionSet {

    public ScaleFragmentTransition() {
        setOrdering(ORDERING_TOGETHER);
        addTransition(new ChangeBounds()).addTransition(new ChangeTransform()).addTransition(new ChangeImageTransform());
    }
}
