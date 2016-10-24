#ifndef VIDEOPLAYER_H_
#define VIDEOPLAYER_H_

#include <stdint.h>
#include <stdio.h>

#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>

#include "ffmpeg_mediaplayer.h"

#include <android/native_window_jni.h>

typedef struct VideoPlayer {
    size_t *native_window;
} VideoPlayer;

void createVideoEngine(VideoPlayer **ps);
void createScreen(VideoPlayer **ps, size_t *surface);
struct SwsContext *createScaler(VideoPlayer **ps, AVCodecContext *codec);
void *createBmp(VideoPlayer **ps, int width, int height);
void destroyBmp(void *bmp);
void updateBmp(VideoPlayer **ps, struct SwsContext *sws_ctx, AVCodecContext *pCodecCtx, void *bmp, AVFrame *pFrame, int width, int height);
void displayBmp(VideoPlayer **ps, void *bmp, AVCodecContext *pCodecCtx, int width, int height);
void shutdownVideoEngine(VideoPlayer **ps);

#endif /* VIDEOPLAYER_H_ */
