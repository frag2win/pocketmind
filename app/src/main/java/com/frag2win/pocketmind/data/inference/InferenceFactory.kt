package com.frag2win.pocketmind.data.inference

import android.os.Build
import com.frag2win.pocketmind.data.inference.implementations.AICoreInference
import com.frag2win.pocketmind.data.inference.implementations.LiteRTCPUInference
import com.frag2win.pocketmind.data.inference.implementations.NeuronInference
import com.frag2win.pocketmind.data.inference.implementations.QNNInference
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory responsible for detecting the device chipset and providing the appropriate
 * PocketMindInference implementation.
 */
@Singleton
class InferenceFactory @Inject constructor(
    private val liteRTEngine: com.frag2win.pocketmind.data.inference.implementations.LiteRTInferenceEngine
) {
    /**
     * Detects hardware and returns the best available inference backend.
     * Note: LiteRTInferenceEngine now handles NPU/CPU delegation internally.
     */
    fun getInferenceImplementation(): PocketMindInference {
        return liteRTEngine
    }
}
