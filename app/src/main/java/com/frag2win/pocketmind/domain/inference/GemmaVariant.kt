package com.frag2win.pocketmind.domain.inference

/**
 * Supported Gemma 4 model variants based on the PRD.
 */
enum class GemmaVariant(
    val label: String,
    val modelSize: String,
    val ramRequired: String,
    val minFreeRamGb: Double
) {
    E4B("Gemma 4 E4B", "3.2 GB", "6 GB+", 6.0),
    E2B("Gemma 4 E2B", "1.5 GB", "3.5-4 GB", 3.5),
    E2B_INT4("Gemma 4 E2B INT4", "0.8 GB", "2.5 GB+", 2.5)
}
