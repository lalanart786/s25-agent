package com.agent.s25.domain.engine

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.agent.s25.S25AgentApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.io.FileOutputStream

class LlamaEngine {
    companion object {
        private const val TAG = "LlamaEngine"
        private const val IDLE_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes

        private val NO_MODEL_RESPONSE = """
નમસ્તે! 🙏 I'm Jerry — your My Agent on-device intelligence.

⚠️ **No GGUF model loaded yet.**

Please place your downloaded model file in:
`Internal Storage > Download > s25-agent > models/`

Or tap **"📂 Select"** at the top to pick your `.gguf` file directly!
        """.trimIndent()
    }

    private var activeModelPath: String? = null
    private var idleJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _loadedModelName = MutableStateFlow<String?>(null)
    val loadedModelName = _loadedModelName.asStateFlow()

    val isNativeAvailable: Boolean
        get() = S25AgentApplication.isNativeLoaded

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

    fun autoScanAndLoad(context: Context): String? {
        val searchDirs = listOfNotNull(
            File("/sdcard/Download/s25-agent/models"),
            File("/sdcard/Download/s25-agent"),
            File("/sdcard/Download"),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.resolve("s25-agent/models"),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.resolve("s25-agent"),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(null)?.resolve("models"),
            context.filesDir.resolve("models")
        )

        for (dir in searchDirs) {
            try {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles() ?: continue
                    val ggufFile = files.firstOrNull { it.isFile && it.name.endsWith(".gguf", ignoreCase = true) }
                    if (ggufFile != null) {
                        Log.i(TAG, "Found GGUF model: ${ggufFile.absolutePath}")
                        val success = loadModel(ggufFile.absolutePath)
                        if (success) {
                            _loadedModelName.value = ggufFile.name
                            return ggufFile.name
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Directory scan failed for ${dir.path}: ${e.message}")
            }
        }
        return null
    }

    fun loadModelFromUri(context: Context, uri: Uri): Boolean {
        try {
            val contentResolver = context.contentResolver
            val fileName = queryFileName(context, uri) ?: "imported_model.gguf"
            val targetDir = File(context.filesDir, "models").apply { mkdirs() }
            val destFile = File(targetDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.i(TAG, "Model copied to sandbox: ${destFile.absolutePath}")
            val success = loadModel(destFile.absolutePath)
            if (success) {
                _loadedModelName.value = destFile.name
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model from URI: ${e.message}")
            return false
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) name = cursor.getString(idx)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }

    fun loadModel(path: String, nThreads: Int = 6, nGpuLayers: Int = 32): Boolean {
        cancelIdleTimer()
        if (activeModelPath == path) {
            return true
        }

        Log.i(TAG, "Swapping active model to: $path via Snapdragon 8 Elite UFS 4.0...")
        val success = if (isNativeAvailable) {
            try {
                LlamaBridge.nativeInitModel(path, nGpuLayers, nThreads, 4096)
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "JNI init failed: ${e.message}")
                false
            } catch (t: Throwable) {
                Log.e(TAG, "Native init error: ${t.message}")
                false
            }
        } else {
            Log.e(TAG, "Native engine is not available. Error: ${S25AgentApplication.nativeLoadError}")
            false
        }

        if (success) {
            activeModelPath = path
            _loadedModelName.value = File(path).name
        }
        return success
    }

    fun generateStream(prompt: String, grammar: String? = null): Flow<String> {
        if (activeModelPath == null) {
            return flowOf(NO_MODEL_RESPONSE)
        }

        if (!isNativeAvailable) {
            val errMsg = "⚠️ Native llama.cpp engine could not be loaded.\n\n" +
                    "Diagnostics: ${S25AgentApplication.nativeLoadError ?: "Native libraries missing or failed to initialize."}\n\n" +
                    "Please ensure the device supports 64-bit ARM and the model file is not corrupted."
            return flowOf(errMsg)
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
                trySend("[Error: Native engine call failed]")
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
