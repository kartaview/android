//
// Created by Kalman Bencze on 05/10/16.
//
#include "crash_handler.h"
#include "unwind.h"
#include <unistd.h>
#include <sstream>
#include <assert.h>

#ifdef ANDROID
#include <android/log.h>
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "FFMPEG E ", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "FFMPEG I ", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("FFMPEG E " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("FFMPEG I " format "\n", ##__VA_ARGS__)
#endif

/* Maximum value of a caught signal. */
#define SIG_NUMBER_MAX 32

/* Signals to be caught. */
#define SIG_CATCH_COUNT 7
static const int native_sig_catch[SIG_CATCH_COUNT + 1]
        = {SIGABRT, SIGILL, SIGTRAP, SIGBUS, SIGFPE, SIGSEGV
#ifdef SIGSTKFLT
                , SIGSTKFLT
#endif
                , 0};
struct sigaction *sa_old;

void (*callbackAddress)();

void upcase(char *s) {
    while (*s) {
        *s = toupper(*s);
        s++;
    }
}

/* Call the old handler. */
static void call_old_signal_handler(const int code, siginfo_t *const si,
                                    void *const sc) {
    /* Call the "real" Java handler for JIT and internals. */
    if (code >= 0 && code < SIG_NUMBER_MAX) {
        if (sa_old[code].sa_sigaction != NULL) {
            LOGI("entered verbose crash");
            sa_old[code].sa_sigaction(code, si, sc);
        } else if (sa_old[code].sa_handler != NULL) {
            LOGI("entered small crash");
            sa_old[code].sa_handler(code);
        }
    }
    LOGI("exiting old signaling mechanism");
}

void *notifyThread(void *arg) {
    if (callbackAddress != NULL) {
        callbackAddress();
    }
}

void handleCrash(int signalNumber, siginfo_t *sigInfo, void *context) {
    static volatile sig_atomic_t fatal_error_in_progress = 0;
    if (fatal_error_in_progress) //Stop a signal loop.
        _exit(1);
    fatal_error_in_progress = 1;

    char *j;
    char *signumber = strdup(sys_signame[signalNumber]);
    upcase(signumber);
    asprintf(&j, "Crash Signal: %s, crashed on: %li, UID: %ld\n", signumber, (long) sigInfo->si_addr, (long) sigInfo->si_uid);  //%x prints out the faulty memory address in hex
    LOGE("%s", j);

    const size_t max = 30;
    void *buffer[max];
    std::ostringstream oss;

    bool encodingCrash = dumpBacktrace(oss, buffer, captureBacktrace(buffer, max));

    LOGE("%s", oss.str().c_str());
    if (encodingCrash) {
        pthread_t *id = (pthread_t *) malloc(sizeof(pthread_t));
        pthread_create(id, NULL, notifyThread, NULL);
        pthread_join(*id, NULL);
    }
    call_old_signal_handler(signalNumber, sigInfo, context);
}

extern "C" {

int initSignalHandler(void (*response)()) {
    LOGI("Crash handler started");

    callbackAddress = response;
    if (sa_old != NULL) {
        LOGE("Crash handler already set up");
    }
    size_t i;
    struct sigaction sa_pass;

    memset(&sa_pass, 0, sizeof(sa_pass));
    sigemptyset(&sa_pass.sa_mask);
    sa_pass.sa_sigaction = handleCrash;
    sa_pass.sa_flags = SA_SIGINFO | SA_ONSTACK;

    /* Allocate */
    sa_old = (struct sigaction *) calloc(sizeof(struct sigaction), SIG_NUMBER_MAX);
    if (sa_old == NULL) {
        return -1;
    }

    /* Setup signal handlers for SIGABRT (Java calls abort()) and others. **/
    for (i = 0; native_sig_catch[i] != 0; i++) {
        const int sig = native_sig_catch[i];
        const struct sigaction *const action = &sa_pass;
        assert(sig < SIG_NUMBER_MAX);
        if (sigaction(sig, action, &sa_old[sig]) != 0) {
            return -1;
        }
    }

    /* OK. */
    return 0;
}
}