package com.telenav.osv.manager.playback;

import java.util.ArrayList;
import android.view.View;
import android.widget.SeekBar;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.item.Sequence;

/**
 * abstract playback manager
 * Created by Kalman on 27/07/16.
 */
public abstract class PlaybackManager {

    public static PlaybackManager get(OSVActivity activity, Sequence sequence) {
        if (!sequence.isOnline()) {
            return sequence.isSafe() ? new SafePlaybackManager(activity, sequence) : new LocalPlaybackManager(activity, sequence);
        } else {
            return new OnlinePlaybackManager(activity, sequence);
        }
    }

    public abstract View getSurface();

    public abstract void setSurface(View surface);

    public abstract void prepare();

    public abstract void next();

    public abstract void previous();

    public abstract void play();

    public abstract void pause();

    public abstract void stop();

    public abstract void fastForward();

    public abstract void fastBackward();

    public abstract boolean isPlaying();

    public abstract void setSeekBar(SeekBar seekBar);

    public abstract int getLength();

    public abstract void destroy();

    public abstract void addPlaybackListener(PlaybackListener playbackListener);

    public abstract void removePlaybackListener(PlaybackListener playbackListener);

    public abstract boolean isSafe();

    public abstract Sequence getSequence();

    public abstract ArrayList<SKCoordinate> getTrack();

    public abstract void onSizeChanged();

    public interface PlaybackListener {

        void onPlaying();

        void onPaused();

        void onStopped();

        void onPrepared();

        void onProgressChanged(int index);

        void onExit();
    }
}
