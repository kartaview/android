#include "include/libavcodec/avcodec.h"
#include "include/libavformat/avformat.h"
#include "include/libavutil/imgutils.h"
#include "crashlitics.h"

#include <jni.h>
#include <libswscale/swscale.h>
#include <libavutil/opt.h>

#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <asm/siginfo.h>
#include <signal.h>
#include <unistd.h>

#ifdef ANDROID
#include <android/log.h>
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "FFMPEG E ", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "FFMPEG I ", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("FFMPEG E " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("FFMPEG I " format "\n", ##__VA_ARGS__)
#endif
AVCodecContext *h264_codec_ctx = NULL;
AVFrame *yuvframe;
AVPacket pkt;
FILE *file;

crashlytics_context_t *crashlytics_ctx;

//for jpeg decode
AVFormatContext *jpg_fmt_ctx;
AVCodec *jpg_codec;
AVCodecContext *jpg_codec_ctx;
AVInputFormat *jpg_inf;
struct SwsContext *sws_ctx;
unsigned char *jpg_avbuff;
AVPacket jpg_pkt;
unsigned char *jpg_buff;
int jpg_buff_len;
int jpg_buff_pos;

//for filtering (rotate)
const char *filter_rotate_cw = "transpose=clock";
const char *filter_rotate_ccw = "transpose=cclock";
const char *filter_flip_vert = "vflip";
AVFilterContext *buffersink_ctx;
AVFilterContext *buffersrc_ctx;
AVFilterGraph *filter_graph;

//for encoding
AVFormatContext *ofmt_ctx;
AVStream *video_st;
AVCodec *pCodec;
int framecnt = 0;
int total_framecnt = 0;
int FPS = 4;


void custom_log(void *ptr, int level, const char *fmt, va_list vl) {
    FILE *fp = fopen("/storage/emulated/0/av_log.txt", "a+");
    if (fp) {
        vfprintf(fp, fmt, vl);
        fflush(fp);
        fclose(fp);
    }
}

int read_packet(void *opaque, uint8_t *buf, int buf_size) {
    int size = buf_size;
    if (jpg_buff_len - jpg_buff_pos < buf_size)
        size = jpg_buff_len - jpg_buff_pos;
    if (size > 0) {
        memcpy(buf, jpg_buff + jpg_buff_pos, size);
        jpg_buff_pos += size;
    }
    return size;
}

int initializeEncoder(const char *out_path, int width, int height) {
    //output initialize
    avformat_alloc_output_context2(&ofmt_ctx, NULL, "mp4", out_path);
    //output encoder initialize
    pCodec = avcodec_find_encoder(AV_CODEC_ID_H264);
    if (!pCodec) {
        LOGE("Can not find encoder!\n");
        return -1;
    }
    h264_codec_ctx = avcodec_alloc_context3(pCodec);
    h264_codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;
    h264_codec_ctx->color_range = AVCOL_RANGE_JPEG;
    h264_codec_ctx->width = width;
    h264_codec_ctx->height = height;
    h264_codec_ctx->time_base.num = 1;
    h264_codec_ctx->time_base.den = FPS;
    h264_codec_ctx->bit_rate = 800000;
    h264_codec_ctx->gop_size = 0;
    h264_codec_ctx->i_quant_offset = 0;
    h264_codec_ctx->i_quant_factor = 0;
    h264_codec_ctx->profile = FF_PROFILE_H264_HIGH;
    /* Some formats want stream headers to be separate. */
    if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
        h264_codec_ctx->flags |= CODEC_FLAG_GLOBAL_HEADER;

    //H264 codec param
    //h264_codec_ctx->me_range = 16;
    h264_codec_ctx->max_qdiff = 4;
    h264_codec_ctx->qcompress = 0.6;
    h264_codec_ctx->qmin = 12;
    h264_codec_ctx->qmax = 22;
    //Optional Param
//    c->max_b_frames = 3;
    // Set H264 preset and tune
    AVDictionary *param = 0;
    int set;
    set = av_dict_set(&param, "preset", "ultrafast", 0);
    LOGI("Setting preset = %i", set);
    set = av_dict_set(&param, "tune", "zerolatency", 0);//todo remove this
    LOGI("Setting tune = %i", set);
    set = av_dict_set(&param, "level", "4.1", 0);
    LOGI("Setting level = %i", set);
    set = av_dict_set(&param, "profile", "High", 0);//todo defaults to Constrained Baseline-4.1 or High-5.0 because of frame MB size limit
    LOGI("Setting level = %i", set);
    set = av_dict_set(&param, "profile", "high", 0);
    LOGI("Setting level = %i", set);
    h264_codec_ctx->profile = FF_PROFILE_H264_HIGH;

    av_opt_set(h264_codec_ctx->priv_data, "profile", "High", AV_OPT_SEARCH_CHILDREN);
    av_opt_set(h264_codec_ctx->priv_data, "profile", "high", AV_OPT_SEARCH_CHILDREN);
    av_opt_set(h264_codec_ctx->priv_data, "level", "high", AV_OPT_SEARCH_CHILDREN);
    av_opt_set_dict(h264_codec_ctx->priv_data, &param);
    av_opt_set_dict(h264_codec_ctx, &param);
    if (avcodec_open2(h264_codec_ctx, pCodec, &param) < 0) {
        LOGE("Failed to open encoder!\n");
        return -1;
    }


    //Add a new stream to output,should be called by the user before avformat_write_header() for muxing
    video_st = avformat_new_stream(ofmt_ctx, pCodec);
    if (video_st == NULL) {
        return -1;
    }
    video_st->time_base.num = 1;
    video_st->time_base.den = FPS;
    video_st->codec = h264_codec_ctx;

    //Open output URL,set before avformat_write_header() for muxing
    if (avio_open(&ofmt_ctx->pb, out_path, AVIO_FLAG_READ_WRITE) < 0) {
        LOGE("Failed to open output file!\n");
        return -1;
    }
    //Write File Header
    avformat_write_header(ofmt_ctx, NULL);
    total_framecnt = 0;
    framecnt = 0;

    LOGI("Initialized file successfully, height %i, width %i\n", height, width);
    LOGI("----------------------------------------------");
    return 0;
}

static int init_filters(const char *filters_descr) {
    char args[512];
    int ret = 0;
    AVFilter *buffersrc = avfilter_get_by_name("buffer");
    AVFilter *buffersink = avfilter_get_by_name("buffersink");
    AVFilterInOut *outputs = avfilter_inout_alloc();
    AVFilterInOut *inputs = avfilter_inout_alloc();
    AVRational time_base = jpg_fmt_ctx->streams[0]->time_base;
    enum AVPixelFormat pix_fmts[] = {jpg_codec_ctx->pix_fmt, AV_PIX_FMT_NONE};

    filter_graph = avfilter_graph_alloc();
    if (!outputs || !inputs || !filter_graph) {
        ret = AVERROR(ENOMEM);
        goto end;
    }

    /* buffer video source: the decoded frames from the decoder will be inserted here. */
    snprintf(args, sizeof(args),
             "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
             jpg_codec_ctx->width, jpg_codec_ctx->height, jpg_codec_ctx->pix_fmt,
             time_base.num, time_base.den,
             jpg_codec_ctx->sample_aspect_ratio.num, jpg_codec_ctx->sample_aspect_ratio.den);

    ret = avfilter_graph_create_filter(&buffersrc_ctx, buffersrc, "in",
                                       args, NULL, filter_graph);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot create buffer source\n");
        goto end;
    }

    /* buffer video sink: to terminate the filter chain. */
    ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink, "out",
                                       NULL, NULL, filter_graph);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot create buffer sink\n");
        goto end;
    }

    ret = av_opt_set_int_list(buffersink_ctx, "pix_fmts", pix_fmts,
                              AV_PIX_FMT_NONE, AV_OPT_SEARCH_CHILDREN);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot set output pixel format\n");
        goto end;
    }

    /*
     * Set the endpoints for the filter graph. The filter_graph will
     * be linked to the graph described by filters_descr.
     */

    /*
     * The buffer source output must be connected to the input pad of
     * the first filter described by filters_descr; since the first
     * filter input label is not specified, it is set to "in" by
     * default.
     */
    outputs->name = av_strdup("in");
    outputs->filter_ctx = buffersrc_ctx;
    outputs->pad_idx = 0;
    outputs->next = NULL;

    /*
     * The buffer sink input must be connected to the output pad of
     * the last filter described by filters_descr; since the last
     * filter output label is not specified, it is set to "out" by
     * default.
     */
    inputs->name = av_strdup("out");
    inputs->filter_ctx = buffersink_ctx;
    inputs->pad_idx = 0;
    inputs->next = NULL;

    if ((ret = avfilter_graph_parse_ptr(filter_graph, filters_descr,
                                        &inputs, &outputs, NULL)) < 0)
        goto end;

    if ((ret = avfilter_graph_config(filter_graph, NULL)) < 0)
        goto end;

    end:
    if (inputs) {
        avfilter_inout_free(&inputs);
    }
    if (outputs) {
        avfilter_inout_free(&outputs);
    }
    if (ret < 0) {
        if (filter_graph) {
            avfilter_graph_free(&filter_graph);
        }
//        if (buffersink_ctx) {
//            avfilter_free(buffersink_ctx);
//        }
//        if (buffersrc_ctx) {
//            avfilter_free(buffersrc_ctx);
//        }
    }

    return ret;
}


int decodeJpegData(jbyte *jpeg, int length) {
    jpg_buff_len = length;
    jpg_buff = (unsigned char *) jpeg;
    jpg_buff_pos = 0;

    LOGI("decode jpeg data");
    yuvframe = av_frame_alloc();
    av_init_packet(&jpg_pkt);
    int frameFinished = 0;
    av_read_frame(jpg_fmt_ctx, &jpg_pkt);
    int ret = avcodec_decode_video2(jpg_codec_ctx, yuvframe, &frameFinished, &jpg_pkt);
//    const char *out_path = "/sdcard/jpegmeta.txt";
//    av_dump_format(jpg_fmt_ctx,0,out_path,1);
    // TAGS reading
    if (ret <= 0) {
        LOGI("error obtaining frame from byte array");
        return -1;
    }
    AVDictionaryEntry *tag = av_dict_get(yuvframe->metadata, "Orientation", NULL, AV_DICT_MATCH_CASE);
    if (tag) {
        LOGI("ORIENTATION IS %s=%s\n", tag->key, tag->value);
        int ret_val;
        if (!filter_graph) {
            switch (atoi(tag->value)) {
                case 6:
                    LOGI("Initializing cw rotate filter");
                    if ((init_filters(filter_rotate_cw)) < 0) {
                        LOGE("Filter init failed");
                        return -1;
                        goto formatCheck;
                    }
                    break;
                case 3:
                    LOGI("Initializing vertical flip filter");
                    if ((init_filters(filter_flip_vert)) < 0) {
                        LOGE("Filter init failed");
                        return -1;
                        goto formatCheck;
                    }
                    break;
                case 8:
                    LOGI("Initializing ccw rotate filter");
                    if ((init_filters(filter_rotate_ccw)) < 0) {
                        LOGE("Filter init failed");
                        return -1;
                        goto formatCheck;
                    }
                    break;
                case 1:
                default:
                    LOGI("No need to rotate");
                    goto formatCheck;
            }
        }
        LOGI("Filtering...");
        if (av_buffersrc_add_frame_flags(buffersrc_ctx, yuvframe, AV_BUFFERSRC_FLAG_KEEP_REF) < 0) {//AV_BUFFERSRC_FLAG_PUSH
            av_log(NULL, AV_LOG_ERROR, "Error while feeding the filtergraph\n");
            LOGE("Error while feeding the filtergraph");
            goto formatCheck;
        }
        AVFrame *filt_frame = av_frame_alloc();
        /* pull filtered frames from the filtergraph */
        while (1) {
            ret_val = av_buffersink_get_frame(buffersink_ctx, filt_frame);
            if (ret_val == AVERROR(EAGAIN) || ret_val == AVERROR_EOF)
                goto formatCheck;
            if (ret_val < 0)
                goto formatCheck;
            if (yuvframe) {
                av_frame_free(&yuvframe);
            }
            yuvframe = filt_frame;
            break;
            LOGI("Applied filter");
        }
        if (filter_graph && framecnt >= 49) {
            avfilter_graph_free(&filter_graph);
        }


    }
    formatCheck:
    if (yuvframe->format != AV_PIX_FMT_YUVJ420P && yuvframe->format != AV_PIX_FMT_YUV420P) {
        LOGI("converting to proper color format...");
//        swsscale the shit out of this motherfucker
        if (sws_ctx) {
            sws_freeContext(sws_ctx);
            sws_ctx = NULL;
        }
        sws_ctx = sws_getContext(yuvframe->width, yuvframe->height, (enum AVPixelFormat) yuvframe->format,
                                 yuvframe->width, yuvframe->height, AV_PIX_FMT_YUV420P,
                                 SWS_BICUBIC, NULL, NULL, NULL);
        AVFrame *temp = av_frame_alloc();
        av_image_fill_arrays(temp->data, temp->linesize, NULL, AV_PIX_FMT_YUVJ420P, yuvframe->width, yuvframe->height, 2);
        av_image_alloc(temp->data, temp->linesize, yuvframe->width, yuvframe->height, AV_PIX_FMT_YUVJ420P, 2);
        temp->format = AV_PIX_FMT_YUVJ420P;
        ret = sws_scale(sws_ctx, (const uint8_t *const *) yuvframe->data, yuvframe->linesize, 0, yuvframe->height, temp->data, temp->linesize);
        if (ret <= 0) {
            LOGE("Something went wrong while color space conversion returned %i", ret);
            av_frame_free(&temp);
            goto end;
        }
        if (temp->width == 0 || temp->height == 0) {
            temp->width = yuvframe->width;
            temp->height = yuvframe->height;
        }
        if (yuvframe) {
            LOGI("Freeing yuv422 frame");
            av_frame_free(&yuvframe);
            LOGI("Freed yuv422 frame");
        }
        yuvframe = temp;
    }
    end:
    av_packet_unref(&jpg_pkt);
    LOGI("Decoded jpeg data successfully");
    return 0;
}

JNIEXPORT jint JNICALL Java_com_telenav_ffmpeg_FFMPEG_flush(JNIEnv *env, jobject obj) {
    int ret;
    int got_frame;
    AVPacket enc_pkt;
    LOGI("Flushing encoder");
    LOGI("----------------------------------------------");
    if (!(ofmt_ctx
          && ofmt_ctx->streams[0]
          && ofmt_ctx->streams[0]->codec
          && ofmt_ctx->streams[0]->codec->codec)) {
        LOGI("Context null");
        return 0;
    }
    if (!(ofmt_ctx->streams[0]->codec->codec->capabilities & CODEC_CAP_DELAY)) {
        LOGI("No CODEC_CAP_DELAY compatibility");
        return 0;
    }
    LOGI("trying to flush remaining frames, total = %i, written %i", total_framecnt, framecnt);
    while (total_framecnt > framecnt) {
        enc_pkt.data = NULL;
        enc_pkt.size = 0;
        av_init_packet(&enc_pkt);
        ret = avcodec_encode_video2(ofmt_ctx->streams[0]->codec, &enc_pkt,
                                    NULL, &got_frame);
        LOGI("flushing frame code %i", ret);
        if (ret < 0)
            LOGE("error flushing frame");
        break;
        if (!got_frame) {
            ret = 0;
            LOGI("obtained no frame");
            break;
        }
        framecnt++;
        pkt.stream_index = video_st->index;

        //Write PTS
        AVRational time_base = ofmt_ctx->streams[0]->time_base;//{ 1, 1000 };
//        AVRational r_framerate1 = {60, 2};//{ 50, 2 };
        AVRational time_base_q = {1, AV_TIME_BASE};
        //Duration between 2 frames (us)
        int64_t calc_duration = (int64_t) ((double) (AV_TIME_BASE) * (1 / FPS));
        //Parameters
        enc_pkt.pts = av_rescale_q(framecnt * calc_duration, time_base_q, time_base);
        enc_pkt.dts = enc_pkt.pts;
        enc_pkt.duration = av_rescale_q(calc_duration, time_base_q, time_base);

        enc_pkt.pos = -1;

//        LOGI("pts = %i , dts = &i , dur = %i",pkt.pts,pkt.dts,pkt.duration);

        ofmt_ctx->duration = enc_pkt.duration * framecnt;

        /* mux encoded frame */
        ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
        if (ret < 0)
            break;
        LOGI("Flush Encoder: Succeed to encode 1 frame!\tsize:%5d\n", enc_pkt.size);
    }
    //Write file trailer
    if (ofmt_ctx && framecnt > 0) {
        LOGI("Writing file trailer %i", ofmt_ctx);
        av_write_trailer(ofmt_ctx);
        LOGI("Wrote file trailer");
    }
    if (total_framecnt == 0 && framecnt == 0 && ofmt_ctx) {
        LOGI("entered remove file");
        remove(ofmt_ctx->filename);
        LOGI("finished remove file");
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_com_telenav_ffmpeg_FFMPEG_nextFile(JNIEnv *env, jobject obj, jstring s_, jint width, jint height) {
    const char *out_path = (*env)->GetStringUTFChars(env, s_, 0);
    Java_com_telenav_ffmpeg_FFMPEG_flush(env, obj);

    if (sws_ctx) {
        sws_freeContext(sws_ctx);
        sws_ctx = NULL;
    }
    if (video_st) {
        avcodec_close(video_st->codec);
    }

    if (ofmt_ctx && ofmt_ctx->pb) {
        avio_close(ofmt_ctx->pb);
    }

    if (ofmt_ctx) {
        avformat_free_context(ofmt_ctx);
        ofmt_ctx = NULL;
    }
    initializeEncoder(out_path, width, height);
    (*env)->ReleaseStringUTFChars(env, s_, out_path);
    return 0;
}

struct sigaction psa, oldPsa;


void handleCrash(int signalNumber, siginfo_t *sigInfo, void *context) {
    static volatile sig_atomic_t fatal_error_in_progress = 0;
    if (fatal_error_in_progress) //Stop a signal loop.
        _exit(1);
    fatal_error_in_progress = 1;

    char *j;
    asprintf(&j, "Crash Signal: %d, crashed on: %x, UID: %ld\n", signalNumber, (long) sigInfo->si_addr, (long) sigInfo->si_uid);  //%x prints out the faulty memory address in hex
    LOGE("%s", j);

//    getStackTrace();
//    sigaction(signalNumber, &oldPsa, NULL);
}

void initSignalHandler() {
    LOGI("Crash handler started");

    psa.sa_sigaction = handleCrash;
    psa.sa_flags = SA_SIGINFO;

    //sigaction(SIGBUS, &psa, &oldPsa);
    sigaction(SIGSEGV, &psa, &oldPsa);
    //sigaction(SIGSYS, &psa, &oldPsa);
    //sigaction(SIGFPE, &psa, &oldPsa);
    //sigaction(SIGILL, &psa, &oldPsa);
    //sigaction(SIGHUP, &psa, &oldPsa);
}

JNIEXPORT jint JNICALL Java_com_telenav_ffmpeg_FFMPEG_initial(JNIEnv *env, jobject obj, jint width, jint height, jstring file) {
    //FFmpeg av_log() callback
    av_log_set_callback(custom_log);
//    crashlytics_ctx = crashlytics_init();

//    initSignalHandler();
//    const char* message = "Native crash simulation";
//    crashlytics_ctx->log(crashlytics_ctx, message);
    /* initialize libavcodec, and register all codecs and formats */
    av_register_all();
    avfilter_register_all();

    jpg_inf = av_find_input_format("mjpeg");
    if (jpg_inf == NULL) {
        LOGI("probe failed\n");
    }
    jpg_avbuff = (unsigned char *) av_malloc(4096);

    AVIOContext *ioctx = avio_alloc_context(jpg_avbuff, 4096, 0, &jpg_buff_pos, &read_packet, NULL, NULL);

    jpg_fmt_ctx = avformat_alloc_context();
    jpg_fmt_ctx->pb = ioctx;
    if (avformat_open_input(&jpg_fmt_ctx, "memory/jpegdata", jpg_inf, NULL) != 0) {
        LOGI("error opening AVFormatContext\n");
    }

    jpg_codec_ctx = jpg_fmt_ctx->streams[0]->codec;
    jpg_codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;
    jpg_codec_ctx->color_range = AVCOL_RANGE_JPEG;
    jpg_codec = avcodec_find_decoder(jpg_codec_ctx->codec_id);
    avcodec_open2(jpg_codec_ctx, jpg_codec, NULL);

    const char *out_path = (*env)->GetStringUTFChars(env, file, 0);
    initializeEncoder(out_path, width, height);
    (*env)->ReleaseStringUTFChars(env, file, out_path);
    return 0;
}


JNIEXPORT int JNICALL Java_com_telenav_ffmpeg_FFMPEG_encode(JNIEnv *env, jobject obj, jbyteArray jpeg) {
    int ret;
    int enc_got_frame = 0;
    LOGI("Encoding frame");
    LOGI("----------------------------------------------");

    jbyte *in = (*env)->GetByteArrayElements(env, jpeg, 0);

    ret = decodeJpegData(in, (*env)->GetArrayLength(env, jpeg));

    if (ret < 0) {
        LOGE("Error while decoding frame");
        return -1;
    }
    LOGI("decoded jpeg data");
    pkt.data = NULL;
    pkt.size = 0;
    av_init_packet(&pkt);

    if (!&pkt || !yuvframe || !h264_codec_ctx) {
        LOGE("Error while encoding, not initialized correctly");
        return -1;
    }
    LOGI("Encoding frame with w %i h %i, to file with w %i h %i", yuvframe->width, yuvframe->height, h264_codec_ctx->width, h264_codec_ctx->height);
    if (yuvframe->height != h264_codec_ctx->height || yuvframe->width != h264_codec_ctx->width) {
        LOGE("Error while encoding frame, incorrect frame orientation");
        ret = -1;

        if (filter_graph) {
            avfilter_graph_free(&filter_graph);
        }
        goto skip;
    }

    LOGI("encoding video frame with height %i, width %i", yuvframe->height, yuvframe->width);
    ret = avcodec_encode_video2(h264_codec_ctx, &pkt, yuvframe, &enc_got_frame);
    LOGI("encoded video frame, success = %i", ret);
    skip:
    total_framecnt++;
    if (enc_got_frame == 1) {
        LOGI("Succeed to encode frame: %5d\tsize:%5d\n", framecnt, pkt.size);
        framecnt++;
        pkt.stream_index = video_st->index;

//        //Write PTS
//        AVRational time_base = ofmt_ctx->streams[0]->time_base;//{ 1, 1000 };
//        AVRational r_framerate1 = {12, 2};//{ 50, 2 };
//        AVRational time_base_q = {1, AV_TIME_BASE};
//        //Duration between 2 frames (us)
//        int64_t calc_duration = (double) (AV_TIME_BASE) * (1 / av_q2d(r_framerate1));
//        //Parameters
//        //enc_pkt.pts = (double)(framecnt*calc_duration)*(double)(av_q2d(time_base_q)) / (double)(av_q2d(time_base));
//        pkt.pts = av_rescale_q(framecnt * calc_duration, time_base_q, time_base);
//        pkt.dts = pkt.pts;
//        pkt.duration = av_rescale_q(calc_duration, time_base_q, time_base); //(double)(calc_duration)*(double)(av_q2d(time_base_q)) / (double)(av_q2d(time_base));
//        pkt.pos = -1;
        pkt.pts = framecnt - 1;
        pkt.dts = pkt.pts - 2;
        pkt.duration = 1; //(double)(calc_duration)*(double)(av_q2d(time_base_q)) / (double)(av_q2d(time_base));
        pkt.pos = -1;

//        ofmt_ctx->duration = pkt.duration * framecnt;

//        LOGI("pts = %f , dts = %f , dur = %i , totaldur = %f", (double) pkt.pts, (double) pkt.dts, pkt.duration, (double) ofmt_ctx->duration);
        //Delay
//        int64_t pts_time = av_rescale_q(pkt.dts, time_base, time_base_q);
//        int64_t now_time = av_gettime() - start_time;
//        if (pts_time > now_time)
//            av_usleep(pts_time - now_time);

        ret = av_interleaved_write_frame(ofmt_ctx, &pkt);
        LOGI("Wrote frame, result = %i", ret);
        av_packet_unref(&pkt);
        ret = 0;
    } else {
        LOGI("No frame yet.");
    }
    av_frame_free(&yuvframe);
    (*env)->ReleaseByteArrayElements(env, jpeg, in, JNI_ABORT);
    return ret;
}

JNIEXPORT jint JNICALL Java_com_telenav_ffmpeg_FFMPEG_close(JNIEnv *env, jobject obj) {
    LOGI("Closing encoder");
    LOGI("----------------------------------------------");
    if (filter_graph) {
        avfilter_graph_free(&filter_graph);
    }
    if (jpg_codec_ctx) {
        avcodec_close(jpg_codec_ctx);
    }
    if (sws_ctx) {
        sws_freeContext(sws_ctx);
        sws_ctx = NULL;
    }
    if (video_st)
        avcodec_close(video_st->codec);

    if (ofmt_ctx && ofmt_ctx->pb) {
        avio_close(ofmt_ctx->pb);
    }

    if (ofmt_ctx) {
        avformat_free_context(ofmt_ctx);
        ofmt_ctx = NULL;
    }

    if (jpg_fmt_ctx) {
        avformat_close_input(&jpg_fmt_ctx);
    }

    if (jpg_fmt_ctx) {
        if (jpg_fmt_ctx->pb)
            avio_close(jpg_fmt_ctx->pb);
        avformat_free_context(jpg_fmt_ctx);
        jpg_fmt_ctx = NULL;
    }
    if (total_framecnt == 0 && framecnt == 0) {
        return 1;
    }

    LOGI("Done");
    LOGI("----------------------------------------------");
    if (crashlytics_ctx) {
        crashlytics_free(&crashlytics_ctx);
    }
    return 0;
}

//void abortHandler( int signum, siginfo_t* si, void* unused )
//{
//    const char* name = NULL;
//    switch( signum )
//    {
//        case SIGABRT: name = "SIGABRT";  break;
//        case SIGSEGV: name = "SIGSEGV";  break;
//        case SIGBUS:  name = "SIGBUS";   break;
//        case SIGILL:  name = "SIGILL";   break;
//        case SIGFPE:  name = "SIGFPE";   break;
//        case SIGPIPE: name = "SIGPIPE";  break;
//    }
//
//    if ( name )
//        printf( stderr, "Caught signal %d (%s)\n", signum, name );
//    else
//        printf( stderr, "Caught signal %d\n", signum );
//
//    printStackTrace( stderr );
//
//    exit( signum );
//}
//
//void handleCrashes()
//{
//    struct sigaction sa;
//    sa.sa_flags = SA_SIGINFO;
//    sa.sa_sigaction = abortHandler;
//    sigemptyset( &sa.sa_mask );
//
//    sigaction( SIGABRT, &sa, NULL );
//    sigaction( SIGSEGV, &sa, NULL );
//    sigaction( SIGBUS,  &sa, NULL );
//    sigaction( SIGILL,  &sa, NULL );
//    sigaction( SIGFPE,  &sa, NULL );
//    sigaction( SIGPIPE, &sa, NULL );
//}