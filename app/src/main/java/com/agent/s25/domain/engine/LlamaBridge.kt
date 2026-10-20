package com.agent.s25.domain.engine

interface TokenCallback {
    fun onToken(token: String)
    fun onComplete()
}

object LlamaBridge {
    external fun nativeInitModel(
        modelPath: String,
        nGpuLayers: Int,
        nThreads: Int,
        contextSize: Int
    ): Boolean

    external fun nativeGenerateStream(
        prompt: String,
        grammarUtf8: String?,
        callback: TokenCallback
    )

    external fun nativeStopGeneration()
    external fun nativeSetThreadCount(threads: Int)
    external fun nativeUnloadModel()
    external fun nativeCalculateEmbedding(text: String): FloatArray
}
