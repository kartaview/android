#ifndef UNWIND_H
#define UNWIND_H

#include <iostream>
#include <iomanip>

#include <unwind.h>
#include <dlfcn.h>
#include <stdint.h>

#ifdef ANDROID
#include <android/log.h>
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "FFMPEG E ", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "FFMPEG I ", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("FFMPEG_MEDIAPLAYER_CPP E " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("FFMPEG_MEDIAPLAYER_CPP I " format "\n", ##__VA_ARGS__)
#endif


namespace {

    struct BacktraceState {
        void **current;
        void **end;
    };

    static _Unwind_Reason_Code unwindCallback(struct _Unwind_Context *context, void *arg) {
        BacktraceState *state = static_cast<BacktraceState *>(arg);
        uintptr_t pc = _Unwind_GetIP(context);
        if (pc) {
            if (state->current == state->end) {
                return _URC_END_OF_STACK;
            } else {
                *state->current++ = reinterpret_cast<void *>(pc);
            }
        }
        return _URC_NO_REASON;
    }

}

static size_t captureBacktrace(void **buffer, size_t max) {
    BacktraceState state = {buffer, buffer + max};
    _Unwind_Backtrace(unwindCallback, &state);

    return state.current - buffer;
}

static void dumpBacktrace(std::ostream &os, void **buffer, size_t count) {
    for (size_t idx = 0; idx < count; ++idx) {
        const void *addr = buffer[idx];
        const char *symbol = "";
        const char *path = "";
        const void *addr2 = "";

        Dl_info info;
        int a = dladdr(addr, &info);
        if (a && info.dli_sname) {
            symbol = info.dli_sname;
        }
        if (a && info.dli_fname){
            path = info.dli_fname;
        }
        if (a && info.dli_saddr){
            addr2 = info.dli_saddr;
        }

        LOGE("# %x : %x %s \n                                            %s", idx,addr,symbol,path);
        os << "  #" << std::setw(2) << idx << ": " << addr << "  " << addr2 << "  " << symbol << "\n                                         " << path << "\n\n";
    }
}

//static void backtraceToLogcat();

#endif