package com.telenav.osv.manager;

import java.util.ArrayList;
import android.view.View;
import android.widget.SeekBar;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.ui.fragment.TrackPreviewFragment;

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

    public abstract boolean isOnline();

    public abstract Sequence getSequence();

    public abstract ArrayList<ImageFile> getImages();

    public interface PlaybackListener {
        void onPlaying();

        void onPaused();

        void onStopped();

        void onPrepared();

        void onProgressChanged(int index);
    }
}
