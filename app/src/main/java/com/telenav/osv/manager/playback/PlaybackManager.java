package com.telenav.osv.manager.playback;

import java.util.ArrayList;
import android.view.View;
import android.widget.SeekBar;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.item.Sequence;

/**
 * abstract playback manager
 * Created by Kalman on 27/07/16.
 */
public interface PlaybackManager {

    void setSource(Sequence sequence);

    View getSurface();

    void setSurface(View surface);

    void prepare();

    void next();

    void previous();

    void play();

    void pause();

    void stop();

    void fastForward();

    void fastBackward();

    boolean isPlaying();

    void setSeekBar(SeekBar seekBar);

    int getLength();

    void destroy();

    void addPlaybackListener(PlaybackListener playbackListener);

    void removePlaybackListener(PlaybackListener playbackListener);

    boolean isSafe();

    Sequence getSequence();

    ArrayList<SKCoordinate> getTrack();

    void onSizeChanged();

    interface PlaybackListener {

        void onPlaying();

        void onPaused();

        void onStopped();

        void onPreparing();

        void onPrepared(boolean success);

        void onProgressChanged(int index);

        void onExit();
    }
}
