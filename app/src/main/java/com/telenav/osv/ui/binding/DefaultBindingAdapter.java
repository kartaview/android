package com.telenav.osv.ui.binding;

import android.databinding.BindingAdapter;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.v7.widget.SwitchCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.telenav.osv.R;
import com.telenav.osv.item.Sequence;

/**
 * Created by kalmanb on 8/29/17.
 */
public class DefaultBindingAdapter {

    private static final String TAG = "DefaultBindingAdapter";

    @BindingAdapter("android:text")
    public void setText(TextView view, String text) {
        if (text == null) {
            return;
        }
        view.setText(text);
    }

    @BindingAdapter("android:text")
    public void setText(TextView view, @StringRes int resId) {
        if (resId == -1) {
            return;
        }
        view.setText(resId);
    }

    @BindingAdapter("android:text")
    public void setText(TextView view, Spannable text) {
        if (text == null) {
            return;
        }
        view.setText(text);
    }

    @BindingAdapter("android:text")
    public void setText(TextView view, SpannableString text) {
        if (text == null) {
            return;
        }
        view.setText(text);
    }

    @BindingAdapter("android:textColor")
    public void setTextColor(TextView view, @ColorRes int color) {
        if (color == -1) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
            view.setTextColor(view.getContext().getResources().getColor(color));
        }
    }

    @BindingAdapter("imageUrl")
    public void loadImage(ImageView view, Sequence sequence) {
        if (sequence == null) {
            return;
        }
        Glide.with(view.getContext())
                .load(sequence.getThumbLink())
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .skipMemoryCache(false)
                .signature(new StringSignature(
                        sequence.getThumbLink() + " " + sequence.getLocation().getLatitude() + ", " + sequence.getLocation().getLongitude()))
                .error(R.drawable.vector_picture_placeholder)
                .into(view);
    }

    @BindingAdapter("profilePicture")
    public void loadImage(ImageView view, String url) {
        if (url == null) {
            return;
        }
        Glide.with(view.getContext())
                .load(url)
                .centerCrop()
                .priority(Priority.IMMEDIATE)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .skipMemoryCache(false)
                .signature(new StringSignature(url))
                .placeholder(R.drawable.vector_profile_placeholder)
                .error(R.drawable.vector_profile_placeholder)
                .into(view);
    }

    @BindingAdapter("onToggle")
    public void onToggleChecked(SwitchCompat view, CompoundButton.OnCheckedChangeListener listener) {
        view.setOnCheckedChangeListener(listener);
    }
}
