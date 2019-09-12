package com.telenav.osv.manager.playback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.SeekBar;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.Sequence;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Video Player implementation which supports video concatenation using the native library {@code ExoPlayer}.
 * This is added to support the new encoding format without changing the old player logic.
 */
//TODO: This should be refactored properly with an scalable architecture.
public class VideoPlayerManager extends PlaybackManager implements SeekBar.OnSeekBarChangeListener, Player.EventListener {

    private static final String TAG = VideoPlayerManager.class.getSimpleName();

    /**
     * The maximum video duration in milliseconds for the new encoder version.
     * The frame rate is 5 frames/sec with a maximum frame number 50 frames/video,
     * therefore the maximum duration for a video is 10 seconds.
     */
    private static final int MAX_VIDEO_DURATION_MS = 10000;

    /**
     * The interval between each video frame in milliseconds for the new encoder version.
     */
    private static final int FRAME_INTERVAL_MS = 200;

    /**
     * The maximum video duration in milliseconds for the ffmpeg version.
     * The frame rate is 4 frames/sec with a maximum frame number 64 frames/video,
     * therefore the maximum duration for a video is 16 seconds.
     */
    private static final int FFMPEG_MAX_VIDEO_DURATION_MS = 16000;

    /**
     * The interval between each video frame in milliseconds for the ffmpeg version.
     */
    private static final int FFMPEG_FRAME_INTERVAL_MS = 250;

    /**
     * The time interval between the seek bar updates in milliseconds.
     */
    private static final int SEEK_BAR_UPDATE_DELAY = 200;

    private static final String APP_PACKAGE_NAME = "com.telenav.streetview";

    private Context context;

    private LocalSequence sequence;

    private VideoLocalDataSource videoLocalDataSource;

    /**
     * The simple implementation of the player which can display all the videos from a sequence.
     */
    private SimpleExoPlayer player;

    private Disposable videoDisposable;

    /**
     * The surface on which the video frames are rendered.
     */
    private View playerView;

    /**
     * The coordinates of teh video frames.
     */
    private ArrayList<SKCoordinate> sequenceCoordinates = new ArrayList<>();

    /**
     * The seek bar displayed for all the videos from a sequence.
     */
    private SeekBar seekBar;

    /**
     * A flag to determine if the player is prepared to play.
     */
    private boolean prepared = false;

    /**
     * The duration of a video in milliseconds.
     */
    private int videoDuration = MAX_VIDEO_DURATION_MS;

    /**
     * The interval between each video frame in milliseconds.
     */
    private int frameInterval = FRAME_INTERVAL_MS;

    /**
     * The list of listeners for the playback events.
     */
    private List<PlaybackListener> playbackListeners = new ArrayList<>();

    /**
     * Handler responsible to update constantly the seek bar during a playback with a given delay.
     * This is necessary because the player doesn't provide a notification method about the playback progress.
     */
    private Handler handler = new Handler();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long currentPosition = player.getCurrentPosition();
            int i = (int) (((player.getCurrentWindowIndex() * videoDuration) + currentPosition) / frameInterval);
            seekBar.setProgress(i + 1);
            handler.postDelayed(this, SEEK_BAR_UPDATE_DELAY);
        }
    };

    public VideoPlayerManager(Context context, LocalSequence sequence, VideoLocalDataSource videoLocalDataSource) {
        this.context = context;
        this.sequence = sequence;
        this.videoLocalDataSource = videoLocalDataSource;
        player = ExoPlayerFactory.newSimpleInstance(context);
        player.addListener(this);
        prepare();
    }

    @Override
    public boolean isPlaying() {
        return player.getPlayWhenReady();
    }

    @Override
    public View getSurface() {
        return playerView;
    }

    @Override
    public void setSurface(View surface) {
        this.playerView = surface;
        ((PlayerView) playerView).setPlayer(player);
    }

    @Override
    public void prepare() {
        notifyWaitingEvent(true);
        videoDisposable = videoLocalDataSource.getVideos(sequence.getID())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        //OnSuccess
                        videos -> {
                            if (videos != null && !videos.isEmpty()) {
                                initVideoDurationAndFrameInterval(videos.get(0).getLocationsCount());
                            }
                            DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
                            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                                    Util.getUserAgent(context, APP_PACKAGE_NAME), defaultBandwidthMeter);
                            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
                            MediaSource[] mediaSources = new MediaSource[videos.size()];
                            for (int i = 0; i < videos.size(); i++) {
                                mediaSources[i] = new ExtractorMediaSource(Uri.parse(videos.get(i).getPath()),
                                        dataSourceFactory, extractorsFactory, null, null);
                            }
                            ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource(mediaSources);
                            player.prepare(concatenatingMediaSource, false, true);
                            play();
                            Collections.sort(videos, (lhs, rhs) -> lhs.getIndex() - rhs.getIndex());
                            notifyWaitingEvent(false);
                            sequenceCoordinates = new ArrayList<>(Utils.toSKCoordinatesFromLocation(sequence.getCompressionDetails().getCoordinates()));
                            if (seekBar != null) {
                                seekBar.setMax(sequenceCoordinates.size());
                            }
                            Log.d(TAG, "init. Status: success. Message: Init player with the videos from current sequence.");
                        },
                        //OnError
                        throwable -> {
                            notifyWaitingEvent(false);
                            Log.d(TAG, "init. Status: error. Message: Couldn't retrieve the videos for current sequence.");
                        },
                        //onComplete
                        () -> {
                            notifyWaitingEvent(false);
                            Log.d(TAG, "init. Status: completed. Message: Completed with no video for current sequence.");
                        });
    }

    @Override
    public void next() {
        int nextFrame = seekBar.getProgress() + 1;
        if (nextFrame > seekBar.getMax()) {
            return;
        }
        Log.d(TAG, String.format("next frame: %s", nextFrame));
        seekBar.setProgress(nextFrame);
        seekToPosition(nextFrame);
    }

    @Override
    public void previous() {
        int previousFrame = seekBar.getProgress() - 1;
        if (previousFrame < 0) {
            return;
        }
        Log.d(TAG, String.format("previous frame: %s", previousFrame));
        seekBar.setProgress(previousFrame);
        seekToPosition(previousFrame);
    }

    @Override
    public void play() {
        Log.d(TAG, "play");
        player.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause");
        player.setPlayWhenReady(false);
        for (PlaybackListener pl : playbackListeners) {
            pl.onPaused();
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public void fastForward() {
        //double the speed for the fast forward
        float speed = player.getPlaybackParameters().speed * 2;
        Log.d(TAG, String.format("fastForward: speed = %s", speed));
        PlaybackParameters parameters = new PlaybackParameters(speed);
        player.setPlaybackParameters(parameters);
    }

    @Override
    public void fastBackward() {
        //split the speed in half for fast backward
        //the minimum speed is 1f
        float speed = player.getPlaybackParameters().speed / 2;
        Log.d(TAG, String.format("fastBackward: speed = %s", speed));
        PlaybackParameters parameters = new PlaybackParameters(speed < 1 ? 1f : speed);
        player.setPlaybackParameters(parameters);
    }

    @Override
    public void setSeekBar(SeekBar seekBar) {
        this.seekBar = seekBar;
        seekBar.setOnSeekBarChangeListener(this);
        if (sequenceCoordinates.size() > 0) {
            seekBar.setMax(sequenceCoordinates.size());
        }
        prepared = true;
        for (PlaybackListener pl : playbackListeners) {
            pl.onPrepared();
        }
    }

    @Override
    public int getLength() {
        return sequenceCoordinates.size();
    }

    @Override
    public void destroy() {
        Log.d(TAG, "destroy player");
        if (videoDisposable != null) {
            videoDisposable.dispose();
        }
        for (PlaybackListener pl : playbackListeners) {
            pl.onExit();
        }
        player.stop();
        player.release();
        playbackListeners.clear();
    }

    @Override
    public void addPlaybackListener(PlaybackListener playbackListener) {
        if (!playbackListeners.contains(playbackListener)) {
            playbackListeners.add(playbackListener);
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
        return sequence;
    }

    @Override
    public ArrayList<SKCoordinate> getTrack() {
        return sequenceCoordinates;
    }

    @Override
    public void onSizeChanged() {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!prepared) {
            return;
        }
        Log.d(TAG, "onProgressChanged: " + progress);
        for (PlaybackListener pl : playbackListeners) {
            pl.onProgressChanged(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (!prepared) {
            return;
        }
        seekToPosition(seekBar.getProgress());
        for (PlaybackListener pl : playbackListeners) {
            pl.onProgressChanged(seekBar.getProgress());
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, String.format("onPlayerStateChanged: playWhenReady  - %s, playbackState - %s", playWhenReady, playbackState));
        switch (playbackState) {
            case Player.STATE_IDLE:
                handler.removeCallbacks(runnable);
                break;
            case Player.STATE_BUFFERING:
                notifyWaitingEvent(true);
                handler.removeCallbacks(runnable);
                break;
            case Player.STATE_READY:
                if (playWhenReady) {
                    handler.post(runnable);
                    for (PlaybackListener pl : playbackListeners) {
                        pl.onPlaying();
                    }
                } else {
                    handler.removeCallbacks(runnable);
                }
                notifyWaitingEvent(false);
                break;
            case Player.STATE_ENDED:
                handler.removeCallbacks(runnable);
                player.seekTo(0, 0);
                if (seekBar != null) {
                    seekBar.setProgress(0);
                }
                player.setPlayWhenReady(false);
                for (PlaybackListener pl : playbackListeners) {
                    pl.onStopped();
                }
                break;
        }
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    /**
     * Initializes the video duration and the frame interval for the specific encoder version.
     * @param videoTotalFrames the total number of frames in a video.
     */
    private void initVideoDurationAndFrameInterval(int videoTotalFrames) {
        if (videoTotalFrames > MAX_VIDEO_DURATION_MS / FRAME_INTERVAL_MS) {
            videoDuration = FFMPEG_MAX_VIDEO_DURATION_MS;
            frameInterval = FFMPEG_FRAME_INTERVAL_MS;
        }
    }

    /**
     * Notify the listener about the waiting event in order to update the UI.
     * @param shouldWait
     */
    private void notifyWaitingEvent(boolean shouldWait) {
        for (PlaybackListener pl : playbackListeners) {
            pl.onWaiting(shouldWait);
        }
    }

    private void seekToPosition(int progress) {
        int videoProgress = (progress - 1) * frameInterval;
        int videoIndex = videoProgress / videoDuration;
        int frameInMs = videoProgress - videoIndex * videoDuration;
        Log.d(TAG, String.format("seekToPosition: frame progress %s, video index %s, frame in ms %s", videoProgress, videoIndex, frameInMs));
        player.seekTo(videoIndex, frameInMs);
    }
}