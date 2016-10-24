#ifndef FFMPEG_PLAYER_H_
#define FFMPEG_PLAYER_H_

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavformat/avio.h>
#include <libswresample/swresample.h>
#include <libswscale/swscale.h>
#include <libavutil/avstring.h>
#include <libavutil/opt.h>
#include <libavutil/time.h>
#include <libavutil/dict.h>
#include <libavutil/avassert.h>

#include <android/native_window_jni.h>

#include <SDL.h>
#include <SDL_thread.h>

#include <stdio.h>
#include <math.h>

#include <pthread.h>
#include "videoplayer.h"
#include <unistd.h>
#include "Errors.h"

#include "ffmpeg_utils.h"


#ifdef ANDROID
#include <android/log.h>
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "FFMPEG_MEDIAPLAYER E ", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "FFMPEG_MEDIAPLAYER I ", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("FFMPEG_MEDIAPLAYER E " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("FFMPEG_MEDIAPLAYER I " format "\n", ##__VA_ARGS__)
#endif

#define MAX_VIDEOQ_NR (1)
#define VIDEO_PICTURE_QUEUE_SIZE 2 //TODO 1 only but make a flag

typedef enum media_event_type {
    MEDIA_NOP               = 0, // interface test message
    MEDIA_PREPARED          = 1,
    MEDIA_PLAYBACK_COMPLETE = 2,
    MEDIA_BUFFERING_UPDATE  = 3,
    MEDIA_SEEK_COMPLETE     = 4,
    MEDIA_SET_VIDEO_SIZE    = 5,
    MEDIA_ON_FRAME          = 6,
    MEDIA_PLAYBACK_PLAYING  = 7,
    MEDIA_PLAYBACK_PAUSED   = 8,
    MEDIA_ON_NEXT_FILE      = 9,
    MEDIA_ON_PREVIOUS_FILE  = 10,
    MEDIA_ERROR             = 100,
} media_event_type;

typedef int media_error_type;
static const media_error_type MEDIA_ERROR_UNKNOWN = 1;
static const media_error_type MEDIA_ERROR_SERVER_DIED = 100;

typedef enum {
    MEDIA_PLAYER_STATE_ERROR        = 0,
    MEDIA_PLAYER_IDLE               = 1 << 0,
    MEDIA_PLAYER_PREPARED           = 1 << 1,
    MEDIA_PLAYER_STARTED            = 1 << 2,
    MEDIA_PLAYER_PAUSED             = 1 << 3,
    MEDIA_PLAYER_STOPPED            = 1 << 4
} media_player_states;

typedef struct PacketQueue {
  SDL_Window     *screen;
  SDL_Renderer *renderer;
  SDL_Texture *texture;
  int initialized;
  AVPacketList *first_pkt, *last_pkt;
  int nb_packets;
  int size;
  SDL_mutex *mutex;
  SDL_cond *cond;
} PacketQueue;

typedef struct Picture {
	int linesize;
	void *buffer;
} Picture;

typedef struct VideoPicture {
  Picture *bmp;
  int width, height; /* source height & width */
  int allocated;
  double pts;
  int index;
} VideoPicture;

typedef struct VideoState {
  AVFormatContext *pFormatCtx;
  int             videoStream;

  int             seek_req;
  int             seek_flags;
  int64_t         seek_pos;
  int64_t         seek_rel;

    int step_req_read;
    int step_req_decode;
    int step_req_display;
    int step_dir_back;

  AVStream        *video_st;
  PacketQueue     videoq;
  VideoPicture    pictq[VIDEO_PICTURE_QUEUE_SIZE];
  int             pictq_size, pictq_rindex, pictq_windex;
  SDL_mutex       *pictq_mutex;
  SDL_mutex       *display_mutex;
  SDL_cond        *pictq_cond;
  pthread_t       *parse_tid;
  pthread_t       *frame_decode_tid;
  pthread_t       *display_tid;

  char            filename[1024];
  int             file_index;

  AVIOContext     *io_context;
  struct SwsContext *sws_ctx;
  struct VideoPlayer *video_player;

  void (*notify_callback) (void*, int, int, int, int);
  void* clazz;

  int             quit;
  int             prepared;
  int             started;
  int             player_started;

  int             paused;
  int             last_paused;

  int             eof;

  AVPacket flush_pkt;
  int pkt_index;
    int last_frame;
  int flush_counter;
  void *next;
  void *previous;

  size_t *native_window;
  int *fps_delay_ptr;
  unsigned char *avbuffer;
  int *backwards;
  int *seeking;
  int64_t frame_count;
  int64_t frame_dur;
} VideoState;

struct AVDictionary {
	int count;
	AVDictionaryEntry *elems;
};

VideoState *create();
void disconnect(VideoState **ps);
int setDataSourceURI(VideoState **ps, const char *url);
int setVideoSurface(VideoState **ps, ANativeWindow* native_window);
int setListener(VideoState **ps,  void* clazz, void (*listener) (void*, int, int, int, int));
int prepare(VideoState **ps);
int start(VideoState **ps);
int stop(VideoState **ps);
int pause_l(VideoState **ps);
int stepForward(VideoState **ps);
int stepBackward(VideoState **ps);
int isPlaying(VideoState **ps);
int getVideoWidth(VideoState **ps, int *w);
int getVideoHeight(VideoState **ps, int *h);
int seekTo(VideoState **ps, int fr_index);
int getCurrentPosition(VideoState **ps, int *msec);
int getDuration(VideoState **ps, int *msec);
int startPlayback(VideoState *is);
int reset(VideoState **ps);
void notify(VideoState *is, int msg, int ext1, int ext2);
void notify_from_thread(VideoState *is, int msg, int ext1, int ext2);
void clear_l(VideoState **ps);
int seekTo_l(VideoState **ps, int fr_index);
int prepareAsync_l(VideoState **ps);
int getDuration_l(VideoState **ps, int *msec);

#endif /* FFMPEG_PLAYER_H_ */
