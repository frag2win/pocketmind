package com.frag2win.pocketmind.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frag2win.pocketmind.data.local.ChatDao
import com.frag2win.pocketmind.data.local.ChatMessage
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val inferenceEngine: PocketMindInference,
    private val chatDao: ChatDao
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingMessage = MutableStateFlow<String?>(null)
    val streamingMessage: StateFlow<String?> = _streamingMessage.asStateFlow()

    fun sendMessage(content: String) {
        if (content.isBlank() || _isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            
            // Add user message to DB
            chatDao.insertMessage(ChatMessage(role = "user", content = content))
            
            try {
                var fullResponse = ""
                val responseFlow = inferenceEngine.generateStream(content)
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
