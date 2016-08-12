package com.telenav.osv.ui.list;

import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.signature.StringSignature;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.item.ImageFile;

/**
 * Created by Kalman on 11/12/15.
 */
public class ImageListAdapter extends BaseAdapter {
    private static final String TAG = "ImageListAdapter";

    private final ArrayList<ImageFile> images;

    private final LayoutInflater inflater;

    private final Activity activity;

    private int mItemWidth = 0;

    private int mItemHeight = 0;

    public ImageListAdapter(Activity activity, ArrayList<ImageFile> nodes) {
        this.images = nodes;
        this.activity = activity;
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        if (images != null) {
            return images.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        if (images != null) {
            return images.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView != null) {
            if (mItemWidth == 0 || mItemHeight == 0) {
                mItemWidth = convertView.getWidth();
                mItemHeight = convertView.getHeight();
                for (int i = 0; i < Math.min(100, images.size()); i++) {
                    Glide.with(ImageListAdapter.this.activity).load(images.get(i).thumb).asBitmap().into(new SimpleTarget<Bitmap>(mItemWidth, mItemHeight) {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            Glide.clear(this);
                        }
                    });
                }
            }
        }
        if (view == null) {
            view = inflater.inflate(R.layout.item_image_scroll_view, parent, false);
            assert view != null;
        }
        Glide.with(activity)
                .load(images.get(position).thumb)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .signature(new StringSignature(images.get(position).coords.getLatitude() + "," + images.get(position).coords.getLongitude()))
                .priority(Priority.IMMEDIATE)
                .placeholder(R.drawable.image_loading_background)
                .error(R.drawable.image_broken_background)
                .listener(MainActivity.mGlideRequestListener)
                .into((ImageView) view);

        return view;
    }

    public ArrayList<ImageFile> getData() {
        return images;
    }
}
