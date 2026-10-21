#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <sstream>
#include <ctime>
#include <unistd.h>
#include <cerrno>
#include <algorithm>
#include <android/log.h>
#include "include/llama.h"
#include "audio_router.h"

#define TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static struct llama_model * g_model = nullptr;
static struct llama_context * g_context = nullptr;
static struct llama_sampler * g_sampler = nullptr;
static std::atomic<bool> g_is_generating{false};
static std::atomic<int> g_active_threads{6};
static std::string g_loaded_path = "";
static FILE * g_model_file = nullptr;

static bool is_valid_utf8(const std::string& str) {
    const unsigned char* bytes = (const unsigned char*)str.data();
    size_t len = str.size();
    size_t i = 0;
    while (i < len) {
        if (bytes[i] <= 0x7F) {
            i += 1;
        } else if ((bytes[i] & 0xE0) == 0xC0) {
            if (i + 1 >= len || (bytes[i+1] & 0xC0) != 0x80) return false;
            i += 2;
        } else if ((bytes[i] & 0xF0) == 0xE0) {
            if (i + 2 >= len || (bytes[i+1] & 0xC0) != 0x80 || (bytes[i+2] & 0xC0) != 0x80) return false;
            i += 3;
        } else if ((bytes[i] & 0xF8) == 0xF0) {
            if (i + 3 >= len || (bytes[i+1] & 0xC0) != 0x80 || (bytes[i+2] & 0xC0) != 0x80 || (bytes[i+3] & 0xC0) != 0x80) return false;
            i += 4;
        } else {
            return false;
        }
    }
    return true;
}

static void free_resources() {
    if (g_sampler != nullptr) {
        llama_sampler_free(g_sampler);
        g_sampler = nullptr;
    }
    if (g_context != nullptr) {
        llama_free(g_context);
        g_context = nullptr;
    }
    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    if (g_model_file != nullptr) {
        fclose(g_model_file);
        g_model_file = nullptr;
    }
}

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
    LOGI("nativeInitModel: Loading GGUF weights: %s", path);

    // If same model already active, reuse
    if (g_model != nullptr && g_loaded_path == std::string(path)) {
        LOGI("Model already initialized and active: %s", path);
        env->ReleaseStringUTFChars(modelPath, path);
        return JNI_TRUE;
    }

    free_resources();
    llama_backend_init();

    struct llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;
    mparams.load_mode = LLAMA_LOAD_MODE_MMAP;

    g_model = llama_model_load_from_file(path, mparams);
    if (g_model == nullptr) {
        LOGE("Failed to load GGUF model from file: %s", path);
        env->ReleaseStringUTFChars(modelPath, path);
        return JNI_FALSE;
    }

    struct llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = contextSize > 0 ? contextSize : 2048;
    cparams.n_threads = nThreads > 0 ? nThreads : 6;
    cparams.n_threads_batch = nThreads > 0 ? nThreads : 6;
    cparams.n_batch = 1024;
    cparams.n_ubatch = 256;

    g_context = llama_init_from_model(g_model, cparams);
    if (g_context == nullptr) {
        LOGE("Failed to create llama_context for model: %s", path);
        llama_model_free(g_model);
        g_model = nullptr;
        env->ReleaseStringUTFChars(modelPath, path);
        return JNI_FALSE;
    }

    g_active_threads.store(nThreads > 0 ? nThreads : 6);
    g_loaded_path = std::string(path);

    LOGI("Model successfully loaded from file & initialized on Snapdragon 8 Elite!");
    env->ReleaseStringUTFChars(modelPath, path);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_agent_s25_domain_engine_LlamaBridge_nativeInitModelFromFd(
        JNIEnv* env,
        jobject /* this */,
        jint fd,
        jint nGpuLayers,
        jint nThreads,
        jint contextSize) {

    LOGI("nativeInitModelFromFd: Loading GGUF directly from descriptor fd: %d", fd);

    free_resources();
    llama_backend_init();

    int dup_fd = dup(fd);
    if (dup_fd < 0) {
        LOGE("Failed to dup fd %d: errno %d", fd, errno);
        return JNI_FALSE;
    }

    FILE* fp = fdopen(dup_fd, "rb");
    if (fp == nullptr) {
        LOGE("Failed to fdopen dup_fd %d: errno %d", dup_fd, errno);
        close(dup_fd);
        return JNI_FALSE;
    }

    struct llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;
    mparams.load_mode = LLAMA_LOAD_MODE_MMAP;

    g_model = llama_model_load_from_file_ptr(fp, mparams);
    if (g_model == nullptr) {
        LOGE("Failed to load GGUF model from file pointer!");
        fclose(fp);
        return JNI_FALSE;
    }
    g_model_file = fp; // Hold open so descriptor remains valid

    struct llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = contextSize > 0 ? contextSize : 2048;
    cparams.n_threads = nThreads > 0 ? nThreads : 6;
    cparams.n_threads_batch = nThreads > 0 ? nThreads : 6;
    cparams.n_batch = 1024;
    cparams.n_ubatch = 256;

    g_context = llama_init_from_model(g_model, cparams);
    if (g_context == nullptr) {
        LOGE("Failed to create llama_context for model from fd %d", fd);
        llama_model_free(g_model);
        g_model = nullptr;
        fclose(fp);
        g_model_file = nullptr;
        return JNI_FALSE;
    }

    g_active_threads.store(nThreads > 0 ? nThreads : 6);
    g_loaded_path = "fd:" + std::to_string(fd);

    LOGI("Model successfully loaded from FileDescriptor via zero-copy mmap!");
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
    g_is_generating.store(true);

    jclass callbackClass = env->GetObjectClass(tokenCallback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onCompleteMethod = env->GetMethodID(callbackClass, "onComplete", "()V");

    if (g_model == nullptr || g_context == nullptr) {
        LOGE("nativeGenerateStream called but model is not initialized!");
        jstring err = env->NewStringUTF("[Error: Model not loaded in VRAM]");
        env->CallVoidMethod(tokenCallback, onTokenMethod, err);
        env->DeleteLocalRef(err);
        env->CallVoidMethod(tokenCallback, onCompleteMethod);
        g_is_generating.store(false);
        env->ReleaseStringUTFChars(prompt, promptText);
        return;
    }

    const struct llama_vocab * vocab = llama_model_get_vocab(g_model);
    int prompt_len = strlen(promptText);

    // 1. Tokenize prompt safely with dynamic sizing
    std::vector<llama_token> tokens_list(prompt_len + 128);
    int n_tokens = llama_tokenize(vocab, promptText, prompt_len, tokens_list.data(), tokens_list.size(), true, true);
    if (n_tokens < 0) {
        int required = -n_tokens;
        if (required > 0 && required < 32768) {
            tokens_list.resize(required);
            n_tokens = llama_tokenize(vocab, promptText, prompt_len, tokens_list.data(), tokens_list.size(), true, true);
        }
    }

    if (n_tokens <= 0) {
        LOGE("Tokenization failed or prompt empty! n_tokens: %d", n_tokens);
        jstring err = env->NewStringUTF("[Error: Tokenization failed]");
        env->CallVoidMethod(tokenCallback, onTokenMethod, err);
        env->DeleteLocalRef(err);
        env->CallVoidMethod(tokenCallback, onCompleteMethod);
        g_is_generating.store(false);
        env->ReleaseStringUTFChars(prompt, promptText);
        return;
    }
    tokens_list.resize(n_tokens);

    LOGI("Tokenized prompt into %d tokens. Beginning inference...", n_tokens);

    // 2. Clear KV memory cache for clean turn
    llama_memory_t mem = llama_get_memory(g_context);
    if (mem) {
        llama_memory_clear(mem, true);
    }

    // 3. Configure sampler chain
    if (g_sampler != nullptr) {
        llama_sampler_free(g_sampler);
        g_sampler = nullptr;
    }
    struct llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    g_sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(g_sampler, llama_sampler_init_penalties(llama_vocab_n_tokens(vocab), 64, 1.15f, 0.1f, 0.1f));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_top_p(0.90f, 1));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_dist(static_cast<uint32_t>(time(nullptr))));

    for (llama_token t : tokens_list) {
        llama_sampler_accept(g_sampler, t);
    }

    // 4. Decode prompt in safe chunks (max 512 tokens per batch) to prevent GGML_ASSERT abort
    const int chunk_size = 512;
    for (int i = 0; i < n_tokens; i += chunk_size) {
        int n_eval = std::min((int)n_tokens - i, chunk_size);
        struct llama_batch chunk_batch = llama_batch_get_one(tokens_list.data() + i, n_eval);
        if (llama_decode(g_context, chunk_batch) != 0) {
            LOGE("llama_decode failed on prompt chunk starting at %d", i);
            jstring err = env->NewStringUTF("[Inference decode error]");
            env->CallVoidMethod(tokenCallback, onTokenMethod, err);
            env->DeleteLocalRef(err);
            env->CallVoidMethod(tokenCallback, onCompleteMethod);
            g_is_generating.store(false);
            env->ReleaseStringUTFChars(prompt, promptText);
            return;
        }
    }

    // 5. Autoregressive token generation loop with UTF-8 byte accumulator
    const int max_tokens_to_gen = 768;
    int n_gen = 0;
    std::string byte_buffer = "";

    while (n_gen < max_tokens_to_gen && g_is_generating.load()) {
        llama_token new_token_id = llama_sampler_sample(g_sampler, g_context, -1);
        llama_sampler_accept(g_sampler, new_token_id);

        if (llama_vocab_is_eog(vocab, new_token_id)) {
            LOGI("Encountered EOG/EOS token. Finished generation.");
            break;
        }

        char buf[256];
        int n_chars = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, false);
        if (n_chars > 0) {
            byte_buffer.append(buf, n_chars);

            if (byte_buffer.find("<|im_end|>") != std::string::npos ||
                byte_buffer.find("<|eot_id|>") != std::string::npos ||
                byte_buffer.find("<|end_of_text|>") != std::string::npos) {
                break;
            }

            // Only emit to Android Java when buffer forms complete, valid UTF-8
            if (is_valid_utf8(byte_buffer)) {
                jstring tokenStr = env->NewStringUTF(byte_buffer.c_str());
                if (tokenStr != nullptr) {
                    env->CallVoidMethod(tokenCallback, onTokenMethod, tokenStr);
                    env->DeleteLocalRef(tokenStr);
                }
                byte_buffer.clear();
            }
        }

        llama_token next_token[1] = { new_token_id };
        struct llama_batch next_batch = llama_batch_get_one(next_token, 1);
        if (llama_decode(g_context, next_batch) != 0) {
            LOGE("llama_decode failed during token sampling loop!");
            break;
        }

        n_gen++;
    }

    // Flush any remaining valid bytes
    if (!byte_buffer.empty() && is_valid_utf8(byte_buffer)) {
        jstring tokenStr = env->NewStringUTF(byte_buffer.c_str());
        if (tokenStr != nullptr) {
            env->CallVoidMethod(tokenCallback, onTokenMethod, tokenStr);
            env->DeleteLocalRef(tokenStr);
        }
    }

    env->CallVoidMethod(tokenCallback, onCompleteMethod);
    g_is_generating.store(false);
    env->ReleaseStringUTFChars(prompt, promptText);
    LOGI("Inference complete. Generated %d tokens.", n_gen);
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
    LOGI("ThermalGuard thread count updated to: %d", threads);
    g_active_threads.store(threads);
    if (g_context != nullptr) {
        llama_set_n_threads(g_context, threads, threads);
    }
}

JNIEXPORT void JNICALL
Java_com_agent_s25_domain_engine_LlamaBridge_nativeUnloadModel(
        JNIEnv* /* env */,
        jobject /* this */) {
    LOGI("nativeUnloadModel requested.");
    free_resources();
    g_loaded_path = "";
    LOGI("Model unloaded from memory.");
}

JNIEXPORT jfloatArray JNICALL
Java_com_agent_s25_domain_engine_LlamaBridge_nativeCalculateEmbedding(
        JNIEnv* env,
        jobject /* this */,
        jstring text) {

    const char* str = env->GetStringUTFChars(text, nullptr);
    jfloatArray result = env->NewFloatArray(384);
    std::vector<float> vec(384, 0.05f);

    for (size_t i = 0; i < strlen(str) && i < 384; ++i) {
        vec[i] = static_cast<float>(str[i] % 100) / 100.0f;
    }

    env->SetFloatArrayRegion(result, 0, 384, vec.data());
    env->ReleaseStringUTFChars(text, str);
    return result;
}

}
