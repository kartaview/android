/*
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

//#define LOG_NDEBUG 0
#define LOG_TAG "FFmpegMediaPlayer"
//#include <android/log.h>

#include <sys/types.h>
#include <Errors.h>
#include "mediaplayer.h"

extern "C" {
}

MediaPlayer::MediaPlayer() {
    //LOGI("constructor");

    state = NULL;

    mListener = NULL;
    mCookie = NULL;
    mDuration = -1;
    mStreamType = 3;
    mCurrentPosition = -1;
    mSeekPosition = -1;
    mCurrentState = MEDIA_PLAYER_IDLE;
    mPrepareSync = false;
    mPrepareStatus = NO_ERROR;
    mLoop = false;
    mLeftVolume = mRightVolume = 1.0;
    mVideoWidth = mVideoHeight = 0;
    //mLockThreadId = 0;
    //pthread_mutex_init(mLock, NULL);
    mAudioSessionId = 0;
    mSendLevel = 0;
}

MediaPlayer::~MediaPlayer() {
    LOGI("destructor");
    disconnect();
    //IPCThreadState::self()->flushCommands();
}

void MediaPlayer::disconnect() {
    LOGI("disconnect");
    VideoState *p = NULL;
    {
        //Mutex::Autolock _l(mLock);
        p = state;
        ::reset(&p);
    }

    if (state != 0) {
        ::disconnect(&state);
    }
}

// always call with lock held
void MediaPlayer::clear_l() {
    mDuration = -1;
    mCurrentPosition = -1;
    mSeekPosition = -1;
    mVideoWidth = mVideoHeight = 0;
}

static void
notifyListener(void *clazz, int msg, int ext1, int ext2, int fromThread) {
    MediaPlayer *mp = (MediaPlayer *) clazz;
    mp->notify(msg, ext1, ext2, fromThread);
}

status_t MediaPlayer::setListener(MediaPlayerListener *listener) {
//    LOGI("setListener");
    //Mutex::Autolock _l(mLock);
    mListener = listener;
    if (state != 0) {
        ::setListener(&state, this, notifyListener);
    }
    return NO_ERROR;
}

MediaPlayerListener *MediaPlayer::getListener() {
    return mListener;
}

status_t MediaPlayer::setDataSource(VideoState *player) {
    status_t err = UNKNOWN_ERROR;
    VideoState *p;
    { // scope for the lock
        //Mutex::Autolock _l(mLock);

        if (!((mCurrentState & MEDIA_PLAYER_IDLE) ||
              (mCurrentState == MEDIA_PLAYER_STATE_ERROR))) {
            LOGI("setDataSource called in state %d", mCurrentState);
            return INVALID_OPERATION;
        }

        ::clear_l(&player);
        ::setListener(&player, this, notifyListener);
        clear_l();
        p = state;
        state = player;
        if (player != 0) {
            mCurrentState = MEDIA_PLAYER_INITIALIZED;
            err = NO_ERROR;
        } else {
            LOGI("Unable to to create media player");
        }
    }

    if (p != 0) {
        ::disconnect(&p);
    }

    return err;
}

status_t MediaPlayer::setDataSource(const char *url, const char *headers) {
//    LOGI("setDataSource(%s)", url);
    status_t err = BAD_VALUE;
    if (url != NULL) {
        //const sp<IMediaPlayerService>& service(getMediaPlayerService());
        //if (service != 0) {
        //sp<IMediaPlayer> player(
        //        service->create(getpid(), this, url, headers, mAudioSessionId));
        VideoState *state = ::create();
        err = ::setDataSourceURI(&state, url, headers);
        if (err == NO_ERROR) {
            err = setDataSource(state);
        }
        //}
    }
    return err;
}

status_t MediaPlayer::setDataSource(int fd, int64_t offset, int64_t length) {
//    LOGI("setDataSource(%d, %lld, %lld)", fd, offset, length);
    status_t err = UNKNOWN_ERROR;
    //const sp<IMediaPlayerService>& service(getMediaPlayerService());
    //if (state != 0) {
    //sp<IMediaPlayer> player(service->create(getpid(), this, fd, offset, length, mAudioSessionId));
    VideoState *state = create();
    err = ::setDataSourceFD(&state, fd, offset, length);
    err = setDataSource(state);
    //}
    return err;
}

status_t MediaPlayer::setMetadataFilter(char *allow[], char *block[]) {
//    LOGI("setMetadataFilter");
    //Mutex::Autolock lock(mLock);
    if (state == NULL) {
        return NO_INIT;
    }
    return ::setMetadataFilter(&state, allow, block);
}

status_t MediaPlayer::getMetadata(bool update_only, bool apply_filter, AVDictionary **metadata) {
    LOGI("getMetadata");
    //Mutex::Autolock lock(mLock);
    if (state == NULL) {
        return NO_INIT;
    }
    return ::getMetadata(&state, metadata);
}

status_t MediaPlayer::setVideoSurface(ANativeWindow *native_window) {
    LOGI("setVideoSurface");
    //Mutex::Autolock _l(mLock);
    if (state == 0) return NO_INIT;
    if (native_window != NULL)
        return ::setVideoSurface(&state, native_window);
    else
        return ::setVideoSurface(&state, NULL);
}

// must call with lock held
status_t MediaPlayer::prepareAsync_l() {
    if ((state != 0) && (mCurrentState & (MEDIA_PLAYER_INITIALIZED | MEDIA_PLAYER_STOPPED))) {
        setAudioStreamType(mStreamType);
        mCurrentState = MEDIA_PLAYER_PREPARING;
        return ::prepareAsync(&state);
    }
    LOGI("prepareAsync called in state %d", mCurrentState);
    return INVALID_OPERATION;
}

// TODO: In case of error, prepareAsync provides the caller with 2 error codes,
// one defined in the Android framework and one provided by the implementation
// that generated the error. The sync version of prepare returns only 1 error
// code.
status_t MediaPlayer::prepare() {
    LOGI("prepare");
    //Mutex::Autolock _l(mLock);
    //mLockThreadId = getThreadId();
    if (mPrepareSync) {
        //mLockThreadId = 0;
        return -EALREADY;
    }
    mPrepareSync = true;

    status_t ret = ::prepare(&state);
    //status_t ret = prepareAsync_l();
    if (ret != NO_ERROR) {
        //mLockThreadId = 0;
        return ret;
    }
    if (mPrepareSync) {
        //mSignal.wait(mLock); // wait for prepare done
        mPrepareSync = false;
    }
    LOGI("prepare complete - status = %d", mPrepareStatus);
    //mLockThreadId = 0;
    return mPrepareStatus;
}

status_t MediaPlayer::prepareAsync() {
    LOGI("prepareAsync");
    //Mutex::Autolock _l(mLock);
    return prepareAsync_l();
}

status_t MediaPlayer::start() {
    LOGI("start");
    //Mutex::Autolock _l(mLock);
    if (mCurrentState & MEDIA_PLAYER_STARTED)
        return NO_ERROR;
    if ((state != 0) && (mCurrentState & (MEDIA_PLAYER_PREPARED |
                                          MEDIA_PLAYER_PLAYBACK_COMPLETE | MEDIA_PLAYER_PAUSED))) {
        setLooping(mLoop);
        setVolume(mLeftVolume, mRightVolume);
        setAuxEffectSendLevel(mSendLevel);
        mCurrentState = MEDIA_PLAYER_STARTED;
        status_t ret = ::start(&state);
        if (ret != NO_ERROR) {
            mCurrentState = MEDIA_PLAYER_STATE_ERROR;
        } else {
            if (mCurrentState == MEDIA_PLAYER_PLAYBACK_COMPLETE) {
                LOGI("playback completed immediately following start()");
            }
        }
        return ret;
    }
    LOGI("start called in state %d", mCurrentState);
    return INVALID_OPERATION;
}

status_t MediaPlayer::stop() {
    LOGI("stop");
    //Mutex::Autolock _l(mLock);
    if (mCurrentState & MEDIA_PLAYER_STOPPED) return NO_ERROR;
    if ((state != 0) && (mCurrentState & (MEDIA_PLAYER_STARTED | MEDIA_PLAYER_PREPARED |
                                          MEDIA_PLAYER_PAUSED | MEDIA_PLAYER_PLAYBACK_COMPLETE))) {
        status_t ret = ::stop(&state);
        if (ret != NO_ERROR) {
            mCurrentState = MEDIA_PLAYER_STATE_ERROR;
        } else {
            mCurrentState = MEDIA_PLAYER_STOPPED;
        }
        return ret;
    }
    LOGI("stop called in state %d", mCurrentState);
    return INVALID_OPERATION;
}

status_t MediaPlayer::pause() {
    LOGI("pause");
    //Mutex::Autolock _l(mLock);
    if (mCurrentState & (MEDIA_PLAYER_PAUSED | MEDIA_PLAYER_PLAYBACK_COMPLETE))
        return NO_ERROR;
    if ((state != 0) && (mCurrentState & (MEDIA_PLAYER_STARTED | MEDIA_PLAYER_PREPARED))) {
        status_t ret = ::pause_l(&state);
        if (ret != NO_ERROR) {
            mCurrentState = MEDIA_PLAYER_STATE_ERROR;
        } else {
            mCurrentState = MEDIA_PLAYER_PAUSED;
        }
        return ret;
    }
    LOGI("pause called in state %d", mCurrentState);
    return INVALID_OPERATION;
}

bool MediaPlayer::isPlaying() {
    //Mutex::Autolock _l(mLock);
    if (state != 0) {
        bool temp = false;
        //mPlayer->isPlaying(&temp); // TODO fix this!
        if (::isPlaying(&state)) {
            temp = true;
        }
        LOGI("isPlaying: %d", temp);
        if ((mCurrentState & MEDIA_PLAYER_STARTED) && !temp) {
            LOGI("internal/external state mismatch corrected");
            mCurrentState = MEDIA_PLAYER_PAUSED;
        }
        return temp;
    }
    LOGI("isPlaying: no active player");
    return false;
}

status_t MediaPlayer::getVideoWidth(int *w) {
    LOGI("getVideoWidth");
    //Mutex::Autolock _l(mLock);
    if (state == 0) return INVALID_OPERATION;
    *w = mVideoWidth;
    return NO_ERROR;
}

status_t MediaPlayer::getVideoHeight(int *h) {
    LOGI("getVideoHeight");
    //Mutex::Autolock _l(mLock);
    if (state == 0) return INVALID_OPERATION;
    *h = mVideoHeight;
    return NO_ERROR;
}

status_t MediaPlayer::getCurrentPosition(int *msec) {
    LOGI("getCurrentPosition");
    //Mutex::Autolock _l(mLock);
    if (state != 0) {
        if (mCurrentPosition >= 0) {
            LOGI("Using cached seek position: %d", mCurrentPosition);
            *msec = mCurrentPosition;
            return NO_ERROR;
        }
        return ::getCurrentPosition(&state, msec);
    }
    return INVALID_OPERATION;
}

status_t MediaPlayer::getDuration_l(int *msec) {
//    LOGI("getDuration");
    bool isValidState = (mCurrentState & (MEDIA_PLAYER_PREPARED | MEDIA_PLAYER_STARTED | MEDIA_PLAYER_PAUSED | MEDIA_PLAYER_STOPPED | MEDIA_PLAYER_PLAYBACK_COMPLETE));
    if (state != 0 && isValidState) {
        status_t ret = NO_ERROR;
        if (mDuration <= 0)
            ret = ::getDuration(&state, &mDuration);
        if (msec)
            *msec = mDuration;
        return ret;
    } else {
        if (state != 0){
            LOGI("Attempt to call getDuration without a valid mediaplayer");
        } else {
            LOGI("Attempt to call getDuration without a videostate object");
        }
    }
    return INVALID_OPERATION;
}

status_t MediaPlayer::getDuration(int *msec) {
    //Mutex::Autolock _l(mLock);
    return getDuration_l(msec);
}

status_t MediaPlayer::seekTo_l(int msec) {
    LOGI("seekTo %d", msec);
    if ((state != 0) && (mCurrentState & (MEDIA_PLAYER_STARTED | MEDIA_PLAYER_PREPARED | MEDIA_PLAYER_PAUSED | MEDIA_PLAYER_PLAYBACK_COMPLETE))) {
        if (msec < 0) {
            LOGI("Attempt to seek to invalid position: %d", msec);
            msec = 0;
        } else if ((mDuration > 0) && (msec > mDuration)) {
            LOGI("Attempt to seek to past end of file: request = %d, EOF = %d", msec, mDuration);
            msec = mDuration;
        }
        // cache duration
        mCurrentPosition = msec;
        if (mSeekPosition < 0) {
            getDuration_l(NULL);
            mSeekPosition = msec;
            return ::seekTo(&state, msec);
        }
        else {
            LOGI("Seek in progress - queue up seekTo[%d]", msec);
            return NO_ERROR;
        }
    }
    LOGI("Attempt to perform seekTo in wrong state: mPlayer=%p, mCurrentState=%u", state, mCurrentState);
    return INVALID_OPERATION;
}

status_t MediaPlayer::seekTo(int msec) {
    //mLockThreadId = getThreadId();
    //Mutex::Autolock _l(mLock);
    status_t result = seekTo_l(msec);
    //mLockThreadId = 0;

    return result;
}

status_t MediaPlayer::reset() {
    LOGI("reset");
    //Mutex::Autolock _l(mLock);
    mLoop = false;
    if (mCurrentState == MEDIA_PLAYER_IDLE) return NO_ERROR;
    mPrepareSync = false;
    if (state != 0) {
        status_t ret = ::reset(&state);
        if (ret != NO_ERROR) {
            LOGI("reset() failed with return code (%d)", ret);
            mCurrentState = MEDIA_PLAYER_STATE_ERROR;
        } else {
            mCurrentState = MEDIA_PLAYER_IDLE;
        }
        return ret;
    }
    clear_l();
    return NO_ERROR;
}

status_t MediaPlayer::setAudioStreamType(int type) {
    LOGI("MediaPlayer::setAudioStreamType");
    //Mutex::Autolock _l(mLock);
    if (mStreamType == type) return NO_ERROR;
    if (mCurrentState & (MEDIA_PLAYER_PREPARED | MEDIA_PLAYER_STARTED |
                         MEDIA_PLAYER_PAUSED | MEDIA_PLAYER_PLAYBACK_COMPLETE)) {
        // Can't change the stream type after prepare
        LOGI("setAudioStream called in state %d", mCurrentState);
        return INVALID_OPERATION;
    }
    // cache
    mStreamType = type;
    return OK;
}

status_t MediaPlayer::setLooping(int loop) {
    LOGI("MediaPlayer::setLooping");
    //Mutex::Autolock _l(mLock);
    mLoop = (loop != 0);
    if (state != 0) {
        return ::setLooping(&state, loop);
    }
    return OK;
}

bool MediaPlayer::isLooping() {
    LOGI("isLooping");
    //Mutex::Autolock _l(mLock);
    if (state != 0) {
        return mLoop;
    }
    LOGI("isLooping: no active player");
    return false;
}

status_t MediaPlayer::setVolume(float leftVolume, float rightVolume) {
    LOGI("MediaPlayer::setVolume(%f, %f)", leftVolume, rightVolume);
    //Mutex::Autolock _l(mLock);
    mLeftVolume = leftVolume;
    mRightVolume = rightVolume;
    if (state != 0) {
        MediaPlayerListener *listener = mListener;
        if (listener != 0) {
            return ::setVolume(&state, leftVolume, rightVolume);
        }
    }
    return OK;
}

status_t MediaPlayer::setAudioSessionId(int sessionId) {
    LOGI("MediaPlayer::setAudioSessionId(%d)", sessionId);
    //Mutex::Autolock _l(mLock);
    if (!(mCurrentState & MEDIA_PLAYER_IDLE)) {
        LOGI("setAudioSessionId called in state %d", mCurrentState);
        return INVALID_OPERATION;
    }
    if (sessionId < 0) {
        return BAD_VALUE;
    }
    mAudioSessionId = sessionId;
    return NO_ERROR;
}

int MediaPlayer::getAudioSessionId() {
    //Mutex::Autolock _l(mLock);
    return mAudioSessionId;
}

status_t MediaPlayer::setAuxEffectSendLevel(float level) {
    LOGI("MediaPlayer::setAuxEffectSendLevel(%f)", level);
    //Mutex::Autolock _l(mLock);
    mSendLevel = level;
    if (state != 0) {
        MediaPlayerListener *listener = mListener;
        if (listener != 0) {
            return 0;
            //return listener->setAuxEffectSendLevel(level);
        }
    }
    return OK;
}

status_t MediaPlayer::attachAuxEffect(int effectId) {
    LOGI("MediaPlayer::attachAuxEffect(%d)", effectId);
    //Mutex::Autolock _l(mLock);
    if (state == 0 ||
        (mCurrentState & MEDIA_PLAYER_IDLE) ||
        (mCurrentState == MEDIA_PLAYER_STATE_ERROR)) {
        LOGI("attachAuxEffect called in state %d", mCurrentState);
        return INVALID_OPERATION;
    }

    MediaPlayerListener *listener = mListener;
    if (listener != 0) {
        return 0;
        //return listener->attachAuxEffect(effectId);
    } else {
        return -3;
    }
}

void MediaPlayer::notify(int msg, int ext1, int ext2, int fromThread) {
//    LOGI("message received msg=%d, ext1=%d, ext2=%d", msg, ext1, ext2);
    bool send = true;
    bool locked = false;

    // TODO: In the future, we might be on the same thread if the app is
    // running in the same process as the media server. In that case,
    // this will deadlock.
    //
    // The threadId hack below works around this for the care of prepare
    // and seekTo within the same process.
    // FIXME: Remember, this is a hack, it's not even a hack that is applied
    // consistently for all use-cases, this needs to be revisited.
    /*if (mLockThreadId != getThreadId()) {
        mLock.lock();
        locked = true;
    }*/

    // Allows calls from JNI in idle state to notify errors
    if (!(msg == MEDIA_ERROR && mCurrentState == MEDIA_PLAYER_IDLE) && state == 0) {
        LOGI("notify(%d, %d, %d) callback on disconnected mediaplayer", msg, ext1, ext2);
        //if (locked) mLock.unlock(); // release the lock when done.
        return;
    }

    switch (msg) {
        case MEDIA_NOP: // interface test message
            break;
        case MEDIA_PREPARED:
            LOGI("prepared");
            mCurrentState = MEDIA_PLAYER_PREPARED;
            if (mPrepareSync) {
//                LOGI("signal application thread");
                mPrepareSync = false;
                mPrepareStatus = NO_ERROR;
//            mSignal.signal();
            }
            break;
        case MEDIA_PLAYBACK_COMPLETE:
            LOGI("playback complete");
            if (mCurrentState == MEDIA_PLAYER_IDLE) {
                LOGI("playback complete in idle state");
            }
            if (!mLoop) {
//                LOGI("playback complete when not looping");
                mCurrentState = MEDIA_PLAYER_PLAYBACK_COMPLETE;
            }
            break;
        case MEDIA_ERROR:
            // Always log errors.
            // ext1: Media framework error code.
            // ext2: Implementation dependant error code.
            LOGI("error (%d, %d)", ext1, ext2);
            mCurrentState = MEDIA_PLAYER_STATE_ERROR;
            if (mPrepareSync) {
                LOGI("signal application thread");
                mPrepareSync = false;
                mPrepareStatus = ext1;
                //mSignal.signal();
                send = false;
            }
            break;
            /*case MEDIA_INFO:
                // ext1: Media framework error code.
                // ext2: Implementation dependant error code.
                LOGI("info/warning (%d, %d)", ext1, ext2);
                break;*/
        case MEDIA_SEEK_COMPLETE:
            LOGI("Received seek complete");
            if (mSeekPosition != mCurrentPosition) {
                LOGI("Executing queued seekTo(%d)", mSeekPosition);
                mSeekPosition = -1;
                seekTo_l(mCurrentPosition);
            }
            else {
                LOGI("All seeks complete - return to regularly scheduled program");
                mCurrentPosition = mSeekPosition = -1;
            }
            break;
        case MEDIA_BUFFERING_UPDATE:
            LOGI("buffering %d", ext1);
            break;
            /*case MEDIA_SET_VIDEO_SIZE:
        //    	LOGI("New video size %d x %d", ext1, ext2);
                mVideoWidth = ext1;
                mVideoHeight = ext2;
                break;*/
        default:
            LOGI("unrecognized message: (%d, %d, %d)", msg, ext1, ext2);
            break;
    }

    MediaPlayerListener *listener = mListener;
    //if (locked) mLock.unlock();

    // this prevents re-entrant calls into client code
    if ((listener != 0) && send) {
        //Mutex::Autolock _l(mNotifyLock);
//        LOGI("callback application");
        listener->notify(msg, ext1, ext2, fromThread);
//        LOGI("back from callback");
    }
}

status_t MediaPlayer::setNextMediaPlayer(const MediaPlayer *next) {
    if (state == NULL) {
        return NO_INIT;
    }
    return ::setNextPlayer(&state, next == NULL ? NULL : next->state);
}

/*static*/ /*sp<IMemory> MediaPlayer::decode(const char* url, uint32_t *pSampleRate, int* pNumChannels, int* pFormat)
{
    LOGV("decode(%s)", url);
     sp<IMemory> p;
     const sp<IMediaPlayerService>& service = getMediaPlayerService();
     if (service != 0) {
     p = service->decode(url, pSampleRate, pNumChannels, pFormat);
     } else {
     LOGE("Unable to locate media service");
     }
    return p;
    
}*/
