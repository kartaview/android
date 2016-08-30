package com.telenav.osv.ui.fragment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.StringSignature;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.external.glview.GLPhotoView;
import com.telenav.osv.external.model.Photo;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import uk.co.senab.photoview.PhotoView;

/**
 * Created by Kalman on 12/8/15.
 */
public class FullscreenPreviewFragment extends Fragment {

    public static final String TAG = "FullscreenPrevFragment";

    private static final int OFFSCREEN_LIMIT = 5;

    public static boolean mHasFocus;

    private ScrollDisabledViewPager fullscreenPager;

    private FullscreenPagerAdapter fullscreenPagerAdapter;

    private MainActivity activity;

    private int startPosition;

    private ArrayList<ImageFile> images = new ArrayList<>();

    private Glide mGlide;

    private OnPageSelectedListener mPageSelectedListener;

    private boolean fabHidden = false;

    private boolean mPanoramaSequence = false;

    private boolean mDinamic = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_fullscreen_pager, container, false);
        activity = (MainActivity) getActivity();
        fullscreenPager = (ScrollDisabledViewPager) rootView.findViewById(R.id.fullscreen_pager);
//        if (mDinamic) {
//            activity.showActionBar("Image " + (startPosition + 1), true);
//        }
        mGlide = Glide.get(activity);
        fullscreenPagerAdapter = new FullscreenPagerAdapter(activity);
        fullscreenPager.setOffscreenPageLimit(OFFSCREEN_LIMIT / 2);
        fullscreenPager.setAdapter(fullscreenPagerAdapter);
        fullscreenPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mHasFocus = true;
                return false;
            }
        });
        fullscreenPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (mPageSelectedListener != null && mHasFocus) {
                    mPageSelectedListener.onPageSelected(position);
                }
                View view = fullscreenPager.getChildAt(0);
                if (view != null) {
                    View image = view.findViewById(R.id.image);
                    if (image instanceof PhotoView) {
                        Matrix matrix = ((PhotoView) image).getDisplayMatrix();
                        matrix.setScale(1, 1);
                        ((PhotoView) image).setDisplayMatrix(matrix);
                    }
                }
                view = fullscreenPager.getChildAt(1);
                if (view != null) {
                    View image = view.findViewById(R.id.image);
                    if (image instanceof PhotoView) {
                        Matrix matrix = ((PhotoView) image).getDisplayMatrix();
                        matrix.setScale(1, 1);
                        ((PhotoView) image).setDisplayMatrix(matrix);
                    }
                }
                view = fullscreenPager.getChildAt(2);
                if (view != null) {
                    View image = view.findViewById(R.id.image);
                    if (image instanceof PhotoView) {
                        Matrix matrix = ((PhotoView) image).getDisplayMatrix();
                        matrix.setScale(1, 1);
                        ((PhotoView) image).setDisplayMatrix(matrix);
                    }
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        fullscreenPager.post(new Runnable() {
            @Override
            public void run() {
                fullscreenPager.setCurrentItem(startPosition);
            }
        });
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!mPanoramaSequence) {
            fullscreenPager.setScrollEnabled(true);
        }
    }

    public long getMemoryStats() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem / 1048576L;

//Percentage can be calculated for API 16+
        long percentAvail = mi.availMem / mi.totalMem;
        return availableMegs;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mDinamic) {
//            MainActivity.sFragmentOverlayTag = TAG;
        }
    }

    @Override
    public void onDetach() {
        if (mDinamic) {
//            MainActivity.sFragmentOverlayTag = "";
        }
        super.onDetach();
        if (mGlide != null) {
            mGlide.clearMemory();
        }
    }

    public void setFABHidden() {
        fabHidden = true;
    }

    public void setType(boolean dinamic) {
        this.mDinamic = dinamic;
    }

    public void setSource(ArrayList<ImageFile> images, int position, boolean postUpdating, OnPageSelectedListener listener) {
        mPageSelectedListener = listener;
        setSource(images, position, postUpdating);
    }

    public void setSource(ArrayList<ImageFile> images, int position, boolean postUpdating) {
        this.images = images;
        if (fullscreenPagerAdapter != null) {
            fullscreenPagerAdapter.notifyDataSetChanged();
        }
        if (images.size() > 0 && images.get(0).isPano) {
            mPanoramaSequence = true;
        } else {
            mPanoramaSequence = false;
            if (fullscreenPager != null) {
                fullscreenPager.setScrollEnabled(true);
            }
        }
        this.startPosition = position;
        if (postUpdating && fullscreenPagerAdapter != null && fullscreenPager != null) {
            fullscreenPagerAdapter.notifyDataSetChanged();
            fullscreenPager.post(new Runnable() {
                @Override
                public void run() {
                    fullscreenPager.setCurrentItem(startPosition, true);
                }
            });
        }
    }

    public void scrollToPosition(int position, boolean smooth) {
        fullscreenPager.setCurrentItem(position, smooth);
    }

    public interface OnPageSelectedListener {
        void onPageSelected(int index);
    }

    private class FullscreenPagerAdapter extends PagerAdapter {
        private final LayoutInflater inflater;

        public FullscreenPagerAdapter(final MainActivity activity) {
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view;
            if (images.get(position).isPano) {
                view = inflater.inflate(R.layout.item_image_view_pager_pano, container, false);
                final FrameLayout holder = (FrameLayout) view.findViewById(R.id.image);
                final GLPhotoView imageView = new GLPhotoView(activity);
                imageView.setZOrderMediaOverlay(true);
                holder.addView(imageView);
//                imageView.setZOrderOnTop(true);
                Glide.with(FullscreenPreviewFragment.this).load(images.get(position).link).asBitmap().into(new SimpleTarget<Bitmap>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                        imageView.setTexture(new Photo(resource));
                    }
                });
            } else {
                view = inflater.inflate(R.layout.item_image_view_pager, container, false);
                if (view != null) {
                    Glide.clear(view);
                    if (position % OFFSCREEN_LIMIT == 0) {
                        mGlide.trimMemory(OFFSCREEN_LIMIT);
                    }
                    DrawableRequestBuilder<String> builder = Glide.with(FullscreenPreviewFragment.this)
                            .load(images.get(position).link)
                            .fitCenter()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .skipMemoryCache(false)
                            .crossFade(250)
                            .signature(new StringSignature(images.get(position).coords.getLatitude() + "," + images.get(position).coords.getLongitude() + " full"))
                            .priority(Priority.IMMEDIATE)
                            .placeholder(R.drawable.image_loading_background)
                            .error(R.drawable.image_broken_background)
                            .listener(MainActivity.mGlideRequestListener);
                    if (!images.get(position).thumb.equals("") && images.get(position).file == null) {
                        builder.thumbnail(Glide.with(FullscreenPreviewFragment.this)
                                .load(images.get(position).thumb)
                                .fitCenter()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .skipMemoryCache(false)
                                .signature(new StringSignature(images.get(position).coords.getLatitude() + "," + images.get(position).coords.getLongitude()))
                                .priority(Priority.IMMEDIATE)
                                .placeholder(R.drawable.image_loading_background)
                                .error(R.drawable.image_broken_background)
                                .listener(MainActivity.mGlideRequestListener));
                    } else if (images.get(position).file != null) {
                        builder.thumbnail(0.2f);
                    }
                    builder.into((ImageView) view);
                }
            }
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (object != null) {
                Glide.clear(((View) object));
                if (((View) object).findViewById(R.id.image) != null && ((View) object).findViewById(R.id.image) instanceof FrameLayout) {
                    ((FrameLayout) ((View) object).findViewById(R.id.image)).removeAllViews();
                }
            }
            container.removeView((View) object);
        }
    }
}
