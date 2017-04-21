package com.telenav.osv.manager.playback;

import java.util.ArrayList;
import android.widget.SeekBar;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.item.Sequence;

/**
 * Created by Kalman on 27/07/16.
 */

public abstract class PlaybackManager {
    public abstract void setSurface(Object surface);

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
