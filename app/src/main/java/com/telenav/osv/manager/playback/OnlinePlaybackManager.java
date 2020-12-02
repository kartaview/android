package com.telenav.osv.manager.playback;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.data.sequence.model.Sequence;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.item.network.PhotoCollection;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;

import uk.co.senab.photoview.PhotoView;

/**
 * component responsible for playing back online content
 * Created by Kalman on 27/07/16.
 */
public class OnlinePlaybackManager extends PlaybackManager implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "OnlinePlaybackManager";

    private static final int OFFSCREEN_LIMIT = 20;

    private static final Object object = new Object();

    private static long PLAYBACK_RATE = 250;

    private final Sequence mSequence;

    private final OSVActivity activity;

    private ArrayList<ImageFile> mImages = new ArrayList<>();

    private ArrayList<Location> mTrack = new ArrayList<>();

    private Glide mGlide;

    private FullscreenPagerAdapter mPagerAdapter;

    private ScrollDisabledViewPager mPager;

    private SeekBar mSeekbar;

    private Handler mPlayHandler = new Handler(Looper.getMainLooper());

    private boolean mPlaying = false;

    private int mModifier = 1;

    private boolean mImageLoaded = true;

    private ArrayList<PlaybackListener> mPlaybackListeners = new ArrayList<>();

    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            if (mPager != null) {
                if (!mImageLoaded) {
                    mPlayHandler.postDelayed(mRunnable, 20);
                    return;
                }
                int requestedFrameIndex = getFrameIndex();
                mPager.setCurrentItem(getFrameIndex());
                mPager.setCurrentItem(requestedFrameIndex + mModifier, false);
                if (requestedFrameIndex + mModifier >= 0 || requestedFrameIndex + mModifier <= mImages.size()) {
                    mPlayHandler.postDelayed(mRunnable, PLAYBACK_RATE);
                } else {
                    pause();
                }
            }
        }
    };

    private RequestListener<Drawable> mGlideListener = new RequestListener<Drawable>() {

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            mImageLoaded = true;
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            mImageLoaded = true;
            return false;
        }
    };

    private ViewPager.OnPageChangeListener mPageChangedListener;

    private boolean mPrepared;

    private ApplicationPreferences applicationPreferences;

    public OnlinePlaybackManager(OSVActivity act, Sequence sequence, ApplicationPreferences applicationPreferences) {
        this.applicationPreferences = applicationPreferences;
        activity = act;
        mSequence = sequence;
        mGlide = Glide.get(activity);
        mPagerAdapter = new FullscreenPagerAdapter(activity);
    }

    @Override
    public View getSurface() {
        return mPager;
    }

    @Override
    public void setSurface(View surface) {
        Log.d(TAG, "setSurface: " + surface);
        if (mPager != null) {
            mPager.removeOnPageChangeListener(mPageChangedListener);
            mPager.setAdapter(null);
        }
        mPager = (ScrollDisabledViewPager) surface;
        mPager.setOffscreenPageLimit(OFFSCREEN_LIMIT / 2);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(getFrameIndex());
        mPageChangedListener = new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (mSeekbar != null) {
                    mSeekbar.setProgress(position);
                }
                setFrameIndex(position);
                View view = mPager.getChildAt(0);
                if (view != null) {
                    View image = view.findViewById(R.id.image);
                    if (image instanceof PhotoView) {
                        Matrix matrix = ((PhotoView) image).getDisplayMatrix();
                        matrix.setScale(1, 1);
                        ((PhotoView) image).setDisplayMatrix(matrix);
                    }
                }
                view = mPager.getChildAt(1);
                if (view != null) {
                    View image = view.findViewById(R.id.image);
                    if (image instanceof PhotoView) {
                        Matrix matrix = ((PhotoView) image).getDisplayMatrix();
                        matrix.setScale(1, 1);
                        ((PhotoView) image).setDisplayMatrix(matrix);
                    }
                }
                view = mPager.getChildAt(2);
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
        };
        mPager.addOnPageChangeListener(mPageChangedListener);
        if (mPager != null) {
            mPager.setCurrentItem(getFrameIndex());
        }
    }

    @Override
    public void prepare() {
        if (!mPrepared) {
            loadFrames();
        }
    }

    @Override
    public void next() {
        pause();
        if (mPager != null) {
            mPager.setCurrentItem(getFrameIndex() + 1);
        }
    }

    @Override
    public void previous() {
        pause();
        if (mPager != null) {
            mPager.setCurrentItem(getFrameIndex() - 1);
        }
    }

    @Override
    public void play() {
        mModifier = 1;
        PLAYBACK_RATE = 250;
        playImpl();
    }

    @Override
    public void pause() {
        if (mPlaying) {
            mPlaying = false;
            mPlayHandler.removeCallbacks(mRunnable);
            Iterator<PlaybackListener> iterator = mPlaybackListeners.iterator();
            //using iterator due to a crash on popBackStackImmediate
            while (iterator.hasNext()) {
                iterator.next().onPaused();
            }
        }
    }

    @Override
    public void stop() {
        Log.d(TAG, "stop");
        mPlaying = false;
        mPlayHandler.removeCallbacks(mRunnable);

        Iterator<PlaybackListener> iterator = mPlaybackListeners.iterator();
        //using iterator due to a crash on popBackStackImmediate
        while (iterator.hasNext()) {
            iterator.next().onStopped();
        }
    }

    @Override
    public void fastForward() {
        mModifier = 1;
        PLAYBACK_RATE = 125;
        playImpl();
    }

    @Override
    public void fastBackward() {
        mModifier = -1;
        PLAYBACK_RATE = 125;
        playImpl();
    }

    @Override
    public boolean isPlaying() {
        return mPlaying;
    }

    @Override
    public void setSeekBar(SeekBar seekBar) {
        mSeekbar = seekBar;
        if (mSeekbar != null) {
            mSeekbar.setProgress(0);
            mSeekbar.setOnSeekBarChangeListener(this);
        }
        loadFrames();
    }

    @Override
    public int getLength() {
        if (mImages != null) {
            return mImages.size();
        }
        return 0;
    }

    @Override
    public void destroy() {
        synchronized (object) {
            Log.d(TAG, "destroy");
            stop();
            Iterator<PlaybackListener> iterator = mPlaybackListeners.iterator();
            //using iterator due to a crash on popBackStackImmediate
            while (iterator.hasNext()) {
                iterator.next().onExit();
                iterator.remove();
            }
            mGlide.clearMemory();
        }
    }

    @Override
    public void addPlaybackListener(PlaybackListener playbackListener) {
        if (!mPlaybackListeners.contains(playbackListener)) {
            mPlaybackListeners.add(playbackListener);
        }
    }

    @Override
    public void removePlaybackListener(PlaybackListener playbackListener) {
        synchronized (object) {
            mPlaybackListeners.remove(playbackListener);
        }
    }

    @Override
    public boolean isSafe() {
        return true;
    }

    @Override
    public Sequence getSequence() {
        return mSequence;
    }

    public ArrayList<Location> getTrack() {
        return mTrack;
    }

    @Override
    public void onSizeChanged() {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            mPager.setCurrentItem(progress, false);
        }

        Iterator<PlaybackListener> iterator = mPlaybackListeners.iterator();
        //using iterator due to a crash on popBackStackImmediate
        while (iterator.hasNext()) {
            iterator.next().onProgressChanged(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    /**
     * @return the index of the frame in case the compression is {@code SequenceDetailsCompressionJpeg} otherwise it will return default value which is 0.
     */
    private int getFrameIndex() {
        SequenceDetailsCompressionBase compressionBase = mSequence.getCompressionDetails();
        return compressionBase instanceof SequenceDetailsCompressionJpeg ? ((SequenceDetailsCompressionJpeg) compressionBase).getFrameIndex() : 0;
    }

    /**
     * Sets the frame index for the current sequence in case the compression is {@code SequenceDetailsCompressionJpeg}.
     */
    private void setFrameIndex(int frameIndex) {
        SequenceDetailsCompressionBase compressionBase = mSequence.getCompressionDetails();
        if (compressionBase instanceof SequenceDetailsCompressionJpeg) {
            ((SequenceDetailsCompressionJpeg) compressionBase).setFrameIndex(frameIndex);
        }
    }

    private void loadFrames() {
        activity.enableProgressBar(true);
        activity.getUserDataManager().listImages(mSequence.getDetails().getOnlineId(), new NetworkResponseDataListener<PhotoCollection>() {

            @Override
            public void requestFailed(int status, PhotoCollection details) {
                Log.w(TAG, "displaySequence: failed, " + details);
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        activity.enableProgressBar(false);
                    }
                });
            }

            @Override
            public void requestFinished(int status, final PhotoCollection collectionData) {
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mPrepared = true;
                        mImages.clear();
                        mImages.addAll(collectionData.getNodes());
                        mTrack = new ArrayList<>(Utils.toLocationFromKVLatLng(collectionData.getTrack()));
                        if (mSeekbar != null) {
                            mSeekbar.setMax(mImages.size());
                            mSeekbar.setProgress(0);
                        }
                        mPagerAdapter.notifyDataSetChanged();
                        for (PlaybackListener pl : mPlaybackListeners) {
                            pl.onPrepared();
                        }
                        if (mPager != null) {
                            mPager.setCurrentItem(getFrameIndex());
                        }
                        activity.enableProgressBar(false);
                    }
                });
            }
        });
    }

    private void playImpl() {
        try {
            if (mPlayHandler != null && !isPlaying()) {
                mPlaying = true;
                mPlayHandler.post(mRunnable);
                for (PlaybackListener pl : mPlaybackListeners) {
                    pl.onPlaying();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "onConnected: " + Log.getStackTraceString(e));
        }
    }

    private class FullscreenPagerAdapter extends PagerAdapter {

        private final LayoutInflater inflater;

        FullscreenPagerAdapter(final OSVActivity activity) {
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mImages.size();
        }

        @SuppressWarnings("deprecation")
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view;
            view = inflater.inflate(R.layout.item_image_view_pager, container, false);
            if (view != null) {
                //                Glide.clear(view);
                if (position % OFFSCREEN_LIMIT == 0) {
                    mGlide.trimMemory(OFFSCREEN_LIMIT);
                }
                try {
                    RequestBuilder<Drawable> builder =
                            Glide.with(activity).load(NetworkUtils.provideGlideUrlWithAuthorizationIfRequired(applicationPreferences, (PLAYBACK_RATE < 200 && isPlaying()) ?
                                    mImages.get(position).thumb : mImages.get(position).link))
                                    .fitCenter().diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false)
                                    .priority(Priority.NORMAL).error(R.drawable.vector_picture_placeholder).listener(mGlideListener);
                    if (PLAYBACK_RATE > 200 || !isPlaying()) {
                        if (!mImages.get(position).thumb.equals("") && mImages.get(position).file == null) {
                            builder.thumbnail(Glide.with(activity).load(NetworkUtils.provideGlideUrlWithAuthorizationIfRequired(applicationPreferences,
                                    mImages.get(position).thumb)).fitCenter()
                                    .diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false)
                                    .priority(Priority.IMMEDIATE).error(R.drawable.vector_picture_placeholder).listener(mGlideListener));
                        } else if (mImages.get(position).file != null) {
                            builder.thumbnail(0.2f).listener(mGlideListener);
                        }
                    }
                    mImageLoaded = false;
                    builder.into((ImageView) view);
                } catch (Exception e) {
                    Log.w(TAG, "instantiateItem: " + Log.getStackTraceString(e));
                }
            }
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (object != null) {
                Glide.with(activity).clear(((View) object));
            }
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }
    }
}
