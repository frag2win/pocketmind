package com.frag2win.pocketmind.ui.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frag2win.pocketmind.data.inference.implementations.LiteRTInferenceEngine
import com.frag2win.pocketmind.data.local.ModelPreferences
import com.frag2win.pocketmind.domain.inference.GemmaVariant
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import com.frag2win.pocketmind.domain.inference.PromptBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State representing the interactive AI Canvas bridge.
 */
sealed interface CanvasUiState {
    data object Idle : CanvasUiState
    data object Generating : CanvasUiState
    data class Ready(val rawJsonPayload: String) : CanvasUiState
    data class Error(val message: String) : CanvasUiState
}

@HiltViewModel
class CanvasViewModel @Inject constructor(
    private val inferenceEngine: PocketMindInference,
    private val modelPreferences: ModelPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<CanvasUiState>(CanvasUiState.Idle)
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    private val _streamingRawPayload = MutableStateFlow("")
    val streamingRawPayload: StateFlow<String> = _streamingRawPayload.asStateFlow()

    /**
     * Intercepts and cleans model output to isolate valid JSON strings.
     * Strips Markdown block delimiters (` ```json ` ... ` ``` `) if present.
     */
    fun sanitizeJsonPayload(rawOutput: String): String {
        var clean = rawOutput.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substringAfter("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.substringBeforeLast("```")
        }
        return clean.trim()
    }

    /**
     * Determines whether an incoming message payload is structured JSON intended for the Canvas.
     */
    fun isCanvasPayload(content: String): Boolean {
        val clean = sanitizeJsonPayload(content)
        return (clean.startsWith("{") && clean.endsWith("}")) &&
                (clean.contains("\"slides\"") || clean.contains("\"bullets\"") || clean.contains("\"type\""))
    }

    /**
     * Triggers Gemma generation with the strict Canvas JSON system prompt.
     */
    fun generateCanvas(userPrompt: String, canvasType: String = "presentation") {
        if (userPrompt.isBlank()) return

        viewModelScope.launch {
            _uiState.value = CanvasUiState.Generating
            _streamingRawPayload.value = ""

            try {
                if (!inferenceEngine.isReady()) {
                    val variantName = modelPreferences.getSelectedVariant()
                    val variant = variantName?.let { GemmaVariant.valueOf(it) } ?: GemmaVariant.E2B
                    if (inferenceEngine is LiteRTInferenceEngine) {
                        inferenceEngine.initializeSafe(variant)
                    }
                }

                val prompt = PromptBuilder.buildCanvasPrompt(userPrompt, canvasType)
                var accumulated = ""

                inferenceEngine.generateStream(
                    userMessage = prompt,
                    displayName = modelPreferences.getDisplayName()
                ).collect { token ->
                    accumulated += token
                    _streamingRawPayload.value = accumulated
                }

                val sanitizedJson = sanitizeJsonPayload(accumulated)
                if (sanitizedJson.startsWith("{") || sanitizedJson.startsWith("[")) {
                    _uiState.value = CanvasUiState.Ready(sanitizedJson)
                } else {
                    _uiState.value = CanvasUiState.Error("Output did not conform to expected JSON schema.")
                }
            } catch (e: Exception) {
                _uiState.value = CanvasUiState.Error("Canvas generation error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Direct injection method for intercepted payload routing from ChatViewModel.
     */
    fun setCanvasPayload(rawJson: String) {
        val sanitized = sanitizeJsonPayload(rawJson)
        _uiState.value = CanvasUiState.Ready(sanitized)
    }

    fun resetCanvas() {
        _uiState.value = CanvasUiState.Idle
        _streamingRawPayload.value = ""
    }
}
