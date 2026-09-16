package com.frag2win.pocketmind.data.inference.implementations

import com.frag2win.pocketmind.data.local.ChatMessage
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Implementation for Google Tensor chipsets using Google AICore / ML Kit GenAI.
 */
class AICoreInference @Inject constructor() : PocketMindInference {
    override suspend fun generate(prompt: String): String {
        return "Response from AICore (Tensor)"
    }

    override suspend fun generateStream(
        userMessage: String,
        history: List<ChatMessage>,
        displayName: String
    ): Flow<String> = flow {
        emit("Response ")
        emit("from ")
        emit("AICore ")
        emit("(Tensor)")
    }

    override fun resetSession() {}

    override fun isReady(): Boolean {
        return true
    }
}
