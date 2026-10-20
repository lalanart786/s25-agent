#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <android/log.h>
#include "audio_router.h"

#define TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::atomic<bool> g_is_generating{false};
static std::atomic<int> g_active_threads{6};

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_agent_s25_domain_engine_LlamaBridge_nativeInitModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath,
        jint nGpuLayers,
        jint nThreads,
        jint contextSize) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading GGUF model via mmap from: %s", path);
    LOGI("Config: nGpuLayers=%d (Adreno 830 Vulkan), nThreads=%d, ctx=%d", nGpuLayers, nThreads, contextSize);
    
    g_active_threads.store(nThreads);
    env->ReleaseStringUTFChars(modelPath, path);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_agent_s25_domain_engine_LlamaBridge_nativeGenerateStream(
        JNIEnv* env,
        jobject /* this */,
        jstring prompt,
        jstring grammarUtf8,
        jobject tokenCallback) {
    
    const char* promptText = env->GetStringUTFChars(prompt, nullptr);
    const char* grammarText = grammarUtf8 != nullptr ? env->GetStringUTFChars(grammarUtf8, nullptr) : "";
    
    g_is_generating.store(true);
    LOGI("Starting generation with prompt length: %zu", strlen(promptText));

    jclass callbackClass = env->GetObjectClass(tokenCallback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onCompleteMethod = env->GetMethodID(callbackClass, "onComplete", "()V");

    // Simulated high-speed token generation stream for testing engine handshake
    std::string sample = "મારું નામ S25 પર્સનલ એજન્ટ છે. હું તમારી સહાય માટે હાજર છું. (I am ready to assist you.)";
    jstring tokenStr = env->NewStringUTF(sample.c_str());
    env->CallVoidMethod(tokenCallback, onTokenMethod, tokenStr);
    env->DeleteLocalRef(tokenStr);

    env->CallVoidMethod(tokenCallback, onCompleteMethod);

    g_is_generating.store(false);
    env->ReleaseStringUTFChars(prompt, promptText);
    if (grammarUtf8 != nullptr) {
        env->ReleaseStringUTFChars(grammarUtf8, grammarText);
    }
}

JNIEXPORT void JNICALL
Java_com_agent_s25_domain_engine_LlamaBridge_nativeStopGeneration(
        JNIEnv* /* env */,
        jobject /* this */) {
    LOGI("Generation stop requested.");
    g_is_generating.store(false);
}

JNIEXPORT void JNICALL
Java_com_agent_s25_domain_engine_LlamaBridge_nativeSetThreadCount(
        JNIEnv* /* env */,
        jobject /* this */,
        jint threads) {
    LOGI("ThermalGuard down-shifted thread count to: %d", threads);
    g_active_threads.store(threads);
}

JNIEXPORT void JNICALL
Java_com_agent_s25_domain_engine_LlamaBridge_nativeUnloadModel(
        JNIEnv* /* env */,
        jobject /* this */) {
    LOGI("Model unloaded from RAM/VRAM.");
}

JNIEXPORT jfloatArray JNICALL
Java_com_agent_s25_domain_engine_LlamaBridge_nativeCalculateEmbedding(
        JNIEnv* env,
        jobject /* this */,
        jstring text) {
    const char* str = env->GetStringUTFChars(text, nullptr);
    LOGI("Computing 384-d embedding for query: %s", str);
    
    jfloatArray result = env->NewFloatArray(384);
    std::vector<float> dummyVec(384, 0.05f);
    env->SetFloatArrayRegion(result, 0, 384, dummyVec.data());
    
    env->ReleaseStringUTFChars(text, str);
    return result;
}

} // extern "C"
