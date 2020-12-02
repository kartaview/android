package com.telenav.osv.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import com.telenav.osv.R;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

/**
 * Utils class for any animation related generic functionality such as:
 * <ul>
 * <li>{@link #getFadeInAnimator(View)}</li>
 * <li>{@link #getFadeOutAnimator(View)}</li>
 * <li>{@link #getFadeInAnimator(View, int, int, Interpolator, Animator.AnimatorListener)}</li>
 * <li>{@link #getFadeOutAnimator(View, int, int, Interpolator, Animator.AnimatorListener)}</li>
 * </ul>
 * @author horatiuf
 */

public class AnimationUtils {

    /**
     * Animation with 200ms duration.
     */
    public static final int ANIMATION_DURATION_200 = 200;

    /**
     * The value for a 180 degree.
     */
    public static final float DEGREES_180_VALUE = 180f;

    /**
     * The value for a 360 degree.
     */
    public static final float DEGREES_360_VALUE = 360f;

    /**
     * The size of the view coordinates array.
     */
    private static final int VIEW_COORDINATES_DEFAULT_SIZE = 2;

    /**
     * The index of the x coordinate of a view.
     */
    private static final int VIEW_COORDINATE_X_INDEX = 0;

    /**
     * The index of the y coordinate of a view.
     */
    private static final int VIEW_COORDINATE_Y_INDEX = 1;

    /**
     * The property name for an object animator x coordinate property.
     */
    private static final String OBJECT_ANIMATOR_PROPERTY_X = "x";

    /**
     * The property name for an object animator rotation property.
     */
    private static final String OBJECT_ANIMATOR_PROPERTY_ROTATION = "rotation";

    /**
     * @param view view that will be animated
     * @return {@code ObjectAnimator} representing default fade in animator.
     */
    public static ObjectAnimator getFadeInAnimator(View view) {
        return getFadeInAnimator(view, 0, 0, null, null);
    }

    /**
     * @param view view that will be animated
     * @param duration animation duration
     * @param delay animation delay
     * @param interpolator animation interpolator
     * @param listener animation listener
     * @return {@code ObjectAnimator} representing fade in animator.
     */
    public static ObjectAnimator getFadeInAnimator(View view, int duration, int delay, Interpolator interpolator, Animator.AnimatorListener listener) {
        ObjectAnimator fadeInAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f);
        if (interpolator != null) {
            fadeInAnimator.setInterpolator(interpolator);
        } else {
            fadeInAnimator.setInterpolator(new LinearInterpolator());
        }
        fadeInAnimator.setDuration(duration);
        fadeInAnimator.setStartDelay(delay);
        if (listener != null) {
            fadeInAnimator.addListener(listener);
        }

        return fadeInAnimator;
    }

    /**
     * Delayed animation which changes the bounds of a view using {@link TransitionManager}.
     * @param viewToAnimate the {@code ViewGroup} to animate.
     */
    public static void changeBoundsDelayedTransition(ViewGroup viewToAnimate) {
        TransitionManager
                .beginDelayedTransition(
                        viewToAnimate,
                        new TransitionSet()
                                .addTransition(new ChangeBounds()));
    }

    /**
     * returns a fade out animator
     * @param view view that will be animated
     * @return {@code ObjectAnimator} representing fade out animator.
     */
    public static ObjectAnimator getFadeOutAnimator(View view) {
        return getFadeOutAnimator(view, 0, 0, null, null);
    }

    /**
     * @param view view that will be animated
     * @param duration animation duration
     * @param delay animation delay
     * @param interpolator animation interpolator
     * @param listener animation listener
     * @return {@code ObjectAnimator} representing fade out animator.
     */
    public static ObjectAnimator getFadeOutAnimator(View view, int duration, int delay, Interpolator interpolator, Animator.AnimatorListener listener) {
        ObjectAnimator fadeOutAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f);
        if (interpolator != null) {
            fadeOutAnimator.setInterpolator(interpolator);
        } else {
            fadeOutAnimator.setInterpolator(new LinearInterpolator());
        }
        fadeOutAnimator.setDuration(duration);
        fadeOutAnimator.setStartDelay(delay);
        if (listener != null) {
            fadeOutAnimator.addListener(listener);
        }

        return fadeOutAnimator;
    }

    /**
     * @param view The view for which the translate animation will be performed.
     * @param listener the animation listener if required. Can be null.
     * @param pixels A set of values that the animation will animate between over time.
     * @return {@code ObjectAnimator} representing the translation animation.
     */
    public static ObjectAnimator getTranslationObjectAnimator(View view, @Nullable Animator.AnimatorListener listener, float... pixels) {
        ObjectAnimator translateObjectAnimator = ObjectAnimator.ofFloat(view, OBJECT_ANIMATOR_PROPERTY_X, pixels);
        if (listener != null) {
            translateObjectAnimator.addListener(listener);
        }
        return translateObjectAnimator;
    }

    /**
     * @param isSequential {@code true} if the animations is required to play sequentially, {@code false} otherwise.
     * @param duration the duration for the animation.
     * @param delayDuration the delay duration for the animation.
     * @param animatorListener the animation listener. Can be null.
     * @param animators the animator objects which will be included in the animator set.
     * @return {@code AnimatorSet} setup based on the params specified.
     */
    public static AnimatorSet createAnimatorSet(boolean isSequential, long duration, long delayDuration, @Nullable Animator.AnimatorListener animatorListener, Animator...
            animators) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(duration);
        animatorSet.setStartDelay(delayDuration);
        if (isSequential) {
            animatorSet.playSequentially(animators);
        } else {
            animatorSet.playTogether(animators);
        }
        if (animatorListener != null) {
            animatorSet.addListener(animatorListener);
        }
        return animatorSet;
    }

    /**
     * @param view The view for which the rotation animation will be performed.
     * @param duration The duration of the animation.
     * @param listener the animation listener if required. Can be null.
     * @param pixels A set of values that the animation will animate between over time.
     * @return {@code ObjectAnimator} representing the rotation animation.
     */
    public static ObjectAnimator getRotationObjectAnimator(View view, long duration, @Nullable Animator.AnimatorListener listener, float... pixels) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, View.ROTATION, pixels);
        objectAnimator.setDuration(duration);
        if (listener != null) {
            objectAnimator.addListener(listener);
        }
        return objectAnimator;
    }

    /**
     * @param myView the view for which the left position will be returned.
     * @return {@code int} representing the left position of the view in pixels relative to the parent.
     */
    public static int getRelativeLeft(View myView) {
        if (myView.getParent() == myView.getRootView())
            return myView.getLeft();
        else
            return myView.getLeft() + getRelativeLeft((View) myView.getParent());
    }

    /**
     * @param myView the view for which the top position will be returned.
     * @return {@code int} representing the left position of the view in pixel relative to the parent.
     */
    public static int getRelativeTop(View myView) {
        if (myView.getParent() == myView.getRootView())
            return myView.getTop();
        else
            return myView.getTop() + getRelativeTop((View) myView.getParent());
    }

    /**
     * @param view The view for which the x coordinate will be returned.
     * @return {@code int} representing the absolute value of the x coordinate on the screen. <p> The method calls internally {@link #getViewCoordinate(View)}.</p>
     */
    public static int getViewAbsoluteX(View view) {
        return getViewCoordinate(view)[VIEW_COORDINATE_X_INDEX];
    }

    /**
     * @param view The view for which the y coordinate will be returned.
     * @return {@code int} representing the absolute value of the y coordinate on the screen. <p> The method calls internally {@link #getViewCoordinate(View)}.</p>
     */
    public static int getViewAbsoluteY(View view) {
        return getViewCoordinate(view)[VIEW_COORDINATE_Y_INDEX];
    }

    /**
     * @param view The view for which the array of coordinates will be returned.
     * @return {@code int[2]} representing the array of coordinates for a view on the screen, positioned first the x, respectively the y in this order.
     */
    public static int[] getViewCoordinate(View view) {
        int[] viewCoordinates = new int[VIEW_COORDINATES_DEFAULT_SIZE];
        view.getLocationInWindow(viewCoordinates);
        return viewCoordinates;
    }

    public static void resizeCameraUI(Activity activity, int resizeContainerId, int largerContainerId) {
        if (activity == null) {
            return;
        }
        Resources resources = activity.getResources();
        ViewGroup resizeView = activity.findViewById(resizeContainerId);
        ViewGroup largerView = activity.findViewById(largerContainerId);
        AnimationUtils.changeBoundsDelayedTransition(largerView);
        AnimationUtils.changeBoundsDelayedTransition(resizeView);
        ConstraintSet set = new ConstraintSet();
        ConstraintLayout parent = activity.findViewById(R.id.layout_activity_obd_parent);
        if (parent == null) {
            return;
        }
        set.clone(parent);

        set.clear(largerContainerId, ConstraintSet.TOP);
        set.clear(largerContainerId, ConstraintSet.END);
        set.clear(resizeContainerId, ConstraintSet.START);
        set.clear(resizeContainerId, ConstraintSet.BOTTOM);
        set.connect(largerContainerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        set.connect(largerContainerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
        set.connect(largerContainerId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        set.connect(largerContainerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        largerView.setClipToOutline(false);
        largerView.bringToFront();
        set.clear(resizeContainerId, ConstraintSet.TOP);
        set.clear(resizeContainerId, ConstraintSet.END);
        set.clear(resizeContainerId, ConstraintSet.START);
        set.clear(resizeContainerId, ConstraintSet.BOTTOM);
        int margin = Math.round(resources.getDimension(R.dimen.camera_preview_tagging_margin));
        set.connect(resizeContainerId, ConstraintSet.TOP, R.id.guidelineHorizontal, ConstraintSet.BOTTOM, 0);
        set.connect(resizeContainerId, ConstraintSet.END, R.id.guidelineVertical, ConstraintSet.START, 0);
        set.connect(resizeContainerId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin);
        set.connect(resizeContainerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin);
        resizeView.setClipToOutline(true);
        resizeView.bringToFront();
        ViewGroup cameraControls = activity.findViewById(R.id.layout_activity_obd_fragment_container);
        cameraControls.bringToFront();
        ViewGroup clickArea = activity.findViewById(R.id.frame_layout_activity_obd_click_area);
        clickArea.bringToFront();
        ViewGroup loader = activity.findViewById(R.id.frame_layout_activity_obd_loader);
        loader.bringToFront();
        set.applyTo(parent);
    }

    /**
     * Values for animation durations, such as:
     * <ul>
     * <li>{@link #ANIMATION_DURATION_1000_MS}</li>
     * <li>{@link #ANIMATION_DURATION_2000_MS}</li>
     * <li>{@link #ANIMATION_DURATION_1250_MS}</li>
     * </ul>
     */
    @IntDef
    public @interface AnimationDurations {

        /**
         * The value for 500 milliseconds of animation duration.
         */
        int ANIMATION_DURATION_500_MS = 1000;

        /**
         * The value for 1000 milliseconds of animation duration.
         */
        int ANIMATION_DURATION_1000_MS = 1000;

        /**
         * The value for 1250 milliseconds of animation duration.
         */
        int ANIMATION_DURATION_1250_MS = 1250;

        /**
         * The value for 2000 milliseconds of animation duration.
         */
        int ANIMATION_DURATION_2000_MS = 2000;
    }
}
