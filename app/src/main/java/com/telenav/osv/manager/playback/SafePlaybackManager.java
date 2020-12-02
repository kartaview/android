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
import com.telenav.osv.common.model.KVLatLng;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.frame.model.Frame;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.Sequence;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.item.KVFile;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import uk.co.senab.photoview.PhotoView;

/**
 * component responsible of local jpeg playback
 * Created by Kalman on 27/07/16.
 */
public class SafePlaybackManager extends PlaybackManager implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "SafePlaybackManager";

    private static final int OFFSCREEN_LIMIT = 20;

    private static long PLAYBACK_RATE = 250;

    private final LocalSequence mSequence;

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

    private Disposable framesDisposable;

    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            if (mPager != null) {
                if (!mImageLoaded) {
                    mPlayHandler.postDelayed(mRunnable, 20);
                    return;
                }
                mPager.setCurrentItem(mPager.getCurrentItem() + mModifier, false);
                int current = mPager.getCurrentItem();
                if (current + mModifier >= 0 || current + mModifier <= mImages.size()) {
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

    private boolean mPrepared;

    private ApplicationPreferences applicationPreferences;

    /**
     * The data source for frame on the device persistence.
     */
    private FrameLocalDataSource frameLocalDataSource;

    public SafePlaybackManager(OSVActivity act, LocalSequence sequence, FrameLocalDataSource frameLocalDataSource, ApplicationPreferences applicationPreferences) {
        activity = act;
        mSequence = sequence;
        mGlide = Glide.get(activity);
        this.applicationPreferences = applicationPreferences;
        this.frameLocalDataSource = frameLocalDataSource;
        mPagerAdapter = new FullscreenPagerAdapter(activity);
    }

    @Override
    public View getSurface() {
        return mPager;
    }

    @Override
    public void setSurface(View surface) {
        mPager = (ScrollDisabledViewPager) surface;
        mPager.setOffscreenPageLimit(OFFSCREEN_LIMIT / 2);
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mSeekbar.setProgress(position);
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
            public void onPageScrollStateChanged(int state) {}
        });
        setFrameIndex();
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
            mPager.setCurrentItem(mPager.getCurrentItem() + 1);
        }
    }

    @Override
    public void previous() {
        pause();
        if (mPager != null) {
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
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
        if (framesDisposable != null) {
            framesDisposable.dispose();
        }
        mPlaying = false;
        mPlayHandler.removeCallbacks(mRunnable);
        if (mPager != null) {
            mPager.setCurrentItem(0, false);
        }

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
        mSeekbar.setProgress(0);
        mSeekbar.setOnSeekBarChangeListener(this);
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
     * Sets the frame index for the pager.
     */
    private void setFrameIndex() {
        if (mSequence.getCompressionDetails() instanceof SequenceDetailsCompressionJpeg) {
            if (mPager != null) {
                mPager.setCurrentItem(((SequenceDetailsCompressionJpeg) mSequence.getCompressionDetails()).getFrameIndex());
            }
        }
    }

    private void loadFrames() {
        activity.enableProgressBar(true);
        final ArrayList<ImageFile> nodes = new ArrayList<>();
        final ArrayList<KVLatLng> track = new ArrayList<>();
        final String sequenceId = mSequence.getID();
        framesDisposable = frameLocalDataSource
                .getFramesWithLocations(sequenceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        //onSuccess
                        frames -> {
                            Log.d(TAG, String.format("loadFrames. Status: successful. Sequence id: %s. Message: Frames loaded successful for the sequence.", sequenceId));
                            for (Frame frame : frames) {
                                Location frameLocation = frame.getLocation();
                                if (frameLocation != null && frameLocation.getLatitude() != 0.0 && frameLocation.getLongitude() != 0.0) {
                                    int frameSeqIndex = frame.getIndex();
                                    KVLatLng coord = new KVLatLng(frameLocation.getLatitude(), frameLocation.getLongitude(), frameSeqIndex);
                                    nodes.add(new ImageFile(new KVFile(frame.getFilePath()), frameSeqIndex, coord, false));
                                    track.add(coord);
                                }
                            }
                            Collections.sort(nodes, (lhs, rhs) -> lhs.index - rhs.index);
                            Collections.sort(track, (lhs, rhs) -> lhs.getIndex() - rhs.getIndex());
                            activity.enableProgressBar(false);
                            Log.d(TAG, "listImages: id=" + sequenceId + " length=" + nodes.size());
                            mPrepared = true;
                            mImages = nodes;
                            mTrack = new ArrayList<>(Utils.toLocationFromKVLatLng(track));
                            mSeekbar.setMax(mImages.size());
                            mSeekbar.setProgress(0);
                            mPagerAdapter.notifyDataSetChanged();
                            for (PlaybackListener pl : mPlaybackListeners) {
                                pl.onPrepared();
                            }
                            setFrameIndex();
                            play();
                        },
                        //onError
                        throwable -> Log.d(TAG, String.format("loadFrames. Status: error. Sequence id: %s. Message: %s.", sequenceId, throwable.getLocalizedMessage())),
                        //OnComplete
                        () -> Log.d(TAG, String.format("loadFrames. Status: complete. Sequence id: %s. Message: Frames not found.", sequenceId)));
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
                            Glide.with(activity).load(mImages.get(position).link).fitCenter().diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .priority(Priority.NORMAL).error(R.drawable.vector_picture_placeholder).listener(mGlideListener);
                    if (PLAYBACK_RATE > 200 || !isPlaying()) {
                        builder.thumbnail(0.2f).listener(mGlideListener);
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
            if (object != null && !activity.isFinishing()) {
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
