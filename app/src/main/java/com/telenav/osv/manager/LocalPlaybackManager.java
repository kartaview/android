package com.telenav.osv.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.view.Surface;
import android.widget.SeekBar;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.VideoFile;
import com.telenav.osv.utils.Log;
import wseemann.media.FFmpegMediaPlayer;

/**
 * Created by Kalman on 30/06/16.
 */

public class LocalPlaybackManager extends PlaybackManager implements FFmpegMediaPlayer.OnSeekCompleteListener, FFmpegMediaPlayer.OnBufferingUpdateListener, FFmpegMediaPlayer
        .OnCompletionListener,
        FFmpegMediaPlayer.OnErrorListener, FFmpegMediaPlayer.OnInfoListener, FFmpegMediaPlayer.OnPreparedListener, SeekBar.OnSeekBarChangeListener {
    public static final String TAG = "LocalPlaybackManager";

    private final Handler mMediaHandler;

    private final HandlerThread mHandlerThread;

    private ArrayList<FFmpegMediaPlayer> mPlayers = new ArrayList<>();

    private Sequence mSequence;

    private SeekBar mSeekBar;

    private Surface mSurface;

    private FFmpegMediaPlayer mCurrentPlayer = null;

    private int mTotalDuration;

    private boolean mPrepared = false;

    private boolean mTouching = false;

    private ArrayList<PlaybackListener> mPlaybackListeners = new ArrayList<>();

    public LocalPlaybackManager(Sequence sequence) {
        mHandlerThread = new HandlerThread("MediaHandlerThread", Process.THREAD_PRIORITY_FOREGROUND);
        mHandlerThread.start();
        mMediaHandler = new Handler(mHandlerThread.getLooper());
        this.mSequence = sequence;
        mTotalDuration = 0;
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                final ArrayList<VideoFile> nodes = new ArrayList<>();
                try {
                    Cursor records = SequenceDB.instance.getVideos(mSequence.sequenceId);
                    if (records != null && records.getCount() > 0) {
                        while (!records.isAfterLast()) {
                            try {
                                String path = records.getString(records.getColumnIndex(SequenceDB.VIDEO_FILE_PATH));
                                int index = records.getInt(records.getColumnIndex(SequenceDB.VIDEO_INDEX));
                                OSVFile video = new OSVFile(path);
                                nodes.add(new VideoFile(video, index));
                            } catch (Exception e) {
                                Log.d(TAG, "LocalPlaybackManager: " + Log.getStackTraceString(e));
                            }
                            records.moveToNext();
                        }
                        Collections.sort(nodes, new Comparator<VideoFile>() {
                            @Override
                            public int compare(VideoFile lhs, VideoFile rhs) {
                                return lhs.startIndex - rhs.startIndex;
                            }
                        });
                        for (VideoFile vf : nodes) {
                            try {
                                FFmpegMediaPlayer player = new FFmpegMediaPlayer();
                                player.setOnSeekCompleteListener(LocalPlaybackManager.this);
                                player.setOnBufferingUpdateListener(LocalPlaybackManager.this);
                                player.setOnCompletionListener(LocalPlaybackManager.this);
                                player.setOnErrorListener(LocalPlaybackManager.this);
                                player.setOnInfoListener(LocalPlaybackManager.this);
                                player.setOnPreparedListener(LocalPlaybackManager.this);
                                player.setDataSource(vf.link);

                                if (mPlayers.size() > 0) {
                                    mPlayers.get(mPlayers.size() - 1).setNextMediaPlayer(player);
                                } else {
                                    mCurrentPlayer = player;
                                }
                                mPlayers.add(player);
                            } catch (Exception e) {
                                Log.d(TAG, "LocalPlaybackManager: " + Log.getStackTraceString(e));
                            }
                        }
                    } else {
                        Log.d(TAG, "displaySequence: cursor has 0 elements");
                    }
                    if (records != null) {
                        records.close();
                    }
                } catch (Exception e) {
                    Log.d(TAG, "displaySequence: " + e.getLocalizedMessage());
                }
            }
        });
    }

    @Override
    public void setSeekBar(final SeekBar seekBar) {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                mSeekBar = seekBar;
                mSeekBar.setOnSeekBarChangeListener(LocalPlaybackManager.this);
//                mSeekBar.setMax(mTotalDuration);
//                Log.d(TAG, "setSeekBar: total duration = " + mTotalDuration);
            }
        });
    }

    @Override
    public void setSurface(final Object surface) {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                mSurface = (Surface) surface;
                mCurrentPlayer.setSurface(mSurface);
                try {
                    mCurrentPlayer.prepare();
                    int duration = mCurrentPlayer.getDuration();
                    mTotalDuration = mTotalDuration + duration;
                    mSeekBar.setMax(mTotalDuration);
//                    Log.d(TAG, "run: duration after = " + mCurrentPlayer.getDuration());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public int getLength() {
        return mTotalDuration;
//        return mCurrentPlayer.getDuration();
    }

    public void seekTo(final int milliseconds) {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mCurrentPlayer.isPlaying()) {
                    mCurrentPlayer.start();
                }
                mCurrentPlayer.seekTo(milliseconds);
            }
        });
    }

    public int getCurrentPosition(){
        return mCurrentPlayer.getCurrentPosition();
    }

    @Override
    public void destroy() {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                for (FFmpegMediaPlayer player : mPlayers) {
                    if (player != null) {
                        try {
                            if (player.isPlaying()) {
                                player.stop();
                            }
                        } catch (Exception e) {

                        }
                        try {
                            player.release();
                        } catch (Exception e) {

                        }
                    }
                }
            }
        });
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
    public boolean isOnline() {
        return false;
    }

    @Override
    public Sequence getSequence() {
        return mSequence;
    }

    @Override
    public ArrayList<ImageFile> getImages() {
        return null;
    }

    @Override
    public void onSeekComplete(final FFmpegMediaPlayer mp) {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onSeekComplete: " + mp.getCurrentPosition());
//                if (mCurrentPlayer != null) {
//                    mCurrentPlayer.pause();
//                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(FFmpegMediaPlayer mp, int percent) {
        Log.d(TAG, "onBufferingUpdate: " + percent);
    }

    @Override
    public void onCompletion(final FFmpegMediaPlayer mp) {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onCompletion: ");
//                if (mp.getNextPlayer() != null){
//                    FFmpegMediaPlayer player = mp.getNextPlayer();
//                    if (mp.isPlaying()) {
//                        player.setSurface(null);
//                        mp.reset();
//                        mp.stop();
//                    }
//                    player.setSurface(mSurface);
//                    player.prepareAsync();
//                }
            }
        });
    }

    @Override
    public boolean onError(FFmpegMediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError: " + what + " - " + extra);
        return false;
    }

    @Override
    public boolean onInfo(FFmpegMediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onInfo: " + what + " - " + extra);
        return false;
    }

    @Override
    public void onPrepared(FFmpegMediaPlayer mp) {
//        mp.pause();
        mCurrentPlayer = mp;
        Log.d(TAG, "onPrepared: " + mTotalDuration);
        Log.d(TAG, "onPrepared: " + getCurrentPosition());
        mPrepared = true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!mPrepared || !mTouching) {
            return;
        }
        Log.d(TAG, "onProgressChanged: " + progress);
        mCurrentPlayer.seekTo(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (!mPrepared) {
            return;
        }
        mTouching = true;
        Log.d(TAG, "onStartTrackingTouch: ");
        mCurrentPlayer.start();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (!mPrepared) {
            return;
        }
        mTouching = false;
        Log.d(TAG, "onStopTrackingTouch: ");
    }

    @Override
    public void next() {

    }

    @Override
    public void previous() {

    }

    @Override
    public void play() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void fastForward() {

    }

    @Override
    public void fastBackward() {

    }

    @Override
    public boolean isPlaying() {
        return mCurrentPlayer.isPlaying();
    }
}
