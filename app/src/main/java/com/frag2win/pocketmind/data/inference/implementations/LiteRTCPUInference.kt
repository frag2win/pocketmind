package com.frag2win.pocketmind.data.inference.implementations

import com.frag2win.pocketmind.domain.inference.PocketMindInference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Fallback implementation using LiteRT on CPU.
 */
class LiteRTCPUInference @Inject constructor() : PocketMindInference {
    override suspend fun generate(prompt: String): String {
        // TODO: Implement LiteRT CPU inference
        return "Response from LiteRT CPU (Fallback)"
    }

    override suspend fun generateStream(prompt: String): Flow<String> = flow {
        // TODO: Implement LiteRT streaming
        emit("Response ")
        emit("from ")
        emit("LiteRT ")
        emit("CPU ")
        emit("(Fallback)")
    }

    override fun isReady(): Boolean {
        // TODO: Check if model is loaded on CPU
        return true
    }
}
