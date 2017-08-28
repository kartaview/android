package com.telenav.osv.manager.playback;

import android.content.Context;
import android.database.Cursor;
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
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.ui.custom.ScrollDisabledViewPager;
import com.telenav.osv.utils.Log;
import java.util.ArrayList;
import java.util.Collections;
import uk.co.senab.photoview.PhotoView;

/**
 * component responsible of local jpeg playback
 * Created by Kalman on 27/07/16.
 */
public class SafePlaybackManager extends PlaybackManager implements SeekBar.OnSeekBarChangeListener {

  private static final String TAG = "SafePlaybackManager";

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
    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache,
                                   boolean isFirstResource) {
      mImageLoaded = true;
      return false;
    }
  };

  private boolean mPrepared;

  public SafePlaybackManager(OSVActivity act, Sequence sequence) {
    activity = act;
    mSequence = sequence;
    mGlide = Glide.get(activity);
    mPagerAdapter = new FullscreenPagerAdapter(activity);
  }

  private void loadFrames() {
    activity.enableProgressBar(true);
    final ArrayList<ImageFile> nodes = new ArrayList<>();
    final ArrayList<ImageCoordinate> track = new ArrayList<>();
    final int finalSeqId = mSequence.getId();
    Cursor cursor = SequenceDB.instance.getFrames(finalSeqId);
    while (cursor != null && !cursor.isAfterLast()) {
      int index = cursor.getInt(cursor.getColumnIndex(SequenceDB.FRAME_SEQ_INDEX));
      String path = cursor.getString(cursor.getColumnIndex(SequenceDB.FRAME_FILE_PATH));
      double lat = cursor.getDouble(cursor.getColumnIndex(SequenceDB.FRAME_LAT));
      double lon = cursor.getDouble(cursor.getColumnIndex(SequenceDB.FRAME_LON));
      if (lat != 0.0 && lon != 0.0) {
        ImageCoordinate coord = new ImageCoordinate(lat, lon, index);
        nodes.add(new ImageFile(new OSVFile(path), index, coord));
        track.add(coord);
      }
      cursor.moveToNext();
    }
    if (cursor != null) {
      cursor.close();
    }
    Collections.sort(nodes, (lhs, rhs) -> lhs.index - rhs.index);
    Collections.sort(track, (lhs, rhs) -> lhs.index - rhs.index);
    activity.enableProgressBar(false);
    Log.d(TAG, "listImages: id=" + finalSeqId + " length=" + nodes.size());
    activity.runOnUiThread(() -> {
      mPrepared = true;
      mImages = nodes;
      mTrack = new ArrayList<>(track);
      mSeekbar.setMax(mImages.size());
      mSeekbar.setProgress(0);
      mPagerAdapter.notifyDataSetChanged();
      for (PlaybackListener pl : mPlaybackListeners) {
        pl.onPrepared();
      }
      if (mSequence.getRequestedFrameIndex() != 0) {
        if (mPager != null) {
          mPager.setCurrentItem(mSequence.getRequestedFrameIndex());
        }
      }
      //                                play();
    });
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
      public void onPageScrollStateChanged(int state) {

      }
    });
    if (mSequence.getRequestedFrameIndex() != 0) {
      if (mPager != null) {
        mPager.setCurrentItem(mSequence.getRequestedFrameIndex());
      }
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
              Glide.with(activity).load(mImages.get(position).link).fitCenter().diskCacheStrategy(DiskCacheStrategy.ALL)
                  .animate(new AlphaAnimation(1f, 1f)).skipMemoryCache(false).signature(new StringSignature(
                  mImages.get(position).coords.getLatitude() + "," + mImages.get(position).coords.getLongitude() + " full"))
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
