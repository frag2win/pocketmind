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
    private val aiCoreInference: AICoreInference,
    private val qnnInference: QNNInference,
    private val neuronInference: NeuronInference,
    private val liteRTCPUInference: LiteRTCPUInference
) {
    /**
     * Detects hardware and returns the best available inference backend.
     */
    fun getInferenceImplementation(): PocketMindInference {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()

        return when {
            // Google Tensor detection
            hardware.contains("tensor") || hardware.contains("gs") -> aiCoreInference

            // Qualcomm Snapdragon detection
            hardware.contains("qcom") || board.contains("msm") || board.contains("sm") || board.contains("sdm") -> qnnInference

            // MediaTek Dimensity detection
            hardware.contains("mt") || board.contains("mt") -> neuronInference

            // Fallback for others
            else -> liteRTCPUInference
        }
    }
}
