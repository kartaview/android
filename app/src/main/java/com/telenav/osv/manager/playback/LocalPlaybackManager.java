package com.telenav.osv.manager.playback;

import android.content.Context;
import android.database.Cursor;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.SeekBar;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.ffmpeg.FFMPEGTrackPlayer;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.VideoFile;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import com.telenav.streetview.scalablevideoview.ScalableVideoView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Kalman on 30/06/16.
 */

public class LocalPlaybackManager
    implements PlaybackManager, FFMPEGTrackPlayer.OnSeekCompleteListener, FFMPEGTrackPlayer.OnBufferingUpdateListener,
    FFMPEGTrackPlayer.OnCompletionListener, FFMPEGTrackPlayer.OnErrorListener, FFMPEGTrackPlayer.OnInfoListener,
    FFMPEGTrackPlayer.OnPreparedListener, SeekBar.OnSeekBarChangeListener, FFMPEGTrackPlayer.OnVideoSizeChangedListener,
    FFMPEGTrackPlayer.OnPlaybackListener {

  private static final String TAG = "LocalPlaybackManager";

  private final ThreadPoolExecutor mThreadPoolExec;

  private Sequence mSequence;

  private SeekBar mSeekBar;

  private ScalableVideoView mVideoView;

  private ArrayList<SKCoordinate> mTrack = new ArrayList<>();

  private FFMPEGTrackPlayer mCurrentPlayer = null;

  private boolean mPrepared = false;

  private boolean mTouching = false;

  private ArrayList<PlaybackListener> mPlaybackListeners = new ArrayList<>();

  private ArrayList<VideoFile> videoFiles = new ArrayList<>();

  private boolean mSeekComplete = true;

  private SequenceDB mSequenceDB;

  public LocalPlaybackManager(Context context, SequenceDB db) {
    mSequenceDB = db;
    mCurrentPlayer = new FFMPEGTrackPlayer();
    mCurrentPlayer.setOnSeekCompleteListener(LocalPlaybackManager.this);
    mCurrentPlayer.setOnVideoSizeChangedListener(LocalPlaybackManager.this);
    mCurrentPlayer.setOnBufferingUpdateListener(LocalPlaybackManager.this);
    mCurrentPlayer.setOnCompletionListener(LocalPlaybackManager.this);
    mCurrentPlayer.setOnErrorListener(LocalPlaybackManager.this);
    mCurrentPlayer.setOnInfoListener(LocalPlaybackManager.this);
    mCurrentPlayer.setOnPlaybackListener(LocalPlaybackManager.this);
    mCurrentPlayer.setOnPreparedListener(LocalPlaybackManager.this);
    mCurrentPlayer.setLooping(true);
    if (!Utils.isDebuggableFlag(context)) {
      mCurrentPlayer.initSignalHandler();
    }

    BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
    mThreadPoolExec = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, workQueue,
                                             new ThreadFactoryBuilder().setDaemon(false).setNameFormat("PlaybackThreadPool")
                                                 .setPriority(Thread.NORM_PRIORITY).build());
  }

  @Override
  public void setSource(Sequence sequence) {
    mSequence = sequence;
    mThreadPoolExec.execute(() -> {
      videoFiles = new ArrayList<>();
      for (PlaybackListener pl : mPlaybackListeners) {
        pl.onPreparing();
      }
      try {
        Cursor records = mSequenceDB.getVideos(mSequence.getId());
        if (records != null && records.getCount() > 0) {
          while (!records.isAfterLast()) {
            try {
              String path = records.getString(records.getColumnIndex(SequenceDB.VIDEO_FILE_PATH));
              int index = records.getInt(records.getColumnIndex(SequenceDB.VIDEO_INDEX));
              int count = records.getInt(records.getColumnIndex(SequenceDB.VIDEO_FRAME_COUNT));
              OSVFile video = new OSVFile(path);
              videoFiles.add(new VideoFile(video, index, count));
            } catch (Exception e) {
              Log.w(TAG, "LocalPlaybackManager: " + Log.getStackTraceString(e));
            }
            records.moveToNext();
          }
          Collections.sort(videoFiles, (lhs, rhs) -> lhs.fileIndex - rhs.fileIndex);
          String[] paths = new String[videoFiles.size()];
          for (int i = 0; i < videoFiles.size(); i++) {
            try {
              paths[i] = videoFiles.get(i).link;
            } catch (Exception e) {
              Log.w(TAG, "LocalPlaybackManager: " + Log.getStackTraceString(e));
            }
          }
          mCurrentPlayer.setDataSource(paths);
          mPrepared = true;
          for (PlaybackListener pl : mPlaybackListeners) {
            pl.onPrepared(true);
          }
          if (mVideoView != null) {
            play();
          }
        } else {
          Log.w(TAG, "displaySequence: cursor has 0 elements");
        }
        if (records != null) {
          records.close();
        }
        mTrack = new ArrayList<>(mSequence.getPolyline().getNodes());
      } catch (Exception e) {

        Log.w(TAG, "displaySequence: " + e.getLocalizedMessage());
      }
    });
  }

  @Override
  public View getSurface() {
    return mVideoView;
  }

  @Override
  public void setSurface(final View surface) {
    mVideoView = (ScalableVideoView) surface;
    mVideoView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

      @Override
      public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
        mThreadPoolExec.execute(() -> {
          if (mCurrentPlayer != null) {
            mCurrentPlayer.setSurface(new Surface(surface));
            play();
          }
        });
      }

      @Override
      public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

      }

      @Override
      public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
      }

      @Override
      public void onSurfaceTextureUpdated(SurfaceTexture surface) {

      }
    });
  }

  @Override
  public void prepare() {

  }

  @Override
  public void next() {
    if (mCurrentPlayer != null) {
      mThreadPoolExec.execute(() -> mCurrentPlayer.stepFrame(true));
    }
  }

  @Override
  public void previous() {
    if (mCurrentPlayer != null) {
      mThreadPoolExec.execute(() -> mCurrentPlayer.stepFrame(false));
    }
  }

  @Override
  public void play() {
    if (mCurrentPlayer != null && mPrepared) {
      if (!mCurrentPlayer.isPlaying()) {
        mThreadPoolExec.execute(() -> {
          mCurrentPlayer.setFPSDelay(false);
          mCurrentPlayer.start();
        });
      } else {
        mThreadPoolExec.execute(() -> mCurrentPlayer.setFPSDelay(false));
      }
    }
  }

  @Override
  public void pause() {
    if (mCurrentPlayer != null) {
      mThreadPoolExec.execute(() -> mCurrentPlayer.pause());
    }
  }

  @Override
  public void stop() {
    try {
      if (mCurrentPlayer != null) {
        mThreadPoolExec.execute(() -> mCurrentPlayer.stop());
      }
    } catch (Exception ignored) {
      Log.d(TAG, Log.getStackTraceString(ignored));
    }
  }

  @Override
  public void fastForward() {
    if (mCurrentPlayer != null) {
      mThreadPoolExec.execute(() -> {
        mCurrentPlayer.setFPSDelay(true);
        mCurrentPlayer.setBackwards(false);
        mCurrentPlayer.start();
      });
    }
  }

  @Override
  public void fastBackward() {
    if (mCurrentPlayer != null) {
      mThreadPoolExec.execute(() -> {
        mCurrentPlayer.setFPSDelay(true);
        mCurrentPlayer.setBackwards(true);
        mCurrentPlayer.start();
      });
    }
  }

  @Override
  public boolean isPlaying() {
    return (mCurrentPlayer != null && mCurrentPlayer.isPlaying());
  }

  @Override
  public void setSeekBar(final SeekBar seekBar) {
    mThreadPoolExec.execute(() -> {
      mSeekBar = seekBar;
      mSeekBar.setOnSeekBarChangeListener(LocalPlaybackManager.this);
      mSeekBar.setMax(mTrack.size());
      for (PlaybackListener pl : mPlaybackListeners) {
        pl.onPrepared(true);
      }
    });
  }

  @Override
  public int getLength() {
    return mTrack.size();
  }

  @Override
  public void destroy() {
    mThreadPoolExec.execute(() -> {
      if (mCurrentPlayer != null) {
        try {
          if (mCurrentPlayer.isPlaying()) {
            mCurrentPlayer.stop();
          }
        } catch (Exception e) {
          Log.d(TAG, "destroy: " + Log.getStackTraceString(e));
        }
        try {
          mCurrentPlayer.release();
        } catch (Exception e) {
          Log.d(TAG, "destroy: " + Log.getStackTraceString(e));
        }
      }
      for (PlaybackListener pl : mPlaybackListeners) {
        pl.onExit();
      }
      mPlaybackListeners.clear();
    });
    mThreadPoolExec.shutdown();
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
    return false;
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
    mVideoView.onSizeChanged();
  }

  private void seekTo(final int milliseconds) {
    mThreadPoolExec.execute(() -> {
      if (!mCurrentPlayer.isPlaying()) {
        mCurrentPlayer.start();
      }
      mCurrentPlayer.seekTo(milliseconds);
    });
  }

  public int getCurrentPosition() {
    if (mCurrentPlayer != null) {
      return mCurrentPlayer.getCurrentPosition();
    }
    return 0;
  }

  @Override
  public void onSeekComplete(final FFMPEGTrackPlayer mp) {
    mSeekComplete = true;
  }

  @Override
  public void onVideoSizeChanged(FFMPEGTrackPlayer mp, final int width, final int height) {
    if (mVideoView != null) {
      mVideoView.post(() -> mVideoView.onVideoSizeChanged(width, height));
    }
  }

  @Override
  public void onBufferingUpdate(FFMPEGTrackPlayer mp, int percent) {
    Log.d(TAG, "onBufferingUpdate: " + percent);
  }

  @Override
  public void onCompletion(final FFMPEGTrackPlayer mp) {
    for (PlaybackListener pl : mPlaybackListeners) {
      pl.onStopped();
    }
  }

  @Override
  public boolean onError(FFMPEGTrackPlayer mp, int what, int extra) {
    Log.e(TAG, "onError: " + what + " - " + extra);
    return false;
  }

  @Override
  public boolean onInfo(FFMPEGTrackPlayer mp, int what, int extra) {
    Log.d(TAG, "onInfo: " + what + " - " + extra);
    return false;
  }

  @Override
  public void onPrepared(FFMPEGTrackPlayer mp) {
    mCurrentPlayer = mp;
    mPrepared = true;
    for (PlaybackListener pl : mPlaybackListeners) {
      pl.onPrepared(true);
    }
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    for (PlaybackListener pl : mPlaybackListeners) {
      pl.onProgressChanged(progress);
    }
    if (!mPrepared || !fromUser) {
      return;
    }
    Log.d(TAG, "onStopTrackingTouch: ");
    if (!mPrepared || !mTouching) {
      return;
    }
    Log.d(TAG, "onProgressChanged: " + progress);
    mSeekComplete = false;
    seekTo(progress);
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    if (!mPrepared) {
      return;
    }
    mTouching = true;
    Log.d(TAG, "onStartTrackingTouch: ");
    if (mCurrentPlayer != null) {
      mCurrentPlayer.seeking(true);
    }
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    if (!mPrepared) {
      return;
    }
    mTouching = false;

    if (mCurrentPlayer != null) {
      mCurrentPlayer.seeking(false);
    }
  }

  @Override
  public void onPlay(FFMPEGTrackPlayer player) {
    for (PlaybackListener pl : mPlaybackListeners) {
      pl.onPlaying();
    }
  }

  @Override
  public void onPause(FFMPEGTrackPlayer player) {
    for (PlaybackListener pl : mPlaybackListeners) {
      pl.onPaused();
    }
  }

  @Override
  public void onFrameChanged(FFMPEGTrackPlayer mp, int fileIndex, int globalIndex) {
    if (mSeekBar != null && mSeekComplete) {
      mSeekBar.setProgress(globalIndex);
    }
  }

  @Override
  public void onNextFile(FFMPEGTrackPlayer mp, int fileIndex) {

  }

  @Override
  public void onPreviousFile(FFMPEGTrackPlayer mp, int fileIndex) {

  }
}
