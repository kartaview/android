package com.telenav.osv.common.ui.loader;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.telenav.osv.utils.AnimationUtils;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

/**
 * Class that displays a loading screen above fragment container.
 * @author horatiuf
 */
public class LoadingScreen extends PopupBase {

    @LayoutRes
    private int loadingScreenLayoutId;

    @IdRes
    private int messageLayoutId;

    @StringRes
    private int stringResId;

    /**
     * Default constructor for the current class.
     * @param loadingScreenLayoutId the loading screen layout id. Must be a {@link LayoutRes}.
     * @param messageLayoutId the message id layout
     */
    public LoadingScreen(@LayoutRes int loadingScreenLayoutId, @IdRes int messageLayoutId, @StringRes int stringResId) {
        this.loadingScreenLayoutId = loadingScreenLayoutId;
        this.messageLayoutId = messageLayoutId;
        this.stringResId = stringResId;
    }

    @Override
    public void show(ViewGroup container) {
        addViewToPopupFrame(container);
        setText();
        fadeInScreen(AnimationUtils.ANIMATION_DURATION_200);
    }

    @Override
    public void show(ViewGroup container, int fadeInTimeMs) {
        addViewToPopupFrame(container);
        setText();
        fadeInScreen(fadeInTimeMs);
    }

    @Override
    public void hide(ViewGroup container) {
        if (isVisible()) {
            fadeOutScreen(container);
        }
    }

    @Override
    int getLayoutId() {
        return loadingScreenLayoutId;
    }

    /**
     * Sets the text to the loading indicator.
     */
    private void setText() {
        TextView textView = panel.findViewById(messageLayoutId);
        if (textView != null) {
            textView.setText(stringResId);
        }
    }

    /**
     * Fades in the screen.
     */
    private void fadeInScreen(int fadeInTimeMs) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(AnimationUtils.getFadeInAnimator(panel, 0, 0, null, null));
        animatorSet.setDuration(fadeInTimeMs);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                panel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {}

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        animatorSet.start();
    }

    /**
     * Fades out down the screen.
     * @param container the {@code ViewGroup} representing the container which holds the popup.
     */
    private void fadeOutScreen(final ViewGroup container) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(AnimationUtils.getFadeOutAnimator(panel, 0, 0, null, null));
        animatorSet.setDuration(AnimationUtils.ANIMATION_DURATION_200);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                if (panel != null) {
                    panel.setVisibility(View.GONE);
                }
                removeViewFromPopupFrame(container);
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        animatorSet.start();
    }
}
