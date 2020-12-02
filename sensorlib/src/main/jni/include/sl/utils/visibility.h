#ifndef ORBB_UTILS_VISIBILITY_H
#define ORBB_UTILS_VISIBILITY_H

#if (defined WIN32 || defined _WIN32 || defined WINCE || defined __CYGWIN__)
#  define ORBB_EXPORTS __declspec(dllexport)
#elif (defined __GNUC__ && __GNUC__ >= 4)
#  define ORBB_EXPORTS __attribute__ ((visibility ("default")))
#elif (defined __llvm__ || defined __clang__)
#  define ORBB_EXPORTS __attribute__ ((visibility ("default")))
#else
#  define ORBB_EXPORTS
#endif

#endif // ORBB_UTILS_VISIBILITY_H
