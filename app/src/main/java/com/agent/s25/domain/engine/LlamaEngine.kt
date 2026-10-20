package com.agent.s25.domain.engine

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LlamaEngine {
    companion object {
        private const val TAG = "LlamaEngine"
        private const val IDLE_TIMEOUT_MS = 3 * 60 * 1000L // 3 minutes
    }

    private var activeModelPath: String? = null
    private var idleJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Full Unicode GBNF grammar guaranteeing strict JSON with Gujarati and Hindi support
    val INDIC_JSON_GRAMMAR = """
        root   ::= object
        object ::= "{" ws ( pair ( "," ws pair )* )? "}"
        pair   ::= string ":" ws value
        value  ::= string | number | object | array | "true" | "false" | "null"
        string ::= "\"" ( [^"\\\u0000-\u001F] | "\\" ( ["\\/bfnrt] | "u" [0-9a-fA-F]{4} ) )* "\""
        number ::= "-"? [0-9]+ ( "." [0-9]+ )?
        array  ::= "[" ws ( value ( "," ws value )* )? "]"
        ws     ::= [ \t\n\r]*
    """.trimIndent()

    fun loadModel(path: String, nThreads: Int = 6, nGpuLayers: Int = 32): Boolean {
        cancelIdleTimer()
        if (activeModelPath == path) {
            return true
        }

        Log.i(TAG, "Swapping active model to: $path via UFS 4.0 mmap...")
        val success = LlamaBridge.nativeInitModel(path, nGpuLayers, nThreads, 4096)
        if (success) {
            activeModelPath = path
        }
        return success
    }

    fun generateStream(prompt: String, grammar: String? = null): Flow<String> = callbackFlow {
        cancelIdleTimer()

        val callback = object : TokenCallback {
            override fun onToken(token: String) {
                trySend(token)
            }

            override fun onComplete() {
                close()
                startIdleTimer()
            }
        }

        LlamaBridge.nativeGenerateStream(prompt, grammar, callback)

        awaitClose {
            LlamaBridge.nativeStopGeneration()
            startIdleTimer()
        }
    }

    private fun startIdleTimer() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(IDLE_TIMEOUT_MS)
            Log.i(TAG, "Idle timeout reached. Unloading model weights to free RAM.")
            LlamaBridge.nativeUnloadModel()
            activeModelPath = null
        }
    }

    private fun cancelIdleTimer() {
        idleJob?.cancel()
        idleJob = null
    }
}
