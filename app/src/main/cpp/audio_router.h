#ifndef AUDIO_ROUTER_H
#define AUDIO_ROUTER_H

#include <vector>
#include <mutex>
#include <cstdint>
#include <functional>

enum class AudioDestination {
    WAKE_WORD,
    INDIC_STT,
    BOTH,
    MUTED
};

class AudioStreamRouter {
public:
    static AudioStreamRouter& getInstance();

    void setDestination(AudioDestination dest);
    AudioDestination getDestination() const;

    void pushAudioBuffer(const int16_t* samples, size_t length);
    void registerWakeWordCallback(std::function<void(const int16_t*, size_t)> cb);
    void registerSttCallback(std::function<void(const int16_t*, size_t)> cb);

private:
    AudioStreamRouter();
    mutable std::mutex mtx_;
    AudioDestination currentDest_ = AudioDestination::WAKE_WORD;
    std::function<void(const int16_t*, size_t)> wakeWordCallback_;
    std::function<void(const int16_t*, size_t)> sttCallback_;
};

#endif // AUDIO_ROUTER_H
