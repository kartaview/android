#ifndef FFMPEG_UTILS_H_
#define FFMPEG_UTILS_H_

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/opt.h>

static const char *DURATION = "duration";
static const char *VIDEO_CODEC = "video_codec";
static const char *ICY_METADATA = "icy_metadata";
static const char *ROTATE = "rotate";
static const char *FRAMERATE = "framerate";
static const char *CHAPTER_START_TIME = "chapter_start_time";
static const char *CHAPTER_END_TIME = "chapter_end_time";
static const char *CHAPTER_COUNT = "chapter_count";
static const char *FILESIZE = "filesize";

static const int SUCCESS = 0;
static const int FAILURE = -1;

void set_shoutcast_metadata(AVFormatContext *ic);
void set_duration(AVFormatContext *ic);
void set_codec(AVFormatContext *ic, int i);
void set_rotation(AVFormatContext *ic, AVStream *video_st);
void set_framerate(AVFormatContext *ic, AVStream *video_st);
void set_filesize(AVFormatContext *ic);
void set_chapter_count(AVFormatContext *ic);
const char* extract_metadata_internal(AVFormatContext *ic, AVStream *video_st, const char* key);

#endif /*FFMPEG_UTILS_H_*/
