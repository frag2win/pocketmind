package com.frag2win.pocketmind.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frag2win.pocketmind.data.local.ChatDao
import com.frag2win.pocketmind.data.local.ChatMessage
import com.frag2win.pocketmind.data.local.ModelPreferences
import com.frag2win.pocketmind.data.inference.implementations.LiteRTInferenceEngine
import com.frag2win.pocketmind.data.repository.SearchRepository
import com.frag2win.pocketmind.domain.docs.PdfGenerator
import com.frag2win.pocketmind.domain.inference.GemmaPromptFormatter
import com.frag2win.pocketmind.domain.inference.GemmaVariant
import com.frag2win.pocketmind.domain.inference.PocketMindInference
import com.frag2win.pocketmind.domain.inference.PromptBuilder
import com.frag2win.pocketmind.data.local.ChatSession
import com.frag2win.pocketmind.util.DocumentParser
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
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val inferenceEngine: PocketMindInference,
    private val chatDao: ChatDao,
    private val pdfGenerator: PdfGenerator,
    private val modelPreferences: ModelPreferences,
    private val searchRepository: SearchRepository,
    private val documentParser: DocumentParser
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

    private val _attachedFileUri = MutableStateFlow<Uri?>(null)
    val attachedFileUri = _attachedFileUri.asStateFlow()

    private val _attachedFileName = MutableStateFlow<String?>(null)
    val attachedFileName = _attachedFileName.asStateFlow()

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

    fun attachFile(uri: Uri, name: String) {
        _attachedFileUri.value = uri
        _attachedFileName.value = name
    }

    fun detachFile() {
        _attachedFileUri.value = null
        _attachedFileName.value = null
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

    private fun isSearchRequired(query: String): Boolean {
        val temporalKeywords = listOf(
            "today", "latest", "current", "news", "weather", 
            "2024", "2025", "now", "recent", "price", "stock",
            "market", "time", "date"
        )
        return temporalKeywords.any { query.contains(it, ignoreCase = true) }
    }

    fun sendMessage(content: String, context: android.content.Context? = null) {
        if (_attachedFileUri.value != null && context != null) {
            viewModelScope.launch {
                val fileUri = _attachedFileUri.value!!
                val fileName = _attachedFileName.value ?: "Document"
                _isGenerating.value = true
                _streamingMessage.value = "Extracting document text..."
                
                try {
                    // Strictly await extraction on IO thread
                    val extractedText = withContext(Dispatchers.IO) {
                        documentParser.extractTextFromPdf(context, fileUri)
                    }
                    
                    if (extractedText.isBlank()) {
                        _streamingMessage.value = "Failed to read PDF content."
                        _isGenerating.value = false
                        return@launch
                    }

                    val processedPrompt = PromptBuilder.buildPdfRAGPrompt(fileName, extractedText, content)
                    val internalMarker = "__PDF_ATTACHED_FILE__:$fileName"
                    val uiDisplay = "$internalMarker $content"
                    
                    detachFile() // Clear attachment after successful processing
                    _isGenerating.value = false // Reset before entering processMessage
                    processMessage(uiDisplay, processedPrompt, null, fileUri.toString())
                } catch (e: Exception) {
                    _streamingMessage.value = "Error parsing PDF: ${e.localizedMessage}"
                    _isGenerating.value = false
                }
            }
        } else {
            processMessage(content, content)
        }
    }

    fun sendGitHubMessage(uiDisplay: String, actualPrompt: String) {
        processMessage(uiDisplay, actualPrompt)
    }

    private fun processMessage(uiDisplay: String, actualPrompt: String, pdfContext: String? = null, fileUri: String? = null) {
        if (uiDisplay.isBlank() || _isGenerating.value) return

        viewModelScope.launch {
            val isFirstTurn = messages.value.isEmpty()
            val sessionId = _currentSessionId.value ?: run {
                val id = chatDao.createSession(ChatSession(title = "New Chat"))
                _currentSessionId.value = id.toInt()
                id.toInt()
            }

            _isGenerating.value = true
            
            var processedContent = actualPrompt
            
            // Handle PDF Context injection
            if (pdfContext != null) {
                processedContent = """
                    SYSTEM: You are a document analysis assistant. Use the text below to answer.
                    
                    EXTRACTED TEXT:
                    $pdfContext
                    
                    USER QUESTION:
                    $actualPrompt
                """.trimIndent()
            } else {
                val requiresSearch = isSearchRequired(actualPrompt)
                if (requiresSearch) {
                    _streamingMessage.value = "Searching the web..."
                    try {
                        val searchResults = searchRepository.performWebSearch(actualPrompt)
                        if (searchResults.isNotEmpty()) {
                            processedContent = PromptBuilder.buildRAGPrompt(actualPrompt, searchResults)
                            _streamingMessage.value = "Analyzing web results..."
                        } else {
                            _streamingMessage.value = "No relevant web results found. Falling back to local knowledge..."
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _streamingMessage.value = "Web search failed. Falling back to local knowledge..."
                    }
                }
            }

            // Add user message to DB with the FULL processed content for the model
            // and the UI display string for the user.
            chatDao.insertMessage(
                ChatMessage(
                    sessionId = sessionId, 
                    role = "user", 
                    content = processedContent,
                    displayContent = uiDisplay,
                    fileUri = fileUri
                )
            )
            
            // Update session title with temporary snippet if it was the first message
            if (isFirstTurn) {
                val displayTitle = if (uiDisplay.startsWith("__PDF_ATTACHED_FILE__:")) {
                    uiDisplay.substringAfter(":").substringAfter(" ").take(30)
                } else {
                    uiDisplay.take(30)
                }
                chatDao.updateSessionTitle(sessionId, "$displayTitle...")
            }
            
            try {
                // Architectural Patch: Ensure model is initialized before first inference
                if (!inferenceEngine.isReady()) {
                    _isModelLoading.value = true
                    try {
                        val selectedVariantName = modelPreferences.getSelectedVariant()
                        val variant = if (selectedVariantName != null) {
                            GemmaVariant.valueOf(selectedVariantName)
                        } else {
                            GemmaVariant.E2B // Default
                        }
                        
                        if (inferenceEngine is LiteRTInferenceEngine) {
                            inferenceEngine.initializeSafe(variant)
                        }
                    } finally {
                        _isModelLoading.value = false
                    }
                }

                // Construct the full history including the newly added user message (retrieved from DB/state)
                val currentHistory = messages.value.filter { it.sessionId == sessionId }
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
                    generateSessionTitle(sessionId, uiDisplay, fullResponse)
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
                val cleanUserMsg = if (userMsg.startsWith("__PDF_ATTACHED_FILE__:")) {
                    userMsg.substringAfter(":").substringAfter(" ")
                } else userMsg
                val prompt = GemmaPromptFormatter.formatSummaryPrompt(cleanUserMsg, assistantMsg)
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
