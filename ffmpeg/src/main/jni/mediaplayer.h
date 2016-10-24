#ifndef MEDIAPLAYER_H
#define MEDIAPLAYER_H

#include <android/native_window_jni.h>

#include <Errors.h>
#include <pthread.h>
#include <stdio.h>
#include <vector>
#include <deque>
#include <sys/types.h>
#include "untrunc/mp4.h"
#include "untrunc/atom.h"

#ifdef ANDROID
#include <android/log.h>
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "FFMPEG_TRACKPLAYER_CPP E ", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "FFMPEG_TRACKPLAYER_CPP I ", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("FFMPEG_MEDIAPLAYER_CPP E " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("FFMPEG_MEDIAPLAYER_CPP I " format "\n", ##__VA_ARGS__)
#endif

#define DEFAULT_FPS_DELAY 200
#define FAST_FPS_DELAY 100

extern "C" {
    #include "ffmpeg_mediaplayer.h"
}

class MediaPlayerListener
{
public:
    virtual void notify(int msg, int ext1, int ext2, int fromThread) = 0;
};

class MediaPlayer
{
public:
    MediaPlayer();
    ~MediaPlayer();

            void            disconnect();
            void            initSigHandler();
            status_t        setDataSource(const char *url[], int size);
            status_t        setVideoSurface(ANativeWindow* native_window);
            status_t        setListener(MediaPlayerListener *listener);
            MediaPlayerListener * getListener();
            status_t        start();
            status_t        stop();
            status_t        pause();
            bool            isPlaying();
            status_t        getVideoWidth(int *w);
            status_t        getVideoHeight(int *h);
            status_t        seekTo(int msec);
            status_t        getCurrentPosition(int *gIndex);
            status_t        getDuration(int *msec);
            status_t        setFPSDelay(bool fast);
            status_t        setLooping(int loop);
            bool            isLooping();
            status_t        reset();
            void            notify(void* is,int msg, int ext1, int ext, int fromThread);

    std::deque<size_t>          states;
    VideoState*                 state;
    int                         mFpsDelay;
    int                         mBackwards;
    int                         mSeeking;
    ANativeWindow               *native_window;

    int stepFrame(bool forward);

    int setBackwards(bool backwards);

    int seeking(bool i);

private:
            void            clear_l();
            void            jumpTo(int fileIndex);
            status_t        seekTo_l(int video, int index);
//            status_t        prepareAsync_l();
            status_t        getDuration_l(int *msec);
            status_t        setFPSDelay_l(bool fast);
            status_t        mapGlobalIndexToLocal(int gIndex, std::pair<int, int> *data);
            int             mapLocalIndexToGlobal(int video, int index);
            status_t        setDataSource(VideoState*& ps);
            status_t        setCurrentPlayer(int index);

    MediaPlayerListener*        mListener;
    media_player_states         mPlayerState;
    bool                        mLoop;
    int                         mVideoWidth;
    int                         mVideoHeight;
};

#endif // MEDIAPLAYER_H
