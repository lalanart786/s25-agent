#include "audio_router.h"
#include <android/log.h>

#define TAG "AudioRouter"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

AudioStreamRouter::AudioStreamRouter() = default;

AudioStreamRouter& AudioStreamRouter::getInstance() {
    static AudioStreamRouter instance;
    return instance;
}

void AudioStreamRouter::setDestination(AudioDestination dest) {
    std::lock_guard<std::mutex> lock(mtx_);
    currentDest_ = dest;
    LOGI("Audio destination switched to %d", static_cast<int>(dest));
}

AudioDestination AudioStreamRouter::getDestination() const {
    std::lock_guard<std::mutex> lock(mtx_);
    return currentDest_;
}

void AudioStreamRouter::pushAudioBuffer(const int16_t* samples, size_t length) {
    std::lock_guard<std::mutex> lock(mtx_);
    if (currentDest_ == AudioDestination::MUTED) return;

    if ((currentDest_ == AudioDestination::WAKE_WORD || currentDest_ == AudioDestination::BOTH) && wakeWordCallback_) {
        wakeWordCallback_(samples, length);
    }

    if ((currentDest_ == AudioDestination::INDIC_STT || currentDest_ == AudioDestination::BOTH) && sttCallback_) {
        sttCallback_(samples, length);
    }
}

void AudioStreamRouter::registerWakeWordCallback(std::function<void(const int16_t*, size_t)> cb) {
    std::lock_guard<std::mutex> lock(mtx_);
    wakeWordCallback_ = cb;
}

void AudioStreamRouter::registerSttCallback(std::function<void(const int16_t*, size_t)> cb) {
    std::lock_guard<std::mutex> lock(mtx_);
    sttCallback_ = cb;
}
