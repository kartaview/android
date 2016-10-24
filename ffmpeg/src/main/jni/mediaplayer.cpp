#define LOG_TAG "FFMPEGTrackPlayer"

#include "mediaplayer.h"

#include "crash_handler.h"

extern "C" {
}

MediaPlayer::MediaPlayer() {
    //LOGI("constructor");

    state = NULL;
    mListener = NULL;
    mPlayerState = MEDIA_PLAYER_IDLE;
    mLoop = false;
    mFpsDelay = DEFAULT_FPS_DELAY;
    native_window = NULL;
    mBackwards = 0;
    mSeeking = 0;
    mVideoWidth = mVideoHeight = 0;
}

MediaPlayer::~MediaPlayer() {
    LOGI("destructor");
    disconnect();
}

void MediaPlayer::disconnect() {
    LOGI("disconnect");
    for (int i = 0; i < states.size(); i++) {
        VideoState *state = (VideoState *) states[i];
        VideoState *p = NULL;
        {
            //Mutex::Autolock _l(mLock);
            p = state;
            ::clear_l(&p);
        }

        if (state != 0) {
            ::disconnect(&state);
        }
    }
}

void MediaPlayer::initSigHandler() {
    LOGI("Initializing signal handler");
    initSignalHandler();
}

// always call with lock held
void MediaPlayer::clear_l() {
    mVideoWidth = mVideoHeight = 0;
}

status_t MediaPlayer::mapGlobalIndexToLocal(int gIndex, std::pair<int, int> *data) {
    if (mPlayerState == MEDIA_PLAYER_IDLE) {
        return INVALID_OPERATION;
    }
    int video = 0, index = 0;
    int tempCount = 0;
    for (int i = 0; i < states.size(); ++i) {
        VideoState *state = (VideoState *) states.at((unsigned int) i);
        if (tempCount + state->frame_count > gIndex) {
            index = gIndex - tempCount;
            video = state->file_index;
            break;
        } else {
            tempCount = tempCount + state->frame_count;
        }
    }
    data->first = video;
    data->second = index;
    return NO_ERROR;
}

int MediaPlayer::mapLocalIndexToGlobal(int video, int index) {
    int global = 0;
    for (int i = 0; i < states.size(); ++i) {
        VideoState *state = (VideoState *) states.at((unsigned int) i);
        if (state->file_index < video) {
            global = global + state->frame_count;
        } else {
            break;
        }
    }
    global = global + index;
    return global;
}

static void
notifyListener(void *clazz, int msg, int ext1, int ext2, int fromThread) {
    MediaPlayer *mp = (MediaPlayer *) ((VideoState *) clazz)->clazz;
    mp->notify(clazz, msg, ext1, ext2, fromThread);
}

status_t MediaPlayer::setListener(MediaPlayerListener *listener) {
//    LOGI("setListener");
    //Mutex::Autolock _l(mLock);
    mListener = listener;
    for (int i = 0; i < states.size(); i++) {
        VideoState *state = (VideoState *) states[i];
        if (state != 0) {
            ::setListener(&state, this, notifyListener);
        }
    }
    return NO_ERROR;
}

MediaPlayerListener *MediaPlayer::getListener() {
    return mListener;
}

status_t MediaPlayer::setDataSource(VideoState *&ps) {
    VideoState *is = ps;
    status_t err = UNKNOWN_ERROR;
    if (is != NULL) { // scope for the lock
        //Mutex::Autolock _l(mLock);

        if (!((mPlayerState & MEDIA_PLAYER_IDLE) ||
              (mPlayerState == MEDIA_PLAYER_STATE_ERROR))) {
            LOGI("setDataSource called in state %d", mPlayerState);
            return INVALID_OPERATION;
        }

        ::reset(&is);
        ::setListener(&is, this, notifyListener);
        clear_l();
        states.push_back((size_t) ps);
        err = NO_ERROR;
    } else {
        LOGI("Unable to to create media player");
    }

    return err;
}

status_t MediaPlayer::setCurrentPlayer(int index) {
    status_t err = UNKNOWN_ERROR;
    VideoState *temp = (VideoState *) states.at((unsigned int) index);
//    if (state == temp) {
//        LOGI("This player is already set as current.");
//        return NO_ERROR;
//    }
    { // scope for the lock
        //Mutex::Autolock _l(mLock);
        if (state) {
            ::stop(&state);
        }
        state = (VideoState *) states.at((unsigned int) index);
        ::setListener(&state, this, notifyListener);
        if (state != 0) {
            mPlayerState = MEDIA_PLAYER_PREPARED;
            err = NO_ERROR;
        }
    }

    return err;
}

int fixFile(std::string broken, std::string ok) {
    Mp4 mp4;

    try {
        mp4.open(ok);
//        if(info) {
//            mp4.printMediaInfo();
//            mp4.printAtoms();
//        }
//        if(analyze) {
//            mp4.analyze();
//        }
        if (broken.size()) {
            mp4.repair(broken);
            mp4.saveVideo(broken);
        }
    } catch (std::string e) {
        LOGI("%s", e.c_str());
        return -1;
    }
    return 0;
}

status_t MediaPlayer::setDataSource(const char *urls[], int size) {
    status_t err = BAD_VALUE;
    state = NULL;
    if (size <= 0) {
        return err;
    }
    VideoState *previous = NULL;
//    states.reserve((unsigned int) size);
    for (int i = 0; i < size; i++) {
        int triedFix = 0;
        beginning:
        const char *url = urls[i];
        if (url != NULL) {
            VideoState *state = ::create();
            err = ::setDataSourceURI(&state, url);
            if (previous != NULL) {
                state->previous = previous;
                previous->next = state;
            }
            state->fps_delay_ptr = &mFpsDelay;
            state->backwards = &mBackwards;
            state->seeking = &mSeeking;
            state->native_window = (size_t *) &native_window;
            setDataSource(state);
            status_t ret = ::prepare(&state);
            //status_t ret = prepareAsync_l();
            if (ret != NO_ERROR) {
                if (ret == AVERROR_INVALIDDATA && !triedFix) {
                    triedFix = 1;
                    VideoState* vs;
                    if (state->previous){
                        vs = (VideoState *) state->previous;
                    } else {
                        vs = (VideoState*) state->next;
                    }
                    if (vs && state != vs) {
                        std::string broken(state->filename);
                        std::string ok(vs->filename);
                        broken.erase(0,7);
                        ok.erase(0,7);
                        ::disconnect(&vs);
                        ::disconnect(&state);
                        int resolution = fixFile(broken, ok);
                        LOGI("Mp4 file corrupted, tried fix, result = %i",resolution);
                        states.clear();
                        i = 0;
                        goto beginning;
                    } else {
                        continue;
                    }
                }
                //mLockThreadId = 0;
                return ret;
            }
            previous = state;
        }
    }
    if (mLoop) {//tie the ends together if looping mode
        VideoState *first = (VideoState *) states.at(0);
        VideoState *last = (VideoState *) states.at(states.size() - 1);
        first->previous = last;
        last->next = first;
    }
    setCurrentPlayer(0);
    mPlayerState = MEDIA_PLAYER_PREPARED;
    return err;
}

status_t MediaPlayer::setVideoSurface(ANativeWindow *window) {
    LOGI("setVideoSurface");
    status_t err;
    //Mutex::Autolock _l(mLock);
    native_window = window;
    err = NO_ERROR;
    return err;
}

// must call with lock held
//status_t MediaPlayer::prepareAsync_l() {
//    if (mPlayerState & (MEDIA_PLAYER_INITIALIZED | MEDIA_PLAYER_STOPPED))) {
//        mPlayerState = MEDIA_PLAYER_PREPARING;
//        return ::prepareAsync(&state);
//    }
//    LOGI("prepareAsync called in state %d", mPlayerState);
//    return INVALID_OPERATION;
//}

// TODO: In case of error, prepareAsync provides the caller with 2 error codes,
// one defined in the Android framework and one provided by the implementation
// that generated the error. The sync version of prepare returns only 1 error
// code.
//status_t MediaPlayer::prepare() {
//    LOGI("prepare");
//    //Mutex::Autolock _l(mLock);
//    //mLockThreadId = getThreadId();
//    if (mPrepareSync) {
//        //mLockThreadId = 0;
//        return -EALREADY;
//    }
//    mPrepareSync = true;
//
//    status_t ret = ::prepare(&state);
//    //status_t ret = prepareAsync_l();
//    if (ret != NO_ERROR) {
//        //mLockThreadId = 0;
//        return ret;
//    }
//    if (mPrepareSync) {
//        //mSignal.wait(mLock); // wait for prepare done
//        mPrepareSync = false;
//    }
//    LOGI("prepare complete - status = %d", mPrepareStatus);
//    //mLockThreadId = 0;
//    return mPrepareStatus;
//}

//status_t MediaPlayer::prepareAsync() {
//    LOGI("prepareAsync");
//    //Mutex::Autolock _l(mLock);
//    return prepareAsync_l();
//}

status_t MediaPlayer::start() {
    LOGI("start");
    //Mutex::Autolock _l(mLock);
    if (mPlayerState & MEDIA_PLAYER_STARTED)
        return NO_ERROR;
    if ((state != 0) && (mPlayerState & (MEDIA_PLAYER_PREPARED | MEDIA_PLAYER_PAUSED))) {
        mPlayerState = MEDIA_PLAYER_STARTED;
        status_t ret = ::start(&state);
        if (ret != NO_ERROR) {
            mPlayerState = MEDIA_PLAYER_STATE_ERROR;
        }
        return ret;
    }
    LOGI("start called in state %d", mPlayerState);
    return INVALID_OPERATION;
}

status_t MediaPlayer::stop() {
    LOGI("stop");
    //Mutex::Autolock _l(mLock);
    if (mPlayerState & MEDIA_PLAYER_STOPPED) return NO_ERROR;
    if ((state != 0) && (mPlayerState & (MEDIA_PLAYER_STARTED | MEDIA_PLAYER_PREPARED |
                                         MEDIA_PLAYER_PAUSED))) {
        status_t ret = ::stop(&state);
        if (ret != NO_ERROR) {
            mPlayerState = MEDIA_PLAYER_STATE_ERROR;
        } else {
            mPlayerState = MEDIA_PLAYER_STOPPED;
        }
        return ret;
    }
    LOGI("stop called in state %d", mPlayerState);
    return INVALID_OPERATION;
}

status_t MediaPlayer::pause() {
    LOGI("pause");
    //Mutex::Autolock _l(mLock);
    if (mPlayerState & (MEDIA_PLAYER_PAUSED))
        return NO_ERROR;
    if ((state != 0) && (mPlayerState & (MEDIA_PLAYER_STARTED | MEDIA_PLAYER_PREPARED))) {
        status_t ret = ::pause_l(&state);
        if (ret != NO_ERROR) {
            mPlayerState = MEDIA_PLAYER_STATE_ERROR;
        } else {
            mPlayerState = MEDIA_PLAYER_PAUSED;
        }
        return ret;
    }
    LOGI("pause called in state %d", mPlayerState);
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
//        LOGI("isPlaying: %d", temp);
        if ((mPlayerState & MEDIA_PLAYER_STARTED) && !temp) {
            LOGI("internal/external state mismatch corrected");
            mPlayerState = MEDIA_PLAYER_PAUSED;
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

status_t MediaPlayer::getCurrentPosition(int *gIndex) {
    LOGI("getCurrentPosition");
    //Mutex::Autolock _l(mLock);
    if (states.size() <= 0) {
        *gIndex = mapLocalIndexToGlobal(state->file_index, state->pkt_index);
        return NO_ERROR;
    }
    return INVALID_OPERATION;
}

status_t MediaPlayer::getDuration_l(int *msec) {
//    LOGI("getDuration");
    bool isValidState = (mPlayerState &
                         (MEDIA_PLAYER_PREPARED | MEDIA_PLAYER_STARTED | MEDIA_PLAYER_PAUSED | MEDIA_PLAYER_STOPPED));
    if (states.size() <= 0 && isValidState) {
        status_t ret = NO_ERROR;
        int tempCount = 0;
        for (int i = 0; i < states.size(); ++i) {
            VideoState *state = (VideoState *) states.at((unsigned int) i);
            tempCount = tempCount + state->frame_count;
        }
        *msec = tempCount;
        return ret;
    } else {
        if (states.size() <= 0) {
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

status_t MediaPlayer::setFPSDelay_l(bool fast) {
//    LOGI("getDuration");
    status_t ret = NO_ERROR;
    if (fast) {
        mFpsDelay = FAST_FPS_DELAY;
    } else {
        mFpsDelay = DEFAULT_FPS_DELAY;
    }
    return ret;
}

status_t MediaPlayer::setFPSDelay(bool fast) {
    //Mutex::Autolock _l(mLock);
    return setFPSDelay_l(fast);
}

void MediaPlayer::jumpTo(int fileIndex) {
    LOGI("JumpTo file from %d to %d", state->file_index, fileIndex);
    setCurrentPlayer(fileIndex);
    start();
}

status_t MediaPlayer::seekTo_l(int video, int index) {
    if ((state != 0) && (mPlayerState & (MEDIA_PLAYER_STARTED | MEDIA_PLAYER_PREPARED | MEDIA_PLAYER_PAUSED))) {
        if (video != state->file_index) {
            jumpTo(video);
        }
        if (index < 0) {
            LOGI("Attempt to seek to invalid position: %d", index);
            index = 0;
        } else if (index >= state->frame_count) {
            LOGI("Attempt to seek to past end of file: request = %d, EOF = %d", index, (int) state->frame_count);
            index = (int) (state->frame_count - 1);
        }
        // cache duration
//        mCurrentPosition = msec;
//        if (mSeekPosition < 0) {
//            getDuration_l(NULL);

        return ::seekTo(&state, index);
//        } else {
//            LOGI("Seek in progress - queue up seekTo[%d]", msec);
//            return NO_ERROR;
//        }
    }
    LOGI("Attempt to perform seekTo in wrong state: mPlayer=%p, mPlayerState=%u", state, mPlayerState);
    return INVALID_OPERATION;
}

status_t MediaPlayer::seekTo(int index) {
    std::pair<int, int> data;
    mapGlobalIndexToLocal(index, &data);
    status_t result = seekTo_l(data.first, data.second);

    return result;
}

status_t MediaPlayer::reset() {
    LOGI("reset");
    //Mutex::Autolock _l(mLock);
    mLoop = false;
    if (mPlayerState == MEDIA_PLAYER_IDLE) return NO_ERROR;

    for (int i = 0; i < states.size(); i++) {
        VideoState *state = (VideoState *) states[i];
        if (state != 0) {
            status_t ret = ::reset(&state);
            if (ret != NO_ERROR) {
                LOGI("reset() failed with return code (%d)", ret);
                mPlayerState = MEDIA_PLAYER_STATE_ERROR;
            } else {
                mPlayerState = MEDIA_PLAYER_IDLE;
            }
        }
    }
    clear_l();
    return NO_ERROR;
}


status_t MediaPlayer::setLooping(int loop) {
    mLoop = (loop != 0);
    return OK;
}

bool MediaPlayer::isLooping() {
    if (state != 0) {
        return mLoop;
    }
    return false;
}

void MediaPlayer::notify(void *is, int msg, int ext1, int ext2, int fromThread) {
//    LOGI("message received msg=%d, ext1=%d, ext2=%d", msg, ext1, ext2);
    bool send = true;
    bool locked = false;
    int temp = 0;
    VideoState *tempState;

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
    if (!(msg == MEDIA_ERROR && mPlayerState == MEDIA_PLAYER_IDLE) && state == 0) {
        LOGI("notify(%d, %d, %d) callback on disconnected mediaplayer", msg, ext1, ext2);
        //if (locked) mLock.unlock(); // release the lock when done.
        return;
    }

    switch (msg) {
        case MEDIA_NOP: // interface test message
            break;
        case MEDIA_PREPARED:
            LOGI("prepared");
            mPlayerState = MEDIA_PLAYER_PREPARED;
            break;
        case MEDIA_PLAYBACK_COMPLETE:
            LOGI("playback complete");
            if (mPlayerState == MEDIA_PLAYER_IDLE) {
                LOGI("playback complete in idle state");
            }
            if (!mLoop) {
                mPlayerState = MEDIA_PLAYER_PREPARED;
            }
            break;
        case MEDIA_ERROR:
            // Always log errors.
            // ext1: Media framework error code.
            // ext2: Implementation dependant error code.
            LOGI("error (%d, %d)", ext1, ext2);
            mPlayerState = MEDIA_PLAYER_STATE_ERROR;
            break;
            /*case MEDIA_INFO:
                // ext1: Media framework error code.
                // ext2: Implementation dependant error code.
                LOGI("info/warning (%d, %d)", ext1, ext2);
                break;*/
        case MEDIA_SEEK_COMPLETE:
//            if (mSeekPosition != mCurrentPosition) {
//                seekTo_l(mCurrentPosition);
//            }
//            else {
//                LOGI("All seeks complete - return to regularly scheduled program");
//                mCurrentPosition = mSeekPosition = -1;
//            }
            break;
        case MEDIA_BUFFERING_UPDATE:
            LOGI("buffering %d", ext1);
            break;
        case MEDIA_ON_FRAME:
            tempState = (VideoState *) states.at((unsigned int) ext1);
            if (state != tempState) {
                //synchronization bug, 2 files are playing
                ::stop(&tempState);
            } else {
                temp = ext2;
                ext2 = mapLocalIndexToGlobal(ext1, ext2);
                LOGI("onFrame video %d, frame %d,   ---   GlobalFrame %d", ext1, temp, ext2);
            }
            break;
        case MEDIA_ON_NEXT_FILE:
            LOGI("onNextFile video %d", ext1);
            setCurrentPlayer(ext1);
            ::seekTo(&state, 0);
            start();
            if (ext2) {
                pause();
            }
            break;
        case MEDIA_ON_PREVIOUS_FILE:
            LOGI("onPreviousFile video %d", ext1);
            setCurrentPlayer(ext1);
            ::seekTo(&state, state->frame_count);
            start();
            if (ext2) {
                pause();
            }
            break;
        case MEDIA_PLAYBACK_PLAYING:
            LOGI("Playing video from %d, frame %d", ext1, ext2);
            break;
        case MEDIA_PLAYBACK_PAUSED:
            LOGI("Paused video at %d, frame %d", ext1, ext2);
            break;
        case MEDIA_SET_VIDEO_SIZE:
            LOGI("New video size %d x %d", ext1, ext2);
            mVideoWidth = ext1;
            mVideoHeight = ext2;
            break;
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

int MediaPlayer::stepFrame(bool forward) {
    bool isValidState = (mPlayerState &
                         (MEDIA_PLAYER_PREPARED | MEDIA_PLAYER_STARTED | MEDIA_PLAYER_PAUSED));
    if (state && isValidState) {
        if (forward) {
            mBackwards = 0;
            ::stepForward(&state);
        } else {
            mBackwards = 1;
            ::stepBackward(&state);
        }

    }
    return OK;
}

int MediaPlayer::setBackwards(bool backwards) {
    mBackwards = backwards;
    return OK;
}

int MediaPlayer::seeking(bool started) {
    mSeeking = started;
    return OK;
}






