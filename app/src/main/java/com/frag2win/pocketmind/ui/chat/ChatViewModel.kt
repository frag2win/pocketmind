package com.frag2win.pocketmind.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frag2win.pocketmind.data.local.ChatDao
import com.frag2win.pocketmind.data.local.ChatMessage
import com.frag2win.pocketmind.domain.docs.PdfGenerator
import com.frag2win.pocketmind.domain.inference.GemmaPromptFormatter
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val inferenceEngine: PocketMindInference,
    private val chatDao: ChatDao,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingMessage = MutableStateFlow<String?>(null)
    val streamingMessage: StateFlow<String?> = _streamingMessage.asStateFlow()

    private val _pdfExportStatus = MutableStateFlow<File?>(null)
    val pdfExportStatus = _pdfExportStatus.asStateFlow()

    fun exportToPdf(message: ChatMessage) {
        viewModelScope.launch {
            val file = pdfGenerator.generateChatPdf(message.content)
            _pdfExportStatus.value = file
        }
    }

    fun clearPdfStatus() {
        _pdfExportStatus.value = null
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            
            // Add user message to DB
            chatDao.insertMessage(ChatMessage(role = "user", content = content))
            
            try {
                // Construct the full history including the newly added message
                val currentHistory = messages.value + ChatMessage(role = "user", content = content)
                val formattedPrompt = GemmaPromptFormatter.formatHistory(currentHistory)

                var fullResponse = ""
                val responseFlow = inferenceEngine.generateStream(formattedPrompt)
                responseFlow.collect { token ->
                    fullResponse += token
                    _streamingMessage.value = fullResponse
                }
                
                chatDao.insertMessage(ChatMessage(role = "assistant", content = fullResponse))
                _streamingMessage.value = null
            } catch (e: Exception) {
                chatDao.insertMessage(ChatMessage(role = "assistant", content = "Error: ${e.message}"))
                _streamingMessage.value = null
            } finally {
                _isGenerating.value = false
            }
        }
    }
}
