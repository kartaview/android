//
// Created by Kalman Bencze on 05/10/16.
//
#include "crash_handler.h"
#include "unwind.h"
#include <asm/siginfo.h>
#include <signal.h>
#include <unistd.h>
#include <sstream>
#include <stdlib.h>

#ifdef ANDROID
#include <android/log.h>
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "FFMPEG E ", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "FFMPEG I ", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("FFMPEG E " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("FFMPEG I " format "\n", ##__VA_ARGS__)
#endif

struct sigaction psa, oldPsa;

void handleCrash(int signalNumber, siginfo_t *sigInfo, void *context) {
    static volatile sig_atomic_t fatal_error_in_progress = 0;
    if (fatal_error_in_progress) //Stop a signal loop.
        _exit(1);
    fatal_error_in_progress = 1;

    char *j;
    asprintf(&j, "Crash Signal: %d, crashed on: %x, UID: %ld\n", signalNumber, (long) sigInfo->si_addr, (long) sigInfo->si_uid);  //%x prints out the faulty memory address in hex
    LOGE("%s", j);

    const size_t max = 30;
    void* buffer[max];
    std::ostringstream oss;

    dumpBacktrace(oss, buffer, captureBacktrace(buffer, max));

    LOGE("%s", oss.str().c_str());
//    getStackTrace();
//    sigaction(signalNumber, &oldPsa, NULL);
}

void initSignalHandler() {
    LOGI("Crash handler started");

    psa.sa_sigaction = handleCrash;
    psa.sa_flags = SA_SIGINFO;

    sigaction(SIGBUS, &psa, &oldPsa);
    sigaction(SIGSEGV, &psa, &oldPsa);
    sigaction(SIGSYS, &psa, &oldPsa);
    sigaction(SIGFPE, &psa, &oldPsa);
    sigaction(SIGILL, &psa, &oldPsa);
    sigaction(SIGHUP, &psa, &oldPsa);
}