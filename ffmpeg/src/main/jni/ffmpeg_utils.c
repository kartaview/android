#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/opt.h>
#include "ffmpeg_utils.h"

void set_shoutcast_metadata(AVFormatContext *ic) {
    char *value = NULL;

    if (av_opt_get(ic, "icy_metadata_packet", 1, (uint8_t **) &value) < 0) {
        value = NULL;
    }

    if (value && value[0]) {
        av_dict_set(&ic->metadata, ICY_METADATA, value, 0);
    }
}

void set_duration(AVFormatContext *ic) {
    char value[30] = "0";
    int duration = 0;

    if (ic) {
        if (ic->duration != AV_NOPTS_VALUE) {
            duration = ((ic->duration / AV_TIME_BASE) * 1000);
        }
    }

    sprintf(value, "%d", duration); // %i
    av_dict_set(&ic->metadata, DURATION, value, 0);
}

void set_codec(AVFormatContext *ic, int i) {
    const char *codec_type = av_get_media_type_string(ic->streams[i]->codec->codec_type);

    if (!codec_type) {
        return;
    }

    const char *codec_name = avcodec_get_name(ic->streams[i]->codec->codec_id);

    if (codec_type && strcmp(codec_type, "video") == 0) {
        av_dict_set(&ic->metadata, VIDEO_CODEC, codec_name, 0);
    }
}

void set_rotation(AVFormatContext *ic, AVStream *video_st) {
    if (!extract_metadata_internal(ic, video_st, ROTATE) && video_st && video_st->metadata) {
        AVDictionaryEntry *entry = av_dict_get(video_st->metadata, ROTATE, NULL, AV_DICT_MATCH_CASE);

        if (entry && entry->value) {
            av_dict_set(&ic->metadata, ROTATE, entry->value, 0);
        } else {
            av_dict_set(&ic->metadata, ROTATE, "0", 0);
        }
    }
}

void set_framerate(AVFormatContext *ic, AVStream *video_st) {
    char value[30] = "0";

    if (video_st && video_st->avg_frame_rate.den && video_st->avg_frame_rate.num) {
        double d = av_q2d(video_st->avg_frame_rate);
        uint64_t v = lrintf(d * 100);
        if (v % 100) {
            sprintf(value, "%3.2f", d);
        } else if (v % (100 * 1000)) {
            sprintf(value, "%1.0f", d);
        } else {
            sprintf(value, "%1.0fk", d / 1000);
        }

        av_dict_set(&ic->metadata, FRAMERATE, value, 0);
    }
}

void set_filesize(AVFormatContext *ic) {
    char value[30] = "0";

    int64_t size = ic->pb ? avio_size(ic->pb) : -1;
    sprintf(value, "%"PRId64, size);
    av_dict_set(&ic->metadata, FILESIZE, value, 0);
}

void set_chapter_count(AVFormatContext *ic) {
    char value[30] = "0";
    int count = 0;

    if (ic) {
        if (ic->nb_chapters) {
            count = ic->nb_chapters;
        }
    }

    sprintf(value, "%d", count); // %i
    av_dict_set(&ic->metadata, CHAPTER_COUNT, value, 0);
}

const char *extract_metadata_internal(AVFormatContext *ic, AVStream *video_st, const char *key) {
    char *value = NULL;

    if (!ic) {
        return value;
    }

    if (key) {
        if (av_dict_get(ic->metadata, key, NULL, AV_DICT_MATCH_CASE)) {
            value = av_dict_get(ic->metadata, key, NULL, AV_DICT_MATCH_CASE)->value;
        } else if (video_st && av_dict_get(video_st->metadata, key, NULL, AV_DICT_MATCH_CASE)) {
            value = av_dict_get(video_st->metadata, key, NULL, AV_DICT_MATCH_CASE)->value;
        }
    }

    return value;
}