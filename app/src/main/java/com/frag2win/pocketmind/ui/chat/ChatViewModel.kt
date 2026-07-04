package com.frag2win.pocketmind.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frag2win.pocketmind.data.local.ChatDao
import com.frag2win.pocketmind.data.local.ChatMessage
import com.frag2win.pocketmind.data.local.ModelPreferences
import com.frag2win.pocketmind.data.inference.implementations.LiteRTInferenceEngine
import com.frag2win.pocketmind.domain.docs.PdfGenerator
import com.frag2win.pocketmind.domain.inference.GemmaPromptFormatter
import com.frag2win.pocketmind.domain.inference.GemmaVariant
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import com.frag2win.pocketmind.data.local.ChatSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val inferenceEngine: PocketMindInference,
    private val chatDao: ChatDao,
    private val pdfGenerator: PdfGenerator,
    private val modelPreferences: ModelPreferences
) : ViewModel() {

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSession>> = chatDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val messages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList())
            else chatDao.getMessagesForSession(sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isModelLoading = MutableStateFlow(false)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading.asStateFlow()

    private val _streamingMessage = MutableStateFlow<String?>(null)
    val streamingMessage: StateFlow<String?> = _streamingMessage.asStateFlow()

    private val _pdfExportStatus = MutableStateFlow<Uri?>(null)
    val pdfExportStatus = _pdfExportStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val searchResults: StateFlow<List<ChatMessage>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else chatDao.searchMessages("*$query*")
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Automatically start a session if none exists
        viewModelScope.launch {
            chatDao.getAllSessions().collect { sessionList ->
                if (sessionList.isEmpty()) {
                    startNewChat()
                } else if (_currentSessionId.value == null) {
                    _currentSessionId.value = sessionList.first().id
                }
            }
        }
    }

    fun exportToPdf(message: ChatMessage) {
        viewModelScope.launch {
            val uri = pdfGenerator.generateChatPdf(message.content)
            _pdfExportStatus.value = uri
        }
    }

    fun clearPdfStatus() {
        _pdfExportStatus.value = null
    }

    fun clearChat() {
        viewModelScope.launch {
            _currentSessionId.value?.let { 
                chatDao.deleteMessagesForSession(it)
            }
        }
    }

    fun startNewChat() {
        viewModelScope.launch {
            // Check if current session is empty
            val currentMessages = messages.value
            if (currentMessages.isEmpty() && _currentSessionId.value != null) {
                // Already in an empty session, just stay here
                return@launch
            }

            val newSessionId = chatDao.createSession(ChatSession(title = "New Chat"))
            _currentSessionId.value = newSessionId.toInt()
        }
    }

    fun selectSession(sessionId: Int) {
        _currentSessionId.value = sessionId
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            chatDao.deleteMessagesForSession(sessionId)
            chatDao.deleteSession(sessionId)
            
            // If we deleted the current session, switch to another one or create new
            if (_currentSessionId.value == sessionId) {
                val remainingSessions = sessions.value
                if (remainingSessions.isNotEmpty()) {
                    _currentSessionId.value = remainingSessions.first().id
                } else {
                    startNewChat()
                }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isGenerating.value) return

        viewModelScope.launch {
            val isFirstTurn = messages.value.isEmpty()
            val sessionId = _currentSessionId.value ?: run {
                val id = chatDao.createSession(ChatSession(title = "New Chat"))
                _currentSessionId.value = id.toInt()
                id.toInt()
            }

            _isGenerating.value = true
            
            // Add user message to DB
            chatDao.insertMessage(ChatMessage(sessionId = sessionId, role = "user", content = content))
            
            // Update session title with temporary snippet if it was the first message
            if (isFirstTurn) {
                chatDao.updateSessionTitle(sessionId, content.take(30) + "...")
            }
            
            try {
                // Architectural Patch: Ensure model is initialized before first inference
                if (!inferenceEngine.isReady() && inferenceEngine is LiteRTInferenceEngine) {
                    _isModelLoading.value = true
                    val selectedVariantName = modelPreferences.getSelectedVariant()
                    val variant = if (selectedVariantName != null) {
                        GemmaVariant.valueOf(selectedVariantName)
                    } else {
                        GemmaVariant.E2B // Default
                    }
                    inferenceEngine.initializeSafe(variant)
                    _isModelLoading.value = false
                }

                // Construct the full history INCLUDING the newly added user message
                val currentHistory = messages.value + ChatMessage(sessionId = sessionId, role = "user", content = content)
                val formattedPrompt = GemmaPromptFormatter.formatHistory(currentHistory)

                // Inject temporary "Thinking..." state
                _streamingMessage.value = "Thinking..."

                var fullResponse = ""
                var isFirstToken = true
                val responseFlow = inferenceEngine.generateStream(formattedPrompt)
                responseFlow.collect { token ->
                    if (isFirstToken) {
                        _streamingMessage.value = "" // Clear "Thinking..."
                        isFirstToken = false
                    }
                    fullResponse += token
                    _streamingMessage.value = fullResponse
                }
                
                chatDao.insertMessage(ChatMessage(sessionId = sessionId, role = "assistant", content = fullResponse))
                _streamingMessage.value = null

                if (isFirstTurn) {
                    generateSessionTitle(sessionId, content, fullResponse)
                }
            } catch (e: Exception) {
                _isModelLoading.value = false
                chatDao.insertMessage(ChatMessage(sessionId = sessionId, role = "assistant", content = "Error: ${e.message}"))
                _streamingMessage.value = null
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun generateSessionTitle(sessionId: Int, userMsg: String, assistantMsg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = GemmaPromptFormatter.formatSummaryPrompt(userMsg, assistantMsg)
                val rawTitle = inferenceEngine.generate(prompt)
                val cleanTitle = rawTitle
                    .replace("\"", "")
                    .replace("'", "")
                    .replace("*", "")
                    .trim()
                    .take(50)
                
                if (cleanTitle.isNotBlank()) {
                    chatDao.updateSessionTitle(sessionId, cleanTitle)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
