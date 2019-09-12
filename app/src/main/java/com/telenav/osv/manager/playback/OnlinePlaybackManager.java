package com.telenav.osv.manager.playback;

import java.util.ArrayList;
import android.content.Context;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.StringSignature;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.data.sequence.model.Sequence;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.item.network.PhotoCollection;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import com.telenav.osv.utils.Log;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import uk.co.senab.photoview.PhotoView;

/**
 * component responsible for playing back online content
 * Created by Kalman on 27/07/16.
 */
public class OnlinePlaybackManager extends PlaybackManager implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "OnlinePlaybackManager";

    private static final int OFFSCREEN_LIMIT = 20;

    private static long PLAYBACK_RATE = 250;

    private final Sequence mSequence;

    private final OSVActivity activity;

    private ArrayList<ImageFile> mImages = new ArrayList<>();

    private ArrayList<SKCoordinate> mTrack = new ArrayList<>();

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

    private RequestListener<? super String, GlideDrawable> mGlideListener = new RequestListener<String, GlideDrawable>() {

        @Override
        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
            mImageLoaded = true;
            return false;
        }

        @Override
        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache,
                                       boolean isFirstResource) {
            mImageLoaded = true;
            return false;
        }
    };

    private ViewPager.OnPageChangeListener mPageChangedListener;

    private boolean mPrepared;

    public OnlinePlaybackManager(OSVActivity act, Sequence sequence) {
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
            for (PlaybackListener pl : mPlaybackListeners) {
                pl.onPaused();
            }
        }
    }

    @Override
    public void stop() {
        mPlaying = false;
        mPlayHandler.removeCallbacks(mRunnable);

        for (PlaybackListener pl : mPlaybackListeners) {
            pl.onStopped();
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
        stop();
        for (PlaybackListener pl : mPlaybackListeners) {
            pl.onExit();
        }
        mPlaybackListeners.clear();
        mGlide.clearMemory();
    }

    @Override
    public void addPlaybackListener(PlaybackListener playbackListener) {
        if (!mPlaybackListeners.contains(playbackListener)) {
            mPlaybackListeners.add(playbackListener);
        }
    }

    @Override
    public void removePlaybackListener(PlaybackListener playbackListener) {
        mPlaybackListeners.remove(playbackListener);
    }

    @Override
    public boolean isSafe() {
        return true;
    }

    @Override
    public Sequence getSequence() {
        return mSequence;
    }

    public ArrayList<SKCoordinate> getTrack() {
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

        for (PlaybackListener pl : mPlaybackListeners) {
            pl.onProgressChanged(progress);
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
                        mTrack = new ArrayList<>(collectionData.getTrack());
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
                    DrawableRequestBuilder<String> builder =
                            Glide.with(activity).load((PLAYBACK_RATE < 200 && isPlaying()) ? mImages.get(position).thumb : mImages.get(position).link)
                                    .fitCenter().diskCacheStrategy(DiskCacheStrategy.ALL).animate(new AlphaAnimation(1f, 1f)).skipMemoryCache(false)
                                    .signature(new StringSignature(
                                            mImages.get(position).coords.getLatitude() + "," + mImages.get(position).coords.getLongitude() + " full"))
                                    .priority(Priority.NORMAL).error(R.drawable.vector_picture_placeholder).listener(mGlideListener);
                    if (PLAYBACK_RATE > 200 || !isPlaying()) {
                        if (!mImages.get(position).thumb.equals("") && mImages.get(position).file == null) {
                            builder.thumbnail(Glide.with(activity).load(mImages.get(position).thumb).fitCenter().animate(new AlphaAnimation(1f, 1f))
                                    .diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false).signature(
                                            new StringSignature(mImages.get(position).coords.getLatitude() + "," + mImages.get(position).coords.getLongitude()))
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
                Glide.clear(((View) object));
            }
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }
    }
}
