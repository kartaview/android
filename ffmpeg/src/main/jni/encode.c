#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/imgutils.h"
#include "crashlitics.h"
#include "crash_handler.h"

#include <jni.h>
#include <libswscale/swscale.h>
#include <libavutil/opt.h>

#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>

#ifdef ANDROID
#include <android/log.h>
#include "crash_handler.h"
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "FFMPEG E ", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "FFMPEG I ", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("FFMPEG E " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("FFMPEG I " format "\n", ##__VA_ARGS__)
#endif

#define FRAME_COUNT_LIMIT 64

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
const char *filter_flip_vert = "vflip,hflip";
AVFilterContext *buffersink_ctx;
AVFilterContext *buffersink_ctx_cc;
AVFilterContext *buffersink_ctx_flip;

AVFilterContext *buffersrc_ctx;
AVFilterContext *buffersrc_ctx_cc;
AVFilterContext *buffersrc_ctx_flip;

AVFilterGraph *filter_graph;
AVFilterGraph *filter_graph_cc;
AVFilterGraph *filter_graph_flip;

//for encoding
AVFormatContext *ofmt_ctx;
AVStream *video_st;
AVCodec *pCodec;
int framecnt = 0;
int total_framecnt = 0;
int FPS = 4;


//struct sigaction psa, oldPsa;


char *folder_path;

int video_index = -1;

void custom_log(void *ptr, int level, const char *fmt, va_list vl) {
    FILE *fp = fopen("/storage/emulated/0/Android/data/com.telenav.streetview/files/av_recording_log.txt", "a+");
    if (fp) {
        vfprintf(fp, fmt, vl);
        fflush(fp);
        fclose(fp);
    }
}

void remove_char(char *str, char c) {
    char *pr = str, *pw = str;
    while (*pr) {
        *pw = *pr++;
        pw += (*pw != c);
    }
    *pw = '\0';
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
    if (!ofmt_ctx) {
        LOGE("Could not create format CTX");
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
    if (avformat_write_header(ofmt_ctx, NULL) < 0) {
        LOGE("Failed to write header to output format context");
    }
    total_framecnt = 0;
    framecnt = 0;

    LOGI("Initialized file successfully, height %i, width %i\n", height, width);
    LOGI("----------------------------------------------");
    return 0;
}

int flush() {
    int ret;
    int got_frame;
    AVPacket enc_pkt;
    if (!ofmt_ctx || !ofmt_ctx->streams || !ofmt_ctx->streams[0] || !ofmt_ctx->streams[0]->codec || !ofmt_ctx->streams[0]->codec->codec) {
        return 0;
    }
    LOGI("Flushing encoder");
    LOGI("----------------------------------------------");
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
        enc_pkt.stream_index = video_st->index;

        AVRational time_base = ofmt_ctx->streams[0]->time_base;
        AVRational time_base_q = {1, AV_TIME_BASE};
        int64_t calc_duration = (int64_t) ((double) (AV_TIME_BASE) / (double) FPS);
        enc_pkt.pts = av_rescale_q(framecnt * calc_duration, time_base_q, time_base);
        enc_pkt.dts = enc_pkt.pts;
        enc_pkt.duration = av_rescale_q(calc_duration, time_base_q, time_base);
        enc_pkt.pos = -1;
        ofmt_ctx->duration = enc_pkt.duration * framecnt;
        /* mux encoded frame */
        if (ofmt_ctx && ofmt_ctx->streams && ofmt_ctx->streams[0]) {
            ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
        } else {
            ret = -1;
        }
        if (ret < 0)
            break;
        LOGI("Flush Encoder: Succeed to encode 1 frame!\tsize:%5d\n", enc_pkt.size);
    }
    //Write file trailer
    if (ofmt_ctx && ofmt_ctx->pb && framecnt > 0) {
        int retval = av_write_trailer(ofmt_ctx);
        LOGI("Writing file trailer %i", retval);
        if (retval < 0) {
            char arr[200];
            av_strerror(retval, (char *) &arr, 200);
            LOGE("Error while writing file trailer: %s", (char *) &arr);
        }
    }
    if (total_framecnt == 0 && framecnt == 0 && ofmt_ctx) {
        LOGI("entered remove file");
        remove(ofmt_ctx->filename);
        LOGI("finished remove file");
    }
    return 0;
}

int nextFile(jint width, jint height) {
    flush();
    char *out_path = (char *) malloc(1024);
    video_index++;
    int ret = video_index;
    sprintf(out_path, "%s%i.mp4\0", folder_path, video_index);
    remove_char(out_path, 11);//removing vertical tab
    LOGI("Creating new file %s", out_path);
    if (sws_ctx) {
        sws_freeContext(sws_ctx);
        sws_ctx = NULL;
    }


    if (h264_codec_ctx) {
        if (h264_codec_ctx->codec && h264_codec_ctx->codec == pCodec) {
            if (!h264_codec_ctx->codec || !h264_codec_ctx->codec->name) {
                LOGE("'kali crash codec is null while releasing");
            }
            avcodec_close(h264_codec_ctx);
        } else {
            LOGE("Codecs are not the same withing context");
        }
    }
    h264_codec_ctx = NULL;

    if (ofmt_ctx) {
//        if (ofmt_ctx->filename && strlen(ofmt_ctx->filename) != 0) {
//            free(ofmt_ctx->filename);
//        }
        if (ofmt_ctx->streams && ofmt_ctx->pb) {
            avio_close(ofmt_ctx->pb);
        }
        avformat_free_context(ofmt_ctx);
        ofmt_ctx = NULL;
        LOGI("KALI setting null ofmtcontext at nextfile");
    }
    int res = initializeEncoder(out_path, width, height);
    if (res < 0) {
        ret = res;
    }
    return ret;
}

static int init_filter(AVFilterGraph **filt_graph, AVFilterContext **bufsink_ctx, AVFilterContext **bufsrc_ctx, const char *filters_descr) {
    LOGI("Initializing filter %s", filters_descr);
    char args[512];
    int ret = 0;
    AVFilterGraph *filter_graph = *filt_graph;
    AVFilterContext *buffersink_ctx = *bufsink_ctx;
    AVFilterContext *buffersrc_ctx = *bufsrc_ctx;
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
        LOGE("Cannot create buffer source");
        goto end;
    }

    /* buffer video sink: to terminate the filter chain. */
    ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink, "out",
                                       NULL, NULL, filter_graph);
    if (ret < 0) {
        LOGE("Cannot create buffer sink");
        goto end;
    }

    ret = av_opt_set_int_list(buffersink_ctx, "pix_fmts", pix_fmts,
                              AV_PIX_FMT_NONE, AV_OPT_SEARCH_CHILDREN);
    if (ret < 0) {
        LOGE("Cannot set output pixel format");
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
    if (filter_graph) {
        *(filt_graph) = filter_graph;
        *(bufsink_ctx) = buffersink_ctx;
        *(bufsrc_ctx) = buffersrc_ctx;
    }
    return ret;
}


int filter(int rotation) {

    int ret_val;
    AVFilterGraph *filt_graph;
    AVFilterContext *buf_src_ctx;
    AVFilterContext *buf_sink_ctx;
    const char *filter_type;
    switch (rotation) {
        case 6:
            LOGI("Selecting cw rotate filter");
            filt_graph = filter_graph;
            buf_sink_ctx = buffersink_ctx;
            buf_src_ctx = buffersrc_ctx;
            filter_type = filter_rotate_cw;
            break;
        case 3:
            LOGI("Selecting vertical flip filter");
            filt_graph = filter_graph_flip;
            buf_sink_ctx = buffersink_ctx_flip;
            buf_src_ctx = buffersrc_ctx_flip;
            filter_type = filter_flip_vert;
            break;
        case 8:
            LOGI("Selecting ccw rotate filter");
            filt_graph = filter_graph_cc;
            buf_sink_ctx = buffersink_ctx_cc;
            buf_src_ctx = buffersrc_ctx_cc;
            filter_type = filter_rotate_ccw;
            break;
        case 1:
        default:
            LOGI("No need to rotate");
            return 0;
    }
    if (!filt_graph) {
        if ((init_filter(&filt_graph, &buf_sink_ctx, &buf_src_ctx, filter_type)) < 0) {
            LOGE("Filter init failed");
            return -1;
        }
        switch (rotation) {
            case 6:
                filter_graph = filt_graph;
                buffersink_ctx = buf_sink_ctx;
                buffersrc_ctx = buf_src_ctx;
                break;
            case 3:
                filter_graph_flip = filt_graph;
                buffersink_ctx_flip = buf_sink_ctx;
                buffersrc_ctx_flip = buf_src_ctx;
                break;
            case 8:
                filter_graph_cc = filt_graph;
                buffersink_ctx_cc = buf_sink_ctx;
                buffersrc_ctx_cc = buf_src_ctx;
                break;
        }
    }
    LOGI("Filtering...");
    if (av_buffersrc_add_frame_flags(buf_src_ctx, yuvframe, AV_BUFFERSRC_FLAG_KEEP_REF) < 0) {//AV_BUFFERSRC_FLAG_PUSH
        LOGE("Error while feeding the filtergraph");
        return -1;
    }
    AVFrame *filt_frame = av_frame_alloc();
    /* pull filtered frames from the filtergraph */
    while (1) {
        ret_val = av_buffersink_get_frame(buf_sink_ctx, filt_frame);
        if (ret_val == AVERROR(EAGAIN) || ret_val == AVERROR_EOF)
            return 0;// formatCheck;
        if (ret_val < 0)
            return -1; //goto formatCheck;
        if (yuvframe) {
            av_frame_free(&yuvframe);
        }
        yuvframe = filt_frame;
        break;
        LOGI("Applied filter");
    }
    return 0;
}

int *decodeJpegData(jbyte *jpeg, int length) {
    jpg_buff_len = length;
    jpg_buff = (unsigned char *) jpeg;
    jpg_buff_pos = 0;
    int *ret = (int *) malloc(2 * sizeof(int));
    ret[0] = -1;
    ret[1] = 0;
    yuvframe = av_frame_alloc();
    av_init_packet(&jpg_pkt);
    int frameFinished = 0;
    av_read_frame(jpg_fmt_ctx, &jpg_pkt);
    int ret2 = avcodec_decode_video2(jpg_codec_ctx, yuvframe, &frameFinished, &jpg_pkt);
    if (ret2 <= 0) {
        LOGI("error obtaining frame from byte array");
        ret[1] = -1;
        return ret;
    }
    AVDictionaryEntry *tag = av_dict_get(yuvframe->metadata, "Orientation", NULL, AV_DICT_MATCH_CASE);
    if (tag) {
        LOGI("ORIENTATION IS %s=%s\n", tag->key, tag->value);
        filter(atoi(tag->value));
    }
    int retval = 0;
    if (!h264_codec_ctx || yuvframe->height != h264_codec_ctx->height || yuvframe->width != h264_codec_ctx->width || framecnt >= FRAME_COUNT_LIMIT) {
        retval = nextFile(yuvframe->width, yuvframe->height);
    }
    if (retval < 0) {
        ret[0] = -1;
        return ret;
    }
    ret[0] = video_index;
    if (yuvframe->format != AV_PIX_FMT_YUVJ420P && yuvframe->format != AV_PIX_FMT_YUV420P) {
        LOGI("converting to proper color format...");
//        swsscale the shit out of this motherfucker
        if (!sws_ctx) {
            sws_ctx = sws_getContext(yuvframe->width, yuvframe->height, (enum AVPixelFormat) yuvframe->format,
                                     yuvframe->width, yuvframe->height, AV_PIX_FMT_YUV420P,
                                     SWS_BICUBIC, NULL, NULL, NULL);
        }
        AVFrame *temp = av_frame_alloc();
        av_image_fill_arrays(temp->data, temp->linesize, NULL, AV_PIX_FMT_YUVJ420P, yuvframe->width, yuvframe->height, 1);
        av_image_alloc(temp->data, temp->linesize, yuvframe->width, yuvframe->height, AV_PIX_FMT_YUVJ420P, 1);
        temp->format = AV_PIX_FMT_YUVJ420P;
        int ret3 = sws_scale(sws_ctx, (const uint8_t *const *) yuvframe->data, yuvframe->linesize, 0, yuvframe->height, temp->data, temp->linesize);
        if (ret3 <= 0) {
            LOGE("Something went wrong while color space conversion returned %i", ret3);
            av_frame_free(&temp);
            ret[1] = -1;
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
    if (&jpg_pkt) {
        av_packet_unref(&jpg_pkt);
    }
    LOGI("Decoded jpeg data successfully");
    return ret;
}


JavaVM *jvm;
jobject *jffmpeg;
jmethodID method;

void onError(){
    LOGE("FFMPEG caused a crash...");
    JNIEnv *env;
    jint rs = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    LOGI("JNIEnv attached %d success %s",rs, rs == JNI_OK ? "true" : "false");
    (*env)->CallVoidMethod(env,jffmpeg,method);
    LOGI("called method onerror");
    (*jvm)->DetachCurrentThread(jvm);
}


JNIEXPORT jint JNICALL Java_com_telenav_ffmpeg_FFMPEG_initial(JNIEnv *env, jobject obj, jstring folder) {
    //FFmpeg av_log() callback
    int ret = 0;
    av_log_set_callback(custom_log);

    (*env)->GetJavaVM(env, &jvm);
    jffmpeg = (*env)->NewGlobalRef(env, obj);
    jclass clazz = (*env)->FindClass(env,"com/telenav/ffmpeg/FFMPEG");
    method = (*env)->GetMethodID(env, clazz, "onerror", "()V");

    initSignalHandler(onError);

    /* initialize libavcodec, and register all codecs and formats */
    av_register_all();
    avfilter_register_all();

    jpg_inf = av_find_input_format("mjpeg");
    if (jpg_inf == NULL) {
        LOGI("probe failed\n");
        ret = -1;
    }
    jpg_avbuff = (unsigned char *) av_malloc(4096);

    AVIOContext *ioctx = avio_alloc_context(jpg_avbuff, 4096, 0, &jpg_buff_pos, &read_packet, NULL, NULL);

    jpg_fmt_ctx = avformat_alloc_context();
    jpg_fmt_ctx->pb = ioctx;
    if (avformat_open_input(&jpg_fmt_ctx, "memory/jpegdata", jpg_inf, NULL) != 0) {
        LOGI("error opening AVFormatContext\n");
        ret = -1;
    }

    jpg_codec_ctx = jpg_fmt_ctx->streams[0]->codec;
    jpg_codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;
    jpg_codec_ctx->color_range = AVCOL_RANGE_JPEG;
    jpg_codec = avcodec_find_decoder(jpg_codec_ctx->codec_id);
    if (avcodec_open2(jpg_codec_ctx, jpg_codec, NULL) < 0) {
        ret = -1;
    }
    video_index = -1;
    const char *temp = (*env)->GetStringUTFChars(env, folder, 0);
    folder_path = (char *) malloc(1024);
    size_t length = strlen(temp);
    strncpy(folder_path, temp, length);
    folder_path[length] = '\0';
    (*env)->ReleaseStringUTFChars(env, folder, temp);
    return ret;
}


JNIEXPORT jintArray JNICALL Java_com_telenav_ffmpeg_FFMPEG_encode(JNIEnv *env, jobject obj, jbyteArray jpeg) {
    int *ret;
    int enc_got_frame = 0;
    LOGI("Encoding frame");
    LOGI("----------------------------------------------");

    jbyte *in = (*env)->GetByteArrayElements(env, jpeg, 0);

    ret = decodeJpegData(in, (*env)->GetArrayLength(env, jpeg));

    if (ret[0] < 0 || ret[1] < 0) {
        LOGE("Error while decoding frame");
        goto returning;
    }
    pkt.data = NULL;
    pkt.size = 0;
    av_init_packet(&pkt);

    if (!&pkt || !yuvframe || !h264_codec_ctx) {
        LOGE("Error while encoding, not initialized correctly");
        goto returning;
    }
//    LOGI("Encoding frame with w %i h %i, to file with w %i h %i", yuvframe->width, yuvframe->height, h264_codec_ctx->width, h264_codec_ctx->height);
    if (yuvframe->height != h264_codec_ctx->height || yuvframe->width != h264_codec_ctx->width) {
        LOGE("Error while encoding frame, incorrect frame orientation");
        ret[1] = -1;
        goto skip;
    }

//    LOGI("encoding video frame with height %i, width %i", yuvframe->height, yuvframe->width);
    if (avcodec_encode_video2(h264_codec_ctx, &pkt, yuvframe, &enc_got_frame) < 0) {
        ret[1] = -1;
    }
    LOGI("encoded video frame, success = %i", ret[1]);
    skip:
    total_framecnt++;
    if (enc_got_frame == 1) {
        LOGI("Succeed to encode frame: %5d\tsize:%5d\n", framecnt, pkt.size);
        ret[1] = framecnt;
        framecnt++;
        pkt.stream_index = video_st->index;

        AVRational time_base = ofmt_ctx->streams[0]->time_base;
        AVRational time_base_q = {1, AV_TIME_BASE};
        int64_t calc_duration = (int64_t) ((double) (AV_TIME_BASE) / (double) FPS);
        pkt.pts = av_rescale_q(framecnt * calc_duration, time_base_q, time_base);
        pkt.dts = pkt.pts;
        pkt.duration = av_rescale_q(calc_duration, time_base_q, time_base);
        pkt.pos = -1;
        ofmt_ctx->duration = pkt.duration * framecnt;
//        LOGI("pts = %f , dts = %f , dur = %i , totaldur = %f",(double) pkt.pts,(double) pkt.dts,pkt.duration,(double) ofmt_ctx->duration);

        if (ofmt_ctx && ofmt_ctx->streams && ofmt_ctx->streams[0]) {
            int ret2 = av_interleaved_write_frame(ofmt_ctx, &pkt);
            LOGI("Wrote frame, result = %i", ret2);
            av_packet_unref(&pkt);
            if (ret2 < 0) {
                LOGE("Error writing frame");
                ret[1] = -1;
            }
            if (!(ofmt_ctx && ofmt_ctx->streams && ofmt_ctx->streams[0])) {
                LOGI("KALI ofmt_ctx is broken after interleaved write frame");
            }
        } else {
            if (ofmt_ctx)
                LOGE("File format CTX is NULL");
            else if (ofmt_ctx->streams)
                LOGE("File format CTX streams are NULL");
            else if (ofmt_ctx->streams[0])
                LOGE("File format CTX stream[0] is NULL");
            ret[1] = -1;
        }
    } else {
        LOGI("No frame yet.");
    }
    returning:
    if (yuvframe) {
        av_frame_free(&yuvframe);
    }
    (*env)->ReleaseByteArrayElements(env, jpeg, in, JNI_ABORT);

    jintArray retArray = (*env)->NewIntArray(env, 2);
    (*env)->SetIntArrayRegion(env, retArray, 0, 2, ret);
    return retArray;
}

JNIEXPORT jint JNICALL Java_com_telenav_ffmpeg_FFMPEG_close(JNIEnv *env, jobject obj) {
    flush();
    LOGI("Closing encoder");
    LOGI("----------------------------------------------");
    if (filter_graph) {
        avfilter_graph_free(&filter_graph);
    }
    if (filter_graph_cc) {
        avfilter_graph_free(&filter_graph_cc);
    }
    if (filter_graph_flip) {
        avfilter_graph_free(&filter_graph_flip);
    }
    if (jpg_codec_ctx) {
        if (!jpg_codec_ctx->codec || !jpg_codec_ctx->codec->name) {
            LOGE("'kali crash codec is null while releasing jpeg");
        }
        avcodec_close(jpg_codec_ctx);
        LOGI("Closed jpeg decoder");
    }
    if (sws_ctx) {
        sws_freeContext(sws_ctx);
        sws_ctx = NULL;
    }

    if (h264_codec_ctx) {
        if (h264_codec_ctx->codec && h264_codec_ctx->codec == pCodec) {
            if (!h264_codec_ctx->codec || !h264_codec_ctx->codec->name || !pCodec) {
                LOGE("'kali crash codec is null while releasing");
            }
            avcodec_close(h264_codec_ctx);
            LOGI("Closed h264 encoder");
        } else {
            LOGE("Codecs are not the same withing context");
        }
    }
    h264_codec_ctx = NULL;

    if (ofmt_ctx) {
//        if (ofmt_ctx->filename && strlen(ofmt_ctx->filename) != 0) {
//            free(ofmt_ctx->filename);
//        }
        if (ofmt_ctx->streams && ofmt_ctx->pb) {
            avio_close(ofmt_ctx->pb);
        }
        avformat_free_context(ofmt_ctx);
        ofmt_ctx = NULL;
        LOGI("KALI setting null ofmtcontext at close");
    }

    if (jpg_fmt_ctx) {
        avformat_close_input(&jpg_fmt_ctx);
    }

    if (jpg_fmt_ctx) {
        if (jpg_fmt_ctx->pb && jpg_fmt_ctx->pb->buffer){
            av_freep(jpg_fmt_ctx->pb->buffer);
        }
        if (jpg_fmt_ctx->pb)
            avio_close(jpg_fmt_ctx->pb);
        avformat_free_context(jpg_fmt_ctx);
        jpg_fmt_ctx = NULL;
    }
    if (total_framecnt == 0 && framecnt == 0) {
        return 1;
    }
    if (folder_path) {
        free(folder_path);
    }

    LOGI("Done");
    LOGI("----------------------------------------------");
    if (crashlytics_ctx) {
        crashlytics_free(&crashlytics_ctx);
    }
    if (jffmpeg){
        (*env)->DeleteGlobalRef(env, jffmpeg);
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