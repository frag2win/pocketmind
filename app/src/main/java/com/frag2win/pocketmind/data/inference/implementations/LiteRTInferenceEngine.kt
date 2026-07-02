package com.frag2win.pocketmind.data.inference.implementations

import android.content.Context
import android.os.Build
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import com.google.ai.edge.litert.genai.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRTInferenceEngine @Inject constructor(
    private val context: Context
) : PocketMindInference, Closeable {

    private var llmInference: LlmInference? = null
    private var isInitialized = false

    suspend fun initialize(modelPath: String) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        
        try {
            // 1. Attempt NPU Delegation based on hardware
            val hardware = Build.HARDWARE.lowercase()
            val delegate = when {
                hardware.contains("mt") || hardware.contains("dimensity") -> LlmInference.Delegate.NPU
                hardware.contains("qcom") || hardware.contains("sm") -> LlmInference.Delegate.NPU
                else -> LlmInference.Delegate.CPU
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTemperature(0.7f)
                .setTopK(40)
                .setDelegate(delegate)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isInitialized = true
        } catch (e: Exception) {
            // Fallback to CPU if NPU fails
            val fallbackOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setDelegate(LlmInference.Delegate.CPU)
                .build()
            
            llmInference = LlmInference.createFromOptions(context, fallbackOptions)
            isInitialized = true
        }
    }

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        require(isInitialized) { "Engine not initialized" }
        // The prompt provided here must already be formatted with GemmaPromptFormatter
        return@withContext llmInference?.generateResponse(prompt) ?: ""
    }

    override suspend fun generateStream(prompt: String): Flow<String> = callbackFlow {
        require(isInitialized) { "Engine not initialized" }
        
        val llm = llmInference ?: return@callbackFlow
        
        // Asynchronous generation using the library's async API to populate the Flow
        llm.generateResponseAsync(prompt) { partialResult, done ->
            if (partialResult != null) {
                trySend(partialResult)
            }
            if (done) {
                close()
            }
        }
        
        awaitClose {
            // Note: Native C++ calls can't be easily cancelled mid-stream without explicit API support
        }
    }.flowOn(Dispatchers.IO)

    override fun isReady(): Boolean = isInitialized

    override fun close() {
        // Clear KV Cache and free native memory
        llmInference?.close()
        llmInference = null
        isInitialized = false
    }
}
