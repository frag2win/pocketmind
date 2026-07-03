package com.frag2win.pocketmind.domain.inference

/**
 * Supported Gemma 4 model variants based on the PRD.
 */
enum class GemmaVariant(
    val label: String,
    val modelSize: String,
    val ramRequired: String,
    val minFreeRamGb: Double,
    val downloadUrl: String
) {
    E4B(
        "Gemma 4 E4B", 
        "3.2 GB", 
        "6 GB+", 
        6.0,
        "https://huggingface.co/google/gemma-4-4b-it-litertlm/resolve/main/gemma4-4b-it.litertlm"
    ),
    E2B(
        "Gemma 4 E2B", 
        "1.5 GB", 
        "3.5-4 GB", 
        3.5,
        "https://huggingface.co/google/gemma-4-2b-it-litertlm/resolve/main/gemma4-2b-it.litertlm"
    ),
    E2B_INT4(
        "Gemma 4 E2B INT4", 
        "0.8 GB", 
        "2.5 GB+", 
        2.5,
        "https://huggingface.co/google/gemma-4-2b-it-litertlm/resolve/main/gemma4-2b-it-int4.litertlm"
    )
}
