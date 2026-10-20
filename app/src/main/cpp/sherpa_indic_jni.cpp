#include <jni.h>
#include <string>
#include <android/log.h>
#include "audio_router.h"

#define TAG "SherpaIndicJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_agent_s25_domain_indic_IndicSpeechBridge_nativeInitRecognizer(
        JNIEnv* env,
        jobject /* this */,
        jstring modelDir) {
    const char* dir = env->GetStringUTFChars(modelDir, nullptr);
    LOGI("Sherpa-ONNX Indic Zipformer initialized from: %s", dir);
    env->ReleaseStringUTFChars(modelDir, dir);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_agent_s25_domain_indic_IndicSpeechBridge_nativeFeedAudioSamples(
        JNIEnv* env,
        jobject /* this */,
        jshortArray samples,
        jint length) {
    jshort* buf = env->GetShortArrayElements(samples, nullptr);
    AudioStreamRouter::getInstance().pushAudioBuffer(reinterpret_cast<const int16_t*>(buf), length);
    env->ReleaseShortArrayElements(samples, buf, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_agent_s25_domain_indic_IndicSpeechBridge_nativeSetAudioRoute(
        JNIEnv* /* env */,
        jobject /* this */,
        jint routeCode) {
    // 0: WAKE_WORD, 1: INDIC_STT, 2: BOTH, 3: MUTED
    AudioStreamRouter::getInstance().setDestination(static_cast<AudioDestination>(routeCode));
}

} // extern "C"
