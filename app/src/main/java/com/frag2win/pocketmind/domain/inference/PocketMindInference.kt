package com.frag2win.pocketmind.domain.inference

import com.frag2win.pocketmind.data.local.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Unified interface for on-device LLM inference.
 * Implementations are chipset-specific (Tensor, Snapdragon, Dimensity, CPU).
 */
interface PocketMindInference {
    /**
     * Generates a full single-shot response for a given prompt string.
     */
    suspend fun generate(prompt: String): String

    /**
     * Generates a streaming response for a new user message turn.
     * @param userMessage The new raw user query.
     * @param history The prior chat messages in the session (excluding the new user query).
     * @param displayName The user's display name for personalization context.
     */
    suspend fun generateStream(
        userMessage: String,
        history: List<ChatMessage> = emptyList(),
        displayName: String = "User"
    ): Flow<String>

    /**
     * Resets any active conversation/session state held in memory by the engine.
     */
    fun resetSession()

    /**
     * Checks if the inference engine and model are loaded and ready.
     */
    fun isReady(): Boolean
}
