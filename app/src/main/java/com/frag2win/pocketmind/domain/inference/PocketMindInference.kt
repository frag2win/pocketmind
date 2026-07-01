package com.frag2win.pocketmind.domain.inference

import kotlinx.coroutines.flow.Flow

/**
 * Unified interface for on-device LLM inference.
 * Implementations are chipset-specific (Tensor, Snapdragon, Dimensity, CPU).
 */
interface PocketMindInference {
    /**
     * Generates a full response for a given prompt.
     */
    suspend fun generate(prompt: String): String

    /**
     * Generates a streaming response for a given prompt.
     */
    suspend fun generateStream(prompt: String): Flow<String>

    /**
     * Checks if the inference engine and model are loaded and ready.
     */
    fun isReady(): Boolean
}
