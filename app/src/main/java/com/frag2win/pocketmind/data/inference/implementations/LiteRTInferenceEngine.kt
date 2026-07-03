package com.frag2win.pocketmind.data.inference.implementations

import android.content.Context
import android.os.Build
import com.frag2win.pocketmind.domain.inference.GemmaVariant
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRTInferenceEngine @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : PocketMindInference, Closeable {

    private var engine: Engine? = null
    private var isInitialized = false

    /**
     * Safe initialization with fallback for Play Asset Delivery.
     * @param variant The desired model variant.
     */
    suspend fun initializeSafe(variant: GemmaVariant) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        val modelPath = resolveModelPath(variant)
        
        try {
            val hardware = Build.HARDWARE.lowercase()
            val backend = when {
                hardware.contains("mt") || hardware.contains("dimensity") -> Backend.NPU()
                hardware.contains("qcom") || hardware.contains("sm") -> Backend.NPU()
                else -> Backend.CPU()
            }

            val config = EngineConfig(
                modelPath = modelPath,
                backend = backend
            )

            engine = Engine(config)
            engine?.initialize()
            isInitialized = true
        } catch (e: Exception) {
            // Fallback to CPU
            val fallbackConfig = EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU()
            )
            engine = Engine(fallbackConfig)
            engine?.initialize()
            isInitialized = true
        }
    }

    private fun resolveModelPath(variant: GemmaVariant): String {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val variantFile = File(modelsDir, variant.name.lowercase() + ".litertlm")
        
        return if (variantFile.exists()) {
            variantFile.absolutePath
        } else {
            // Fallback to E2B_INT4
            val fallbackFile = File(modelsDir, GemmaVariant.E2B_INT4.name.lowercase() + ".litertlm")
            check(fallbackFile.exists()) { 
                "Critical Error: Base model asset (E2B_INT4) is missing. Model path: ${fallbackFile.absolutePath}" 
            }
            fallbackFile.absolutePath
        }
    }

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        require(isInitialized) { "Engine not initialized" }
        val conversation = engine?.createConversation()
        var fullText = ""
        conversation?.sendMessageAsync(prompt)?.collect { message ->
            // Correctly extract text from the Message object in LiteRT-LM 2026
            fullText += (message.text ?: "")
        }
        return@withContext fullText
    }

    override suspend fun generateStream(prompt: String): Flow<String> {
        require(isInitialized) { "Engine not initialized" }
        val conversation = engine?.createConversation()
            ?: throw IllegalStateException("Failed to create conversation")
        
        // Correctly map the Message object stream to a String stream
        return conversation.sendMessageAsync(prompt).map { it.text ?: "" }
    }

    override fun isReady(): Boolean = isInitialized

    override fun close() {
        engine?.close()
        engine = null
        isInitialized = false
    }
}
