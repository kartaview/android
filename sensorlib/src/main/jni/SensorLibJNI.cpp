#include <cstdio>
#include <iostream>
#include <vector>

#include <jni.h>
#include "SensorLibJNI.h"

#include <sl/SensorLib.h>
#include <sl/SLSettings.h>
#include <sl/settings_factory_singleton.h>
#include <opencv2/opencv.hpp>

#ifdef ANDROID
#include <android/log.h>
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "SensorLib E ", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "SensorLib I ", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("SensorLib E " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("SensorLib I " format "\n", ##__VA_ARGS__)
#endif

void GetJStringContent(JNIEnv *AEnv, jstring AStr, std::string &ARes) {
    if (!AStr) {
        ARes.clear();
        return;
    }

    const char *s = AEnv->GetStringUTFChars(AStr, NULL);
    ARes = s;
    AEnv->ReleaseStringUTFChars(AStr, s);
}

class JniNG;

static JniNG *gJNI = 0;

class JniNG {
    JavaVM *mJVM;

public:
    JniNG(JavaVM *jvm, JNIEnv *&env) : mJVM(jvm) { }

    ~JniNG() { }

    JavaVM *jvm() { return mJVM; }

    static jint init(JavaVM *jvm) {
        JNIEnv *env;

        if ((jvm->GetEnv((void **) &env, JNI_VERSION_1_6) < 0) ||
            (jvm->AttachCurrentThread(&env, NULL) < 0)) {
            return JNI_ERR; // BAD CASE !!!
        }

        gJNI = new JniNG(jvm, env);
        return JNI_VERSION_1_6;
    }
};

struct JniMethodInfo {
    JNIEnv *env;
    jclass classID;
    jmethodID methodID;
};

// JNI global
JavaVM *gJVM = NULL; // fill in OnLoad
jobject gJSKSensorLibObjectCached = NULL;
JniMethodInfo gJMethodInfo;

std::string &tessdataPath() {
    static std::string path;
    return path;
}

static SL::enSettingsType settingType = SL::eSLUSClient;

SL::SensorLib &gSensorLib() {
    static SL::SensorLib sl = SL::SensorLib(tessdataPath(), settingType);
    return sl;
}

jclass jGMatclass = NULL;
jmethodID jGGetMatPtrMethod = NULL;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void * /*reserved*/) {
    jint result = JniNG::init(jvm);

    if (result == JNI_VERSION_1_6) {
        gJVM = gJNI->jvm();
    }
    return result;
}

jobjectArray ProcessFileMap(JNIEnv *env,
               const std::map<SL::enSignType, std::string> &files) {
    jobjectArray jFileResults;

    jclass dataClass;
    jclass parentClass;
    parentClass = env->FindClass("java/util/ArrayList");
    dataClass = env->FindClass("com/skobbler/sensorlib/sign/SignType");

    if (files.empty()) {
        jFileResults = env->NewObjectArray(0, dataClass, NULL);
    } else {
        jFileResults = env->NewObjectArray(files.size(), dataClass, NULL);
        int i = 0;
        std::map<SL::enSignType, std::string>::const_iterator it = files.begin();
        for (; it != files.end(); ++it, ++i) {

            // do not create new objects, get ref
            const SL::enSignType type = it->first;
            const std::string &name = it->second;
            const jstring jStringProperty = env->NewStringUTF(name.c_str());

            jmethodID midConstructor = env->GetMethodID(dataClass, "<init>", "()V");
            jmethodID jmethod;

            jobject jResult = env->NewObject(dataClass, midConstructor);

            jmethod = env->GetMethodID(dataClass, "setSignType", "(I)V");
            env->CallVoidMethod(jResult, jmethod, type);

            jmethod =
                    env->GetMethodID(dataClass, "setFileName", "(Ljava/lang/String;)V");
            env->CallVoidMethod(jResult, jmethod, jStringProperty);
            env->DeleteLocalRef(jStringProperty);
            env->SetObjectArrayElement(jFileResults, i, jResult);
            env->DeleteLocalRef(jResult);
        }
    }

    return jFileResults;
}

static JNIEnv *getJNIEnv(void) {

    JavaVM *jvm = gJNI->jvm();
    if (NULL == jvm) {
        LOGI(
                "Failed to get JNIEnv. JniHelper::getJavaVM() is NULL");
        return NULL;
    }

    JNIEnv *env = NULL;
    // get jni environment
    jint ret = jvm->GetEnv((void **) &env, JNI_VERSION_1_6);

    switch (ret) {
        case JNI_OK:
            // Success!
            return env;
        case JNI_EDETACHED:
            if (jvm->AttachCurrentThread(&env, NULL) < 0) {
                LOGI(
                        "Failed to get the environment using AttachCurrentThread()");
                return NULL;
            } else {
                // Success : Attached and obtained JNIEnv!
                return env;
            }

        case JNI_EVERSION:
            // Cannot recover from this error
            LOGI("JNI interface version 1.6 not supported");
        default:
            LOGI("Failed to get the environment using GetEnv()");
            return NULL;
    }
}

// get class and make it a global reference, release it at endJni().
static jclass getClassID(JNIEnv *pEnv) {
    jclass ret = pEnv->FindClass("com/skobbler/sensorlib/SensorLib");
    if (!ret) {
        LOGI(
                "Failed to find class of com/skobbler/sensorlib/SensorLib");
    }

    return ret;
}

static bool getMethodInfo(const char *methodName, const char *paramCode) {
    jmethodID methodID = 0;
    JNIEnv *pEnv = 0;
    bool bRet = false;

    do {
        pEnv = getJNIEnv();
        if (!pEnv) {
            break;
        }

        jclass classID = getClassID(pEnv);

        methodID = pEnv->GetMethodID(classID, methodName, paramCode);
        if (!methodID) {
            LOGI("Failed to find static method id of %s",
                 methodName);
            break;
        }

        gJMethodInfo.classID = classID;
        gJMethodInfo.env = pEnv;
        gJMethodInfo.methodID = methodID;

        bRet = true;
    } while (0);

    return bRet;
}

void InitializeGlobals(JNIEnv *env) {

    getMethodInfo("signdetectedcallback", "(I)V");

    jGMatclass = env->FindClass("org/opencv/core/Mat");

    jGGetMatPtrMethod = env->GetMethodID(jGMatclass, "getNativeObjAddr", "()J");
}

std::map<SL::enSignType, cv::Mat> ConvertTemplates(JNIEnv *env,
                                                   jobject thisobject) {
    std::map<SL::enSignType, cv::Mat> result;

    jclass BufferClass =
            env->FindClass("com/skobbler/sensorlib/template/TemplateBuffer");

    jfieldID signtypesfieldid =
            env->GetFieldID(BufferClass, "signTypesArray", "[I");
    jfieldID bufimgsfieldid =
            env->GetFieldID(BufferClass, "matArray", "[Lorg/opencv/core/Mat;");

    // Get the fields
    jobjectArray bufimgsArray =
            (jobjectArray) env->GetObjectField(thisobject, bufimgsfieldid);
    jintArray buftypesArray =
            (jintArray) env->GetObjectField(thisobject, signtypesfieldid);
    // Convert the array

    const jsize length = env->GetArrayLength(buftypesArray);
//    LOGI("original  size %d  \n", length);

    jint *oarr = env->GetIntArrayElements(buftypesArray, NULL);
    for (int i = 0; i < length; i++) {

        SL::enSignType type = (SL::enSignType) oarr[i];
        result.insert(std::pair<SL::enSignType, cv::Mat>(
                type,
                *(cv::Mat *) env->CallLongMethod(
                        env->GetObjectArrayElement(bufimgsArray, i), jGGetMatPtrMethod)));

//        LOGI("idx %d with type %d  \n", i, type);
    }

//    LOGI("result size %d  \n", result.size());
    env->DeleteLocalRef(BufferClass);
    env->DeleteLocalRef(bufimgsArray);
    env->DeleteLocalRef(buftypesArray);
    return result;
}

void signDetectedCallback(const SL::TrackedSignEx sign) {
    if (gJSKSensorLibObjectCached && gJSKSensorLibObjectCached != NULL && gJMethodInfo.methodID != NULL && gJMethodInfo.env != NULL) {
        gJMethodInfo.env = getJNIEnv();
        gJMethodInfo.env->CallVoidMethod(gJSKSensorLibObjectCached,
                                         gJMethodInfo.methodID,
                                         sign.detections_[0].sign_type_);
    }
}

JNIEXPORT jobjectArray JNICALL Java_com_skobbler_sensorlib_SensorLib_getfilenames(JNIEnv *env, jobject obj) {
    std::map<SL::enSignType, std::string> filemap;
    filemap = gSensorLib().getFileNames();
    jobjectArray filesArray = ProcessFileMap(env, filemap);
    return filesArray;
}


JNIEXPORT bool JNICALL Java_com_skobbler_sensorlib_SensorLib_init(
        JNIEnv *env, jobject obj, jobject templates, jobject dashLib, jstring pathToTessdata) {
    GetJStringContent(env, pathToTessdata, tessdataPath());

    // LOGI("size %d elemsize %d
    //    \n",input.total(),input.elemSize());
    gJSKSensorLibObjectCached = env->NewGlobalRef(obj);
    InitializeGlobals(env);
//  LOGI("we update to  %d ", SL::eBGRHSV);
    SL::GlobalSettings(settingType).update_to_color_space(SL::eBGRHSV);
    bool response = gSensorLib().init(ConvertTemplates(env, templates));
    SL::GlobalSettings(settingType).sign_detected_callback_ =
            signDetectedCallback;
    // gSensorLib().trackSignType(SL::eSpeedLimitUSCategory, true);
    SL::GlobalSettings(settingType).update_to_color_space(SL::eYUV420spGRAY);

    return true;
}

JNIEXPORT void JNICALL Java_com_skobbler_sensorlib_SKSensorLib_applytophoto(
        JNIEnv *env, jobject obj, jobject mat, jobject retroMat) {
    gSensorLib().applyToPhoto(
            *(cv::Mat *) env->CallLongMethod(mat, jGGetMatPtrMethod),
            *(cv::Mat *) env->CallLongMethod(retroMat, jGGetMatPtrMethod));
}

std::vector<SL::TrackedSignEx>& sl_rois()
{
    static bool is_init = false;
    static std::vector<SL::TrackedSignEx> rois;

    if (!is_init)
    {
        rois.reserve(128);
        is_init = true;
    }

    return rois;
}

JNIEXPORT void JNICALL Java_com_skobbler_sensorlib_SensorLib_processFrame(JNIEnv *env, jobject obj, jobject mat) {
    cv::Mat *a2 = (cv::Mat *) env->CallLongMethod(mat, jGGetMatPtrMethod);

    std::vector<SL::TrackedSignEx>& rois = sl_rois();
    gSensorLib().processFrame(*a2, NULL, &rois);
}

JNIEXPORT void JNICALL Java_com_skobbler_sensorlib_SensorLib_destroynative(JNIEnv *env, jobject obj) {
//     SL::GlobalSettings().setSignDetectedCallback(NULL);
    LOGI("JNI delete called");
    SL::GlobalSettings(settingType).sign_detected_callback_ = NULL;
    if (gJSKSensorLibObjectCached && gJSKSensorLibObjectCached != NULL) {
        env->DeleteGlobalRef(gJSKSensorLibObjectCached);
    }
    gJMethodInfo.env = NULL;
    gJMethodInfo.methodID = NULL;
    //     env->DeleteGlobalRef(gJMethodInfo.classID);
}

JNIEXPORT void JNICALL Java_com_skobbler_sensorlib_SensorLib_setprocessingsettings(
        JNIEnv *env, jobject obj, int minFramesNrForDetection, int maxLostTrackFrames) {
    SL::GlobalSettings(settingType).frame_count_4_callback_ = (size_t) minFramesNrForDetection;
    SL::GlobalSettings(settingType).max_frame_difference_for_tracking_ = maxLostTrackFrames;
}

JNIEXPORT bool JNICALL Java_com_skobbler_sensorlib_SKSensorLib_startrecordingsession(JNIEnv *env, jobject obj) {
    return gSensorLib().startSession();
}

JNIEXPORT bool JNICALL Java_com_skobbler_sensorlib_SKSensorLib_stoprecordingsession(JNIEnv *env, jobject obj) {
    return gSensorLib().endSession();
}

JNIEXPORT void JNICALL Java_com_skobbler_sensorlib_SKSensorLib_playbackvideo(JNIEnv *env, jobject obj, jstring path) {
    std::string str;
    GetJStringContent(env, path, str);
    // gSensorLib().playbackVideo(str);
}

JNIEXPORT void JNICALL Java_com_skobbler_sensorlib_SKSensorLib_setstoragepath(JNIEnv *env, jobject obj, jstring path) {
    std::string str;
    GetJStringContent(env, path, str);
    SL::GlobalSettings(settingType).set_storage_path(str);
}

JNIEXPORT void JNICALL Java_com_skobbler_sensorlib_SKSensorLib_applyToYuv420(
        JNIEnv *env, jobject callingObject, jbyteArray array, int width, int height, jobject retroMat) {
    int len = env->GetArrayLength(array);
    char *buf = new char[len];
    env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(buf));
    gSensorLib().applyToYUV420SP(
            (const unsigned char *) buf, width, height,
            *(cv::Mat *) env->CallLongMethod(retroMat, jGGetMatPtrMethod));

    //    env->ReleaseByteArrayElements(array, buf, 0);
    delete[] buf;
}

JNIEXPORT void JNICALL Java_com_skobbler_sensorlib_SKSensorLib_convertWithoutProcessing(
        JNIEnv *env, jobject callingObject, jbyteArray array, int width, int height, jobject retroMat) {
    int len = env->GetArrayLength(array);
    char *buf = new char[len];
    env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(buf));
    // gSensorLib().convertWithoutProcessing(buf,width, height,
    // *(cv::Mat*)env->CallLongMethod(retroMat, jGGetMatPtrMethod));

    //    env->ReleaseByteArrayElements(array, buf, 0);
    delete[] buf;
}
