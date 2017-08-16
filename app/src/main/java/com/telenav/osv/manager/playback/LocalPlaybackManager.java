package com.telenav.osv.manager.playback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import android.database.Cursor;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.SeekBar;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.ffmpeg.FFMPEGTrackPlayer;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.VideoFile;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import com.telenav.streetview.scalablevideoview.ScalableVideoView;

/**
 * Created by Kalman on 30/06/16.
 */

public class LocalPlaybackManager extends PlaybackManager implements FFMPEGTrackPlayer.OnSeekCompleteListener, FFMPEGTrackPlayer.OnBufferingUpdateListener,
        FFMPEGTrackPlayer.OnCompletionListener, FFMPEGTrackPlayer.OnErrorListener, FFMPEGTrackPlayer.OnInfoListener, FFMPEGTrackPlayer.OnPreparedListener,
        SeekBar.OnSeekBarChangeListener, FFMPEGTrackPlayer.OnVideoSizeChangedListener, FFMPEGTrackPlayer.OnPlaybackListener {
    private static final String TAG = "LocalPlaybackManager";

    private final OSVActivity activity;

    private final ThreadPoolExecutor mThreadPoolExec;

    private Sequence mSequence;

    private SeekBar mSeekBar;

    private ScalableVideoView mVideoView;

    private ArrayList<SKCoordinate> mTrack = new ArrayList<>();

    private FFMPEGTrackPlayer mCurrentPlayer = null;

    private int mTotalDuration;

    private boolean mPrepared = false;

    private boolean mTouching = false;

    private ArrayList<PlaybackListener> mPlaybackListeners = new ArrayList<>();

    private ArrayList<VideoFile> videoFiles = new ArrayList<>();

    private boolean mSeekComplete = true;

    public LocalPlaybackManager(OSVActivity context, Sequence sequence) {
        activity = context;
        this.mSequence = sequence;
        mTotalDuration = 0;
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
        mThreadPoolExec = new ThreadPoolExecutor(
                1,
                1,
                60,
                TimeUnit.SECONDS,
                workQueue,
                new ThreadFactoryBuilder().setDaemon(false).setNameFormat("PlaybackThreadPool").setPriority(Thread.NORM_PRIORITY).build());
        mThreadPoolExec.execute(new Runnable() {
            @Override
            public void run() {
                videoFiles = new ArrayList<>();
                activity.enableProgressBar(true);
                try {
                    Cursor records = SequenceDB.instance.getVideos(mSequence.getId());
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
                        Collections.sort(videoFiles, new Comparator<VideoFile>() {
                            @Override
                            public int compare(VideoFile lhs, VideoFile rhs) {
                                return lhs.fileIndex - rhs.fileIndex;
                            }
                        });
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
                        activity.enableProgressBar(false);
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
                activity.enableProgressBar(false);
            }
        });
    }

    @Override
    public void setSeekBar(final SeekBar seekBar) {
        mThreadPoolExec.execute(new Runnable() {
            @Override
            public void run() {
                mSeekBar = seekBar;
                mSeekBar.setOnSeekBarChangeListener(LocalPlaybackManager.this);
                mSeekBar.setMax(mTrack.size());
                for (PlaybackListener pl : mPlaybackListeners) {
                    pl.onPrepared();
                }
//                mSeekBar.setMax(mTotalDuration);
//                Log.d(TAG, "setSeekBar: total duration = " + mTotalDuration);
            }
        });
    }

    @Override
    public void setSurface(final View surface) {
        mVideoView = (ScalableVideoView) surface;
        mVideoView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
                mThreadPoolExec.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mCurrentPlayer != null) {
                            mCurrentPlayer.setSurface(new Surface(surface));
                            play();
                        }
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
    public View getSurface(){
        return mVideoView;
    }

    @Override
    public void prepare() {

    }

    @Override
    public int getLength() {
        return mTrack.size();
//        return mCurrentPlayer.getDuration();
    }

    private void seekTo(final int milliseconds) {
        mThreadPoolExec.execute(new Runnable() {
            @Override
            public void run() {
                if (!mCurrentPlayer.isPlaying()) {
                    mCurrentPlayer.start();
                }
                mCurrentPlayer.seekTo(milliseconds);
            }
        });
    }

    public int getCurrentPosition() {
        if (mCurrentPlayer != null) {
            return mCurrentPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void destroy() {
        mThreadPoolExec.execute(new Runnable() {
            @Override
            public void run() {
                if (mCurrentPlayer != null) {
                    try {
                        if (mCurrentPlayer.isPlaying()) {
                            mCurrentPlayer.stop();
                        }
                    } catch (Exception e) {

                    }
                    try {
                        mCurrentPlayer.release();
                    } catch (Exception e) {

                    }
                }
                for (PlaybackListener pl : mPlaybackListeners) {
                    pl.onExit();
                }
                mPlaybackListeners.clear();
            }
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

    @Override
    public void onSeekComplete(final FFMPEGTrackPlayer mp) {
        mSeekComplete = true;
//        mThreadPoolExec.post(new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG, "onSeekComplete: " + mp.getCurrentPosition());
//                if (mCurrentPlayer != null) {
//                    mCurrentPlayer.pause();
//                }
//            }
//        });
    }

    @Override
    public void onVideoSizeChanged(FFMPEGTrackPlayer mp, final int width, final int height) {
        if (mVideoView != null) {
            mVideoView.post(new Runnable() {
                @Override
                public void run() {
                    mVideoView.onVideoSizeChanged(width, height);
                }
            });
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

    @Override
    public void onBufferingUpdate(FFMPEGTrackPlayer mp, int percent) {
        Log.d(TAG, "onBufferingUpdate: " + percent);
    }

    @Override
    public void onCompletion(final FFMPEGTrackPlayer mp) {
        for (PlaybackListener pl : mPlaybackListeners) {
            pl.onStopped();
        }
        mThreadPoolExec.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onCompletion: ");
//                    if (mSeekBar != null) {
//                        mSeekBar.setProgress(0);
//                    }
//                if (mp.getNextPlayer() != null){
//                    FFMPEGTrackPlayer player = mp.getNextPlayer();
//                    if (mp.isPlaying()) {
//                        player.setSurface(null);
//                        mp.reset();
//                        mp.stop();
//                    }
//                    player.setSurface(mVideoView);
//                    player.prepareAsync();
//                }
            }
        });
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
//        mp.pause();
        mCurrentPlayer = mp;
        mPrepared = true;
        for (PlaybackListener pl : mPlaybackListeners) {
            pl.onPrepared();
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
//        if (isPlaying()) {
//            pause();
//        }
        Log.d(TAG, "onStartTrackingTouch: ");
//        mCurrentPlayer.start();
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
    public void next() {
        if (mCurrentPlayer != null) {
            mThreadPoolExec.execute(new Runnable() {
                @Override
                public void run() {
                    mCurrentPlayer.stepFrame(true);
                }
            });
        }
    }

    @Override
    public void previous() {
        if (mCurrentPlayer != null) {
            mThreadPoolExec.execute(new Runnable() {
                @Override
                public void run() {
                    mCurrentPlayer.stepFrame(false);
                }
            });
        }
    }

    @Override
    public void play() {
        if (mCurrentPlayer != null && mPrepared) {
            if (!mCurrentPlayer.isPlaying()) {
                mThreadPoolExec.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentPlayer.setFPSDelay(false);
                        mCurrentPlayer.start();
                    }
                });
            } else {
                mThreadPoolExec.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentPlayer.setFPSDelay(false);
                    }
                });
            }
        }
    }

    @Override
    public void pause() {
        if (mCurrentPlayer != null) {
            mThreadPoolExec.execute(new Runnable() {
                @Override
                public void run() {
                    mCurrentPlayer.pause();
                }
            });
        }
    }

    @Override
    public void stop() {
        try {
            if (mCurrentPlayer != null) {
                mThreadPoolExec.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentPlayer.stop();
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void fastForward() {
        if (mCurrentPlayer != null) {
            mThreadPoolExec.execute(new Runnable() {
                @Override
                public void run() {
                    mCurrentPlayer.setFPSDelay(true);
                    mCurrentPlayer.setBackwards(false);
                    mCurrentPlayer.start();
                }
            });
        }
    }

    @Override
    public void fastBackward() {
        if (mCurrentPlayer != null) {
            mThreadPoolExec.execute(new Runnable() {
                @Override
                public void run() {
                    mCurrentPlayer.setFPSDelay(true);
                    mCurrentPlayer.setBackwards(true);
                    mCurrentPlayer.start();
                }
            });
        }
    }

    @Override
    public boolean isPlaying() {
        return (mCurrentPlayer != null && mCurrentPlayer.isPlaying());
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
}
