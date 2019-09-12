package com.telenav.osv.common.tooltip;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.telenav.osv.R;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.core.widget.TextViewCompat;

/**
 * Class that creates a custom tooltip which can be anchored to left, right, top or bottom to a view.
 * @author cameliao
 */

public class OscTooltip {

    /**
     * Default value for the tooltip properties.
     */
    private static final int UNASSIGNED_VALUE = Integer.MAX_VALUE;

    /**
     * The view to which the tooltip will be anchored.
     */
    private View anchorView;

    /**
     * The tooltip layout
     */
    private View tooltipView;

    /**
     * The tooltip parent view
     */
    private ViewGroup parentView;

    /**
     * A flag to determine if the tooltip should be dismiss on touch or not.
     */
    private boolean isDismissOnTouch;

    /**
     * A flag to determine if the tooltip should be dismiss on outside touch or not.
     */
    private boolean isDismissOnOutsideTouch;

    /**
     * The margin between the tooltip and the anchored view.
     */
    private float margin;

    /**
     * The gravity value to determine the position of the tooltip to the anchored view.
     * The value of the gravity can be {@link Gravity#TOP}, {@link Gravity#BOTTOM}, {@link Gravity#LEFT}, {@link Gravity#RIGHT}.
     */
    private int gravity;

    /**
     * The message which will be displayed in the tooltip.
     */
    private TextView textViewMessage;

    /**
     * The arrow of the tooltip towards the anchored view.
     */
    private ImageView imageViewArrow;

    /**
     * The fade in animation for showing the tooltip.
     */
    private Animation fadeIn;

    /**
     * The fade out animation for dismissing the tooltip.
     */
    private Animation fadeOut;

    /**
     * Represents the screen orientation defined as one of these values:
     * {@link Configuration#ORIENTATION_PORTRAIT}, {@link Configuration#ORIENTATION_LANDSCAPE}.
     */
    private int screenOrientation;

    /**
     * The constructor of the class.
     * @param builder the builder object containing all the tooltip properties.
     */
    OscTooltip(Builder builder) {
        invalidateTooltip(builder);
        RelativeLayout.LayoutParams params;
        if (isDismissOnOutsideTouch) {
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        } else {
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        }
        parentView.addView(tooltipView, params);
        hide();
    }

    /**
     * @return true if the tooltip is visible, false otherwise.
     */
    public boolean isShowing() {
        return (tooltipView.getParent() != null) && (tooltipView.getVisibility() == View.VISIBLE);
    }

    /**
     * Dismisses the tooltip from screen.
     * @param dismissWithAnimation if the flag is set to true the dismiss will be animate with a fade out animation.
     */
    public void dismiss(boolean dismissWithAnimation) {
        if (dismissWithAnimation) {
            tooltipView.startAnimation(fadeOut);
        }
        parentView.removeView(tooltipView);
    }

    /**
     * Invalidates the tooltip style and message and sets new properties defined in {@code Builder}.
     * @param builder all teh properties that should be set to the current tooltip.
     */
    public void invalidateTooltip(Builder builder) {
        screenOrientation = builder.context.getResources().getConfiguration().orientation;
        anchorView = builder.anchorView;
        isDismissOnTouch = builder.isDismissOnTouch;
        isDismissOnOutsideTouch = builder.isDismissOnOutsideTouch;
        parentView = builder.parentView;
        margin = builder.margin;
        gravity = builder.gravity;
        initTooltipView(builder.context);
        initTooltipStyle(builder);
        fadeIn = android.view.animation.AnimationUtils.loadAnimation(builder.context, R.anim.alpha_add);
        fadeOut = android.view.animation.AnimationUtils.loadAnimation(builder.context, R.anim.alpha_remove);
    }

    /**
     * Hides the tooltip from screen.
     */
    public void hide() {
        tooltipView.setVisibility(View.GONE);
    }

    /**
     * Shows the tooltip on the screen.
     * @param showWithAnimation if the flag is set to true the tooltip displaying will be animated with a fade in animation.
     */
    public void show(boolean showWithAnimation) {
        tooltipView.setVisibility(View.VISIBLE);
        if (showWithAnimation) {
            tooltipView.startAnimation(fadeIn);
        }
        setTooltipPosition();
    }

    /**
     * Initialises the tooltip style.
     * @param builder the builder containing the tooltip properties.
     */
    private void initTooltipStyle(Builder builder) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(builder.backgroundColor);
        drawable.setCornerRadius(builder.cornerRadius);

        textViewMessage.setText(builder.message);
        textViewMessage.setBackground(drawable);
        imageViewArrow.setColorFilter(builder.backgroundColor);
        textViewMessage.setPadding(builder.textPadding, builder.textPadding, builder.textPadding, builder.textPadding);
        textViewMessage.setTypeface(builder.typeface, builder.textStyle);

        if (builder.textAppearance != UNASSIGNED_VALUE) {
            TextViewCompat.setTextAppearance(textViewMessage, builder.textAppearance);
        }

        if (builder.textColor != UNASSIGNED_VALUE) {
            textViewMessage.setTextColor(builder.textColor);
        }

        if (builder.textSize != UNASSIGNED_VALUE) {
            textViewMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, builder.textSize);
        }
    }

    /**
     * Initialises the tooltip by anchoring it to the given view and by setting the message which should be displayed.
     * @param context the context used to inflate the tooltip layout.
     */
    private void initTooltipView(Context context) {
        if (tooltipView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (layoutInflater != null) {
                tooltipView = layoutInflater.inflate(R.layout.partial_tooltip, null);
            }
        }
        textViewMessage = tooltipView.findViewById(R.id.text_view_tooltip_message);
        switch (gravity) {
            case Gravity.START:
                imageViewArrow = tooltipView.findViewById(R.id.image_view_triangle_left);
                imageViewArrow.setVisibility(View.VISIBLE);
                break;
            case Gravity.END:
                imageViewArrow = tooltipView.findViewById(R.id.image_view_triangle_right);
                imageViewArrow.setVisibility(View.VISIBLE);
                break;
            case Gravity.TOP:
                imageViewArrow = tooltipView.findViewById(R.id.image_view_triangle_top);
                imageViewArrow.setVisibility(View.VISIBLE);
                break;
            case Gravity.BOTTOM:
                imageViewArrow = tooltipView.findViewById(R.id.image_view_triangle_bottom);
                imageViewArrow.setVisibility(View.VISIBLE);
                break;
            case Gravity.BOTTOM | Gravity.START:
                imageViewArrow = tooltipView.findViewById(R.id.image_view_triangle_bottom_left);
                imageViewArrow.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Sets the tooltip position to the anchor view.
     */
    private void setTooltipPosition() {
        ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!checkIfScreenOrientationIsEquivalentToParentSize()) {
                    return;
                }
                anchorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Rect rect = localizeView(anchorView);
                switch (gravity) {
                    case Gravity.START:
                        tooltipView.setX(rect.left - tooltipView.getWidth() - margin);
                        tooltipView.setY(rect.centerY() - tooltipView.getHeight() / 2);
                        break;
                    case Gravity.END:
                        tooltipView.setX(rect.right + margin);
                        tooltipView.setY(rect.centerY() - tooltipView.getHeight() / 2);
                        break;
                    case Gravity.TOP:
                        tooltipView.setX(rect.centerX() - tooltipView.getWidth() / 2);
                        tooltipView.setY(rect.top - tooltipView.getHeight() - margin);
                        break;
                    case Gravity.BOTTOM:
                        tooltipView.setX(rect.centerX() - tooltipView.getWidth() / 2);
                        tooltipView.setY(rect.bottom + margin);
                        break;
                    case Gravity.BOTTOM | Gravity.START:
                        tooltipView.setX(rect.centerX() / 2);
                        tooltipView.setY(rect.bottom + margin);
                        break;
                }
            }
        };
        anchorView.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        if (isDismissOnTouch || isDismissOnOutsideTouch) {
            tooltipView.setOnClickListener(v ->
                    parentView.removeView(tooltipView));
        }
    }

    /**
     * Checks if the parent view height and width are equivalent with the current screen orientation.
     * For the landscape mode view's width should be grater than it's height and for the portrait mode
     * view's width should be less than it's height.
     * @return true if the parent view's width and height correspond with the screen orientation, false otherwise.
     */
    private boolean checkIfScreenOrientationIsEquivalentToParentSize() {
        return (parentView.getWidth() < parentView.getHeight() && screenOrientation == Configuration.ORIENTATION_PORTRAIT) ||
                (parentView.getWidth() > parentView.getHeight() && screenOrientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    /**
     * Constructs a rectangle around the given view.
     * @param view the view which will be localized on the screen.
     * @return the rectangle created around the given view.
     */
    private Rect localizeView(View view) {
        int[] coordinates = new int[2];
        view.getLocationInWindow(coordinates);
        Rect location = new Rect();
        location.left = coordinates[0];
        location.top = coordinates[1];
        location.right = location.left + view.getWidth();
        location.bottom = location.top + view.getHeight();
        return location;
    }

    /**
     * Builder class to set the tooltip properties.
     */
    public static class Builder {

        /**
         * The context of the application.
         */
        private Context context;

        /**
         * The view to which the tooltip should be anchored.
         */
        private View anchorView;

        /**
         * The parent view group that will hold the tooltip view.
         */
        private ViewGroup parentView;

        /**
         * A flag to determine if the tooltip should be dismiss on outside touch.
         */
        private boolean isDismissOnOutsideTouch;

        /**
         * A flag to determine if the tooltip should be dismiss on touch.
         */
        private boolean isDismissOnTouch;

        /**
         * The tooltip background color.
         */
        private int backgroundColor;

        /**
         * The tooltip corner radius.
         */
        private float cornerRadius;

        /**
         * The margin between the tooltip and anchored view.
         */
        private float margin;

        /**
         * The gravity of the tooltip to the anchored view.
         */
        private int gravity;

        /**
         * The message to be displayed in the tooltip.
         */
        private String message;

        /**
         * The tooltip text appearance specified from a style.
         */
        private int textAppearance;

        /**
         * The text padding in the tooltip.
         */
        private int textPadding;

        /**
         * The tooltip text size.
         */
        private float textSize;

        /**
         * The tooltip text color.
         */
        private int textColor;

        /**
         * The tooltip text style.
         */
        private int textStyle;

        private Typeface typeface;

        /**
         * The constructor for the tooltip builder.
         * @param anchorView the view to which the tooltip is anchored.
         */
        public Builder(@NonNull View anchorView) {
            this(anchorView, (ViewGroup) anchorView.getRootView(), 0);
        }

        /**
         * Constructor for the tooltip builder.
         * @param anchorView the view to which the tooltip is anchored.
         * @param parentView the view to hold the tooltip.
         * @param tooltipStyle the style resource of the tooltip.
         */
        public Builder(@NonNull View anchorView, ViewGroup parentView, @StyleRes int tooltipStyle) {
            this.context = anchorView.getContext();
            this.anchorView = anchorView;
            this.parentView = parentView;
            initTooltipCustomXmlValues(context, tooltipStyle);
        }

        /**
         * Sets the property {@link #isDismissOnOutsideTouch}, the default value is false.
         * @param isDismissOnOutsideTouch the value to be set to the property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setDismissOnOutsideTouch(boolean isDismissOnOutsideTouch) {
            this.isDismissOnOutsideTouch = isDismissOnOutsideTouch;
            return this;
        }

        /**
         * Sets the property {@link #isDismissOnTouch}, the default value is false.
         * @param isDismissOnTouch the value to be set to the property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setDismissOnTouch(boolean isDismissOnTouch) {
            this.isDismissOnTouch = isDismissOnTouch;
            return this;
        }

        /**
         * Sets the property {@link #backgroundColor}, the default value is {@link Color#WHITE}.
         * @param backgroundColor the value to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setBackgroundColor(@ColorRes int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        /**
         * Sets the property {@link #cornerRadius}, the default value is 0.
         * @param cornerRadius the value to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setCornerRadius(@DimenRes int cornerRadius) {
            this.cornerRadius = cornerRadius;
            return this;
        }

        /**
         * Sets the property {@link #message}.
         * @param messageId the value to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setMessage(@StringRes int messageId) {
            return setMessage(context.getResources().getString(messageId));
        }

        /**
         * Sets the property {@link #message}.
         * @param message the value to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the property {@link #margin}, the default value is {@link R.dimen#default_tooltip_margin}.
         * @param marginId the value to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setMargin(@DimenRes int marginId) {
            return setMargin(context.getResources().getDimension(marginId));
        }

        /**
         * Sets the property {@link #margin}, the default value is {@link R.dimen#default_tooltip_margin}.
         * @param margin the value to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setMargin(float margin) {
            this.margin = margin;
            return this;
        }

        /**
         * Sets the property {@link #gravity}, the default value is {@link Gravity#BOTTOM}.
         * @param gravity the value to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setGravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        /**
         * Sets the property {@link #textAppearance}.
         * @param textAppearance the specified style resource to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setTextAppearance(@StyleRes int textAppearance) {
            this.textAppearance = textAppearance;
            return this;
        }

        /**
         * Sets the property {@link #textStyle}, the default value is {@link Typeface#NORMAL}.
         * @param textStyle the value to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setTextStyle(int textStyle) {
            this.textStyle = textStyle;
            return this;
        }

        /**
         * Sets the property {@link #textPadding}, the default value is {@link R.dimen#default_tooltip_padding}.
         * @param textPadding the value to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setTextPadding(@DimenRes int textPadding) {
            this.textPadding = textPadding;
            return this;
        }

        /**
         * Sets the property {@link #textSize}.
         * @param textSize the value to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setTextSize(@DimenRes int textSize) {
            this.textSize = textSize;
            return this;
        }

        /**
         * Sets the property {@link #textColor}.
         * @param textColor the value to be set to the given property.
         * @return the current Builder object to allow further changes to the tooltip properties.
         */
        public Builder setTextColor(@ColorRes int textColor) {
            this.textColor = textColor;
            return this;
        }

        /**
         * Creates a {@link OscTooltip} with the given properties through this builder, without showing the tooltip.
         * @return {@link OscTooltip} instance.
         */
        public OscTooltip build() {
            return new OscTooltip(this);
        }

        /**
         * Builds and shows a {@link OscTooltip} with the given properties through this builder.
         * @param showWithAnimation if the flag is set to true the tooltip displaying will be animated with a fade in animation.
         * @return {@link OscTooltip} instance.
         */
        public OscTooltip show(boolean showWithAnimation) {
            OscTooltip tooltip = build();
            tooltip.show(showWithAnimation);
            return tooltip;
        }

        /**
         * Initializes the value for the tooltip properties.
         * @param context the context application to access the style attributes for the tooltip.
         * @param tooltipStyle the style resource for the tooltip.
         */
        private void initTooltipCustomXmlValues(@NonNull Context context, @StyleRes int tooltipStyle) {
            TypedArray typedArray = context.obtainStyledAttributes(tooltipStyle, R.styleable.Tooltip);

            isDismissOnTouch = typedArray.getBoolean(R.styleable.Tooltip_dismissOnTouch, false);
            isDismissOnOutsideTouch = typedArray.getBoolean(R.styleable.Tooltip_dismissOnOutsideTouch, false);
            backgroundColor = typedArray.getColor(R.styleable.Tooltip_android_background, Color.WHITE);
            cornerRadius = typedArray.getDimension(R.styleable.Tooltip_android_radius, context.getResources().getDimension(R.dimen.default_tooltip_corner_radius));
            margin = typedArray.getDimension(R.styleable.Tooltip_android_layout_margin, context.getResources().getDimension(R.dimen.default_tooltip_margin));
            gravity = typedArray.getInteger(R.styleable.Tooltip_android_gravity, Gravity.BOTTOM);
            message = typedArray.getString(R.styleable.Tooltip_android_text);
            textAppearance = typedArray.getResourceId(R.styleable.Tooltip_android_textAppearance, UNASSIGNED_VALUE);
            textPadding = (int) typedArray.getDimension(R.styleable.Tooltip_android_padding, context.getResources().getDimension(R.dimen.default_tooltip_padding));
            textSize = typedArray.getDimensionPixelSize(R.styleable.Tooltip_android_textSize, UNASSIGNED_VALUE);
            textStyle = typedArray.getInteger(R.styleable.Tooltip_android_textStyle, Typeface.NORMAL);
            textColor = typedArray.getColor(R.styleable.Tooltip_android_textColor, UNASSIGNED_VALUE);
            String textFontFamily = typedArray.getString(R.styleable.Tooltip_android_fontFamily);
            typeface = Typeface.create(textFontFamily, Typeface.NORMAL);

            typedArray.recycle();

        }

    }
}
