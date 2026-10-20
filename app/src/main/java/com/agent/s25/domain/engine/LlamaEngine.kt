package com.agent.s25.domain.engine

import android.util.Log
import com.agent.s25.S25AgentApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

class LlamaEngine {
    companion object {
        private const val TAG = "LlamaEngine"
        private const val IDLE_TIMEOUT_MS = 3 * 60 * 1000L // 3 minutes

        private val NO_MODEL_RESPONSE = """
નમસ્તે! 🙏 I'm your S25 Ultra Personal Agent.

⚠️ **No AI model loaded yet.**

To activate on-device inference, place a GGUF model file at:
`/sdcard/Download/s25-agent/models/`

For example, download a small model like:
• `Phi-3-mini-4k-instruct-q4.gguf` (~2.3 GB)
• `gemma-2-2b-it-Q4_K_M.gguf` (~1.6 GB)

Once a model file is placed there, restart the app and I'll be fully operational on your Snapdragon 8 Elite!

In the meantime, you can explore:
• 🔒 **Memory Vault** — encrypted on-device storage
• 🔧 **MCP Hub** — tool/server connections
• 📍 **Places** — geofence spatial intelligence
        """.trimIndent()
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
        string ::= "\"" ( [^"\\\\\\u0000-\\u001F] | "\\\\" ( ["\\\\/bfnrt] | "u" [0-9a-fA-F]{4} ) )* "\""
        number ::= "-"? [0-9]+ ( "." [0-9]+ )?
        array  ::= "[" ws ( value ( "," ws value )* )? "]"
        ws     ::= [ \t\n\r]*
    """.trimIndent()

    val isNativeAvailable: Boolean
        get() = S25AgentApplication.isNativeLoaded

    fun loadModel(path: String, nThreads: Int = 6, nGpuLayers: Int = 32): Boolean {
        if (!isNativeAvailable) {
            Log.w(TAG, "Native library not loaded. Cannot load model.")
            return false
        }

        cancelIdleTimer()
        if (activeModelPath == path) {
            return true
        }

        Log.i(TAG, "Swapping active model to: $path via UFS 4.0 mmap...")
        return try {
            val success = LlamaBridge.nativeInitModel(path, nGpuLayers, nThreads, 4096)
            if (success) {
                activeModelPath = path
            }
            success
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "JNI call failed: ${e.message}")
            false
        }
    }

    fun generateStream(prompt: String, grammar: String? = null): Flow<String> {
        if (!isNativeAvailable || activeModelPath == null) {
            // Return a helpful message when no model is loaded
            return flowOf(NO_MODEL_RESPONSE)
        }

        return callbackFlow {
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

            try {
                LlamaBridge.nativeGenerateStream(prompt, grammar, callback)
            } catch (e: UnsatisfiedLinkError) {
                trySend("[Error: Native engine unavailable]")
                close()
            }

            awaitClose {
                try {
                    LlamaBridge.nativeStopGeneration()
                } catch (_: UnsatisfiedLinkError) {}
                startIdleTimer()
            }
        }
    }

    private fun startIdleTimer() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(IDLE_TIMEOUT_MS)
            Log.i(TAG, "Idle timeout reached. Unloading model weights to free RAM.")
            try {
                LlamaBridge.nativeUnloadModel()
            } catch (_: UnsatisfiedLinkError) {}
            activeModelPath = null
        }
    }

    private fun cancelIdleTimer() {
        idleJob?.cancel()
        idleJob = null
    }
}
