package com.telenav.osv.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import com.telenav.osv.R;

/**
 * Focus ring HUD that lets user focus (tap to focus)
 * Created by Kalman on 10/7/2015.
 */
public class FocusHudRing extends ImageView {

    private final static float IDLE_ALPHA = 0.25f;

    private final static int ANIMATION_DURATION = 120;


    private Drawable successDrawable;

    private Drawable failDrawable;

    public FocusHudRing(Context context) {
        super(context);
        successDrawable = context.getResources().getDrawable(R.drawable.hud_focus_ring_success);
        failDrawable = context.getResources().getDrawable(R.drawable.hud_focus_ring_fail);
    }

    public FocusHudRing(Context context, AttributeSet attrs) {
        super(context, attrs);
        successDrawable = context.getResources().getDrawable(R.drawable.hud_focus_ring_success);
        failDrawable = context.getResources().getDrawable(R.drawable.hud_focus_ring_fail);
    }

    public FocusHudRing(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        successDrawable = context.getResources().getDrawable(R.drawable.hud_focus_ring_success);
        failDrawable = context.getResources().getDrawable(R.drawable.hud_focus_ring_fail);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setAlpha(IDLE_ALPHA);
        setFocusImage(true);
    }


    public void animatePressDown() {
        animate().alpha(1.0f).setDuration(ANIMATION_DURATION).start();
    }

    public void animatePressUp() {
        animate().alpha(IDLE_ALPHA).rotation(0).setDuration(ANIMATION_DURATION).start();
    }

    public void animateWorking(long duration) {
        animate().rotationBy(45.0f).setDuration(duration).setInterpolator(
                new DecelerateInterpolator()).start();
    }

    public void setFocusImage(boolean success) {
        if (success) {
            setImageDrawable(successDrawable);
        } else {
            setImageDrawable(failDrawable);
        }
    }
}
