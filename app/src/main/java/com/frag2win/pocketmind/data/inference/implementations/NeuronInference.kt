package com.frag2win.pocketmind.data.inference.implementations

import com.frag2win.pocketmind.data.local.ChatMessage
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Implementation for MediaTek Dimensity chipsets using LiteRT + NeuroPilot Delegate.
 */
class NeuronInference @Inject constructor() : PocketMindInference {
    override suspend fun generate(prompt: String): String {
        return "Response from Neuron (Dimensity)"
    }

    override suspend fun generateStream(
        userMessage: String,
        history: List<ChatMessage>,
        displayName: String
    ): Flow<String> = flow {
        emit("Response ")
        emit("from ")
        emit("Neuron ")
        emit("(Dimensity)")
    }

    override fun resetSession() {}

    override fun isReady(): Boolean {
        return true
    }
}
