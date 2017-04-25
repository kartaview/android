package com.telenav.osv.manager.playback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
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
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.http.RequestResponseListener;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import com.telenav.osv.utils.Log;
import uk.co.senab.photoview.PhotoView;

/**
 * Created by Kalman on 27/07/16.
 */

public class OnlinePlaybackManager extends PlaybackManager implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "OnlinePlaybackManager";

    private static final int OFFSCREEN_LIMIT = 20;

    private static long PLAYBACK_RATE = 250;

    private final Sequence mSequence;

    private final OSVActivity activity;

    public ArrayList<ImageFile> mImages = new ArrayList<>();

    public ArrayList<SKCoordinate> mTrack = new ArrayList<>();

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

    private RequestListener<? super String, GlideDrawable> mGlideListener = new RequestListener<String, GlideDrawable>() {
        @Override
        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
            mImageLoaded = true;
            return false;
        }

        @Override
        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
            mImageLoaded = true;
            return false;
        }
    };

    public OnlinePlaybackManager(OSVActivity act, Sequence sequence) {
        activity = act;
        mSequence = sequence;
        mGlide = Glide.get(activity);
        mPagerAdapter = new FullscreenPagerAdapter(activity);
    }

    private void loadFrames() {
        activity.enableProgressBar(true);
        final ArrayList<ImageFile> nodes = new ArrayList<>();
        final ArrayList<ImageCoordinate> track = new ArrayList<>();
        UploadManager uploadManager = ((OSVApplication) activity.getApplication()).getUploadManager();
        final int finalSeqId = mSequence.sequenceId;
        uploadManager.listImages(mSequence.sequenceId, new RequestResponseListener() {
            @Override
            public void requestFinished(final int status, final String result) {
                if (status == RequestResponseListener.STATUS_FAILED) {
                    Log.w(TAG, "displaySequence: failed, " + result);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.enableProgressBar(false);
                        }
                    });
                    return;
                }
                if (result != null && !result.isEmpty()) {
                    try {
                        JSONObject obj = new JSONObject(result);
                        JSONArray array = obj.getJSONObject("osv").getJSONArray("photos");
                        for (int i = 0; i < array.length(); i++) {
                            String link = UploadManager.URL_DOWNLOAD_PHOTO + array.getJSONObject(i).getString("lth_name");
                            String thumbLink = "";
                            try {
                                thumbLink = UploadManager.URL_DOWNLOAD_PHOTO + array.getJSONObject(i).getString("th_name");
                            } catch (Exception e) {
                                Log.w(TAG, "displaySequence: " + e.getLocalizedMessage());
                            }
                            int index = array.getJSONObject(i).getInt("sequence_index");
                            int id = array.getJSONObject(i).getInt("id");
                            double lat = array.getJSONObject(i).getDouble("lat");
                            double lon = array.getJSONObject(i).getDouble("lng");
                            if (lat != 0.0 && lon != 0.0) {
                                ImageCoordinate coord = new ImageCoordinate(lat, lon, index);
                                nodes.add(new ImageFile(finalSeqId, link, thumbLink, id, index, coord, false));
                                track.add(coord);
                            }
                        }
                        Collections.sort(nodes, new Comparator<ImageFile>() {
                            @Override
                            public int compare(ImageFile lhs, ImageFile rhs) {
                                return lhs.index - rhs.index;
                            }
                        });
                        Collections.sort(track, new Comparator<ImageCoordinate>() {
                            @Override
                            public int compare(ImageCoordinate lhs, ImageCoordinate rhs) {
                                return lhs.index - rhs.index;
                            }
                        });
                        Log.d(TAG, "listImages: id=" + finalSeqId + " length=" + nodes.size());
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mImages = nodes;
                                mTrack = new ArrayList<SKCoordinate>(track);
                                mSeekbar.setMax(mImages.size());
                                mSeekbar.setProgress(0);
                                mPagerAdapter.notifyDataSetChanged();
                                for (PlaybackListener pl : mPlaybackListeners) {
                                    pl.onPrepared();
                                }
                                if (mSequence.skipToValue != 0) {
                                    if (mPager != null) {
                                        mPager.setCurrentItem(mSequence.skipToValue);
                                    }
                                }
//                                play();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                activity.enableProgressBar(false);
            }

            @Override
            public void requestFinished(final int status) {
                activity.enableProgressBar(false);
            }
        });
    }

    @Override
    public void setSurface(Object surface) {
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
            public void onPageScrollStateChanged(int state) {

            }
        });
        if (mSequence.skipToValue != 0) {
            if (mPager != null) {
                mPager.setCurrentItem(mSequence.skipToValue);
            }
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

    public void playImpl() {
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


    private class FullscreenPagerAdapter extends PagerAdapter {

        private final LayoutInflater inflater;


        public FullscreenPagerAdapter(final OSVActivity activity) {
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mImages.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }

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
                    DrawableRequestBuilder<String> builder = Glide.with(activity)
                            .load((PLAYBACK_RATE < 200 && isPlaying()) ? mImages.get(position).thumb : mImages.get(position).link)
                            .fitCenter()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .animate(new AlphaAnimation(1f, 1f))
                            .skipMemoryCache(false)
                            .signature(new StringSignature(mImages.get(position).coords.getLatitude() + "," + mImages.get(position).coords.getLongitude() + " full"))
                            .priority(Priority.NORMAL)
                            .error(R.drawable.custom_image_broken_background)
                            .listener(mGlideListener);
                    if (PLAYBACK_RATE > 200 || !isPlaying()) {
                        if (!mImages.get(position).thumb.equals("") && mImages.get(position).file == null) {
                            builder.thumbnail(Glide.with(activity)
                                    .load(mImages.get(position).thumb)
                                    .fitCenter()
                                    .animate(new AlphaAnimation(1f, 1f))
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .skipMemoryCache(false)
                                    .signature(new StringSignature(mImages.get(position).coords.getLatitude() + "," + mImages.get(position).coords.getLongitude()))
                                    .priority(Priority.IMMEDIATE)
                                    .error(R.drawable.custom_image_broken_background)
                                    .listener(mGlideListener));
                        } else if (mImages.get(position).file != null) {
                            builder.thumbnail(0.2f)
                                    .listener(mGlideListener);
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
    }
}
