#include "ffmpeg_mediaplayer.h"

void packet_queue_init(PacketQueue *q) {
    memset(q, 0, sizeof(PacketQueue));
    q->initialized = 1;
    q->mutex = SDL_CreateMutex();
    q->cond = SDL_CreateCond();
}

int packet_queue_put(VideoState *is, PacketQueue *q, AVPacket *pkt, int index) {
//    LOGI("Packet queue put");
    AVPacketList *pkt1;
//    if (pkt != &is->flush_pkt && av_dup_packet(pkt) < 0) {
//        return -1;
//    }
    pkt1 = av_malloc(sizeof(AVPacketList));
//    if (!pkt1)
//        return -1;
    pkt1->pkt = *pkt;
    pkt1->index = index;
    pkt1->next = NULL;

    if (!q->last_pkt)
        q->first_pkt = pkt1;
    else
        q->last_pkt->next = pkt1;
    q->last_pkt = pkt1;
    q->nb_packets++;
    q->size += pkt1->pkt.size;
//    SDL_CondSignal(q->cond);
//    LOGI("PUT Packet queue condition signaled");
    return 0;
}

static int packet_queue_get(VideoState *is, PacketQueue *q, AVPacket *pkt) {
//    LOGI("Packet queue get");
    AVPacketList *pkt1;
    int ret;

    for (; ;) {
        pkt1 = q->first_pkt;
        if (pkt1) {
            q->first_pkt = pkt1->next;
            if (!q->first_pkt)
                q->last_pkt = NULL;
            q->nb_packets--;
            q->size -= pkt1->pkt.size;
            *pkt = pkt1->pkt;
            av_free(pkt1);
            ret = pkt1->index;
            if (ret == -1){
                LOGI("Packet queue returning exit packet");
            }
//            LOGI("Packet queue returning %d", ret);
            break;
        } else {
            SDL_Delay(10);
            continue;
        }
    }
    return ret;
}

static void packet_queue_flush(PacketQueue *q) {
    AVPacketList *pkt, *pkt1;

    SDL_LockMutex(q->mutex);
    for (pkt = q->first_pkt; pkt != NULL; pkt = pkt1) {
        pkt1 = pkt->next;
        av_packet_unref(&pkt->pkt);
        av_freep(&pkt);
    }
    q->last_pkt = NULL;
    q->first_pkt = NULL;
    q->nb_packets = 0;
    q->size = 0;
    SDL_UnlockMutex(q->mutex);
}

void alloc_picture(VideoState **ps) {

    VideoState *is = *ps;
    VideoPicture *vp;

    vp = &is->pictq[is->pictq_windex];
    if (vp->bmp) {
        // we already have one make another, bigger/smaller
        destroyBmp(vp->bmp);
    }
    // Allocate a place to put our YUV image on that screen
    vp->bmp = createBmp(&is->video_player, is->video_st->codec->width, is->video_st->codec->height);

    vp->width = is->video_st->codec->width;
    vp->height = is->video_st->codec->height;

    SDL_LockMutex(is->pictq_mutex);
    vp->allocated = 1;
    SDL_CondSignal(is->pictq_cond);
    SDL_UnlockMutex(is->pictq_mutex);

}

int queue_picture(VideoState *is, AVFrame *pFrame, int index) {

    VideoPicture *vp;

    // wait until we have space for a new pic
    SDL_LockMutex(is->pictq_mutex);
    while (is->pictq_size >= VIDEO_PICTURE_QUEUE_SIZE &&
           !is->quit) {
        SDL_CondWait(is->pictq_cond, is->pictq_mutex);
    }
    SDL_UnlockMutex(is->pictq_mutex);

    // windex is set to 0 initially
    vp = &is->pictq[is->pictq_windex];

    // allocate or resize the buffer!
    if (index >= 0 && (!vp->bmp ||
        vp->width != is->video_st->codec->width ||
        vp->height != is->video_st->codec->height)) {

        vp->allocated = 0;
        alloc_picture(&is);

        // wait until we have a picture allocated
        SDL_LockMutex(is->pictq_mutex);
        while (!vp->allocated && !is->quit) {
            SDL_CondWait(is->pictq_cond, is->pictq_mutex);
        }
        SDL_UnlockMutex(is->pictq_mutex);
    }

    if (vp->bmp) {
        SDL_LockMutex(is->display_mutex);
        updateBmp(&is->video_player, is->sws_ctx, is->video_st->codec, vp->bmp, pFrame, is->video_st->codec->width, is->video_st->codec->height);
        SDL_UnlockMutex(is->display_mutex);
    }
    vp->index = index;
    // now we inform our display thread that we have a pic ready
    if (++is->pictq_windex == VIDEO_PICTURE_QUEUE_SIZE) {
        is->pictq_windex = 0;
    }
    SDL_LockMutex(is->pictq_mutex);
    is->pictq_size++;
//     LOGI("--------------------------------Queue Picture %i for %i.mp4", index, is->file_index);
    SDL_UnlockMutex(is->pictq_mutex);
    return index;
}

void video_display(VideoState *is) {

    VideoPicture *vp;
    SDL_LockMutex(is->display_mutex);

    vp = &is->pictq[is->pictq_rindex];
    if (vp && vp->bmp && vp->index > 0) {
        displayBmp(&is->video_player, vp->bmp, is->video_st->codec, is->video_st->codec->width, is->video_st->codec->height);
        if (vp->bmp && vp->bmp->buffer) {
//            LOGI("Releasing frame buffer %p for file %i", vp->bmp->buffer,is->file_index);
            av_free(vp->bmp->buffer);
            vp->bmp->buffer = NULL;
        }
    }

    SDL_UnlockMutex(is->display_mutex);
}

void display_thread(void *opaque) {
    VideoState *is = (VideoState *) opaque;
//    LOGI("Starting display thread for %d", is->file_index);

    VideoPicture *vp;

    for (; ;) {
        if (is->paused) {
//            LOGI("Playback Paused");
            if (is->step_req_display){
                is->step_req_display = 0;
            } else {
                SDL_Delay(10);
                continue;
            }
        }
        if (is->pictq_size == 0) {
//            LOGI("Playback Paused");
            SDL_Delay(10);
            continue;
        }

        if (is->video_st) {
            vp = &is->pictq[is->pictq_rindex];

            /* show the picture! */
            int index = vp->index;
            video_display(is);

            /* update queue for next picture! */
            if (++is->pictq_rindex == VIDEO_PICTURE_QUEUE_SIZE) {
                is->pictq_rindex = 0;
            }
            SDL_LockMutex(is->pictq_mutex);
            is->pictq_size--;
//            LOGI("----------------------------------------Dequeue picture %i for %i.mp4", index, is->file_index);
            SDL_CondSignal(is->pictq_cond);
            SDL_UnlockMutex(is->pictq_mutex);

            if (index < 0){
                break;
            }
            is->last_frame = index;
            notify_from_thread(is, MEDIA_ON_FRAME, is->file_index, is->last_frame);
            if (*is->seeking) {
                SDL_Delay((Uint32) 5);
            } else {
                if (is->fps_delay_ptr) {
                    SDL_Delay((Uint32) *is->fps_delay_ptr);
                } else {
                    SDL_Delay((Uint32) 200);
                }
            }
            continue;
        } else {
            SDL_Delay(100);
            continue;
        }
    }
    SDL_LockMutex(is->display_mutex);
    int i;
    for (i = 0; i < VIDEO_PICTURE_QUEUE_SIZE; i++){
        VideoPicture pict = is->pictq[i];
        if (pict.allocated && pict.bmp && pict.bmp->buffer){
            if (pict.bmp->buffer) {
                LOGI("Releasing frame buffer %p for file %i", pict.bmp->buffer,is->file_index);
                av_free(pict.bmp->buffer);
                pict.bmp->buffer = NULL;
            }
        }
    }
    SDL_UnlockMutex(is->display_mutex);
    if (is->eof) {
        if (*is->backwards) {
            if (is->previous) {
                LOGI("Starting previous file");
                notify_from_thread(is, MEDIA_ON_PREVIOUS_FILE, ((VideoState *)is->previous)->file_index, is->paused);
            } else {
                notify_from_thread(is, MEDIA_PLAYBACK_COMPLETE, 0, 0);
            }
        } else {
            if (is->next) {
                LOGI("Starting next file");
                notify_from_thread(is, MEDIA_ON_NEXT_FILE, ((VideoState *)is->next)->file_index, is->paused);
            } else {
                notify_from_thread(is, MEDIA_PLAYBACK_COMPLETE, 0, 0);
            }
        }
    }

    LOGI("Exiting display frame thread");
}

int frame_decode_thread(void *arg) {
    VideoState *is = (VideoState *) arg;
//    LOGI("Starting frame decode thread for %d", is->file_index);
    AVPacket pkt1, *packet = &pkt1;
    int frameFinished;
    AVFrame *pFrame;

    pFrame = av_frame_alloc();

    for (; ;) {

//        LOGI("Frame decode thread trying get");
        int pkt_index = packet_queue_get(is, &is->videoq, packet);
        if (pkt_index < 0) {
            // means we quit getting packets
            queue_picture(is, pFrame, pkt_index);
//            av_packet_unref(packet);
            break;
        }
//        LOGI("---------Video Thread getting packet %i", pkt_index);
        if (packet->data == is->flush_pkt.data) {
            LOGI("Flushing on video thread");
            avcodec_flush_buffers(is->video_st->codec);
            continue;
        }
        // Decode video frame
        int ret = avcodec_decode_video2(is->video_st->codec, pFrame, &frameFinished, packet);//todo memory leak 8 mb + 2mb + 2mb per file
        if (ret <= 0) {
//            LOGI("Video Thread decoded packet unsuccessful for %i ", pkt_index);
        }

        // Did we get a video frame?
        if (frameFinished) {
//            LOGI("-----------------Video Thread Frame finished decoding %i for %i.mp4", pkt_index, is->file_index);
            if (queue_picture(is, pFrame, pkt_index) < 0) {
                av_packet_unref(packet);
                break;
            }
            if (is->eof && is->flush_counter > 0) {

//                is->flush_counter--;
            }
        } else {
            LOGI("Decode Thread Frame Not finished successful yet %i  error %i", pkt_index, ret);
            if (!is->eof) {
//                is->flush_counter++;
            }
        }
        av_packet_unref(packet);
    }
    av_frame_free(&pFrame);
    av_free(pFrame);
    LOGI("Exiting decode frame thread");
    return 0;
}

int stream_component_open(VideoState *is, int stream_index) { // Todo 1.59 mb memory leak per file

    AVFormatContext *pFormatCtx = is->pFormatCtx;
    AVCodecContext *codecCtx = NULL;
    AVCodec *codec = NULL;
    AVDictionary *optionsDict = NULL;

    if (stream_index < 0 || stream_index >= pFormatCtx->nb_streams) {
        return -1;
    }

    // Get a pointer to the codec context for the video stream
    codecCtx = pFormatCtx->streams[stream_index]->codec;
    codecCtx->gop_size = 0;
    codecCtx->delay = 0;
    codecCtx->active_thread_type = FF_THREAD_SLICE;
    codecCtx->thread_type = FF_THREAD_SLICE;
    codecCtx->i_quant_offset = 0;
    codecCtx->i_quant_factor = 0;
    if (codecCtx->codec_type == AVMEDIA_TYPE_VIDEO) {
        // Set video settings from codec info
        VideoPlayer *player = malloc(sizeof(VideoPlayer));
        is->video_player = player;
        createVideoEngine(&is->video_player);
        codec = avcodec_find_decoder(codecCtx->codec_id);
        if (!codec || (avcodec_open2(codecCtx, codec, &optionsDict) < 0)) {
            fprintf(stderr, "Unsupported codec!\n");
            return -1;
        }
        is->videoStream = stream_index;
        is->video_st = pFormatCtx->streams[stream_index];
        is->frame_count = is->video_st->nb_frames;
        is->frame_dur = av_rescale_q(is->video_st->duration / is->video_st->nb_frames, is->video_st->time_base, AV_TIME_BASE_Q);;

        packet_queue_init(&is->videoq);

        createScreen(&is->video_player, is->native_window);

        is->sws_ctx = createScaler(&is->video_player, is->video_st->codec);
    }
    return 0;
}

int packet_read_thread(void *arg) {
    VideoState *is = (VideoState *) arg;
//    LOGI("Starting packet read thread for %d", is->file_index);
    AVPacket pkt1, *packet = &pkt1;
    int ret;
    if (!is) {
        return -1;
    }
    if (is->videoStream < 0) {
        LOGE("%s: could not open codecs", is->filename);
        notify_from_thread(is, MEDIA_ERROR, 0, 0);
        return 0;
    }

    is->started = 1;
    notify_from_thread(is, MEDIA_SET_VIDEO_SIZE, is->video_st->codec->width, is->video_st->codec->height);
    notify_from_thread(is, MEDIA_PLAYBACK_PLAYING, is->file_index, is->pkt_index);
    // main decode loop
    for (; ;) {
        if (is->quit) {
//            LOGI("Quit is set, sending exit packet for file %d", is->file_index);
            packet->data = NULL;
            packet->size = 0;
            int res = packet_queue_put(is, &is->videoq, packet, -1);
            break;
        }
        if (is->paused != is->last_paused) {
            is->last_paused = is->paused;
            if (is->paused) {
                LOGI("Decode Paused");
                notify_from_thread(is, MEDIA_PLAYBACK_PAUSED, is->file_index, is->pkt_index);
            } else {
                LOGI("Decode Resumed");
                notify_from_thread(is, MEDIA_PLAYBACK_PLAYING, is->file_index, is->pkt_index);
            }
        }
        if (is->paused) {
//            LOGI("Playback Paused");
            if (is->step_req_read){
                is->step_req_read = 0;
                is->step_req_decode = 0;
//                if (*is->backwards){
//                    seekTo_l(&is,is->pkt_index-2);
//                }
            } else {
                SDL_Delay(10);
                continue;
            }
        }
//        if (is->step_req){
//
//        }
        // seek stuff goes here
        if (is->seek_req) {
            int64_t seek_target = is->seek_pos;
            int64_t seek_min = is->seek_rel > 0 ? seek_target - is->seek_rel + 2 : INT64_MIN;
            int64_t seek_max = is->seek_rel < 0 ? seek_target - is->seek_rel - 2 : INT64_MAX;

            int retseek = avformat_seek_file(is->pFormatCtx, -1, seek_target, seek_target, seek_target, is->seek_flags);
            if (retseek < 0) {
                LOGE("%s: error while seeking\n", is->pFormatCtx->filename);
                notify_from_thread(is, MEDIA_SEEK_COMPLETE, retseek, 0);
            } else {
                if (is->videoStream >= 0) {
//                    packet_queue_flush(&is->videoq);
                    packet_queue_put(is, &is->videoq, &is->flush_pkt, 0);
                }
                LOGI("Completed seek request");
                notify_from_thread(is, MEDIA_SEEK_COMPLETE, 0, 0);
            }
            is->seek_req = 0;
            if (!(*is->backwards && is->pkt_index == 0)){
                is->eof = 0;
            }
        }

//        if (is->videoq.nb_packets >= MAX_VIDEOQ_NR && !is->started) {
//            is->started = 1;
//            notify_from_thread(is, MEDIA_PLAYBACK_PLAYING, 0, 0);
//            VideoState *prev = getPreviousMediaPlayer(&is);
//            if (prev) {
//                while (prev->pictq_size > 0 || (prev != is && !prev->quit)) {
//                    LOGI("waiting for last frames to be rendered");
//                    SDL_Delay(1);
//                }
//                LOGI("Playing next file");
//            }
////            else {
////                LOGI("Playing first file");
////                is->vpaused = 0;
////                pause_l(&is);
////                continue;
////            }
//
//        }

        if (is->videoq.nb_packets >= MAX_VIDEOQ_NR) {
//            LOGI("Loaded max frames, sleeping...");
            if (is->paused){
                is->step_req_read = 1;
                is->step_req_decode = 1;
            }
            SDL_Delay(10);
            continue;
        }
        if ((ret = av_read_frame(is->pFormatCtx, packet)) < 0) {
            if (ret == AVERROR_EOF || is->pFormatCtx->pb->eof_reached) {
                LOGI("entered end of file clause");
                is->eof = 1;
            } else if (ret == NO_MEMORY) {
                LOGI("NO MEMORY, read packet failing");
            } else if (is->pFormatCtx->pb->error == 0) {
                LOGI("No error, wait for user input");
                SDL_Delay(100); /* no error; wait for user input */
                continue;
            }
        } else {
            if (packet->duration > 0) {
                is->pkt_index = (int) (packet->pts / packet->duration) - 1;
            }
            if (*is->backwards && is->pkt_index == 0 && !is->eof){
                packet_queue_put(is, &is->videoq, packet, is->pkt_index);
                is->eof = 1;
                continue;
            }
//            LOGI("-----Read frame %i for %i.mp4", is->pkt_index, is->file_index);
        }

        if (is->eof) {
            LOGI("EOF reached");
            packet->data = NULL;
            packet->size = 0;
            int res = packet_queue_put(is, &is->videoq, packet, -2);
            break;
        }
//        else if (is->eof) {
//            packet->data = NULL;
//            packet->size = 0;
//            int res = packet_queue_put(is, &is->videoq, packet, is->pkt_index);
//            LOGI("FLUSHING %i result %i", is->pkt_index, res);
//        }
        if (packet->stream_index == is->videoStream) {
            packet_queue_put(is, &is->videoq, packet, is->pkt_index);
        } else {
            LOGI("Packet not valid, unreferencing");
            av_packet_unref(packet);
        }
        if (*is->backwards){
            seekTo_l(&is,is->pkt_index);
        }
    }

    is->started = 0;
    LOGI("Exiting read frame thread");
    return 0;
}

void stream_seek(VideoState *is, int64_t pos, int64_t rel, int seek_by_bytes) {
    if (!is->seek_req) {
        LOGI("Seek requested to %"
                     PRId64, pos);
        is->seek_pos = pos;
        is->seek_rel = rel;
        is->seek_flags &= ~AVSEEK_FLAG_BYTE;
        if (seek_by_bytes)
            is->seek_flags |= AVSEEK_FLAG_BYTE;
        is->seek_req = 1;
    }
}

VideoState *create() {
    VideoState *is;

    is = av_mallocz(sizeof(VideoState));
    is->last_paused = -1;
    is->pkt_index = 0;
    is->fps_delay_ptr = 0;
    is->backwards = 0;
    is->seeking = 0;
    is->native_window = 0;

    return is;
}

void disconnect(VideoState **ps) {
    VideoState *is = *ps;

    LOGI("Disconnect, closing contexts for %i", is);
    if (is) {
        if (is->pFormatCtx) {
            avformat_close_input(&is->pFormatCtx);
            if (is->pFormatCtx) {
                avformat_free_context(is->pFormatCtx);
            }
            is->pFormatCtx = NULL;
        }
        if (is->avbuffer) {
            av_free(is->avbuffer);
        }

        if (is->videoq.initialized == 1) {
            if (is->videoq.first_pkt) {
                free(is->videoq.first_pkt);
            }

            if (is->videoq.last_pkt) {
                //free(is->videoq.last_pkt);
            }

            if (is->videoq.mutex) {
                free(is->videoq.mutex);
                is->videoq.mutex = NULL;
            }

            if (is->videoq.cond) {
                free(is->videoq.cond);
                is->videoq.cond = NULL;
            }

            is->videoq.initialized = 0;
        }

        //VideoPicture    pictq[VIDEO_PICTURE_QUEUE_SIZE];

        if (is->pictq_mutex) {
            free(is->pictq_mutex);
            is->pictq_mutex = NULL;
        }

        if (is->display_mutex) {
            free(is->display_mutex);
            is->display_mutex = NULL;
        }

        if (is->pictq_cond) {
            free(is->pictq_cond);
            is->pictq_cond = NULL;
        }

        if (is->parse_tid) {
            free(is->parse_tid);
            is->parse_tid = NULL;
        }

        if (is->frame_decode_tid) {
            free(is->frame_decode_tid);
            is->frame_decode_tid = NULL;
        }

        if (is->io_context) {
            av_free(is->io_context);
            is->io_context = NULL;
        }

        if (is->sws_ctx) {
            sws_freeContext(is->sws_ctx);
            is->sws_ctx = NULL;
        }

        av_packet_unref(&is->flush_pkt);

        av_freep(&is);
        *ps = NULL;
    }
}

int setDataSourceURI(VideoState **ps, const char *url) {
//    printf("setDataSource\n");

    if (!url) {
        return INVALID_OPERATION;
    }

    VideoState *is = *ps;

    // Workaround for FFmpeg ticket #998
    // "must convert mms://... streams to mmsh://... for FFmpeg to work"
    char *restrict_to = strstr(url, "mms://");
    if (restrict_to) {
        strncpy(restrict_to, "mmsh://", 6);
        puts(url);
    }

    char str[sizeof(is->filename)];
    strncpy(is->filename, url, sizeof(is->filename));
    strncpy(str, url, sizeof(is->filename));
    char *token = strtok(str, "/");
    while (token) {
        char saved[20];
        strcpy(saved, token);
        token = strtok(NULL, "/");
        if (!token) {
            char *index;
            index = strtok(saved, ".");
            if (index) {
                int num = atoi(index);
                if (num >= 0) {
                    is->file_index = num;
                }
            }
            break;
        }
    }

    return NO_ERROR;
}

int setListener(VideoState **ps, void *clazz, void (*listener)(void *, int, int, int, int)) {
    VideoState *is = *ps;

    is->clazz = clazz;
    is->notify_callback = listener;

    return NO_ERROR;
}

int prepare(VideoState **ps) {
    VideoState *is = *ps;

    int ret = prepareAsync_l(ps);
    if (ret != NO_ERROR) {
        return ret;
    }
    while (!is->prepared) {
        sleep(1);
    }
    return 0;
}

int start(VideoState **ps) {
    VideoState *is = *ps;
    if (is) {
        is->paused = 0;
        if (!is->parse_tid) {
            startPlayback(is);
        }
        return NO_ERROR;
    }

    return INVALID_OPERATION;
}

int stop(VideoState **ps) {
    VideoState *is = *ps;

    if (is) {
        is->quit = 1;
        /*
         * If the video has finished playing, then both the picture and
         * audio queues are waiting for more data.  Make them stop
         * waiting and terminate normally.
         */


        if (is->parse_tid) {
            pthread_join(*(is->parse_tid), NULL);
            LOGI("Joining parse thread");
        }

        if (is->videoq.initialized == 1) {
            SDL_CondSignal(is->videoq.cond);
            LOGI("Signaling condition for stopping");
        }

        if (is->frame_decode_tid) {
            pthread_join(*(is->frame_decode_tid), NULL);
            LOGI("Joining decode thread");
        }

        if (is->display_tid) {
            pthread_join(*(is->display_tid), NULL);
            LOGI("Joining display thread");
        }

        reset(&is);

        return NO_ERROR;
    }

    return INVALID_OPERATION;
}

int pause_l(VideoState **ps) {
    VideoState *is = *ps;

    if (is) {
        is->paused = !is->paused;
        return NO_ERROR;
    }

    return INVALID_OPERATION;
}

int stepForward(VideoState **ps) {
    VideoState *is = *ps;

    if (is) {
        is->paused = 1;
        is->step_req_display = 1;
        is->step_req_decode = 1;
        is->step_req_read = 1;
        return NO_ERROR;
    }

    return INVALID_OPERATION;
}

int stepBackward(VideoState **ps) {
    VideoState *is = *ps;

    if (is) {
        is->paused = 1;
        is->step_req_display = 1;
        is->step_req_decode = 1;
        is->step_req_read = 1;
        return NO_ERROR;
    }

    return INVALID_OPERATION;
}

int isPlaying(VideoState **ps) {
    VideoState *is = *ps;

    if (is) {
        if (!is->player_started) {
            return 0;
        } else {
            return !is->paused;
        }
    }

    return 0;
}

int getVideoWidth(VideoState **ps, int *w) {
    VideoState *is = *ps;

    if (!is || !is->video_st) {
        return INVALID_OPERATION;
    }

    *w = is->video_st->codec->width;

    return NO_ERROR;
}

int getVideoHeight(VideoState **ps, int *h) {
    VideoState *is = *ps;

    if (!is || !is->video_st) {
        return INVALID_OPERATION;
    }

    *h = is->video_st->codec->height;

    return NO_ERROR;
}

int seekTo(VideoState **ps, int fr_index) {
    int result = seekTo_l(ps, fr_index);
    return result;
}

int getCurrentPosition(VideoState **ps, int *msec) {
    VideoState *is = *ps;

    if (is) {
        *msec = is->pkt_index * 250;//todo incomplete
        return NO_ERROR;
    }

    return INVALID_OPERATION;
}

int getDuration(VideoState **ps, int *msec) {
    return getDuration_l(ps, msec);
}

int reset(VideoState **ps) {
    VideoState *is = *ps;

    if (is) {
        is->quit = 0;
        /*
         * If the video has finished playing, then both the picture and
         * audio queues are waiting for more data.  Make them stop
         * waiting and terminate normally.
         */

        if (is->videoq.initialized == 1) {
            SDL_CondSignal(is->videoq.cond);
        }

        if (is->display_tid) {
            pthread_join(*(is->display_tid), NULL);
        }

        if (is->parse_tid) {
            pthread_join(*(is->parse_tid), NULL);
        }

        if (is->frame_decode_tid) {
            pthread_join(*(is->frame_decode_tid), NULL);
        }

        is->seek_req = 0;
        is->seek_flags = 0;
        is->seek_pos = 0;
        is->seek_rel = 0;

        is->pictq_size = 0;
        is->pictq_rindex = 0;
        is->pictq_windex = 0;

        if (is->pictq_mutex) {
            free(is->pictq_mutex);
            is->pictq_mutex = NULL;
        }

        if (is->display_mutex) {
            free(is->display_mutex);
            is->display_mutex = NULL;
        }

        if (is->pictq_cond) {
            free(is->pictq_cond);
            is->pictq_cond = NULL;
        }

        if (is->display_tid) {
            free(is->display_tid);
            is->display_tid = NULL;
        }

        if (is->parse_tid) {
            free(is->parse_tid);
            is->parse_tid = NULL;
        }

        if (is->frame_decode_tid) {
            free(is->frame_decode_tid);
            is->frame_decode_tid = NULL;
        }

        is->started = 0;

        is->paused = 0;
        is->last_paused = -1;
        is->player_started = 0;
        is->eof = 0;

        return NO_ERROR;
    }

    return INVALID_OPERATION;
}

void notify(VideoState *is, int msg, int ext1, int ext2) {
    if (is->notify_callback) {
        is->notify_callback(is->clazz, msg, ext1, ext2, 0);
    }
}

void notify_from_thread(VideoState *is, int msg, int ext1, int ext2) {
    if (is->notify_callback) {
        is->notify_callback(is, msg, ext1, ext2, 1);
    }
}

void clear_l(VideoState **ps) {
    VideoState *is = *ps;

    if (is) {
        if (is->pFormatCtx) {
            avformat_close_input(&is->pFormatCtx);
            if (is->pFormatCtx) {
                avformat_free_context(is->pFormatCtx);
            }
            is->pFormatCtx = NULL;
        }
        if (is->avbuffer) {
            av_free(is->avbuffer);
        }
        is->videoStream = 0;
        is->frame_count = 0;

        is->seek_req = 0;
        is->seek_flags = 0;
        is->seek_pos = 0;
        is->seek_rel = 0;

        is->video_st = NULL;

        if (is->videoq.initialized == 1) {
            if (is->videoq.first_pkt) {
                free(is->videoq.first_pkt);
            }

            if (is->videoq.last_pkt) {
                //free(is->videoq.last_pkt);
            }

            if (is->videoq.mutex) {
                free(is->videoq.mutex);
                is->videoq.mutex = NULL;
            }

            if (is->videoq.cond) {
                free(is->videoq.cond);
                is->videoq.cond = NULL;
            }

            is->videoq.initialized = 0;
        }

        //VideoPicture    pictq[VIDEO_PICTURE_QUEUE_SIZE];
        is->pictq_size = 0;
        is->pictq_rindex = 0;
        is->pictq_windex = 0;

        if (is->pictq_mutex) {
            free(is->pictq_mutex);
            is->pictq_mutex = NULL;
        }

        if (is->display_mutex) {
            free(is->display_mutex);
            is->display_mutex = NULL;
        }

        if (is->pictq_cond) {
            free(is->pictq_cond);
            is->pictq_cond = NULL;
        }

        if (is->display_tid) {
            free(is->display_tid);
            is->display_tid = NULL;
        }

        if (is->parse_tid) {
            free(is->parse_tid);
            is->parse_tid = NULL;
        }

        if (is->frame_decode_tid) {
            free(is->frame_decode_tid);
            is->frame_decode_tid = NULL;
        }

        //is->filename[0] = '\0';
        //is->quit = 0;

        if (is->io_context) {
            av_free(is->io_context);
            is->io_context = NULL;
        }

        if (is->sws_ctx) {
            sws_freeContext(is->sws_ctx);
            is->sws_ctx = NULL;
        }

        is->prepared = 0;
        is->started = 0;

        is->paused = 0;
        is->last_paused = -1;
        is->player_started = 0;
        is->eof = 0;

        av_packet_unref(&is->flush_pkt);
    }
}

int seekTo_l(VideoState **ps, int fr_index) {
    VideoState *is = *ps;

    if (is) {
        stream_seek(is, fr_index * is->frame_dur, fr_index * is->frame_dur, 0);//AVSEEK_FLAG_BACKWARD
        return NO_ERROR;
    }

    return INVALID_OPERATION;
}

int prepareAsync_l(VideoState **ps) {
    VideoState *is = *ps;

    if (is != 0) {
        av_init_packet(&is->flush_pkt);
        is->flush_pkt.data = (unsigned char *) "FLUSH";

        int video_index = -1;
        int i;

        is->videoStream = -1;

        AVDictionary *options = NULL;
        av_dict_set(&options, "icy", "1", 0);
        av_dict_set(&options, "user-agent", "FFMPEGTrackPlayer", 0);

        // will interrupt blocking functions if we quit!
//    is->pFormatCtx = avformat_alloc_context();
//    callback.callback = decode_interrupt_cb;
//    callback.opaque = is;
//    if (avio_open2(&is->io_context, is->filename, 0, &callback, &io_dict)) {
//        fprintf(stderr, "Unable to open I/O for %s\n", is->filename);
//        notify_from_thread(is, MEDIA_ERROR, 0, 0);
//        return -1;
//    }
//    is->avbuffer = (unsigned char*)av_malloc(4096);
//    is->io_context = avio_alloc_context(is->avbuffer, 4096, 0, &is, &my_read_function, NULL, &my_seek_function);
////    is->io_context = avio_alloc_context(is->avbuffer, 4096, 0, &is, &my_read_function, NULL, NULL);
////    is->io_context = avio_alloc_context(is->avbuffer, 4096, 0, &is, NULL, NULL, NULL);
//    is->pFormatCtx->pb = is->io_context;
//    if (!is->io_context){
//        LOGE("ERROR initializing io context");
//    } else{
//        LOGI("IOContext opened");
//    }
        // Open video file
        int retur = avformat_open_input(&is->pFormatCtx, is->filename, NULL, &options);
        if (retur != 0) {
            if (retur == AVERROR_INVALIDDATA) {
                return retur;
            }
            char arr[200];
            av_strerror(retur, (char *) &arr, 200);
            LOGE("avformat_open_input failed with %i - %s", retur, (char *) &arr);
//            notify_from_thread(is, MEDIA_ERROR, 0, 0);
            return retur; // Couldn't open file
        }

        // Retrieve stream information
        retur = avformat_find_stream_info(is->pFormatCtx, NULL);
        if (retur < 0) {
            char arr[200];
            av_strerror(retur, (char *) &arr, 200);
            LOGE("couldn't find stream info %i - %s at %s", retur, (char *) &arr, is->filename);
//            notify_from_thread(is, MEDIA_ERROR, 0, 0);
            return retur; // Couldn't find stream information
        }

        // Dump information about file onto standard error
        av_dump_format(is->pFormatCtx, 0, is->filename, 0);

        // Find the first video stream
        for (i = 0; i < is->pFormatCtx->nb_streams; i++) {
            if (is->pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO &&
                video_index < 0) {
                video_index = i;
            }

            set_codec(is->pFormatCtx, i);
        }
        if (video_index >= 0) {
            stream_component_open(is, video_index);
        }

        if (is->videoStream < 0) {
            LOGE("%s: could not open codecs", is->filename);
//            notify_from_thread(is, MEDIA_ERROR, 0, 0);
            return retur;
        }

        set_rotation(is->pFormatCtx, is->video_st);
        set_framerate(is->pFormatCtx, is->video_st);
        set_filesize(is->pFormatCtx);
        set_chapter_count(is->pFormatCtx);
        is->prepared = 1;

//        notify_from_thread(is, MEDIA_PREPARED, 0, 0);
        return NO_ERROR;
    }
    return INVALID_OPERATION;
}

int startPlayback(VideoState *is) {
    is->eof = 0;
    is->player_started = 1;
    is->pictq_mutex = SDL_CreateMutex();
    is->display_mutex = SDL_CreateMutex();
    is->pictq_cond = SDL_CreateCond();


    is->parse_tid = malloc(sizeof(*(is->parse_tid)));
    is->frame_decode_tid = malloc(sizeof(*(is->frame_decode_tid)));
    is->display_tid = malloc(sizeof(*(is->display_tid)));

    if (!is->parse_tid) {
        av_free(is);
        return UNKNOWN_ERROR;
    }

    pthread_create(is->parse_tid, NULL, (void *) &packet_read_thread, is);
    pthread_create(is->frame_decode_tid, NULL, (void *) &frame_decode_thread, is);
    pthread_create(is->display_tid, NULL, (void *) &display_thread, is);
    return 0;
}

int getDuration_l(VideoState **ps, int *msec) {
    VideoState *is = *ps;
    if (is) {
        if (is->pFormatCtx && (is->pFormatCtx->duration != AV_NOPTS_VALUE)) {
            *msec = (int) is->pFormatCtx->duration;
        } else {
            *msec = 0;
            LOGI("getDuration unavailable");
        }
        return NO_ERROR;
    }

    return INVALID_OPERATION;
}