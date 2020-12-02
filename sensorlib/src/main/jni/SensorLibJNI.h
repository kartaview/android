#include <jni.h>
#include <sl/SensorLibExports.h>
#include <map>

extern "C" {

JNIEXPORT
bool JNICALL
        Java_com_skobbler_sensorlib_SensorLib_init(JNIEnv *env, jobject obj, jobject templates, jobject dashLib, jstring pathToTessdata);

JNIEXPORT
jobjectArray JNICALL
        Java_com_skobbler_sensorlib_SensorLib_getfilenames(JNIEnv * env, jobject obj);

JNIEXPORT
void JNICALL
        Java_com_skobbler_sensorlib_SKSensorLib_applytophoto(JNIEnv *env, jobject obj, jobject mat, jobject retroMat);

JNIEXPORT
void JNICALL
        Java_com_skobbler_sensorlib_SensorLib_processFrame(JNIEnv *env, jobject obj, jobject mat);

JNIEXPORT
jobject JNICALL
        Java_com_skobbler_sensorlib_SKSensorLib_getrois(JNIEnv *env, jobject obj);

JNIEXPORT
void JNICALL
        Java_com_skobbler_sensorlib_SensorLib_destroynative(JNIEnv *env, jobject obj);

JNIEXPORT
void JNICALL
        Java_com_skobbler_sensorlib_SensorLib_setprocessingsettings(JNIEnv *env, jobject obj, int minFramesNrForDetection, int maxLostTrackFrames);

JNIEXPORT
bool JNICALL
        Java_com_skobbler_sensorlib_SKSensorLib_startrecordingsession(JNIEnv *env, jobject obj);

JNIEXPORT
bool JNICALL
        Java_com_skobbler_sensorlib_SKSensorLib_stoprecordingsession(JNIEnv *env, jobject obj);

JNIEXPORT
void JNICALL
        Java_com_skobbler_sensorlib_SKSensorLib_playbackvideo(JNIEnv *env, jobject obj, jstring path);

JNIEXPORT
void JNICALL
        Java_com_skobbler_sensorlib_SKSensorLib_setstoragepath(JNIEnv *env, jobject obj, jstring path);

JNIEXPORT
void JNICALL
        Java_com_skobbler_sensorlib_SKSensorLib_applyToYuv420(JNIEnv *env, jobject callingObject, jbyteArray array, int width, int height, jobject retroMat);

JNIEXPORT
void JNICALL
        Java_com_skobbler_sensorlib_SKSensorLib_convertWithoutProcessing(JNIEnv *env, jobject callingObject, jbyteArray array, int width, int height, jobject retroMat);
}