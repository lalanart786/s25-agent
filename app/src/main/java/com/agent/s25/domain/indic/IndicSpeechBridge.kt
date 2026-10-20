package com.agent.s25.domain.indic

object IndicSpeechBridge {
    external fun nativeInitRecognizer(modelDir: String): Boolean
    external fun nativeFeedAudioSamples(samples: ShortArray, length: Int)
    external fun nativeSetAudioRoute(routeCode: Int)

    fun feedAudioSamples(samples: ShortArray, length: Int) {
        nativeFeedAudioSamples(samples, length)
    }

    fun setAudioRoute(routeCode: Int) {
        nativeSetAudioRoute(routeCode)
    }
}
