/*
 * OpenStreetView track viewer, using ffmpeg.
 * Plays back the video files associated with the track.
 * Combines multiple files for a seamless playback with seek and backwards playback functionality
 * Supports only the OSV specific files, h264 encoding, with ultra high definition.
 *
 * Based on :
 *
 * https://github.com/wseemann/FFmpegMediaPlayer
 *
 * FFmpegMediaPlayer: A unified interface for playing audio files and streams.
 *
 * Copyright 2016 William Seemann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <assert.h>
#include "android/log.h"
#include "mediaplayer.h"


#ifdef ANDROID
#include <android/log.h>
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "FFMPEG_MP_JNI E ", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "FFMPEG_MP_JNI I ", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("FFMPEG_MP_JNI E " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("FFMPEG_MP_JNI I " format "\n", ##__VA_ARGS__)
#endif

extern "C" {
}

// ----------------------------------------------------------------------------

// ----------------------------------------------------------------------------

struct fields_t {
    jfieldID context;
    jfieldID surface_texture;

    jmethodID post_event;
};
static fields_t fields;

static JavaVM *m_vm;
//static Mutex sLock;

// ----------------------------------------------------------------------------
// ref-counted object for callbacks
class JNIMediaPlayerListener : public MediaPlayerListener {
public:
    JNIMediaPlayerListener(JNIEnv *env, jobject thiz, jobject weak_thiz);

    ~JNIMediaPlayerListener();

    virtual void notify(int msg, int ext1, int ext2, int from_thread);
private:
    jclass mClass;     // Reference to TrackPlayer class
    jobject mObject;    // Weak ref to TrackPlayer Java object to call on
    jobject mThiz;
};

void jniThrowException(JNIEnv *env, const char *className,
                       const char *msg) {
    jclass exception = env->FindClass(className);
    env->ThrowNew(exception, msg);
}

JNIMediaPlayerListener::JNIMediaPlayerListener(JNIEnv *env, jobject thiz, jobject weak_thiz) {

    // Hold onto the MediaPlayer class for use in calling the static method
    // that posts events to the application thread.
    jclass clazz = env->GetObjectClass(thiz);
    if (clazz == NULL) {
        LOGI("Can't find com/telenav/ffmpeg/FFMPEGTrackPlayer");
        jniThrowException(env, "java/lang/Exception", NULL);
        return;
    }
    mClass = (jclass) env->NewGlobalRef(clazz);
    mThiz = (jobject) env->NewGlobalRef(thiz);

    // We use a weak reference so the MediaPlayer object can be garbage collected.
    // The reference is only used as a proxy for callbacks.
    mObject = env->NewGlobalRef(weak_thiz);
}

JNIMediaPlayerListener::~JNIMediaPlayerListener() {
    // remove global references
    //JNIEnv *env = AndroidRuntime::getJNIEnv();
    JNIEnv *env = 0;
    m_vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    env->DeleteGlobalRef(mObject);
    env->DeleteGlobalRef(mClass);
    env->DeleteGlobalRef(mThiz);
}

void JNIMediaPlayerListener::notify(int msg, int ext1, int ext2, int fromThread) {
    JNIEnv *env = 0;
    int isAttached = 0;

    m_vm->GetEnv((void **) &env, JNI_VERSION_1_6);

    if (fromThread) {
        if (m_vm->AttachCurrentThread(&env, NULL) < 0) {
            LOGI("failed to attach current thread");
        }

        isAttached = 1;
    }

    env->CallStaticVoidMethod(mClass, fields.post_event, mObject,
                              msg, ext1, ext2, NULL);

    if (env->ExceptionCheck()) {
        LOGE("An exception occurred while notifying an event.");
        env->ExceptionClear();
    }

    if (fromThread && isAttached) {
        m_vm->DetachCurrentThread();
    }
}

// ----------------------------------------------------------------------------

static MediaPlayer *getMediaPlayer(JNIEnv *env, jobject thiz) {
    //Mutex::Autolock l(sLock);
    MediaPlayer *const p = (MediaPlayer *) env->GetLongField(thiz, fields.context);
    return p;
}

static MediaPlayer *setMediaPlayer(JNIEnv *env, jobject thiz, long mp) {
    //Mutex::Autolock l(sLock);
    MediaPlayer *old = (MediaPlayer *) env->GetLongField(thiz, fields.context);
    env->SetLongField(thiz, fields.context, mp);
    return old;
}

// If exception is NULL and opStatus is not OK, this method sends an error
// event to the client application; otherwise, if exception is not NULL and
// opStatus is not OK, this method throws the given exception to the client
// application.
static void process_media_player_call(JNIEnv *env, jobject thiz, int opStatus, const char *exception, const char *message) {
    if (exception == NULL) {  // Don't throw exception. Instead, send an event.
        if (opStatus != (int) OK) {
            MediaPlayer *mp = getMediaPlayer(env, thiz);
            if (mp != 0) mp->notify(NULL,MEDIA_ERROR, opStatus, 0, 0);
        }
    } else {  // Throw exception!
        if (opStatus == (int) INVALID_OPERATION) {
            jniThrowException(env, "java/lang/IllegalStateException", NULL);
        } else if (opStatus == (int) PERMISSION_DENIED) {
            jniThrowException(env, "java/lang/SecurityException", NULL);
        } else if (opStatus != (int) OK) {
            if (strlen(message) > 230) {
                // if the message is too long, don't bother displaying the status code
                jniThrowException(env, exception, message);
            } else {
                char msg[256];
                // append the status code to the message
                sprintf(msg, "%s: status=0x%X", message, opStatus);
                jniThrowException(env, exception, msg);
            }
        }
    }
}

static void
setVideoSurface_l(JNIEnv *env, jobject thiz, jobject jsurface, jboolean mediaPlayerMustBeAlive) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        if (mediaPlayerMustBeAlive) {
            jniThrowException(env, "java/lang/IllegalStateException", NULL);
        }
        return;
    }

    // obtain a native window from a Java surface
    ANativeWindow *theNativeWindow = ANativeWindow_fromSurface(env, jsurface);

    if (theNativeWindow != NULL) {
        mp->setVideoSurface(theNativeWindow);
    }
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_setDataSource(
        JNIEnv *env, jobject thiz, jobjectArray jpaths) {

    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }

    if (jpaths == NULL) {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return;
    }
    int pathsCount = env->GetArrayLength(jpaths);
    const char* paths[pathsCount];
    for (int i = 0; i < pathsCount; i++) {
        jstring path = (jstring) env->GetObjectArrayElement(jpaths, i);

        const char *tmp = env->GetStringUTFChars(path, NULL);
        if (tmp == NULL) {  // Out of memory
            return;
        }
        paths[i] = tmp;
        LOGI("setDataSource: path %s", tmp);
    }
    status_t opStatus = mp->setDataSource(paths, pathsCount);

    process_media_player_call(
            env, thiz, opStatus, "java/io/IOException",
            "setDataSource failed.");
    for (int i = 0; i < pathsCount; i++) {
        jstring path = (jstring) env->GetObjectArrayElement(jpaths, i);
        const char *tmp = paths[i];
        env->ReleaseStringUTFChars(path, tmp);
    }

}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_setVideoSurface(JNIEnv *env, jobject thiz, jobject jsurface) {
    setVideoSurface_l(env, thiz, jsurface, (jboolean) true /* mediaPlayerMustBeAlive */);
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_initSignalHandler(JNIEnv *env, jobject thiz) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    mp->initSigHandler();
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_start(JNIEnv *env, jobject thiz) {
    LOGI("start");
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call(env, thiz, mp->start(), NULL, NULL);
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_stop(JNIEnv *env, jobject thiz) {
    LOGI("stop");
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call(env, thiz, mp->stop(), NULL, NULL);
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_pause(JNIEnv *env, jobject thiz) {
    LOGI("pause");
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call(env, thiz, mp->pause(), NULL, NULL);
}

static jboolean
com_telenav_ffmpeg_FFMPEGTrackPlayer_isPlaying(JNIEnv *env, jobject thiz) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return (jboolean) false;
    }
    const jboolean is_playing = (const jboolean) mp->isPlaying();
    return is_playing;
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_seekTo(JNIEnv *env, jobject thiz, int index) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call(env, thiz, mp->seekTo(index), NULL, NULL);
}

static int
com_telenav_ffmpeg_FFMPEGTrackPlayer_getVideoWidth(JNIEnv *env, jobject thiz) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }
    int w;
    if (0 != mp->getVideoWidth(&w)) {
        LOGI("getVideoWidth failed");
        w = 0;
    }
    LOGI("getVideoWidth: %d", w);
    return w;
}

static int
com_telenav_ffmpeg_FFMPEGTrackPlayer_getVideoHeight(JNIEnv *env, jobject thiz) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }
    int h;
    if (0 != mp->getVideoHeight(&h)) {
        LOGI("getVideoHeight failed");
        h = 0;
    }
    LOGI("getVideoHeight: %d", h);
    return h;
}


static int
com_telenav_ffmpeg_FFMPEGTrackPlayer_getCurrentPosition(JNIEnv *env, jobject thiz) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }
    int msec;
    process_media_player_call(env, thiz, mp->getCurrentPosition(&msec), NULL, NULL);
//    LOGI("getCurrentPosition: %d (msec)", msec);
    return msec;
}

static int
com_telenav_ffmpeg_FFMPEGTrackPlayer_getDuration(JNIEnv *env, jobject thiz) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }
    int msec;
    process_media_player_call(env, thiz, mp->getDuration(&msec), NULL, NULL);
//    LOGI("getDuration: %d (msec)", msec);
    return msec;
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_setFPSDelay(JNIEnv *env, jobject thiz, jboolean fast) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call(env, thiz, mp->setFPSDelay(fast), NULL, NULL);
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_setLooping(JNIEnv *env, jobject thiz, jboolean looping) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call(env, thiz, mp->setLooping(looping), NULL, NULL);
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_stepFrame(JNIEnv *env, jobject thiz, jboolean forward) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call(env, thiz, mp->stepFrame(forward), NULL, NULL);
}
static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_seeking(JNIEnv *env, jobject thiz, jboolean started) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call(env, thiz, mp->seeking(started), NULL, NULL);
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_setBackwards(JNIEnv *env, jobject thiz, jboolean backwards) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call(env, thiz, mp->setBackwards(backwards), NULL, NULL);
}

static jboolean
com_telenav_ffmpeg_FFMPEGTrackPlayer_isLooping(JNIEnv *env, jobject thiz) {
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return (jboolean) false;
    }
    return (jboolean) mp->isLooping();
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_reset(JNIEnv *env, jobject thiz) {
    LOGI("reset");
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call(env, thiz, mp->reset(), NULL, NULL);
}

void custom_player_log(void *ptr, int level, const char *fmt, va_list vl) {
    FILE *fp = fopen("/storage/emulated/0/Android/data/com.telenav.streetview/files/av_player_log.txt", "a+");
    if (fp) {
        vfprintf(fp, fmt, vl);
        fflush(fp);
        fclose(fp);
    }
}

// This function gets some field IDs, which in turn causes class initialization.
// It is called from a static block in MediaPlayer, which won't run until the
// first time an instance of this class is used.
static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_native_init(JNIEnv *env) {
//    LOGI("native_init");
    jclass clazz;

    clazz = env->FindClass("com/telenav/ffmpeg/FFMPEGTrackPlayer");
    if (clazz == NULL) {
        return;
    }
    fields.context = env->GetFieldID(clazz, "mNativeContext", "J");
    if (fields.context == NULL) {
        return;
    }
    fields.post_event = env->GetStaticMethodID(clazz, "postEventFromNative",
                                               "(Ljava/lang/Object;IIILjava/lang/Object;)V");
    if (fields.post_event == NULL) {
        return;
    }
    fields.surface_texture = env->GetFieldID(clazz, "mNativeSurfaceTexture", "I");
    if (fields.surface_texture == NULL) {
        return;
    }
    av_log_set_callback(custom_player_log);
    // Initialize libavformat and register all the muxers, demuxers and protocols.
    av_register_all();
    avformat_network_init();

    AVInputFormat *jpg_inf = av_find_input_format("mjpeg");
    if (jpg_inf == NULL) {
        LOGE("Mjpeg format inexistent");
    }

    //output encoder initialize
    AVCodec *pCodec = avcodec_find_encoder(AV_CODEC_ID_H264);
    if (!pCodec) {
        LOGI("----------------------------h264 encoder inexistent");
    }
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_native_setup(JNIEnv *env, jobject thiz, jobject weak_this) {
    LOGI("native_setup");
    MediaPlayer *mp = new MediaPlayer();
    if (mp == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "Out of memory");
        return;
    }
    // create new listener and give it to MediaPlayer
    JNIMediaPlayerListener *listener = new JNIMediaPlayerListener(env, thiz, weak_this);
    mp->setListener(listener);

    // Stow our new C++ MediaPlayer in an opaque field in the Java object.
    setMediaPlayer(env, thiz, (long) mp);
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_release(JNIEnv *env, jobject thiz) {
    LOGI("release");
    MediaPlayer *mp = setMediaPlayer(env, thiz, 0);
    if (mp != NULL) {
        // this prevents native callbacks after the object is released
        JNIMediaPlayerListener *listener = (JNIMediaPlayerListener *) mp->getListener();
        delete listener;
        mp->setListener(0);
        delete mp;
        setMediaPlayer(env, thiz, 0);
    }
}

static void
com_telenav_ffmpeg_FFMPEGTrackPlayer_native_finalize(JNIEnv *env, jobject thiz) {
    LOGI("native_finalize");
    MediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp != NULL) {
        LOGI("MediaPlayer finalized without being released");
    }
    com_telenav_ffmpeg_FFMPEGTrackPlayer_release(env, thiz);
}

// ----------------------------------------------------------------------------

static JNINativeMethod gMethods[] = {
        {
                "_setDataSource",
                                            "([Ljava/lang/String;)V",
                                                                                          (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_setDataSource
        },

        {       "_setVideoSurface",         "(Landroid/view/Surface;)V",                  (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_setVideoSurface},
        {       "_start",                   "()V",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_start},
        {       "_initSignalHandler",       "()V",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_initSignalHandler},
        {       "_stop",                    "()V",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_stop},
        {       "getVideoWidth",            "()I",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_getVideoWidth},
        {       "getVideoHeight",           "()I",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_getVideoHeight},
        {       "seekTo",                   "(I)V",                                       (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_seekTo},
        {       "_pause",                   "()V",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_pause},
        {       "isPlaying",                "()Z",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_isPlaying},
        {       "getCurrentPosition",       "()I",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_getCurrentPosition},
        {       "getDuration",              "()I",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_getDuration},
        {       "setFPSDelay",              "(Z)V",                                       (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_setFPSDelay},
        {       "setLooping",               "(Z)V",                                       (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_setLooping},
        {       "stepFrame",                "(Z)V",                                       (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_stepFrame},
        {       "seeking",                  "(Z)V",                                       (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_seeking},
        {       "setBackwards",             "(Z)V",                                       (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_setBackwards},
        {       "isLooping",                "()Z",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_isLooping},
        {       "_release",                 "()V",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_release},
        {       "_reset",                   "()V",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_reset},
        {       "native_init",              "()V",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_native_init},
        {       "native_setup",             "(Ljava/lang/Object;)V",                      (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_native_setup},
        {       "native_finalize",          "()V",                                        (void *) com_telenav_ffmpeg_FFMPEGTrackPlayer_native_finalize},
};

static const char *const kClassPathName = "com/telenav/ffmpeg/FFMPEGTrackPlayer";

// This function only registers the native methods
static int register_com_telenav_ffmpeg_FFMPEGTrackPlayer(JNIEnv *env) {
    int numMethods = (sizeof(gMethods) / sizeof((gMethods)[0]));
    jclass clazz = env->FindClass(kClassPathName);
    if (clazz == NULL) {
        LOGI("Native registration unable to find class 'com/telenav/ffmpeg/FFMPEGTrackPlayer'");
        return JNI_ERR;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGI("RegisterNatives failed for 'com/telenav/ffmpeg/FFMPEGTrackPlayer'");
        return JNI_ERR;
    }
    env->DeleteLocalRef(clazz);

    return JNI_OK;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    m_vm = vm;
    JNIEnv *env = NULL;
    jint result = -1;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGI("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    if (register_com_telenav_ffmpeg_FFMPEGTrackPlayer(env) < 0) {
        LOGI("ERROR: FFMPEGTrackPlayer native registration failed\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_6;

    bail:
    return result;
}

// KTHXBYE
