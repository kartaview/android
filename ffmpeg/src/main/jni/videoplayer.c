#include "videoplayer.h"

const enum AVPixelFormat TARGET_IMAGE_FORMAT = AV_PIX_FMT_RGBA; //AV_PIX_FMT_RGB24;
const enum AVCodecID TARGET_IMAGE_CODEC = AV_CODEC_ID_PNG;

void createVideoEngine(VideoPlayer **ps) {
    VideoPlayer *is = *ps;
}

void createScreen(VideoPlayer **ps, size_t *surface) {
    VideoPlayer *is = *ps;
    is->native_window = surface;
}

struct SwsContext *createScaler(VideoPlayer **ps, AVCodecContext *codec) {
    struct SwsContext *sws_ctx;

    sws_ctx = sws_getContext(codec->width,
                             codec->height,
                             codec->pix_fmt,
                             codec->width,
                             codec->height,//todo scale to surface width height
                             AV_PIX_FMT_RGBA,
                             SWS_BILINEAR,
                             NULL,
                             NULL,
                             NULL);

    return sws_ctx;
}

void *createBmp(VideoPlayer **ps, int width, int height) {
//    LOGI("Video Bitmap created");
    VideoPlayer *is = *ps;

    Picture *bmp = malloc(sizeof(Picture));
    bmp->buffer = NULL;
    return bmp;
}

void destroyBmp(void *bmp) {
//    LOGI("Video Bitmap destroyed");
    Picture *picture = (Picture *) bmp;

    if (picture) {
        if (picture->buffer) {
//            LOGI("Releasing frame buffer %p", picture->buffer);
            av_free(picture->buffer);
            picture->buffer = NULL;
        }

        free(picture);
        picture = NULL;
    }
}

void updateBmp(VideoPlayer **ps, struct SwsContext *sws_ctx, AVCodecContext *pCodecCtx, void *bmp, AVFrame *pFrame, int width, int height) {
    VideoPlayer *is = *ps;

    Picture *picture = (Picture *) bmp;

    AVFrame *frame;

    int got_packet_ptr = 0;

    if (width == -1) {
        width = pCodecCtx->width;
    }

    if (height == -1) {
        height = pCodecCtx->height;
    }

    frame = av_frame_alloc();

    if (!frame) {
        goto fail;
    }

    // Determine required buffer size and allocate buffer
    int numBytes = av_image_get_buffer_size(TARGET_IMAGE_FORMAT, width, height, 1);
    picture->buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));

//    LOGI("Allocating frame buffer %p", picture->buffer);


    if (!picture->buffer) {
        LOGI("updateBmp: no buffer allocated");
        return;
    }
    // set the frame parameters
    frame->format = TARGET_IMAGE_FORMAT;
    frame->width = width;
    frame->height = height;

    av_image_fill_arrays(frame->data, frame->linesize,
                   picture->buffer,
                   TARGET_IMAGE_FORMAT,
                   width,
                   height, 1);

    sws_scale(sws_ctx,
              (const uint8_t *const *) pFrame->data,
              pFrame->linesize,
              0,
              height,//todo swscale to surface resolution only
              frame->data,
              frame->linesize);


    picture->linesize = frame->linesize[0];

    // TODO is this right?
    fail:
    av_free(frame);
}

void displayBmp(VideoPlayer **ps, void *bmp, AVCodecContext *pCodecCtx, int width, int height) {
    VideoPlayer *is = *ps;

    Picture *picture = (Picture *) bmp;
    if (!picture->buffer) {
        LOGI("displayBmp: no buffer allocated");
        return;
    }
    if (width == -1) {
        width = pCodecCtx->width;
    }

    if (height == -1) {
        height = pCodecCtx->height;
    }

    if (is->native_window && *is->native_window) {
        ANativeWindow_setBuffersGeometry((ANativeWindow *) *is->native_window, width, height, WINDOW_FORMAT_RGBA_8888);

        ANativeWindow_Buffer windowBuffer;

        if (ANativeWindow_lock((ANativeWindow *) *is->native_window, &windowBuffer, NULL) == 0) {
            int h = 0;

            for (h = 0; h < height; h++) {
                memcpy(windowBuffer.bits + h * windowBuffer.stride * 4,
                       picture->buffer + h * picture->linesize, (size_t) (width * 4));
            }

            ANativeWindow_unlockAndPost((ANativeWindow *) *is->native_window);
        }
    } else {
        LOGI("NO NATIVE WINDOW");
    }
}

void shutdownVideoEngine(VideoPlayer **ps) {

}
